-- V2__Update_user_table.sql

-- Rename 'name' column to 'real_name' if it exists
SET @renameColumn = (SELECT IF(
                                    EXISTS(SELECT *
                                           FROM information_schema.COLUMNS
                                           WHERE TABLE_SCHEMA = DATABASE()
                                             AND TABLE_NAME = 'users'
                                             AND COLUMN_NAME = 'name'),
                                    'ALTER TABLE users CHANGE COLUMN name real_name VARCHAR(100)',
                                    'SELECT 1'
                            ));
PREPARE renameStmt FROM @renameColumn;
EXECUTE renameStmt;
DEALLOCATE PREPARE renameStmt;

-- Add 'real_name' column if it doesn't exist
SET @addColumn = (SELECT IF(
                                 NOT EXISTS(SELECT *
                                            FROM information_schema.COLUMNS
                                            WHERE TABLE_SCHEMA = DATABASE()
                                              AND TABLE_NAME = 'users'
                                              AND COLUMN_NAME = 'real_name'),
                                 'ALTER TABLE users ADD COLUMN real_name VARCHAR(100)',
                                 'SELECT 1'
                         ));
PREPARE addStmt FROM @addColumn;
EXECUTE addStmt;
DEALLOCATE PREPARE addStmt;

-- Add other new columns if they don't exist
SET @addCreatedAt = (SELECT IF(
                                    NOT EXISTS(SELECT *
                                               FROM information_schema.COLUMNS
                                               WHERE TABLE_SCHEMA = DATABASE()
                                                 AND TABLE_NAME = 'users'
                                                 AND COLUMN_NAME = 'created_at'),
                                    'ALTER TABLE users ADD COLUMN created_at DATETIME',
                                    'SELECT 1'
                            ));
PREPARE addCreatedAtStmt FROM @addCreatedAt;
EXECUTE addCreatedAtStmt;
DEALLOCATE PREPARE addCreatedAtStmt;

SET @addUpdatedAt = (SELECT IF(
                                    NOT EXISTS(SELECT *
                                               FROM information_schema.COLUMNS
                                               WHERE TABLE_SCHEMA = DATABASE()
                                                 AND TABLE_NAME = 'users'
                                                 AND COLUMN_NAME = 'updated_at'),
                                    'ALTER TABLE users ADD COLUMN updated_at DATETIME',
                                    'SELECT 1'
                            ));
PREPARE addUpdatedAtStmt FROM @addUpdatedAt;
EXECUTE addUpdatedAtStmt;
DEALLOCATE PREPARE addUpdatedAtStmt;

SET @addDeletedAt = (SELECT IF(
                                    NOT EXISTS(SELECT *
                                               FROM information_schema.COLUMNS
                                               WHERE TABLE_SCHEMA = DATABASE()
                                                 AND TABLE_NAME = 'users'
                                                 AND COLUMN_NAME = 'deleted_at'),
                                    'ALTER TABLE users ADD COLUMN deleted_at DATETIME',
                                    'SELECT 1'
                            ));
PREPARE addDeletedAtStmt FROM @addDeletedAt;
EXECUTE addDeletedAtStmt;
DEALLOCATE PREPARE addDeletedAtStmt;

-- Modify existing columns
ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    MODIFY COLUMN email VARCHAR(320) NOT NULL,
    MODIFY COLUMN password_hash VARCHAR(60) NOT NULL;

-- Add unique constraints if they don't exist
SET @addUsernameConstraint = (SELECT IF(
                                             NOT EXISTS(SELECT *
                                                        FROM information_schema.TABLE_CONSTRAINTS
                                                        WHERE CONSTRAINT_SCHEMA = DATABASE()
                                                          AND TABLE_NAME = 'users'
                                                          AND CONSTRAINT_NAME = 'uk_users_username'),
                                             'ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username)',
                                             'SELECT 1'
                                     ));
PREPARE usernameConstraintStmt FROM @addUsernameConstraint;
EXECUTE usernameConstraintStmt;
DEALLOCATE PREPARE usernameConstraintStmt;

SET @addEmailConstraint = (SELECT IF(
                                          NOT EXISTS(SELECT *
                                                     FROM information_schema.TABLE_CONSTRAINTS
                                                     WHERE CONSTRAINT_SCHEMA = DATABASE()
                                                       AND TABLE_NAME = 'users'
                                                       AND CONSTRAINT_NAME = 'uk_users_email'),
                                          'ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email)',
                                          'SELECT 1'
                                  ));
PREPARE emailConstraintStmt FROM @addEmailConstraint;
EXECUTE emailConstraintStmt;
DEALLOCATE PREPARE emailConstraintStmt;

-- Update existing rows to set created_at and updated_at if they are NULL
UPDATE users
SET created_at = NOW(),
    updated_at = NOW()
WHERE created_at IS NULL
   OR updated_at IS NULL;