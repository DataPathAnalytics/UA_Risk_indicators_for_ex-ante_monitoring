package com.datapath.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class IncorrectLoginOrPasswordException extends RuntimeException {
    public IncorrectLoginOrPasswordException(Exception e) {
        super("Incorrect login or password", e);
    }

    public IncorrectLoginOrPasswordException() {
        super("Incorrect login or password");
    }
}