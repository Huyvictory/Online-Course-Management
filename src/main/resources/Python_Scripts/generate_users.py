import mysql.connector
import random
from datetime import datetime, timedelta
import bcrypt
from faker import Faker
import unicodedata

# Initialize Faker
fake = Faker()

# Database configuration
db_config = {
    'host': 'localhost',
    'user': 'Huyvictory',
    'password': '12345',
    'database': 'onlinecoursemanagement'
}

# Fixed password for all users
FIXED_PASSWORD = "12345"


def remove_accents(input_str):
    """Remove diacritics from a string and return ASCII-only characters"""
    nfkd_form = unicodedata.normalize('NFKD', input_str)
    return u"".join([c for c in nfkd_form if not unicodedata.combining(c)])


def generate_password_hash(password):
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')


def generate_random_date_2024():
    start_date = datetime(2024, 1, 1)
    end_date = datetime(2024, 12, 31)
    return fake.date_time_between(start_date=start_date, end_date=end_date)


def generate_unique_username(cursor, name):
    """Generate a unique username based on name"""
    # Remove accents and special characters, convert to lowercase
    base_username = remove_accents(name.lower().replace(' ', ''))
    username = base_username
    counter = 1

    while True:
        cursor.execute("SELECT COUNT(*) FROM users WHERE username = %s", (username,))
        if cursor.fetchone()[0] == 0:
            return username
        username = f"{base_username}{counter}"
        counter += 1


def generate_unique_email(cursor, name):
    """Generate a unique email based on name"""
    # Remove accents and special characters, convert to lowercase
    base_email = remove_accents(name.lower().replace(' ', '.'))
    email = f"{base_email}@{fake.free_email_domain()}"
    counter = 1

    while True:
        cursor.execute("SELECT COUNT(*) FROM users WHERE email = %s", (email,))
        if cursor.fetchone()[0] == 0:
            return email
        email = f"{base_email}{counter}@{fake.free_email_domain()}"
        counter += 1


def insert_user(cursor, role_type, count):
    users = []
    for i in range(count):
        # Generate profile based on role
        if role_type == "INSTRUCTOR":
            profile = {
                'name': fake.name(),
                'job': fake.job(),
                'profile': fake.text(max_nb_chars=200)
            }
        else:
            profile = {
                'name': fake.name()
            }

        # Generate unique username and email
        username = generate_unique_username(cursor, profile['name'])
        email = generate_unique_email(cursor, profile['name'])

        # Common user data
        created_at = generate_random_date_2024()
        updated_at = created_at
        password_hash = generate_password_hash(FIXED_PASSWORD)
        status = random.choices(['ACTIVE', 'INACTIVE'], weights=[90, 10])[0]

        # Insert user
        insert_user_sql = """
        INSERT INTO users (username, email, password_hash, real_name, status, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        user_values = (username, email, password_hash, profile['name'], status, created_at, updated_at)
        cursor.execute(insert_user_sql, user_values)
        user_id = cursor.lastrowid
        users.append({
            'id': user_id,
            'username': username,
            'email': email,
            'name': profile['name'],
            'created_at': created_at,
            'password': FIXED_PASSWORD  # Added to show in output
        })

        # Get role ID
        cursor.execute("SELECT id FROM roles WHERE name = %s", (role_type,))
        role_id = cursor.fetchone()[0]

        # Insert user_role
        insert_user_role_sql = """
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        VALUES (%s, %s, %s)
        """
        user_role_values = (user_id, role_id, created_at)
        cursor.execute(insert_user_role_sql, user_role_values)

    return users


def main():
    try:
        # Connect to database
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        print(f"Starting user generation... (All users will have password: {FIXED_PASSWORD})\n")

        # Insert normal users
        print("Generating normal users...")
        normal_users = insert_user(cursor, "USER", 10)
        print(f"\nCreated {len(normal_users)} normal users:")
        for user in normal_users:
            print(f"- Name: {user['name']}")
            print(f"  Username: {user['username']}")
            print(f"  Email: {user['email']}")
            print(f"  Password: {user['password']}")
            print(f"  Created: {user['created_at']}")
            print()

        # Insert instructors
        print("\nGenerating instructors...")
        instructors = insert_user(cursor, "INSTRUCTOR", 10)
        print(f"\nCreated {len(instructors)} instructors:")
        for user in instructors:
            print(f"- Name: {user['name']}")
            print(f"  Username: {user['username']}")
            print(f"  Email: {user['email']}")
            print(f"  Password: {user['password']}")
            print(f"  Created: {user['created_at']}")
            print()

        # Commit the changes
        conn.commit()
        print("\nAll users created successfully!")
        print(f"Remember: All users have password: {FIXED_PASSWORD}")

    except mysql.connector.Error as err:
        print(f"Error: {err}")
        conn.rollback()
    finally:
        if 'conn' in locals() and conn.is_connected():
            cursor.close()
            conn.close()
            print("\nDatabase connection closed.")


if __name__ == "__main__":
    main()
