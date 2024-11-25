import mysql.connector
from faker import Faker
import random
from datetime import datetime, timedelta
from typing import Dict, List, Set, Tuple
import time

fake = Faker()

# Database configuration
db_config = {
    'host': 'localhost',
    'user': 'Huyvictory',
    'password': '12345',
    'database': 'onlinecoursemanagement'
}

def connect_to_database():
    return mysql.connector.connect(**db_config)

def get_lessons_for_course(course_id: int) -> List[Dict]:
    """Get all valid (non-deleted) lessons for a course with their required info"""
    conn = connect_to_database()
    cursor = conn.cursor(dictionary=True)

    try:
        cursor.execute("""
            SELECT DISTINCT
                l.id AS lesson_id,
                l.chapter_id,
                ch.course_id,
                l.order_number AS lesson_order,
                ch.order_number AS chapter_order
            FROM lessons l
            INNER JOIN chapters ch ON l.chapter_id = ch.id
            WHERE ch.course_id = %s
                AND l.deleted_at IS NULL
                AND ch.deleted_at IS NULL
            ORDER BY ch.order_number, l.order_number
        """, (course_id,))
        return cursor.fetchall()
    finally:
        cursor.close()
        conn.close()

def verify_lesson_relationship(lesson_id: int, chapter_id: int, course_id: int) -> bool:
    """Verify the lesson-chapter-course relationship"""
    conn = connect_to_database()
    cursor = conn.cursor()

    try:
        cursor.execute("""
            SELECT COUNT(1)
            FROM lessons l
            INNER JOIN chapters ch ON l.chapter_id = ch.id
            WHERE l.id = %s
                AND l.chapter_id = %s
                AND ch.course_id = %s
                AND l.deleted_at IS NULL
                AND ch.deleted_at IS NULL
        """, (lesson_id, chapter_id, course_id))

        count = cursor.fetchone()[0]
        return count > 0
    finally:
        cursor.close()
        conn.close()

def get_valid_enrollments() -> List[Dict]:
    """Get all active course enrollments"""
    conn = connect_to_database()
    cursor = conn.cursor(dictionary=True)

    try:
        cursor.execute("""
            SELECT 
                uc.user_id,
                uc.course_id,
                uc.status,
                uc.enrollment_date,
                uc.completion_date,
                u.username,
                c.title AS course_title
            FROM user_courses uc
            INNER JOIN users u ON uc.user_id = u.id
            INNER JOIN courses c ON uc.course_id = c.id
            WHERE uc.status != 'DROPPED'
            ORDER BY uc.enrollment_date
        """)
        return cursor.fetchall()
    finally:
        cursor.close()
        conn.close()

def clear_progress_data():
    """Clear existing data from user_lesson_progress table"""
    conn = connect_to_database()
    cursor = conn.cursor()

    try:
        print("Clearing existing progress data...")
        cursor.execute("SET FOREIGN_KEY_CHECKS=0")
        cursor.execute("TRUNCATE TABLE user_lesson_progress")
        cursor.execute("SET FOREIGN_KEY_CHECKS=1")
        conn.commit()
        print("Progress data cleared successfully")
    finally:
        cursor.close()
        conn.close()

def insert_single_progress(record: Dict) -> bool:
    """Insert a single progress record"""
    conn = connect_to_database()
    cursor = conn.cursor()

    try:
        # First verify the relationship is valid
        if not verify_lesson_relationship(
                record['lesson_id'],
                record['chapter_id'],
                record['course_id']
        ):
            print(f"Invalid relationship - Lesson: {record['lesson_id']}, Chapter: {record['chapter_id']}, Course: {record['course_id']}")
            return False

        # Insert the record
        insert_query = """
            INSERT INTO user_lesson_progress (
                user_id, course_id, chapter_id, lesson_id,
                status, last_accessed_at, completion_date
            ) VALUES (
                %(user_id)s, %(course_id)s, %(chapter_id)s, 
                %(lesson_id)s, %(status)s, %(last_accessed_at)s,
                %(completion_date)s
            )
        """
        cursor.execute(insert_query, record)
        conn.commit()
        return True

    except mysql.connector.Error as err:
        print(f"Error inserting record: {err}")
        conn.rollback()
        return False
    finally:
        cursor.close()
        conn.close()

def calculate_progress_dates(
        enrollment_date: datetime,
        lesson_index: int,
        total_lessons: int,
        course_completion_date: datetime = None
) -> Tuple[datetime, datetime]:
    """Calculate access and completion dates for a lesson"""

    # Convert string dates to datetime if needed
    if isinstance(enrollment_date, str):
        enrollment_date = datetime.strptime(enrollment_date, '%Y-%m-%d %H:%M:%S')
    if isinstance(course_completion_date, str) and course_completion_date:
        course_completion_date = datetime.strptime(course_completion_date, '%Y-%m-%d %H:%M:%S')

    # Use completion date if available, otherwise use current time
    end_date = course_completion_date or datetime.now()

    # Calculate time span for lesson progression
    time_span = (end_date - enrollment_date).total_seconds()
    base_interval = time_span / (total_lessons + 1)

    # Add some randomness to the interval
    randomized_interval = base_interval * random.uniform(0.8, 1.2)

    # Calculate last accessed date
    access_date = enrollment_date + timedelta(seconds=randomized_interval * lesson_index)

    # For completed lessons, calculate completion date
    completion_date = None
    if course_completion_date:
        completion_date = min(
            access_date + timedelta(seconds=randomized_interval),
            course_completion_date
        )

    return access_date, completion_date

def determine_lesson_status(
        enrollment_status: str,
        lesson_index: int,
        total_lessons: int,
        course_completed: bool
) -> str:
    """Determine the status for a lesson based on course progress"""

    if course_completed:
        return 'COMPLETED'

    if enrollment_status == 'ENROLLED':
        return 'NOT_STARTED'

    # For IN_PROGRESS courses
    progress_ratio = lesson_index / total_lessons
    if progress_ratio < 0.4:
        return 'COMPLETED'
    elif progress_ratio < 0.6:
        return 'IN_PROGRESS'
    else:
        return 'NOT_STARTED'

def main():
    try:
        # Clear existing progress data
        clear_progress_data()

        # Get valid enrollments
        print("Fetching enrollments...")
        enrollments = get_valid_enrollments()
        print(f"Found {len(enrollments)} valid enrollments")

        successful_inserts = 0
        failed_inserts = 0

        # Process each enrollment
        for enrollment in enrollments:
            print(f"\nProcessing: {enrollment['username']} - {enrollment['course_title']}")

            # Get lessons for this course
            lessons = get_lessons_for_course(enrollment['course_id'])
            if not lessons:
                print(f"No lessons found for course {enrollment['course_id']}")
                continue

            print(f"Processing {len(lessons)} lessons...")

            # Process each lesson
            for idx, lesson in enumerate(lessons):
                # Calculate dates
                access_date, completion_date = calculate_progress_dates(
                    enrollment['enrollment_date'],
                    idx,
                    len(lessons),
                    enrollment['completion_date']
                )

                # Determine status
                status = determine_lesson_status(
                    enrollment['status'],
                    idx,
                    len(lessons),
                    bool(enrollment['completion_date'])
                )

                # Create and insert progress record
                progress_record = {
                    'user_id': enrollment['user_id'],
                    'course_id': lesson['course_id'],
                    'chapter_id': lesson['chapter_id'],
                    'lesson_id': lesson['lesson_id'],
                    'status': status,
                    'last_accessed_at': access_date,
                    'completion_date': completion_date if status == 'COMPLETED' else None
                }

                if insert_single_progress(progress_record):
                    successful_inserts += 1
                else:
                    failed_inserts += 1

                if (successful_inserts + failed_inserts) % 100 == 0:
                    print(f"Progress: {successful_inserts} successful, {failed_inserts} failed")

        # Verify final count
        conn = connect_to_database()
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM user_lesson_progress")
        final_count = cursor.fetchone()[0]
        cursor.close()
        conn.close()

        print(f"""
Progress generation completed successfully:
------------------------------------
Total enrollments processed: {len(enrollments)}
Successful inserts: {successful_inserts}
Failed inserts: {failed_inserts}
Total records in database: {final_count}
Success rate: {(successful_inserts / (successful_inserts + failed_inserts)) * 100:.2f}%
        """)

    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()