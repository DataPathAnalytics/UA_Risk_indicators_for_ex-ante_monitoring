package com.datapath.web.mappers;

import com.datapath.persistence.entities.Indicator;
import com.datapath.web.domain.IndicatorInfo;

public class IndicatorInfoMapper {

    public static IndicatorInfo mapToIndicatorInfo(Indicator indicator) {
        IndicatorInfo indicatorInfo = new IndicatorInfo();
        indicatorInfo.setIndicatorId(indicator.getId());
        indicatorInfo.setIndicatorCode(indicator.getCode());
        indicatorInfo.setIndicatorName(indicator.getName());
        indicatorInfo.setIndicatorShortName(indicator.getShortName());
        indicatorInfo.setIndicatorTenderLotType(indicator.getTenderLotType());
        indicatorInfo.setIndicatorImpact(indicator.getImpact());
        indicatorInfo.setIndicatorRisk(indicator.getRisk());
        indicatorInfo.setIndicatorImpactType(indicator.getImpactType());
        indicatorInfo.setLastCheckingDate(indicator.getDateChecked());
        indicatorInfo.setIndicatorCheckingFrequency(indicator.getCheckingFrequency());
        return indicatorInfo;
    }
}
