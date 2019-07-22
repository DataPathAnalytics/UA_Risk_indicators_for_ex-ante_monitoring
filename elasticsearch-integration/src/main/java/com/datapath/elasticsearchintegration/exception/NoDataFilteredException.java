package com.datapath.elasticsearchintegration.exception;

public class NoDataFilteredException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "No available procedures returned for applied filter.";

    public NoDataFilteredException() {
        super(DEFAULT_MESSAGE_TEMPLATE);
    }
}
