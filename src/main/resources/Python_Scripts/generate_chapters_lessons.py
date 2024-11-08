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


def get_course_data(cursor):
    """Get all courses from the database"""
    cursor.execute("""
        SELECT id, title, status, created_at, deleted_at 
        FROM courses 
        ORDER BY id
    """)
    return cursor.fetchall()


def extract_course_topic(course_title):
    """Extract the main topic from course title"""
    prefixes = ['Ultimate', 'Advanced', 'Introduction to', 'Fundamentals of', 'Essential',
                'Complete Guide to', 'Professional', 'Modern', 'Mastering', 'Practical']

    title = course_title
    for prefix in prefixes:
        if title.startswith(prefix):
            title = title[len(prefix):].strip()

    return title.strip()


def generate_chapter_title(course_topic):
    """Generate a contextual chapter title based on course topic"""
    core_topics = {
        'Video editor': [
            'Introduction to Video Editing Basics',
            'Video Composition and Framing',
            'Advanced Editing Techniques',
            'Color Correction and Grading',
            'Audio Editing and Sound Design',
            'Transitions and Effects',
            'Project Organization and Workflow',
            'Final Output and Delivery'
        ],
        'Engineer': [
            'Engineering Fundamentals',
            'Design Principles and Methodology',
            'Technical Analysis and Planning',
            'Project Implementation Strategies',
            'Quality Assurance and Testing',
            'Documentation and Standards',
            'Performance Optimization',
            'Professional Best Practices'
        ],
        'Management': [
            'Management Principles and Theory',
            'Leadership and Team Building',
            'Strategic Planning and Execution',
            'Resource Allocation and Optimization',
            'Performance Management',
            'Risk Management and Mitigation',
            'Change Management Strategies',
            'Organizational Development'
        ],
        'Development': [
            'Core Development Concepts',
            'Software Architecture and Design',
            'Code Quality and Best Practices',
            'Testing and Debugging Strategies',
            'Performance Optimization',
            'Security Implementation',
            'Deployment and Maintenance',
            'Advanced Development Patterns'
        ],
        'Design': [
            'Design Fundamentals and Principles',
            'User Experience and Interface Design',
            'Visual Composition and Layout',
            'Color Theory and Typography',
            'Interactive Design Patterns',
            'Prototyping and Testing',
            'Design Systems and Standards',
            'Advanced Design Techniques'
        ]
    }

    # Determine which category to use based on course topic
    selected_topics = None
    for key in core_topics:
        if key.lower() in course_topic.lower():
            selected_topics = core_topics[key]
            break

    # Use generic topics if no specific match found
    if not selected_topics:
        selected_topics = [
            f'Understanding {course_topic} Fundamentals',
            f'Core Principles of {course_topic}',
            f'Advanced {course_topic} Concepts',
            f'Practical {course_topic} Applications',
            f'Professional {course_topic} Techniques',
            f'{course_topic} Best Practices',
            f'{course_topic} Analysis and Implementation',
            f'Mastering {course_topic} Skills'
        ]

    return random.choice(selected_topics)


def generate_lesson_title(chapter_title, lesson_number, total_lessons):
    """Generate a contextual lesson title that follows a logical progression"""
    main_topic = chapter_title.split(':')[-1].strip() if ':' in chapter_title else chapter_title

    if lesson_number == 1:
        patterns = [
            f'Fundamentals of {main_topic}',
            f'Introduction to {main_topic}',
            f'Getting Started with {main_topic}',
            f'Core Concepts of {main_topic}'
        ]
    elif lesson_number == total_lessons:
        patterns = [
            f'Advanced Applications in {main_topic}',
            f'Mastering {main_topic}',
            f'Professional {main_topic} Techniques',
            f'Real-world {main_topic} Projects'
        ]
    else:
        position_ratio = lesson_number / total_lessons
        if position_ratio < 0.33:
            patterns = [
                f'Understanding {main_topic} Principles',
                f'Essential {main_topic} Concepts',
                f'Building Blocks of {main_topic}',
                f'{main_topic} Foundations'
            ]
        elif position_ratio < 0.66:
            patterns = [
                f'Practical {main_topic} Applications',
                f'Implementing {main_topic}',
                f'{main_topic} in Practice',
                f'Working with {main_topic}'
            ]
        else:
            patterns = [
                f'Advanced {main_topic} Techniques',
                f'Professional {main_topic} Skills',
                f'Complex {main_topic} Scenarios',
                f'Optimizing {main_topic}'
            ]

    return random.choice(patterns)


def generate_content_section(topic, section_type):
    """Generate specific content sections based on topic and type"""
    if section_type == "objectives":
        objectives = [
            f"Understand key concepts of {topic}",
            f"Apply practical techniques in {topic}",
            f"Analyze various approaches to {topic}",
            f"Evaluate best practices in {topic}",
            f"Implement effective solutions for {topic}",
            f"Master advanced techniques in {topic}"
        ]
        return random.sample(objectives, k=3)

    elif section_type == "key_points":
        points = [
            f"Essential principles of {topic}",
            f"Common challenges in {topic} and their solutions",
            f"Best practices for {topic} implementation",
            f"Important considerations when working with {topic}",
            f"Industry standards for {topic}",
            f"Professional tips for {topic}"
        ]
        return random.sample(points, k=3)

    elif section_type == "practice":
        exercises = [
            f"Implement a basic {topic} solution",
            f"Analyze a real-world {topic} scenario",
            f"Optimize an existing {topic} implementation",
            f"Troubleshoot common {topic} issues",
            f"Design a complete {topic} solution",
            f"Review and improve {topic} performance"
        ]
        return random.sample(exercises, k=3)


def generate_lesson_content(lesson_type, lesson_title):
    """Generate structured lesson content based on lesson type and title"""
    topic = lesson_title.split('of ')[-1] if 'of ' in lesson_title else lesson_title

    def generate_text_content():
        learning_objectives = "\n".join([f"- {obj}" for obj in generate_content_section(topic, "objectives")])
        key_points = "\n".join([f"- {point}" for point in generate_content_section(topic, "key_points")])
        practice_exercises = "\n".join(
            [f"{i + 1}. {ex}" for i, ex in enumerate(generate_content_section(topic, "practice"))])

        content = f"""
Learning Objectives:
{learning_objectives}

Introduction:
{fake.paragraph(nb_sentences=3, variable_nb_sentences=True)}

Key Concepts:
{key_points}

Detailed Explanation:
{fake.paragraph(nb_sentences=4)}
{fake.paragraph(nb_sentences=3)}

Practice Exercises:
{practice_exercises}

Summary:
{fake.paragraph(nb_sentences=2)}
        """
        return content.strip()

    def generate_video_content():
        learning_objectives = "\n".join([f"- {obj}" for obj in generate_content_section(topic, "objectives")])
        key_points = "\n".join([f"- {point}" for point in generate_content_section(topic, "key_points")])

        content = f"""
Learning Objectives:
{learning_objectives}

Video Outline:
1. Introduction to {topic}
2. Core Concepts and Principles
3. Practical Demonstrations
4. Common Challenges and Solutions
5. Best Practices and Tips

Key Points to Remember:
{key_points}

After Watching:
- Practice the demonstrated techniques
- Complete the hands-on exercises
- Review the key concepts
        """
        return content.strip()

    def generate_quiz_content():
        quiz_topics = generate_content_section(topic, "key_points")
        questions = []
        for i, quiz_topic in enumerate(quiz_topics, 1):
            question = f"Question {i}: Regarding {quiz_topic}, explain how you would:"
        options = [
            f"A) {fake.sentence()}",
            f"B) {fake.sentence()}",
            f"C) {fake.sentence()}",
            f"D) {fake.sentence()}"
        ]
        questions.append(question + '\n' + '\n'.join(options))
        content = f"""Quiz Overview:
        This quiz will test your understanding of {topic}.

        {('').join(questions)}

        Note: Complete all questions and review your answers carefully."""
        return content.strip()

    content_generators = {
        'TEXT': generate_text_content,
        'VIDEO': generate_video_content,
        'QUIZ': generate_quiz_content
    }

    return content_generators[lesson_type]()


def get_valid_dates(course_created_at, course_deleted_at=None):
    """Generate valid dates based on course dates"""
    start_date = datetime.strptime(course_created_at, '%Y-%m-%d %H:%M:%S')
    if course_deleted_at:
        end_date = datetime.strptime(course_deleted_at, '%Y-%m-%d %H:%M:%S')
    else:
        end_date = datetime.now()

    created_at = fake.date_time_between(start_date=start_date, end_date=end_date)
    updated_at = fake.date_time_between(start_date=created_at, end_date=end_date)

    return created_at, updated_at


def determine_status(course_status, entity_dates, course_deleted_at):
    """Determine appropriate status based on course status and dates"""
    if course_status == 'ARCHIVED' or course_deleted_at:
        return 'ARCHIVED'
    elif course_status == 'PUBLISHED':
        return random.choice(['PUBLISHED'] * 7 + ['DRAFT'] * 3)  # 70% published, 30% draft
    else:
        return 'DRAFT'


def calculate_records_per_course(total_courses, target_chapters, target_lessons):
    """Calculate minimum number of chapters/lessons needed per course"""
    min_chapters_per_course = max(3, target_chapters // total_courses)
    max_chapters_per_course = min_chapters_per_course + 2

    min_lessons_per_chapter = max(4, target_lessons // (total_courses * min_chapters_per_course))
    max_lessons_per_chapter = min_lessons_per_chapter + 3

    return (min_chapters_per_course, max_chapters_per_course,
            min_lessons_per_chapter, max_lessons_per_chapter)


def insert_test_data(target_chapters=50, target_lessons=50):
    try:
        conn = connect_to_database()
        cursor = conn.cursor()

        # Clear existing data
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        cursor.execute("TRUNCATE TABLE lessons")
        cursor.execute("TRUNCATE TABLE chapters")
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

        courses = get_course_data(cursor)
        total_courses = len(courses)

        # Calculate required records
        (min_chapters, max_chapters,
         min_lessons, max_lessons) = calculate_records_per_course(total_courses,
                                                                  target_chapters,
                                                                  target_lessons)

        chapters_created = 0
        lessons_created = 0

        print(f"""
Starting data generation with:
- Minimum {min_chapters} chapters per course
- {min_lessons}-{max_lessons} lessons per chapter
        """)

        for course in courses:
            course_id, course_title, course_status, course_created_at, course_deleted_at = course
            course_topic = extract_course_topic(course_title)
            num_chapters = random.randint(min_chapters, max_chapters)

            for chapter_order in range(1, num_chapters + 1):
                # Generate chapter
                chapter_created_at, chapter_updated_at = get_valid_dates(
                    course_created_at.strftime('%Y-%m-%d %H:%M:%S'),
                    course_deleted_at.strftime('%Y-%m-%d %H:%M:%S') if course_deleted_at else None
                )

                chapter_title = generate_chapter_title(course_topic)
                chapter_status = determine_status(course_status,
                                                  (chapter_created_at, chapter_updated_at),
                                                  course_deleted_at)

                chapter_sql = """
                INSERT INTO chapters (course_id, title, description, order_number, 
                                   status, created_at, updated_at, deleted_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """
                chapter_values = (
                    course_id,
                    chapter_title,
                    f"Comprehensive coverage of {chapter_title}",
                    chapter_order,
                    chapter_status,
                    chapter_created_at,
                    chapter_updated_at,
                    chapter_updated_at if chapter_status == 'ARCHIVED' else None
                )
                cursor.execute(chapter_sql, chapter_values)
                chapter_id = cursor.lastrowid
                chapters_created += 1

                # Generate lessons
                num_lessons = random.randint(min_lessons, max_lessons)

                for lesson_order in range(1, num_lessons + 1):
                    lesson_created_at, lesson_updated_at = get_valid_dates(
                        chapter_created_at.strftime('%Y-%m-%d %H:%M:%S'),
                        chapter_updated_at.strftime('%Y-%m-%d %H:%M:%S')
                    )

                    lesson_status = determine_status(chapter_status,
                                                     (lesson_created_at, lesson_updated_at),
                                                     chapter_updated_at if chapter_status == 'ARCHIVED' else None)

                    lesson_type = random.choice(['VIDEO', 'TEXT', 'QUIZ'])
                    lesson_title = generate_lesson_title(chapter_title, lesson_order, num_lessons)

                    lesson_sql = """
                    INSERT INTO lessons (chapter_id, title, content, order_number, type,
                                      status, created_at, updated_at, deleted_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    lesson_values = (
                        chapter_id,
                        lesson_title,
                        generate_lesson_content(lesson_type, lesson_title),
                        lesson_order,
                        lesson_type,
                        lesson_status,
                        lesson_created_at,
                        lesson_updated_at,
                        lesson_updated_at if lesson_status == 'ARCHIVED' else None
                    )
                    cursor.execute(lesson_sql, lesson_values)
                    lessons_created += 1

                if chapters_created % 10 == 0:
                    print(f"Progress: {chapters_created} chapters, {lessons_created} lessons created")

        conn.commit()
        print(f"""
Data generation completed successfully:
- {chapters_created} chapters created (Target: {target_chapters})
- {lessons_created} lessons created (Target: {target_lessons})
- Average lessons per chapter: {lessons_created / chapters_created:.1f}
- Generated with contextual titles and structured content
- Maintained proper relationships and data integrity
- All dates and statuses properly aligned

Chapter distribution:
- Minimum chapters per course: {min_chapters}
- Maximum chapters per course: {max_chapters}
- Average chapters per course: {chapters_created / total_courses:.1f}

Lesson distribution:
- Minimum lessons per chapter: {min_lessons}
- Maximum lessons per chapter: {max_lessons}
- Average lessons per chapter: {lessons_created / chapters_created:.1f}
        """)

    except mysql.connector.Error as err:
        print(f"Error: {err}")
        conn.rollback()
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()
            print("Database connection closed.")


if __name__ == "__main__":
    print("Starting chapter and lesson generation...")
    print("This will clear existing chapters and lessons before generating new ones.")
    try:
        insert_test_data(target_chapters=50, target_lessons=50)
    except Exception as e:
        print(f"An error occurred: {str(e)}")
    print("Process completed.")
