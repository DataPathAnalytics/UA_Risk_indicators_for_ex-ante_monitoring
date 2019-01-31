package com.datapath.druidintegration.model.druid.response.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class Event {
    private String date;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> lotIds;
    private String timestamp;
    private String tenderId;
    private String contractId;
    private String indicatorId;
    private String tenderOuterId;
    private String contractOuterId;
    private String indicatorType;
    private String procedureType;
    private String status;
    private Long iterationId;
    private Integer indicatorValue;
    private Double indicatorImpact;

    // Aggregations

    private Long maxIteration;
    private Double tenderScore;
    private String tmax;

}
