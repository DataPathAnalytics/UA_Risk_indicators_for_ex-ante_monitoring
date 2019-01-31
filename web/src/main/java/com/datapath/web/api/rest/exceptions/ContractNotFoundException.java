package com.datapath.web.api.rest.exceptions;

public class ContractNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Contract with id %s not found";

    public ContractNotFoundException(String contractId) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, contractId));
    }

}
