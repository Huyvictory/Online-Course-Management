DELIMITER //

CREATE TRIGGER trg_update_insert_user_lesson_progress_after_enrollment
    AFTER INSERT
    ON user_courses
    FOR EACH ROW

BEGIN
    INSERT INTO user_lesson_progress
        (user_id, course_id, chapter_id, lesson_id, status)
    SELECT NEW.user_id,
           NEW.course_id,
           ch.id,
           l.id,
           'NOT_STARTED'
    FROM (SELECT id, chapter_id
          FROM lessons
          WHERE deleted_at IS NULL
            AND chapter_id IN (SELECT id
                               FROM chapters
                               WHERE course_id = NEW.course_id
                                 AND deleted_at IS NULL)) l
             JOIN chapters ch ON l.chapter_id = ch.id;
END;
/

DELIMITER ;