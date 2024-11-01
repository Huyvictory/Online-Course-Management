-- V8__Make_instructor_id_nullable.sql

-- First, drop the foreign key constraint if it exists
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_NAME = 'courses'
    AND COLUMN_NAME = 'instructor_id'
    AND REFERENCED_TABLE_NAME = 'users'
    AND CONSTRAINT_SCHEMA = DATABASE()
);

SET @drop_fk_sql = IF(
    @constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE courses DROP FOREIGN KEY ', @constraint_name),
    'SELECT 1'
);

PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now modify the instructor_id column to be nullable
ALTER TABLE courses
    MODIFY COLUMN instructor_id BIGINT NULL;

-- Re-add the foreign key constraint but allow NULL values
ALTER TABLE courses
    ADD CONSTRAINT fk_courses_instructor
        FOREIGN KEY (instructor_id) REFERENCES users(id);

-- Add an index on instructor_id if it doesn't exist already
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND INDEX_NAME = 'idx_courses_instructor'
);

SET @create_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_courses_instructor ON courses(instructor_id)',
    'SELECT 1'
);

PREPARE stmt FROM @create_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;