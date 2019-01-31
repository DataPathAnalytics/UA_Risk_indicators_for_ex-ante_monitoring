package com.datapath.web.api.rest.exceptions;

public class TenderNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Tender with id %s not found";

    public TenderNotFoundException(String tenderId) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, tenderId));
    }

}
