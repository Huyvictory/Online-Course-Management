package com.online.course.management.project.exception.business.account;

public class WrongEmailPasswordException extends AccountException {
    public WrongEmailPasswordException(String message) {
        super(message);
    }
}
