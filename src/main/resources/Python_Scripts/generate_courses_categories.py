import mysql.connector
import random
from datetime import datetime, timedelta
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

def generate_unique_category_name(existing_names, base_name=None):
    """Generate a unique category name using Faker"""
    while True:
        if base_name:
            name = f"{base_name} {fake.word().capitalize()}"
        else:
            name_types = [
                lambda: f"{fake.job()} Training",
                lambda: f"{fake.word().capitalize()} Studies",
                lambda: f"Advanced {fake.word().capitalize()}",
                lambda: f"{fake.currency_name()} Management",
                lambda: f"{fake.job()} Development",
                lambda: f"Professional {fake.job()}",
                lambda: f"{fake.company_suffix()} Management",
                lambda: f"Digital {fake.word().capitalize()}",
                lambda: f"{fake.word().capitalize()} Technology",
                lambda: f"{fake.word().capitalize()} Innovation"
            ]
            name = random.choice(name_types)()

        if name not in existing_names and len(name) <= 100:
            existing_names.add(name)
            return name

def connect_to_database():
    try:
        return mysql.connector.connect(**db_config)
    except mysql.connector.Error as err:
        print(f"Error connecting to database: {err}")
        raise

def generate_random_dates():
    """Generate random created_at and updated_at dates"""
    start_date = datetime(2023, 1, 1)
    end_date = datetime(2024, 10, 26)

    created_at = fake.date_time_between(start_date=start_date, end_date=end_date)
    # updated_at should be between created_at and end_date
    updated_at = fake.date_time_between(start_date=created_at, end_date=end_date)

    return created_at, updated_at

def generate_course_description():
    return (f"This comprehensive course focuses on {fake.catch_phrase()}. "
            f"Learn essential skills in {fake.bs()}. "
            f"Master the fundamentals with hands-on projects and real-world applications. "
            f"By the end of this course, you'll be proficient in {fake.catch_phrase()}.")

def generate_categories(num_categories):
    categories = []
    existing_names = set()

    # Define main subject areas
    main_areas = [
        "Programming", "Business", "Design", "Marketing",
        "Technology", "Arts", "Engineering", "Communication",
        "Data Science", "Digital Media"
    ]

    # First, create categories based on main areas
    for area in main_areas:
        if len(categories) < num_categories:
            name = generate_unique_category_name(existing_names, area)
            created_at, updated_at = generate_random_dates()
            categories.append({
                'id': len(categories) + 1,
                'name': name,
                'created_at': created_at,
                'updated_at': updated_at,
                'deleted_at': None
            })

    # Then fill up remaining categories with random names
    while len(categories) < num_categories:
        name = generate_unique_category_name(existing_names)
        created_at, updated_at = generate_random_dates()
        categories.append({
            'id': len(categories) + 1,
            'name': name,
            'created_at': created_at,
            'updated_at': updated_at,
            'deleted_at': None
        })

    return categories

def generate_courses(num_courses, instructor_ids):
    courses = []
    existing_titles = set()

    course_prefixes = [
        "Mastering", "Advanced", "Introduction to", "Professional",
        "Complete Guide to", "Essential", "Practical", "Modern",
        "Fundamentals of", "Ultimate"
    ]

    for i in range(1, num_courses + 1):
        while True:
            prefix = random.choice(course_prefixes)
            title = f"{prefix} {fake.job()} {fake.word().capitalize()}"
            if title not in existing_titles and len(title) <= 255:
                existing_titles.add(title)
                break

        created_at, updated_at = generate_random_dates()

        courses.append({
            'id': i,
            'title': title,
            'description': generate_course_description(),
            'instructor_id': random.choice(instructor_ids),
            'status': random.choice(['DRAFT', 'PUBLISHED', 'ARCHIVED']),
            'created_at': created_at,
            'updated_at': updated_at,
            'deleted_at': None
        })

    return courses

def insert_test_data():
    try:
        conn = connect_to_database()
        cursor = conn.cursor()

        # Clear existing data
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        cursor.execute("TRUNCATE TABLE course_categories")
        cursor.execute("TRUNCATE TABLE courses")
        cursor.execute("TRUNCATE TABLE categories")
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

        # Get instructor IDs
        cursor.execute("""
            SELECT DISTINCT u.id 
            FROM users u 
            JOIN user_roles ur ON u.id = ur.user_id 
            JOIN roles r ON ur.role_id = r.id 
            WHERE r.name = 'INSTRUCTOR'
        """)
        instructor_ids = [row[0] for row in cursor.fetchall()]

        if not instructor_ids:
            print("No instructors found. Please add instructors to the database first.")
            return

        # Insert categories
        categories = generate_categories(50)
        category_sql = """
        INSERT INTO categories (id, name, created_at, updated_at, deleted_at)
        VALUES (%s, %s, %s, %s, %s)
        """
        for category in categories:
            cursor.execute(category_sql, (
                category['id'],
                category['name'],
                category['created_at'],
                category['updated_at'],
                category['deleted_at']
            ))

        # Insert courses
        courses = generate_courses(50, instructor_ids)
        course_sql = """
        INSERT INTO courses (id, title, description, instructor_id, status, 
                           created_at, updated_at, deleted_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        for course in courses:
            cursor.execute(course_sql, (
                course['id'],
                course['title'],
                course['description'],
                course['instructor_id'],
                course['status'],
                course['created_at'],
                course['updated_at'],
                course['deleted_at']
            ))

        # Create course-category relationships
        course_category_sql = """
        INSERT INTO course_categories (course_id, category_id)
        VALUES (%s, %s)
        """

        for course in courses:
            # Assign 1-3 random categories to each course
            num_categories = random.randint(1, 3)
            selected_categories = random.sample([cat['id'] for cat in categories], num_categories)

            for category_id in selected_categories:
                cursor.execute(course_category_sql, (course['id'], category_id))

        conn.commit()
        print(f"""
Test data generated successfully:
- {len(categories)} unique categories inserted
- {len(courses)} unique courses inserted
- Multiple course-category relationships created

Sample categories:
{[(cat['name'], cat['created_at'].strftime('%Y-%m-%d %H:%M:%S'),
   cat['updated_at'].strftime('%Y-%m-%d %H:%M:%S')) for cat in categories[:5]]}

Sample courses:
{[(course['title'], course['created_at'].strftime('%Y-%m-%d %H:%M:%S'),
   course['updated_at'].strftime('%Y-%m-%d %H:%M:%S')) for course in courses[:5]]}
        """)

    except mysql.connector.Error as err:
        print(f"Error: {err}")
        conn.rollback()
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

if __name__ == "__main__":
    insert_test_data()