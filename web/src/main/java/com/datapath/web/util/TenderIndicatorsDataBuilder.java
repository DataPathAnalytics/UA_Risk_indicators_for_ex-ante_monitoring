package com.datapath.web.util;

import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.tendering.TenderIndicators;
import com.datapath.web.domain.tendering.TenderIndicatorsData;
import com.datapath.web.domain.tendering.TenderIndicatorsSummary;

import java.util.List;

public class TenderIndicatorsDataBuilder {

    private String tenderId;
    private String tenderOuterId;
    private String procedureType;
    private String status;
    private TenderIndicators indicators;
    private TenderIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorInfo;

    private TenderIndicatorsDataBuilder() {}

    public static TenderIndicatorsDataBuilder create() {
        return new TenderIndicatorsDataBuilder();
    }

    public TenderIndicatorsData build() {
        TenderIndicatorsData data = new TenderIndicatorsData();
        data.setTenderId(tenderId);
        data.setTenderOuterId(tenderOuterId);
        data.setProcedureType(procedureType);
        data.setStatus(status);
        data.setIndicators(indicators);
        data.setIndicatorsSummary(indicatorsSummary);
        data.setIndicatorsInfo(indicatorInfo);
        return data;
    }

    public TenderIndicatorsDataBuilder setTenderId(String tenderId) {
        this.tenderId = tenderId;
        return this;
    }

    public TenderIndicatorsDataBuilder setTenderOuterId(String outerTenderId) {
        this.tenderOuterId = outerTenderId;
        return this;
    }

    public TenderIndicatorsDataBuilder setProcedureType(String procedureType) {
        this.procedureType = procedureType;
        return this;
    }

    public TenderIndicatorsDataBuilder setStatus(String status) {
        this.status = status;
        return this;
    }

    public TenderIndicatorsDataBuilder setIndicators(TenderIndicators indicators) {
        this.indicators = indicators;
        return this;
    }

    public TenderIndicatorsDataBuilder setIndicatorsSummary(TenderIndicatorsSummary indicatorsSummary) {
        this.indicatorsSummary = indicatorsSummary;
        return this;
    }

    public TenderIndicatorsDataBuilder setIndicatorInfo(List<IndicatorInfo> indicatorInfo) {
        this.indicatorInfo = indicatorInfo;
        return this;
    }

}
