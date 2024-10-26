-- V4__Add_course_and_category_tables.sql

-- Drop any tables that might have been partially created
DROP TABLE IF EXISTS course_categories;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS categories;

-- Create Categories table if not exists
SET @createCategories = (SELECT IF(
                                        NOT EXISTS(SELECT *
                                                   FROM information_schema.TABLES
                                                   WHERE TABLE_SCHEMA = DATABASE()
                                                     AND TABLE_NAME = 'categories'),
                                        'CREATE TABLE categories (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            name VARCHAR(100) NOT NULL UNIQUE,
                                            created_at DATETIME NOT NULL,
                                            updated_at DATETIME NOT NULL,
                                            deleted_at DATETIME
                                        )',
                                        'SELECT 1'
                                ));

PREPARE createCategoriesStmt FROM @createCategories;
EXECUTE createCategoriesStmt;
DEALLOCATE PREPARE createCategoriesStmt;

-- Create Courses table if not exists
SET @createCourses = (SELECT IF(
                                     NOT EXISTS(SELECT *
                                                FROM information_schema.TABLES
                                                WHERE TABLE_SCHEMA = DATABASE()
                                                  AND TABLE_NAME = 'courses'),
                                     'CREATE TABLE courses (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         title VARCHAR(255) NOT NULL,
                                         description TEXT,
                                         instructor_id BIGINT NOT NULL,
                                         status VARCHAR(20) NOT NULL,
                                         created_at DATETIME NOT NULL,
                                         updated_at DATETIME NOT NULL,
                                         deleted_at DATETIME,
                                         FOREIGN KEY (instructor_id) REFERENCES users(id),
                                         CONSTRAINT chk_courses_status CHECK (status IN ("DRAFT", "PUBLISHED", "ARCHIVED"))
                                     )',
                                     'SELECT 1'
                             ));

PREPARE createCoursesStmt FROM @createCourses;
EXECUTE createCoursesStmt;
DEALLOCATE PREPARE createCoursesStmt;

-- Create Course_Categories junction table if not exists
SET @createCourseCategories = (SELECT IF(
                                              NOT EXISTS(SELECT *
                                                         FROM information_schema.TABLES
                                                         WHERE TABLE_SCHEMA = DATABASE()
                                                           AND TABLE_NAME = 'course_categories'),
                                              'CREATE TABLE course_categories (
                                                  course_id BIGINT NOT NULL,
                                                  category_id BIGINT NOT NULL,
                                                  PRIMARY KEY (course_id, category_id),
                                                  FOREIGN KEY (course_id) REFERENCES courses(id),
                                                  FOREIGN KEY (category_id) REFERENCES categories(id)
                                              )',
                                              'SELECT 1'
                                      ));

PREPARE createCourseCategoriesStmt FROM @createCourseCategories;
EXECUTE createCourseCategoriesStmt;
DEALLOCATE PREPARE createCourseCategoriesStmt;

