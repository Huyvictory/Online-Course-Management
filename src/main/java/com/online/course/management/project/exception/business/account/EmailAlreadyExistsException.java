package com.online.course.management.project.exception.business.account;

public class EmailAlreadyExistsException extends AccountException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}