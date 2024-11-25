import logging

import mysql.connector
from datetime import datetime, timedelta
import random
from typing import List, Dict, Tuple
import re
from faker import Faker

# Initialize Faker
fake = Faker()

# Database configuration
db_config = {
    'host': 'localhost',
    'user': 'Huyvictory',
    'password': '12345',
    'database': 'onlinecoursemanagement'
}


def parse_datetime(dt_str: str) -> datetime:
    """Parse datetime string to datetime object, handling microseconds."""
    if dt_str and dt_str.lower() != 'null':
        try:
            # Try parsing with microseconds
            return datetime.strptime(dt_str.strip(), '%Y-%m-%d %H:%M:%S.%f')
        except ValueError:
            # If no microseconds, try without
            try:
                return datetime.strptime(dt_str.strip(), '%Y-%m-%d %H:%M:%S')
            except ValueError:
                print(f"Error parsing datetime: {dt_str}")
                return None
    return None


def parse_data_file(file_content: str, is_user: bool = True) -> List[Dict]:
    """Parse the table-formatted data file content."""
    # Split into lines and remove header/footer
    lines = [line for line in file_content.strip().split('\n')
             if line.startswith('|') and not line.startswith('+-')]
    lines = lines[1:]  # Skip header

    data = []
    for line in lines:
        # Split the line by | and clean up whitespace
        fields = [field.strip() for field in line.split('|')[1:-1]]

        print(fields)

        if is_user:
            try:
                data.append({
                    'id': int(fields[0]),
                    'created_at': parse_datetime(fields[1]),
                    'deleted_at': parse_datetime(fields[2]),
                    'status': fields[8],
                })
            except Exception as e:
                print(f"Error parsing user line: {fields}")
                print(f"Error: {e}")
                continue
        else:
            try:
                data.append({
                    'id': int(fields[0]),
                    'created_at': parse_datetime(fields[5]),
                    'deleted_at': parse_datetime(fields[7]),
                    'status': fields[4],
                })
            except Exception as e:
                print(f"Error parsing course line: {fields}")
                print(f"Error: {e}")
                continue

    return data


def get_published_courses(courses_data: List[Dict]) -> List[int]:
    """Get list of published and non-archived course IDs."""
    return [course['id'] for course in courses_data
            if course['status'].strip() == 'PUBLISHED' and not course['deleted_at']]


def get_active_users(users_data: List[Dict]) -> List[int]:
    """Get list of active and non-deleted user IDs."""

    print("Getting active users..." + str(users_data[0].keys()) + str(users_data[0].values()))

    return [user['id'] for user in users_data
            if user['status'].strip() == 'ACTIVE' and not user['deleted_at']]


def generate_enrollment_date(course_created_at: datetime, user_created_at: datetime) -> datetime:
    """Generate a random enrollment date after both course and user creation."""
    start_date = max(course_created_at, user_created_at)
    end_date = datetime.now()
    if start_date >= end_date:
        start_date = end_date - timedelta(days=1)
    return fake.date_time_between(start_date=start_date, end_date=end_date)


def determine_completion_status(enrollment_date: datetime) -> Tuple[str, datetime]:
    """Determine course completion status based on enrollment duration."""
    now = datetime.now()
    enrollment_duration = (now - enrollment_date).days

    if enrollment_duration < 7:  # Recent enrollments
        status = random.choices(['ENROLLED', 'IN_PROGRESS'], weights=[80, 20])[0]
        completion_date = None
    elif enrollment_duration < 30:  # Within a month
        status = random.choices(['ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED'],
                                weights=[20, 50, 20, 10])[0]
        completion_date = fake.date_time_between(start_date=enrollment_date,
                                                 end_date=now) if status == 'COMPLETED' else None
    else:  # Older enrollments
        status = random.choices(['IN_PROGRESS', 'COMPLETED', 'DROPPED'],
                                weights=[30, 60, 10])[0]
        completion_date = fake.date_time_between(start_date=enrollment_date + timedelta(days=30),
                                                 end_date=now) if status == 'COMPLETED' else None

    return status, completion_date


def generate_enrollments(users_data: List[Dict], courses_data: List[Dict]) -> List[Dict]:
    """Generate enrollment data."""
    enrollments = []
    active_users = get_active_users(users_data)
    published_courses = get_published_courses(courses_data)

    if not active_users:
        raise ValueError("No active users found")
    if not published_courses:
        raise ValueError("No published courses found")

    print(f"Found {len(active_users)} active users and {len(published_courses)} published courses")

    # Get user and course creation dates
    user_dates = {user['id']: user['created_at'] for user in users_data if user['id'] in active_users}
    course_dates = {course['id']: course['created_at'] for course in courses_data if course['id'] in published_courses}

    # Track user-course pairs to avoid duplicates
    user_course_pairs = set()

    for user_id in active_users:
        # Each user enrolls in 2-8 courses
        num_enrollments = random.randint(2, min(8, len(published_courses)))
        selected_courses = random.sample(published_courses, num_enrollments)

        for course_id in selected_courses:
            if (user_id, course_id) in user_course_pairs:
                continue

            user_course_pairs.add((user_id, course_id))

            try:
                enrollment_date = generate_enrollment_date(
                    course_dates[course_id],
                    user_dates[user_id]
                )

                status, completion_date = determine_completion_status(enrollment_date)

                enrollments.append({
                    'user_id': user_id,
                    'course_id': course_id,
                    'enrollment_date': enrollment_date,
                    'completion_date': completion_date,
                    'status': status
                })
            except Exception as e:
                print(f"Error generating enrollment for user {user_id}, course {course_id}: {e}")
                continue

    return enrollments


def insert_enrollments(enrollments: List[Dict]):
    """Insert enrollments into database."""
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Clear existing data
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        cursor.execute("TRUNCATE TABLE user_courses")
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

        insert_sql = """
        INSERT INTO user_courses (user_id, course_id, enrollment_date, completion_date, status)
        VALUES (%s, %s, %s, %s, %s)
        """

        for enrollment in enrollments:
            cursor.execute(insert_sql, (
                enrollment['user_id'],
                enrollment['course_id'],
                enrollment['enrollment_date'],
                enrollment['completion_date'],
                enrollment['status']
            ))

        conn.commit()

        # Print statistics
        cursor.execute("""
            SELECT status, COUNT(*) 
            FROM user_courses 
            GROUP BY status
        """)
        stats = cursor.fetchall()

        print("\nEnrollment Statistics:")
        total = sum(count for _, count in stats)
        for status, count in stats:
            percentage = (count / total) * 100
            print(f"{status}: {count} ({percentage:.1f}%)")

        print(f"\nTotal Enrollments: {total}")
        cursor.execute("SELECT COUNT(DISTINCT user_id) FROM user_courses")
        user_count = cursor.fetchone()[0]
        print(f"Total Users Enrolled: {user_count}")
        print(f"Average Enrollments per User: {total / user_count:.1f}")

    except mysql.connector.Error as err:
        print(f"Error: {err}")
        conn.rollback()
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()
            print("\nDatabase connection closed")


def main():
    print("Starting enrollment data generation...")

    try:
        # Read and parse the data files
        with open('users.txt', 'r') as f:
            users_data = parse_data_file(f.read(), is_user=True)

        with open('courses.txt', 'r') as f:
            courses_data = parse_data_file(f.read(), is_user=False)

        print(f"Loaded {len(users_data)} users and {len(courses_data)} courses")

        # Generate enrollments
        enrollments = generate_enrollments(users_data, courses_data)
        print(f"Generated {len(enrollments)} enrollments")

        # Insert into database
        insert_enrollments(enrollments)

    except Exception as e:
        print(f"An error occurred: {str(e)}")


if __name__ == "__main__":
    main()
