package com.datapath.indicatorsresolver.model;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class TenderDimensions {
    private Long druidCheckIteration;
    private String id;
    private String tenderId;
    private String procedureType;
    private String status;
    private ZonedDateTime modifiedDate;
    private ZonedDateTime date;
    private ZonedDateTime dateCreated;

    public TenderDimensions(){}
    public TenderDimensions(String id){this.id = id;}

}


