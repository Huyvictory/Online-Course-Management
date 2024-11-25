DELIMITER //

-- Trigger to ensure basic enrollment validation
CREATE TRIGGER trg_verify_enrollment_and_lesson
    BEFORE INSERT ON user_lesson_progress
    FOR EACH ROW
BEGIN
    DECLARE is_enrolled INT;
    DECLARE valid_lesson INT;
    DECLARE chapter_course_id BIGINT;

    -- Check enrollment
    SELECT COUNT(*) INTO is_enrolled
    FROM user_courses
    WHERE user_id = NEW.user_id
      AND course_id = NEW.course_id
      AND status != 'DROPPED';

    IF is_enrolled = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'User must be enrolled in the course before tracking progress';
    END IF;

    -- Verify lesson belongs to the specified chapter and course
    SELECT COUNT(*), c.course_id
    INTO valid_lesson, chapter_course_id
    FROM lessons l
             JOIN chapters c ON l.chapter_id = c.id
    WHERE l.id = NEW.lesson_id
      AND l.chapter_id = NEW.chapter_id
      AND c.course_id = NEW.course_id;

    IF valid_lesson = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid lesson-chapter-course relationship';
    END IF;
END//

-- Trigger to update course progress status
CREATE TRIGGER trg_update_course_status_after_progress
    AFTER UPDATE ON user_lesson_progress
    FOR EACH ROW
BEGIN
    DECLARE total_lessons INT;
    DECLARE completed_lessons INT;
    DECLARE in_progress_lessons INT;
    DECLARE course_status VARCHAR(20);

    -- Get total number of lessons in the course
    SELECT COUNT(DISTINCT l.id)
    INTO total_lessons
    FROM lessons l
             JOIN chapters c ON l.chapter_id = c.id
    WHERE c.course_id = NEW.course_id
      AND l.deleted_at IS NULL
      AND c.deleted_at IS NULL;

    -- Get number of completed and in-progress lessons
    SELECT
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END),
        COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END)
    INTO completed_lessons, in_progress_lessons
    FROM user_lesson_progress
    WHERE user_id = NEW.user_id
      AND course_id = NEW.course_id;

    -- Determine course status based on progress
    IF completed_lessons = total_lessons THEN
        SET course_status = 'COMPLETED';
    ELSEIF completed_lessons > 0 OR in_progress_lessons > 0 THEN
        SET course_status = 'IN_PROGRESS';
    ELSE
        SET course_status = 'ENROLLED';
    END IF;

    -- Update user_courses status
    UPDATE user_courses
    SET status = course_status,
        completion_date = IF(course_status = 'COMPLETED', CURRENT_TIMESTAMP, NULL)
    WHERE user_id = NEW.user_id
      AND course_id = NEW.course_id;
END//

-- Helper function to calculate chapter completion percentage
DELIMITER //

CREATE FUNCTION calculate_chapter_completion(
    p_user_id BIGINT,
    p_chapter_id BIGINT
)
    RETURNS DECIMAL(5,2)
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE total_lessons INT;
    DECLARE completed_lessons INT;

    -- Get total lessons in chapter
    SELECT COUNT(*)
    INTO total_lessons
    FROM lessons
    WHERE chapter_id = p_chapter_id
      AND deleted_at IS NULL;

    -- Get completed lessons in chapter
    SELECT COUNT(*)
    INTO completed_lessons
    FROM user_lesson_progress ulp
             JOIN lessons l ON ulp.lesson_id = l.id
    WHERE ulp.user_id = p_user_id
      AND l.chapter_id = p_chapter_id
      AND ulp.status = 'COMPLETED';

    -- Return percentage or 0 if no lessons
    RETURN IF(total_lessons > 0, (completed_lessons / total_lessons) * 100, 0);
END //

DELIMITER ;