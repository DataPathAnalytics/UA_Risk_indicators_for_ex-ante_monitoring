package com.datapath.web.domain;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class DruidIndicator {

    private ZonedDateTime date;
    private String tenderOuterId;
    private String tenderId;
    private String indicatorId;
    private String indicatorType;
    private List<String> lotIds;
    private ZonedDateTime tenderModifiedDate;
    private ZonedDateTime statusChangedDate;
    private Byte indicatorValue;
    private Double indicatorImpact;
    private Long iterationId;
    private Long maxIteration;
    private String contractId;
    private String contractOuterId;
    private String procedureType;
    private String tenderStatus;

}
