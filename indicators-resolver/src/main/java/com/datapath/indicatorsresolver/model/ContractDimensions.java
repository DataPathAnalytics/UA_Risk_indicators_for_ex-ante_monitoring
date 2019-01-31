package com.datapath.indicatorsresolver.model;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ContractDimensions {
    private Long druidCheckIteration;
    private String contractId;
    private String contractIdHr;
    private String tenderId;
    private String tenderIdHr;
    private String modifiedDate;
    private String status;
    private String procedureType;
    private ZonedDateTime dateCreated;

    public ContractDimensions(){}
    public ContractDimensions(String contractId){this.contractId = contractId;}

}

