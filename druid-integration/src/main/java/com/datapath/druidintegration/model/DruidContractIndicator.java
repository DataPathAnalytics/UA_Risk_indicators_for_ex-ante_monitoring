package com.datapath.druidintegration.model;

import lombok.Data;

import java.util.List;

@Data
public class DruidContractIndicator {
    private String date;
    private String time;
    private String tenderOuterId;
    private String tenderId;
    private String status;
    private String procedureType;
    private String contractOuterId;
    private String contractId;
    private String indicatorId;
    private String indicatorType;
    private String contractModifiedDate;
    private Integer indicatorValue;
    private Long iterationId;
    private Double indicatorImpact;
    private List<String> lotIds;

}
