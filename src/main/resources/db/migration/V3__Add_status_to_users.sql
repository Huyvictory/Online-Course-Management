-- V3__Add_status_to_users.sql

-- Add status column if it doesn't exist
SET
@addStatus = (SELECT IF(
    NOT EXISTS(
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'users'
        AND COLUMN_NAME = 'status'
    ),
    'ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT "ACTIVE"',
    'SELECT 1'
));

PREPARE addStatusStmt FROM @addStatus;
EXECUTE addStatusStmt;
DEALLOCATE PREPARE addStatusStmt;

-- Update existing rows to set status if they are NULL
UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

-- Add constraint to ensure status is one of the allowed values
ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));