# Online Course Management System

## Overview
A comprehensive online course management system built with Spring Boot that enables educational institutions and content creators to manage and deliver online courses effectively. The system provides robust features for course creation, user management, content organization, and learning progress tracking.

## Features

### User Management
- Multi-role support (Admin, Instructor, User)
- Secure authentication and authorization
- Profile management

### Course Management
- Course creation and modification
- Chapter and lesson organization
- Course status management (Draft, Published, Archived)
- Category management for course organization

### Learning Features
- Course enrollment system
- Progress tracking
- Chapter and lesson completion monitoring
- Course ratings and reviews

### Administrative Features
- User Role management
- Content moderation
- System monitoring and reporting

## Technical Stack

### Backend
- Java 17
- Spring Boot 3.1.5
- Spring Data JPA
- MySQL 8.0

### Spring Security Implementation
The project uses Spring Security 6.x with extensive customization for robust security:

#### Core Security Features
- Custom JWT-based authentication
- Role-based authorization using @RequiredRole annotation
- BCrypt password encryption
- CORS and CSRF protection

#### Key Security Components
1. Security Configuration
    - SecurityConfig.java: Main security configuration
    - JwtConfig.java: JWT settings and configuration
    - WebConfig.java: Web security settings

2. JWT Implementation
    - JwtUtil.java: JWT token generation and validation
    - JwtAuthenticationFilter.java: Custom filter for JWT processing
    - JwtAuthenticationEntryPoint.java: Handles authentication exceptions

3. Authentication
    - CustomUserDetailsService.java: Implements UserDetailsService
    - CustomUserDetails.java: Custom user details implementation
    - CustomAccessDeniedHandler.java: Handles access denied scenarios

4. Authorization
    - RoleAuthorizationAspect.java: Custom aspect for role-based access
    - UserSecurity.java: Custom security utilities
    - RequiredRole.java: Custom annotation for role checking

5. Security Filters
    - JwtAuthenticationFilter.java: JWT authentication
    - DebugFilter.java: Debugging and logging
    - Custom security filters for specific needs

### Key Libraries
- Lombok
- MapStruct
- Flyway Migration
- Jackson
- JWT (io.jsonwebtoken)

## Project Structure

```
src/main/java/com/online/course/management/project/
├── aspect                             # AOP components
│   ├── LoggingAspect.java            # Logging aspect for controllers and services
│   └── RoleAuthorizationAspect.java  # Role-based authorization aspect
│
├── config                            # Configuration classes
│   ├── AdminInitializer.java         # Admin user initialization
│   ├── CacheConfig.java             # Caffeine cache configuration
│   ├── DatabaseInitializer.java      # Database initialization
│   ├── JacksonConfig.java           # JSON serialization configuration
│   ├── OpenAPIConfig.java           # Swagger/OpenAPI configuration
│   ├── SecurityConfig.java          # Main security configuration
│   └── WebConfig.java               # Web MVC configuration
│
├── constants                         # Constants
│   ├── CategoryConstants.java        # Category-related constants
│   ├── ChapterConstants.java         # Chapter-related constants
│   ├── CourseConstants.java         # Course-related constants
│   ├── CourseRatingConstants.java    # Course rating constants
│   ├── LessonConstants.java         # Lesson-related constants
│   ├── PathConstants.java           # API path constants
│   ├── UserConstants.java           # User-related constants
│   ├── UserCourseConstants.java     # User course constants
│   └── UserLessonProgressConstants.java # Progress constants
│
├── controller                        # REST controllers
│   ├── CategoryController.java       # Category management
│   ├── ChapterController.java        # Chapter management
│   ├── CourseController.java        # Course management
│   ├── CourseRatingController.java   # Course rating management
│   ├── CustomErrorController.java    # Custom error handling
│   ├── LessonController.java        # Lesson management
│   ├── UserController.java          # User management
│   ├── UserCourseController.java    # Course enrollment
│   └── UserLessonProgressController.java # Learning progress
│
├── dto                              # Data Transfer Objects
│   ├── CategoryDTOs.java           # Category-related DTOs
│   ├── ChapterDTOs.java            # Chapter-related DTOs
│   ├── CommonResponseDTOs.java     # Common response DTOs
│   ├── CourseDTOS.java            # Course-related DTOs
│   ├── CourseRatingDTOs.java       # Rating DTOs
│   ├── ErrorResponseDTO.java       # Error response DTO
│   ├── LessonDTOs.java            # Lesson-related DTOs
│   ├── PaginationDto.java         # Pagination DTOs
│   ├── UserCourseDTOs.java        # User course DTOs
│   ├── UserDTOs.java              # User-related DTOs
│   └── UserLessonProgressDtos.java # Progress DTOs
│
├── entity                           # JPA entities
│   ├── Category.java               # Category entity
│   ├── Chapter.java                # Chapter entity
│   ├── Course.java                 # Course entity
│   ├── CourseRating.java           # Course rating entity
│   ├── Lesson.java                 # Lesson entity
│   ├── Role.java                   # Role entity
│   ├── User.java                   # User entity
│   ├── UserCourse.java            # User course relationship
│   ├── UserLessonProgress.java     # Learning progress entity
│   ├── UserRole.java              # User role relationship
│   └── UserRoleId.java            # User role composite key
│
├── enums                            # Enumerations
│   ├── CourseStatus.java           # Course status
│   ├── EnrollmentStatus.java       # Enrollment status
│   ├── LessonType.java            # Lesson type
│   ├── ProgressStatus.java         # Progress status
│   ├── RoleType.java              # Role types
│   └── UserStatus.java            # User status
│
├── exception                        # Exception handling
│   ├── BaseException.java          # Base exception class
│   ├── GlobalExceptionHandler.java  # Global exception handler
│   ├── business/                   # Business exceptions
│   └── technical/                  # Technical exceptions
│
├── filter                           # Filters
│   ├── DebugFilter.java            # Debug logging filter
│   └── JwtAuthenticationFilter.java # JWT authentication filter
│
├── mapper                           # MapStruct mappers
│   ├── CategoryMapper.java         # Category DTO mapper
│   ├── ChapterMapper.java          # Chapter DTO mapper
│   ├── CourseMapper.java           # Course DTO mapper
│   ├── CourseRatingMapper.java     # Rating DTO mapper
│   ├── LessonMapper.java          # Lesson DTO mapper
│   ├── UserCourseMapper.java       # User course mapper
│   ├── UserLessonProgressMapper.java # Progress mapper
│   └── UserMapper.java            # User DTO mapper
│
├── repository                       # JPA repositories
│   ├── ICategoryRepository.java    # Category repository
│   ├── IChapterRepository.java     # Chapter repository
│   ├── ICourseRepository.java      # Course repository
│   ├── ICourseRatingRepository.java # Rating repository
│   ├── ILessonRepository.java      # Lesson repository
│   ├── IRoleRepository.java        # Role repository
│   ├── IUserCourseRepository.java  # User course repository
│   ├── IUserLessonProgressRepository.java # Progress repository
│   ├── IUserRepository.java        # User repository
│   └── IUserRoleRepository.java    # User role repository
│
├── security                         # Security components
│   ├── CustomAccessDeniedHandler.java # Access denied handler
│   ├── CustomUserDetails.java      # User details implementation
│   ├── CustomUserDetailsService.java # User details service
│   ├── JwtAuthenticationEntryPoint.java # JWT entry point
│   ├── JwtUtil.java               # JWT utilities
│   ├── RequiredRole.java          # Role annotation
│   └── UserSecurity.java          # Security utilities
│
├── service                          # Services
│   ├── impl/                       # Service implementations
│   └── interfaces/                 # Service interfaces
│
├── utils                            # Utilities
│   ├── category/                   # Category utilities
│   ├── chapter/                    # Chapter utilities
│   ├── course/                     # Course utilities
│   ├── courserating/              # Rating utilities
│   ├── exception/                 # Exception utilities
│   ├── generator/                 # Generator utilities
│   ├── lesson/                    # Lesson utilities
│   ├── user/                      # User utilities
│   └── usercourse/               # User course utilities
│
└── ProjectApplication.java          # Main application class

resources/
├── Python_Scripts/                  # Python data generation scripts
│   ├── generate_chapters_lessons.py
│   ├── generate_course_ratings.py
│   ├── generate_courses_categories.py
│   ├── generate_lesson_progress.py
│   ├── generate_user_enrollments.py
│   └── generate_users.py
│
├── application.properties          # Application configuration
└── db/migration/                  # Flyway migrations
    ├── V10__Update_non_admin_passwords.sql
    ├── V11__Update_non_admin_passwords.sql
    ├── V12__Add_chapter_and_lesson_tables.sql
    └── ... (other migration files)
```

## Database Schema
The system uses a relational database with the following key entities:
- Users and Roles
- Courses and Categories
- Chapters and Lessons
- User Enrollments and Progress
- Course Ratings

Refer to the database diagram in `resources/static/db_diagram` for detailed relationships.

## Getting Started

### Prerequisites
- JDK 17 or higher
- MySQL 8.0
- Maven 3.6+

### Test Data Generation
The project includes Python scripts in the `resources/Python_Scripts` folder to generate test data:

1. `generate_users.py`: Creates test users with different roles (Admin, Instructor, User)
2. `generate_courses_categories.py`: Generates sample courses and categories
3. `generate_chapters_lessons.py`: Creates chapters and lessons for courses
4. `generate_course_ratings.py`: Adds sample course ratings and reviews
5. `generate_user_enrollments.py`: Creates test course enrollments
6. `generate_lesson_progress.py`: Generates user progress data for lessons

#### Prerequisites for Python Scripts
- Python 3.x
- Required packages: mysql-connector-python, faker, bcrypt
- MySQL database connection

#### Installing Python Dependencies
```bash
pip install mysql-connector-python faker bcrypt
```

#### Running the Scripts
Execute the scripts in the following order:
1. Generate users first:
```bash
python generate_users.py
```
2. Generate courses and categories:
```bash
python generate_courses_categories.py
```
3. Generate chapters and lessons:
```bash
python generate_chapters_lessons.py
```
4. Generate course ratings:
```bash
python generate_course_ratings.py
```
5. Generate enrollments:
```bash
python generate_user_enrollments.py
```
6. Generate lesson progress:
```bash
python generate_lesson_progress.py
```

Each script will provide feedback about the data generation process and confirm successful completion.

### Setup Instructions

1. Clone the repository
```bash
git clone [repository-url]
```

2. Configure database
```properties
# Update src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/onlinecoursemanagement
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Run Flyway migrations
```bash
mvn flyway:migrate
```

4. Build the project
```bash
mvn clean install
```

5. Run the application
```bash
mvn spring-boot:run
```

### Default Credentials
- Admin: admin@gmail.com / adminadmin
- Test users and instructors are created with password: 123456

## API Documentation
- Access Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## Security
- JWT-based authentication
- Role-based access control
- Password encryption using BCrypt
- Input validation and sanitization

## Data Management
- Soft delete implementation
- Audit trails for critical operations
- Data validation at multiple levels

## Testing
- Unit tests for service and controller layers

## Additional Resources
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [JWT Authentication Guide](https://jwt.io/)
- [MySQL Documentation](https://dev.mysql.com/doc/)