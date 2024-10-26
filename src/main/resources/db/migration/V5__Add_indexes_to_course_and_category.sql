-- V5__Add_indexes_to_course_and_category.sql

-- Add index for instructor_id in courses table
CREATE INDEX idx_courses_instructor ON courses(instructor_id);

-- Add index for status in courses table
CREATE INDEX idx_courses_status ON courses(status);

-- Add index for deleted_at in courses table
CREATE INDEX idx_courses_deleted_at ON courses(deleted_at);

-- Add index for deleted_at in categories table
CREATE INDEX idx_categories_deleted_at ON categories(deleted_at);

-- Add index for name in categories table for faster lookups
CREATE INDEX idx_categories_name ON categories(name);