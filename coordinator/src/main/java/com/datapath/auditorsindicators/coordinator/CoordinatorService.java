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

    @Scheduled(cron = "${risk1_1.cron}")
    public void checkRisk_1_1() {
        indicatorsResolver.checkRisk_1_1();
    }

    @Scheduled(cron = "${risk1_2_1.cron}")
    public void checkRisk_1_2_1() {
        indicatorsResolver.checkRisk_1_2_1();
    }

    @Scheduled(cron = "${risk1_2_2.cron}")
    public void checkRisk_1_2_2() {
        indicatorsResolver.checkRisk_1_2_2();
    }

    @Scheduled(cron = "${risk1_3_1.cron}")
    public void checkRisk_1_3_1() {
        indicatorsResolver.checkRisk_1_3_1();
    }

    @Scheduled(cron = "${risk1_3_2.cron}")
    public void checkRisk_1_3_2() {
        indicatorsResolver.checkRisk_1_3_2();
    }

    @Scheduled(cron = "${risk1_3_3.cron}")
    public void checkRisk_1_3_3() {
        indicatorsResolver.checkRisk_1_3_3();
    }

    @Scheduled(cron = "${risk1_4_1.cron}")
    public void checkRisk_1_4_1() {
        indicatorsResolver.checkRisk_1_4_1();
    }

    @Scheduled(cron = "${risk1_4_2.cron}")
    public void checkRisk_1_4_2() {
        indicatorsResolver.checkRisk_1_4_2();
    }

    @Scheduled(cron = "${risk1_5_1.cron}")
    public void checkRisk_1_5_1() {
        indicatorsResolver.checkRisk_1_5_1();
    }

    @Scheduled(cron = "${risk1_5_2.cron}")
    public void checkRisk_1_5_2() {
        indicatorsResolver.checkRisk_1_5_2();
    }

    @Scheduled(cron = "${risk1_6.cron}")
    public void checkRisk_1_6() {
        indicatorsResolver.checkRisk_1_6();
    }

    @Scheduled(cron = "${risk1_8_1.cron}")
    public void checkRisk_1_8_1() {
        indicatorsResolver.checkRisk_1_8_1();
    }

    @Scheduled(cron = "${risk1_8_2.cron}")
    public void checkRisk_1_8_2() {
        indicatorsResolver.checkRisk_1_8_2();
    }

    @Scheduled(cron = "${risk1_10_1.cron}")
    public void checkRisk_1_10_1() {
        indicatorsResolver.checkRisk_1_10_1();
    }

    @Scheduled(cron = "${risk1_10_2.cron}")
    public void checkRisk_1_10_2() {
        indicatorsResolver.checkRisk_1_10_2();
    }

    @Scheduled(cron = "${risk1_10_3.cron}")
    public void checkRisk1_10_3() {
        indicatorsResolver.checkRisk1_10_3();
    }

    @Scheduled(cron = "${risk1_10_4.cron}")
    public void checkRisk_1_10_4() {
        indicatorsResolver.checkRisk_1_10_4();
    }

    @Scheduled(cron = "${risk1_12.cron}")
    public void checkRisk_1_12() {
        indicatorsResolver.checkRisk_1_12();
    }

    @Scheduled(cron = "${risk1_13_1.cron}")
    public void checkRisk_1_13_1() {
        indicatorsResolver.checkRisk_1_13_1();
    }

    @Scheduled(cron = "${risk1_13_2.cron}")
    public void checkRisk_1_13_2() {
        indicatorsResolver.checkRisk_1_13_2();
    }

    @Scheduled(cron = "${risk1_13_3.cron}")
    public void checkRisk_1_13_3() {
        indicatorsResolver.checkRisk_1_13_3();
    }

    @Scheduled(cron = "${risk1_14.cron}")
    public void checkRisk_1_14() {
        indicatorsResolver.checkRisk_1_14();
    }

    @Scheduled(cron = "${risk2_1.cron}")
    public void checkRisk_2_1() {
        indicatorsResolver.checkRisk_2_1();
    }

    @Scheduled(cron = "${risk2_2.cron}")
    public void checkRisk_2_2() {
        indicatorsResolver.checkRisk_2_2();
    }

    @Scheduled(cron = "${risk2_2_1.cron}")
    public void checkRisk_2_2_1() {
        indicatorsResolver.checkRisk_2_2_1();
    }

    @Scheduled(cron = "${risk2_2_2.cron}")
    public void checkRisk_2_2_2() {
        indicatorsResolver.checkRisk_2_2_2();
    }

    @Scheduled(cron = "${risk2_3.cron}")
    public void checkRisk_2_3() {
        indicatorsResolver.checkRisk_2_3();
    }

    @Scheduled(cron = "${risk2_4.cron}")
    public void checkRisk_2_4() {
        indicatorsResolver.checkRisk_2_4();
    }

    @Scheduled(cron = "${risk2_5.cron}")
    public void checkRisk_2_5() {
        indicatorsResolver.checkRisk_2_5();
    }

    @Scheduled(cron = "${risk2_5_1.cron}")
    public void checkRisk_2_5_1() {
        indicatorsResolver.checkRisk_2_5_1();
    }

    @Scheduled(cron = "${risk2_5_2.cron}")
    public void checkRisk_2_5_2() {
        indicatorsResolver.checkRisk_2_5_2();
    }

    @Scheduled(cron = "${risk2_5_3.cron}")
    public void checkRisk_2_5_3() {
        indicatorsResolver.checkRisk_2_5_3();
    }

    @Scheduled(cron = "${risk2_6.cron}")
    public void checkRisk_2_6() {
        indicatorsResolver.checkRisk_2_6();
    }

    @Scheduled(cron = "${risk2_6_1.cron}")
    public void checkRisk_2_6_1() {
        indicatorsResolver.checkRisk_2_6_1();
    }

    @Scheduled(cron = "${risk2_6_2.cron}")
    public void checkRisk_2_6_2() {
        indicatorsResolver.checkRisk_2_6_2();
    }

    @Scheduled(cron = "${risk2_6_3.cron}")
    public void checkRisk_2_6_3() {
        indicatorsResolver.checkRisk_2_6_3();
    }

    @Scheduled(cron = "${risk2_9.cron}")
    public void checkRisk_2_9() {
        indicatorsResolver.checkRisk_2_9();
    }

    @Scheduled(cron = "${risk2_10.cron}")
    public void checkRisk_2_10() {
        indicatorsResolver.checkRisk_2_10();
    }

    @Scheduled(cron = "${risk2_11.cron}")
    public void checkRisk_2_11() {
        indicatorsResolver.checkRisk_2_11();
    }

    @Scheduled(cron = "${risk2_12.cron}")
    public void checkRisk_2_12() {
        indicatorsResolver.checkRisk_2_12();
    }

    @Scheduled(cron = "${risk2_13.cron}")
    public void checkRisk_2_13() {
        indicatorsResolver.checkRisk_2_13();
    }

    @Scheduled(cron = "${risk2_13_1.cron}")
    public void checkRisk_2_13_1() {
        indicatorsResolver.checkRisk_2_13_1();
    }

    @Scheduled(cron = "${risk2_14.cron}")
    public void checkRisk_2_14() {
        indicatorsResolver.checkRisk_2_14();
    }

    @Scheduled(cron = "${risk2_14_1.cron}")
    public void checkRisk_2_14_1() {
        indicatorsResolver.checkRisk_2_14_1();
    }

    @Scheduled(cron = "${risk2_15.cron}")
    public void updateRisk_2_15() {
        indicatorsResolver.checkRisk_2_15();
    }

    @Scheduled(cron = "${risk2_15_1.cron}")
    public void risk2_15_1() {
        indicatorsResolver.checkRisk_2_15_1();
    }

    @Scheduled(cron = "${risk2_16_2.cron}")
    public void risk2_16_2() {
        indicatorsResolver.checkRisk_2_16_2();
    }

    @Scheduled(cron = "${risk2_17_2.cron}")
    public void risk2_17_2() {
        indicatorsResolver.checkRisk_2_17_2();
    }

}