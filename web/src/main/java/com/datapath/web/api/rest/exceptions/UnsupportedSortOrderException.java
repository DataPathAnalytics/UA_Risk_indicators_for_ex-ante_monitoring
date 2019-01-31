package com.datapath.web.api.rest.exceptions;

public class UnsupportedSortOrderException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Sort order %s is not supported";

    public UnsupportedSortOrderException(String order) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, order));
    }

}
