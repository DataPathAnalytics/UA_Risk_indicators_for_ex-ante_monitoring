package com.datapath.web.exceptions;

public class IndicatorNotFountException extends Exception {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Indicator with id (%s) not found.";

    public IndicatorNotFountException(String indicatorId) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, indicatorId));
    }
}
