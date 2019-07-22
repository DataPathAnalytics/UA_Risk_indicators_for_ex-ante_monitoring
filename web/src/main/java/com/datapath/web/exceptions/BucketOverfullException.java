package com.datapath.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class BucketOverfullException extends RuntimeException {
    public BucketOverfullException(Exception e) {
        super("Too many items for bucket. Check less or delete something. Max bucket size: 10000 items.", e);
    }

    public BucketOverfullException(int maxCount) {
        super("Too many items for bucket. Check less or delete something. Max bucket size: " + maxCount + " items.");
    }
}