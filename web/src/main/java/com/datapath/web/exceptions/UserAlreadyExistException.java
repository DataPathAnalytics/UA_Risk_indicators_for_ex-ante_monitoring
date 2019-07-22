package com.datapath.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(Exception e) {
        super("This email already registered", e);
    }

    public UserAlreadyExistException() {
        super("This email already registered");
    }
}