package com.online.course.management.project.exception.technical;

public class DatabaseException extends TechnicalException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
