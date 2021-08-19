package com.datapath.web.api.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ContractNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Contract with id %s not found";

    public ContractNotFoundException(String contractId) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, contractId));
    }

}
