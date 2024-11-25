import mysql.connector
from faker import Faker
import random
from datetime import datetime, timedelta

# Initialize Faker
fake = Faker()

# Database configuration
db_config = {
    'host': 'localhost',
    'user': 'Huyvictory',
    'password': '12345',
    'database': 'onlinecoursemanagement'
}

def connect_to_database():
    """Establish database connection"""
    try:
        return mysql.connector.connect(**db_config)
    except mysql.connector.Error as err:
        print(f"Error connecting to database: {err}")
        raise

def get_active_users_and_courses(cursor):
    """Get all active users and non-archived courses"""

    # Get active users (excluding admin)
    cursor.execute("""
        SELECT DISTINCT u.id 
        FROM users u 
        JOIN user_roles ur ON u.id = ur.user_id 
        JOIN roles r ON ur.role_id = r.id 
        WHERE u.status = 'ACTIVE' 
        AND u.deleted_at IS NULL
        AND r.name = 'USER'
    """)
    active_users = [row[0] for row in cursor.fetchall()]

    # Get published and draft courses (non-archived)
    cursor.execute("""
        SELECT id, created_at, status
        FROM courses 
        WHERE status != 'ARCHIVED' 
        AND deleted_at IS NULL
    """)
    available_courses = [(row[0], row[1], row[2]) for row in cursor.fetchall()]

    return active_users, available_courses

def generate_rating_date(course_created_at):
    """Generate a valid rating date after course creation"""
    course_date = course_created_at
    if isinstance(course_created_at, str):
        course_date = datetime.strptime(course_created_at, '%Y-%m-%d %H:%M:%S')

    # Rating should be between course creation and now
    return fake.date_time_between(start_date=course_date, end_date=datetime.now())

def generate_review_text(rating):
    """Generate appropriate review text based on rating"""

    excellent_reviews = [
        "Exceptionally well-structured course with great practical examples.",
        "Outstanding content and delivery. Highly recommended!",
        "The course exceeded my expectations. Very comprehensive!",
        "Excellent instructor and material. Really helped my understanding.",
    ]

    good_reviews = [
        "Good course with helpful content.",
        "Solid material and well-presented information.",
        "Useful content and good examples.",
        "Clear explanations and practical applications.",
    ]

    average_reviews = [
        "Decent course but could use more examples.",
        "Basic coverage of the topics.",
        "Adequate content but needs more depth.",
        "Okay course overall, meets basic requirements.",
    ]

    below_average_reviews = [
        "Content could be more engaging.",
        "Needs more practical examples.",
        "Some concepts were not explained clearly.",
        "Course structure needs improvement.",
    ]

    poor_reviews = [
        "Content needs significant improvement.",
        "Explanations were unclear and confusing.",
        "Did not meet expectations.",
        "Needs major updates and revisions.",
    ]

    if rating == 5:
        return fake.sentence() + " " + random.choice(excellent_reviews)
    elif rating == 4:
        return fake.sentence() + " " + random.choice(good_reviews)
    elif rating == 3:
        return fake.sentence() + " " + random.choice(average_reviews)
    elif rating == 2:
        return fake.sentence() + " " + random.choice(below_average_reviews)
    else:
        return fake.sentence() + " " + random.choice(poor_reviews)

def insert_course_ratings(num_ratings=200):
    try:
        conn = connect_to_database()
        cursor = conn.cursor()

        # Clear existing ratings
        cursor.execute("TRUNCATE TABLE course_ratings")

        # Get active users and available courses
        active_users, available_courses = get_active_users_and_courses(cursor)

        ratings_created = 0
        existing_combinations = set()  # Track user-course combinations

        # Distribution weights for ratings (favoring 4 and 5 star ratings)
        rating_weights = {
            5: 0.35,  # 35% chance of 5 stars
            4: 0.30,  # 30% chance of 4 stars
            3: 0.20,  # 20% chance of 3 stars
            2: 0.10,  # 10% chance of 2 stars
            1: 0.05   # 5% chance of 1 star
        }

        print(f"Starting rating generation with {len(active_users)} active users and {len(available_courses)} available courses")

        while ratings_created < num_ratings and available_courses and active_users:
            user_id = random.choice(active_users)
            course_id, course_created_at, course_status = random.choice(available_courses)

            # Skip if this combination already exists
            if (user_id, course_id) in existing_combinations:
                continue

            existing_combinations.add((user_id, course_id))

            # Generate rating with weighted distribution
            rating = random.choices(
                list(rating_weights.keys()),
                weights=list(rating_weights.values())
            )[0]

            # Generate timestamps
            created_at = generate_rating_date(course_created_at)
            updated_at = created_at

            # Generate review text based on rating
            review_text = generate_review_text(rating)

            # Insert rating
            cursor.execute("""
                INSERT INTO course_ratings 
                (user_id, course_id, rating, review_text, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, (user_id, course_id, rating, review_text, created_at, updated_at))

            ratings_created += 1

            if ratings_created % 50 == 0:
                print(f"Created {ratings_created} ratings...")

        conn.commit()

        # Calculate and print statistics
        cursor.execute("SELECT rating, COUNT(*) FROM course_ratings GROUP BY rating ORDER BY rating")
        rating_distribution = cursor.fetchall()

        print("\nRating generation completed successfully!")
        print(f"\nTotal ratings created: {ratings_created}")
        print("\nRating distribution:")
        for rating, count in rating_distribution:
            percentage = (count / ratings_created) * 100
            print(f"{rating} stars: {count} ratings ({percentage:.1f}%)")

    except mysql.connector.Error as err:
        print(f"Database error occurred: {err}")
        conn.rollback()
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()
            print("\nDatabase connection closed.")

if __name__ == "__main__":
    print("Starting course rating generation...")
    insert_course_ratings(200)  # Generate 200 ratings