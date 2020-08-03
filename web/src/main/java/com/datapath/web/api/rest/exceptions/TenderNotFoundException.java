package com.datapath.web.api.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TenderNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Tender with id %s not found";

    public TenderNotFoundException(String tenderId) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, tenderId));
    }

}
