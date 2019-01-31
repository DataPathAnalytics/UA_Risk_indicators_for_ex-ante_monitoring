package com.datapath.integration.parsers.exceptions;

public class TenderValidationException extends RuntimeException {

    public TenderValidationException() {
        super();
    }

    public TenderValidationException(String message) {
        super(message);
    }

}
