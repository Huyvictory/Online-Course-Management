package com.online.course.management.project.exception.business;

import com.online.course.management.project.exception.BaseException;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
