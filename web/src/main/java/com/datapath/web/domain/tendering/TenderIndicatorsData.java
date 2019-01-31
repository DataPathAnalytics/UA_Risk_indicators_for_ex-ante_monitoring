package com.datapath.web.domain.tendering;

import com.datapath.web.domain.IndicatorInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TenderIndicatorsData {

    private String tenderId;
    @JsonProperty("id")
    private String tenderOuterId;
    private String procedureType;
    private String status;
    private TenderIndicators indicators;
    private TenderIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorsInfo;

}
