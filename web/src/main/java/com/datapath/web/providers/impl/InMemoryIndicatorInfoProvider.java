package com.datapath.web.providers.impl;

import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.common.IndicatorRisk;
import com.datapath.web.domain.common.IndicatorType;
import com.datapath.web.providers.IndicatorInfoProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryIndicatorInfoProvider implements IndicatorInfoProvider {

    private Map<String, IndicatorInfo> indicatorsInfoList;

    public InMemoryIndicatorInfoProvider() {
        indicatorsInfoList = new HashMap<>();

        IndicatorInfo indicatorInfoT10 = new IndicatorInfo();
        indicatorInfoT10.setIndicatorId("T10");
        indicatorInfoT10.setIndicatorName("Постачальник виступає постачальником тільки для цього Замовника");
        indicatorInfoT10.setIndicatorImpact(0.5);
        indicatorInfoT10.setIndicatorTenderLotType(IndicatorType.LOT.toString());
        indicatorInfoT10.setIndicatorRisk(IndicatorRisk.UNFAIR_COMPETITIVITY_BETWEEN_BIDDERS.toString());

        IndicatorInfo indicatorInfoT47 = new IndicatorInfo();
        indicatorInfoT47.setIndicatorId("T47");
        indicatorInfoT47.setIndicatorName("Число позицій (айтемів) у лоті аномально велике для даного предмету закупівлі (CPV (2))");
        indicatorInfoT47.setIndicatorImpact(0.3);
        indicatorInfoT47.setIndicatorTenderLotType(IndicatorType.LOT.toString());
        indicatorInfoT47.setIndicatorRisk(IndicatorRisk.DISCRIMINATION_OF_BIDDERS.toString());

        IndicatorInfo indicatorInfoT81 = new IndicatorInfo();
        indicatorInfoT81.setIndicatorId("T81");
        indicatorInfoT81.setIndicatorName("Наявність пов'язаних Учасників у конкурентних процедурах Замовника (взаємна участь більше 70%)");
        indicatorInfoT81.setIndicatorImpact(0.5);
        indicatorInfoT81.setIndicatorTenderLotType(IndicatorType.LOT.toString());
        indicatorInfoT81.setIndicatorRisk(IndicatorRisk.UNFAIR_COMPETITIVITY_BETWEEN_BIDDERS.toString());

        indicatorsInfoList.put(indicatorInfoT10.getIndicatorId(), indicatorInfoT10);
        indicatorsInfoList.put(indicatorInfoT47.getIndicatorId(), indicatorInfoT47);
        indicatorsInfoList.put(indicatorInfoT81.getIndicatorId(), indicatorInfoT81);
    }

    @Override
    public IndicatorInfo getIndicatorById(String indicatorId) {
        return indicatorsInfoList.get(indicatorId);
    }

    @Override
    public IndicatorInfo getIndicatorByCode(String indicatorCode) {
        return null;
    }
}
