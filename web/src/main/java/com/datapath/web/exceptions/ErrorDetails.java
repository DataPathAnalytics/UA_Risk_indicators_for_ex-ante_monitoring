package com.datapath.web.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ErrorDetails {
    private Date timestamp;
    private String statusCode;
    private String message;
    private String details;
    private String exceptionName;

    public ErrorDetails(String statusCode, Date timestamp, String message, String details, String exceptionName) {
        super();
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.exceptionName = exceptionName;
    }
}
