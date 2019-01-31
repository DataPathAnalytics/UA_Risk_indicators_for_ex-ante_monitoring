package com.datapath.web.domain.contracting;

import com.datapath.web.domain.IndicatorInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ContractIndicatorsData {

    private String tenderId;
    @JsonProperty("id")
    private String contractOuterId;
    private String contractId;
    private String procedureType;
    private ContractIndicators indicators;
    private ContractIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorsInfo;

}
