package com.datapath.druidintegration.model;

import lombok.Data;

import java.util.List;
@Data
public class DruidTenderIndicator {
    private String date;
    private String time;
    private String tenderOuterId;
    private String tenderId;
    private String status;
    private String procedureType;
    private String indicatorId;
    private String indicatorType;
    private Integer indicatorValue;
    private Long iterationId;
    private Double indicatorImpact;
    private List<String> lotIds;

}
