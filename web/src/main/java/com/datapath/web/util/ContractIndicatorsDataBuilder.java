package com.datapath.web.util;

import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.contracting.ContractIndicators;
import com.datapath.web.domain.contracting.ContractIndicatorsData;
import com.datapath.web.domain.contracting.ContractIndicatorsSummary;

import java.util.List;

public class ContractIndicatorsDataBuilder {

    private String tenderId;
    private String contractId;
    private String contractOuterId;
    private String procedureType;
    private String tenderStatus;
    private ContractIndicators indicators;
    private ContractIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorInfo;

    private ContractIndicatorsDataBuilder() {}

    public static ContractIndicatorsDataBuilder create() {
        return new ContractIndicatorsDataBuilder();
    }

    public ContractIndicatorsData build() {
        ContractIndicatorsData data = new ContractIndicatorsData();
        data.setTenderId(tenderId);
        data.setContractId(contractId);
        data.setContractOuterId(contractOuterId);
        data.setProcedureType(procedureType);
        data.setIndicators(indicators);
        data.setIndicatorsSummary(indicatorsSummary);
        data.setIndicatorsInfo(indicatorInfo);
        return data;
    }

    public ContractIndicatorsDataBuilder setTenderId(String tenderId) {
        this.tenderId = tenderId;
        return this;
    }

    public ContractIndicatorsDataBuilder setContractId(String contractId) {
        this.contractId = contractId;
        return this;
    }

    public ContractIndicatorsDataBuilder setContractOuterId(String contractOuterId) {
        this.contractOuterId = contractOuterId;
        return this;
    }

    public ContractIndicatorsDataBuilder setProcedureType(String procedureType) {
        this.procedureType = procedureType;
        return this;
    }

    public ContractIndicatorsDataBuilder setTenderStatus(String tenderStatus) {
        this.tenderStatus = tenderStatus;
        return this;
    }

    public ContractIndicatorsDataBuilder setIndicators(ContractIndicators indicators) {
        this.indicators = indicators;
        return this;
    }

    public ContractIndicatorsDataBuilder setIndicatorsSummary(ContractIndicatorsSummary indicatorsSummary) {
        this.indicatorsSummary = indicatorsSummary;
        return this;
    }

    public ContractIndicatorsDataBuilder setIndicatorInfo(List<IndicatorInfo> indicatorInfo) {
        this.indicatorInfo = indicatorInfo;
        return this;
    }

}
