-- V14__Add_trigger_archive_lessons.sql

DROP TRIGGER IF EXISTS trg_chapter_archive_lessons;

CREATE TRIGGER trg_chapter_archive_lessons
    AFTER UPDATE ON chapters
    FOR EACH ROW
BEGIN
    -- When chapter is archived
    IF NEW.status = 'ARCHIVED' AND OLD.status != 'ARCHIVED' THEN
        UPDATE lessons
        SET status = 'ARCHIVED',
            deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE chapter_id = NEW.id
          AND deleted_at IS NULL;

        -- When chapter is unarchived
    ELSEIF NEW.status != 'ARCHIVED' AND OLD.status = 'ARCHIVED' THEN
        UPDATE lessons
        SET status = 'DRAFT',
            deleted_at = NULL,
            updated_at = CURRENT_TIMESTAMP
        WHERE chapter_id = NEW.id
          AND status = 'ARCHIVED';
    END IF;
END;