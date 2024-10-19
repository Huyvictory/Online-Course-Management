package com.online.course.management.project.exception.technical;

public class ExternalServiceException extends TechnicalException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
