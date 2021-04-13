package com.datapath.auditorsindicators.coordinator;

import com.datapath.auditorsindicators.analytictables.AnalyticTableService;
import com.datapath.indicatorsresolver.service.IndicatorsResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CoordinatorService {

    private IndicatorsResolver indicatorsResolver;
    private AnalyticTableService analyticTableService;

    @Autowired
    public void setIndicatorsResolver(IndicatorsResolver indicatorsResolver) {
        this.indicatorsResolver = indicatorsResolver;
    }

    @Autowired
    public void setAnalyticTableService(AnalyticTableService analyticTableService) {
        this.analyticTableService = analyticTableService;
    }

    @Scheduled(cron = "${coordinator.analytics-tables.daily.cron}")
    public void dailyAnalyticTablesCalculation() {
        log.info("Start daily analyticTables recalculation");
        analyticTableService.recalculate();
        log.info("Daily analyticTables recalculation finished");
    }

    @Scheduled(cron = "${coordinator.indicators.daily.cron}")
    public void dailyCalculation() {
        log.info("Start daily indicators recalculation");
        indicatorsResolver.updateUncheckedTendersIndicators();
        log.info("Daily indicators recalculation finished");
    }

}
