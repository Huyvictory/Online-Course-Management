-- V7__Add_course_and_category_triggers.sql

-- Drop existing triggers if they exist
DROP TRIGGER IF EXISTS trg_course_archive_before_update;
DROP TRIGGER IF EXISTS trg_category_soft_delete_before_update;
DROP TRIGGER IF EXISTS trg_course_category_validation;

-- Create trigger for handling course archiving
CREATE TRIGGER trg_course_archive_before_update
    BEFORE UPDATE
    ON courses
    FOR EACH ROW
BEGIN
    -- When archiving a course
    IF NEW.status = 'ARCHIVED' AND OLD.status != 'ARCHIVED' THEN
        SET NEW.deleted_at = CURRENT_TIMESTAMP;
        SET NEW.updated_at = CURRENT_TIMESTAMP;
    -- When un-archiving a course
    ELSEIF NEW.status != 'ARCHIVED' AND OLD.status = 'ARCHIVED' THEN
        SET NEW.deleted_at = NULL;
        SET NEW.updated_at = CURRENT_TIMESTAMP;
END IF;
END;

-- Create trigger for handling category soft delete
CREATE TRIGGER trg_category_soft_delete_before_update
    BEFORE UPDATE
    ON categories
    FOR EACH ROW
BEGIN
    -- When soft deleting a category
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- Delete all course-category relationships for this category
    DELETE
    FROM course_categories
    WHERE category_id = OLD.id;

    SET NEW.updated_at = CURRENT_TIMESTAMP;
    -- When restoring a category
    ELSEIF NEW.deleted_at IS NULL AND OLD.deleted_at IS NOT NULL THEN
        SET NEW.updated_at = CURRENT_TIMESTAMP;
END IF;
END;

-- Create trigger to prevent invalid course-category relationships
CREATE TRIGGER trg_course_category_validation
    BEFORE INSERT
    ON course_categories
    FOR EACH ROW
BEGIN
    DECLARE course_status VARCHAR(20);
    DECLARE category_deleted_at DATETIME;

    -- Get course status
    SELECT status, deleted_at
    INTO course_status
    FROM courses
    WHERE id = NEW.course_id;

    -- Get category deleted_at
    SELECT deleted_at
    INTO category_deleted_at
    FROM categories
    WHERE id = NEW.category_id;

    -- Prevent adding relationships for archived courses or deleted categories
    IF course_status = 'ARCHIVED' OR category_deleted_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot create relationship: Course is archived or Category is deleted';
END IF;
END;

-- Update any existing inconsistent data
UPDATE courses
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'ARCHIVED'
  AND deleted_at IS NULL;

UPDATE courses
SET deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE status != 'ARCHIVED' AND deleted_at IS NOT NULL;

-- Remove any invalid course-category relationships
DELETE
cc FROM course_categories cc
INNER JOIN courses c ON cc.course_id = c.id
WHERE c.status = 'ARCHIVED';

DELETE
cc FROM course_categories cc
INNER JOIN categories cat ON cc.category_id = cat.id
WHERE cat.deleted_at IS NOT NULL;