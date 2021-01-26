package com.datapath.indicatorsresolver.request;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RequestDTO {

    private String id;
    private ZonedDateTime dateCreated;
    private ZonedDateTime dateModified;
    private String tenderId;

}
