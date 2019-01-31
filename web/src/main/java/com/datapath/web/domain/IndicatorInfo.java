package com.datapath.web.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class IndicatorInfo {

    private String indicatorId;
    private String indicatorCode;
    private String indicatorName;
    private String indicatorShortName;
    private String indicatorRisk;
    private Double indicatorImpact;
    private String indicatorImpactType;
    private String indicatorTenderLotType;
    private Integer indicatorCheckingFrequency;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime lastCheckingDate;
}
