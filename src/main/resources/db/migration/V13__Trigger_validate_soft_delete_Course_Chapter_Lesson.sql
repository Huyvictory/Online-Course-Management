-- Add triggers for maintaining data consistency
DELIMITER //

-- Prevent modifying chapters of archived courses
CREATE TRIGGER trg_prevent_chapter_mod_archived_course
    BEFORE UPDATE
    ON chapters
    FOR EACH ROW
BEGIN
    DECLARE course_status VARCHAR(20);

    SELECT status
    INTO course_status
    FROM courses
    WHERE id = NEW.course_id;

    IF course_status = 'ARCHIVED' AND
       (NEW.status != 'ARCHIVED' OR
        NEW.title != OLD.title OR
        NEW.description != OLD.description OR
        NEW.order_number != OLD.order_number OR
        NEW.course_id != OLD.course_id)
    THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot modify chapter of an archived course';
    END IF;
END//

-- Prevent modifying lessons of archived chapters or courses
CREATE TRIGGER trg_prevent_lesson_mod_archived_chapter_course
    BEFORE
        UPDATE
    ON lessons
    FOR EACH ROW
BEGIN
    DECLARE chapter_status VARCHAR(20);
    DECLARE course_status VARCHAR(20);

    SELECT c.status, co.status
    INTO chapter_status, course_status
    FROM chapters c
             JOIN course co ON c.course_id = co.id
    WHERE c.id = NEW.chapter_id;

    IF chapter_status = "ARCHIVED" OR course_status = "ARCHIVED" AND
                                      (NEW.status != "ARCHIVED" OR
                                       NEW.type != OLD.type OR
                                       NEW.title != OLD.title OR
                                       NEW.content != OLD.content OR
                                       NEW.order_number != OLD.order_number OR
                                       NEW.chapter_id != OLD.chapter_id
                                          )
    THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot modify lesson of an archived chapter or course';
    END IF;
END//

-- Stored procedure for reordering chapters after bulk operation
CREATE PROCEDURE sp_reorder_chapters(IN p_course_id BIGINT)
BEGIN
    SET @rank := 0;

    -- Update orders of active chapters
    UPDATE chapters c
        JOIN (SELECT id, (@rank := @rank + 1) as new_order
              FROM chapters
              WHERE course_id = p_course_id
                AND deleted_at IS NULL
              ORDER BY order_number) as ranked ON c.id = ranked.id
    SET c.order_number = ranked.new_order
    WHERE c.course_id = p_course_id
      AND c.deleted_at IS NULL;

    -- Set order to 0 for all soft-deleted chapters
    UPDATE chapters
    SET order_number = 0
    WHERE course_id = p_course_id
      AND deleted_at IS NOT NULL;
END//

-- Stored procedure for reordering lessons after bulk operation
CREATE PROCEDURE sp_reorder_lessons(IN p_chapter_id BIGINT)
BEGIN
    SET @rank := 0;

    -- Update orders of active lessons
    UPDATE lessons l
        JOIN (SELECT id, (@rank := @rank + 1) as new_order
              FROM lessons
              WHERE chapter_id = p_chapter_id
                AND deleted_at IS NULL
              ORDER BY order_number) as ranked ON l.id = ranked.id
    SET l.order_number = ranked.new_order
    WHERE l.chapter_id = p_chapter_id
      AND l.deleted_at IS NULL;

    -- Set order to 0 for all soft-deleted lessons
    UPDATE lessons
    SET order_number = 0
    WHERE chapter_id = p_chapter_id
      AND deleted_at IS NOT NULL;
END//
