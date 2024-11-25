-- Create user_courses table
CREATE TABLE user_courses
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    course_id       BIGINT      NOT NULL,
    enrollment_date DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completion_date DATETIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    CONSTRAINT fk_user_courses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_courses_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_user_courses_user_course UNIQUE (user_id, course_id),
    CONSTRAINT chk_user_courses_status CHECK (status IN ('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Create indexes for user_courses
CREATE INDEX idx_user_courses_user_id ON user_courses (user_id);
CREATE INDEX idx_user_courses_course_id ON user_courses (course_id);
CREATE INDEX idx_user_courses_status ON user_courses (status);
CREATE INDEX idx_user_courses_enrollment_date ON user_courses (enrollment_date);

-- Create user_lesson_progress table
CREATE TABLE user_lesson_progress
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    course_id        BIGINT      NOT NULL,
    chapter_id       BIGINT      NOT NULL,
    lesson_id        BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    last_accessed_at DATETIME,
    completion_date  DATETIME,
    CONSTRAINT fk_user_lesson_progress_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_lesson_progress_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_user_lesson_progress_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id),
    CONSTRAINT fk_user_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uk_user_lesson_progress_unique_progress UNIQUE (user_id, course_id, chapter_id, lesson_id),
    CONSTRAINT chk_user_lesson_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Create indexes for user_lesson_progress
CREATE INDEX idx_user_lesson_progress_user_id ON user_lesson_progress (user_id);
CREATE INDEX idx_user_lesson_progress_course_id ON user_lesson_progress (course_id);
CREATE INDEX idx_user_lesson_progress_chapter_id ON user_lesson_progress (chapter_id);
CREATE INDEX idx_user_lesson_progress_lesson_id ON user_lesson_progress (lesson_id);
CREATE INDEX idx_user_lesson_progress_status ON user_lesson_progress (status);
CREATE INDEX idx_user_lesson_progress_last_accessed ON user_lesson_progress (last_accessed_at);

-- Create course_ratings table
CREATE TABLE course_ratings
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    course_id   BIGINT   NOT NULL,
    rating      INT      NOT NULL,
    review_text TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME,
    CONSTRAINT fk_course_ratings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_course_ratings_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_course_ratings_user_course UNIQUE (user_id, course_id),
    CONSTRAINT chk_course_ratings_rating CHECK (rating >= 1 AND rating <= 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Create indexes for course_ratings
CREATE INDEX idx_course_ratings_user_id ON course_ratings (user_id);
CREATE INDEX idx_course_ratings_course_id ON course_ratings (course_id);
CREATE INDEX idx_course_ratings_rating ON course_ratings (rating);
CREATE INDEX idx_course_ratings_deleted_at ON course_ratings (deleted_at);