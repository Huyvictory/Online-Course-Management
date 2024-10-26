-- V6__Add_course_status_validation.sql

-- Drop existing constraint if it exists
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_NAME = 'courses'
    AND CONSTRAINT_TYPE = 'CHECK'
    AND CONSTRAINT_SCHEMA = DATABASE()
);

SET @drop_sql = IF(@constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE courses DROP CONSTRAINT ', @constraint_name),
    'SELECT 1'
);

PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add new constraint that enforces archived course logic
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_status_deleted
        CHECK (
            (status = 'ARCHIVED' AND deleted_at IS NOT NULL) OR
            (status != 'ARCHIVED' AND deleted_at IS NULL)
            );

-- Drop existing trigger if exists
DROP TRIGGER IF EXISTS trg_course_archive_before_update;

-- Create trigger for handling course archiving
CREATE TRIGGER trg_course_archive_before_update
    BEFORE UPDATE ON courses
    FOR EACH ROW
BEGIN
    IF NEW.status = 'ARCHIVED' AND OLD.status != 'ARCHIVED' THEN
        SET NEW.deleted_at = CURRENT_TIMESTAMP;
        SET NEW.updated_at = CURRENT_TIMESTAMP;
    ELSEIF NEW.status != 'ARCHIVED' AND OLD.status = 'ARCHIVED' THEN
        SET NEW.deleted_at = NULL;
        SET NEW.updated_at = CURRENT_TIMESTAMP;
END IF;
END;

-- Update any existing inconsistent data
UPDATE courses
SET deleted_at = CURRENT_TIMESTAMP
WHERE status = 'ARCHIVED' AND deleted_at IS NULL;

UPDATE courses
SET deleted_at = NULL
WHERE status != 'ARCHIVED' AND deleted_at IS NOT NULL;