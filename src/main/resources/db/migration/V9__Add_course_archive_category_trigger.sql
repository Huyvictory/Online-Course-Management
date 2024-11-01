-- V9__Add_course_archive_category_trigger.sql

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trg_course_archive_status_update;

DELIMITER //

CREATE TRIGGER trg_course_archive_status_update
    AFTER UPDATE ON courses
    FOR EACH ROW
BEGIN
    -- When course is archived, remove all its category associations
    IF NEW.status = 'ARCHIVED' AND OLD.status != 'ARCHIVED' THEN
        -- Log the removal (optional, for debugging)
        -- INSERT INTO logs (message) VALUES (CONCAT('Removing categories for archived course: ', NEW.id));

    DELETE FROM course_categories
    WHERE course_id = NEW.id;
END IF;
END//

DELIMITER ;