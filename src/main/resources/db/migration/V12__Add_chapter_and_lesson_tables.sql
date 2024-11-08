-- Create chapters table
CREATE TABLE chapters
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    order_number INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME,
    CONSTRAINT fk_chapter_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Create lessons table
CREATE TABLE lessons
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id   BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content      TEXT,
    order_number INT          NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME,
    CONSTRAINT fk_lesson_chapter
        FOREIGN KEY (chapter_id)
            REFERENCES chapters (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Add indexes for better query performance
CREATE INDEX idx_chapters_course_id ON chapters (course_id);
CREATE INDEX idx_chapters_status ON chapters (status);
CREATE INDEX idx_chapters_order ON chapters (order_number);
CREATE INDEX idx_chapters_deleted_at ON chapters (deleted_at);

CREATE INDEX idx_lessons_chapter_id ON lessons (chapter_id);
CREATE INDEX idx_lessons_status ON lessons (status);
CREATE INDEX idx_lessons_type ON lessons (type);
CREATE INDEX idx_lessons_order ON lessons (order_number);
CREATE INDEX idx_lessons_deleted_at ON lessons (deleted_at);