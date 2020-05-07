package com.datapath.web.providers.impl;

import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.providers.IndicatorInfoProvider;
import com.datapath.web.services.impl.IndicatorInfoService;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Primary
@Component
public class InDatabaseIndicatorInfoProvider implements IndicatorInfoProvider {

    private Map<String, IndicatorInfo> indicatorsInfoList;
    private IndicatorInfoService indicatorInfoService;

    public InDatabaseIndicatorInfoProvider(IndicatorInfoService indicatorInfoService) {
        this.indicatorInfoService = indicatorInfoService;
        this.indicatorsInfoList = new HashMap<>();
    }

    @Scheduled(fixedRate = 100_000, initialDelay = 5_000)
    public void loadIndicatorsInfo() {
        indicatorsInfoList = indicatorInfoService.getIndicatorsInfoMap();
    }

    @Override
    public IndicatorInfo getIndicatorById(String indicatorId) {
        if (indicatorsInfoList.isEmpty()) loadIndicatorsInfo();
        IndicatorInfo indicatorInfo = indicatorsInfoList.get(indicatorId);
        if (indicatorInfo == null) {
            indicatorInfo = new IndicatorInfo();
            indicatorInfo.setIndicatorId(indicatorId);
        }
        return indicatorInfo;
    }

    @Override
    public IndicatorInfo getIndicatorByCode(String indicatorCode) {
        return null;
    }
}
