package com.datapath.druidintegration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data()
@EqualsAndHashCode(callSuper = false)
public class DruidContractIndicator extends DruidIndicator {
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
