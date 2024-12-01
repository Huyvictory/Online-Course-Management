-- Drop existing trigger first
DROP TRIGGER IF EXISTS trg_update_course_status_after_progress;

DELIMITER $$

CREATE TRIGGER trg_update_course_status_after_progress
    AFTER UPDATE
    ON user_lesson_progress
    FOR EACH ROW
BEGIN
    DECLARE completed_lessons INT;
    DECLARE total_lessons INT;
    DECLARE in_progress_lessons INT;
    DECLARE course_status VARCHAR(20);
    DECLARE current_course_status VARCHAR(20);

    -- First check if the course is already dropped
    SELECT status
    INTO current_course_status
    FROM user_courses
    WHERE user_id = NEW.user_id
      AND course_id = NEW.course_id;

    -- Only proceed with status update if the course is not dropped
    IF current_course_status != 'DROPPED' THEN
        -- Get lesson counts
        SELECT COUNT(CASE WHEN ulp.status = 'COMPLETED' THEN 1 END),
               COUNT(CASE WHEN ulp.status = 'IN_PROGRESS' THEN 1 END),
               COUNT(DISTINCT l.id)
        INTO completed_lessons, in_progress_lessons, total_lessons
        FROM user_lesson_progress ulp
                 JOIN lessons l ON ulp.lesson_id = l.id
                 JOIN chapters ch ON l.chapter_id = ch.id
        WHERE ulp.user_id = NEW.user_id
          AND ulp.course_id = NEW.course_id
          AND l.deleted_at IS NULL
          AND ch.deleted_at IS NULL;

        -- Determine course status based on progress
        IF completed_lessons = total_lessons THEN
            SET course_status = 'COMPLETED';
        ELSEIF completed_lessons > 0 OR in_progress_lessons > 0 THEN
            SET course_status = 'IN_PROGRESS';
        ELSE
            SET course_status = 'ENROLLED';
        END IF;

        -- Update user_courses
        UPDATE user_courses
        SET status          = course_status,
            completion_date = IF(course_status = 'COMPLETED', CURRENT_TIMESTAMP, NULL)
        WHERE user_id = NEW.user_id
          AND course_id = NEW.course_id;
    END IF;
END$$

DELIMITER ;