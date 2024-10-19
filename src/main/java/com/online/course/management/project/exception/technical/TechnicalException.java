package com.online.course.management.project.exception.technical;

import com.online.course.management.project.exception.BaseException;

public class TechnicalException extends BaseException {
    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
