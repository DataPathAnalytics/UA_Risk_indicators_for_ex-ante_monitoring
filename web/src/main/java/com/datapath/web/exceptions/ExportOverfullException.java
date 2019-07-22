package com.datapath.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ExportOverfullException extends RuntimeException {
    public ExportOverfullException(Exception e) {
        super("Too many items for bucket. Check less or delete something. Max export size: 10000 items.", e);
    }

    public ExportOverfullException(int maxCount) {
        super("Too many items for export to Excel. Check less or delete something. Max export size: " + maxCount + " items.");
    }
}