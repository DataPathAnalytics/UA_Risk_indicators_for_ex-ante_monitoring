package com.datapath.indicatorsresolver.service;

import com.datapath.indicatorsresolver.service.checkIndicators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class IndicatorsResolver {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorsResolver.class);
    @Autowired
    private Risk_1_1_Extractor risk_1_1_extractor;
    @Autowired
    private Risk_1_2_1_Extractor risk_1_2_1_extractor;
    @Autowired
    private Risk_1_2_2_Extractor risk_1_2_2_extractor;
    @Autowired
    private Risk_1_3_1_Extractor risk_1_3_1_extractor;
    @Autowired
    private Risk_1_3_2_Extractor risk_1_3_2_extractor;
    @Autowired
    private Risk_1_3_3_Extractor risk_1_3_3_extractor;
    @Autowired
    private Risk_1_4_1_Extractor risk_1_4_1_extractor;
    @Autowired
    private Risk_1_4_2_Extractor risk_1_4_2_extractor;
    @Autowired
    private Risk_1_5_1_Extractor risk_1_5_1_extractor;
    @Autowired
    private Risk_1_5_2_Extractor risk_1_5_2_extractor;
    @Autowired
    private Risk_1_6_1_Extractor risk_1_6_1_extractor;
    @Autowired
    private Risk_1_8_1_Extractor risk_1_8_1_extractor;
    @Autowired
    private Risk_1_8_2_Extractor risk_1_8_2_extractor;
    @Autowired
    private Risk_1_8_2ExtractorUpdated risk_1_8_2_extractorUpdated;
    @Autowired
    private Risk_1_10_1_Extractor risk_1_10_1_extractor;
    @Autowired
    private Risk_1_10_2_Extractor risk_1_10_2_extractor;
    @Autowired
    private Risk_1_10_3_Extractor risk_1_10_3_extractor;
    @Autowired
    private Risk_1_10_4_Extractor risk_1_10_4_extractor;
    @Autowired
    private Risk_1_12_Extractor risk_1_12_extractor;
    @Autowired
    private Risk_1_13_1_Extractor risk_1_13_1_extractor;
    @Autowired
    private Risk_1_13_2_Extractor risk_1_13_2_extractor;
    @Autowired
    private Risk_1_13_3_Extractor risk_1_13_3_extractor;
    @Autowired
    private Risk_1_14_Extractor risk_1_14_extractor;
    @Autowired
    private Risk_2_1_Extractor risk_2_1_extractor;
    @Autowired
    private Risk_2_2_Extractor risk_2_2_extractor;
    @Autowired
    private Risk_2_2_1_Extractor risk_2_2_1_extractor;
    @Autowired
    private Risk_2_2_2_Extractor risk_2_2_2_extractor;
    @Autowired
    private Risk_2_3_Extractor risk_2_3_extractor;
    @Autowired
    private Risk_2_4_Extractor risk_2_4_extractor;
    @Autowired
    private Risk_2_5_Extractor risk_2_5_extractor;
    @Autowired
    private Risk_2_5_1_Extractor risk_2_5_1_extractor;
    @Autowired
    private Risk_2_5_2_Extractor risk_2_5_2_extractor;
    @Autowired
    private Risk_2_5_3_Extractor risk_2_5_3_extractor;
    @Autowired
    private Risk_2_6_Extractor risk_2_6_extractor;
    @Autowired
    private Risk_2_6_1_Extractor risk_2_6_1_extractor;
    @Autowired
    private Risk_2_6_2Extractor risk_2_6_2Extractor;
    @Autowired
    private Risk_2_6_3_Extractor risk_2_6_3_extractor;
    @Autowired
    private Risk_2_9_Extractor risk_2_9_extractor;
    @Autowired
    private Risk_2_10_Extractor risk_2_10_extractor;
    @Autowired
    private Risk_2_11_Extractor risk_2_11_extractor;
    @Autowired
    private Risk_2_12_Extractor risk_2_12_extractor;
    @Autowired
    private Risk_2_13_Extractor risk_2_13_extractor;
    @Autowired
    private Risk_2_13_1_Extractor risk_2_13_1_extractor;
    @Autowired
    private Risk_2_14_Extractor risk_2_14_extractor;
    @Autowired
    private Risk_2_14_1_Extractor risk_2_14_1_extractor;
    @Autowired
    private Risk_2_15_Extractor risk_2_15_extractor;
    @Autowired
    private Risk_2_15_1Extractor risk_2_15_1Extractor;
    @Autowired
    private Risk_2_16_2Extractor risk_2_16_2Extractor;
    @Autowired
    private Risk_2_17_2Extractor risk_2_17_2Extractor;
    private boolean tendersIndicatorsResolverAvailable;

    public IndicatorsResolver() {
        tendersIndicatorsResolverAvailable = true;
    }

    @Async
    public void checkRisk_1_1() {
        LOG.info("Start RISK_1_1 recalculation");
        risk_1_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_1 recalculation");
    }

    @Async
    public void checkRisk_1_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_1 recalculation");
        risk_1_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_1 recalculation");
    }

    @Async
    public void checkRisk_1_2_1() {
        LOG.info("Start RISK_1_2_1 recalculation");
        risk_1_2_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_2_1 recalculation");
    }

    @Async
    public void checkRisk_1_2_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_2_1 recalculation");
        risk_1_2_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_2_1 recalculation");
    }

    @Async
    public void checkRisk_1_2_2() {
        LOG.info("Start RISK_1_2_2 recalculation");
        risk_1_2_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_2_2 recalculation");
    }

    @Async
    public void checkRisk_1_2_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_2_2 recalculation");
        risk_1_2_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_2_2 recalculation");
    }

    @Async
    public void checkRisk_1_3_1() {
        LOG.info("Start RISK_1_3_1 recalculation");
        risk_1_3_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_1 recalculation");
    }

    @Async
    public void checkRisk_1_3_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_3_1 recalculation");
        risk_1_3_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_3_1 recalculation");
    }

    @Async
    public void checkRisk_1_3_2() {
        LOG.info("Start RISK_1_3_2 recalculation");
        risk_1_3_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_2 recalculation");
    }

    @Async
    public void checkRisk_1_3_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_3_2 recalculation");
        risk_1_3_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_3_2 recalculation");
    }

    @Async
    public void checkRisk_1_3_3() {
        LOG.info("Start RISK_1_3_3 recalculation");
        risk_1_3_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_3 recalculation");
    }

    @Async
    public void checkRisk_1_3_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_3_3 recalculation");
        risk_1_3_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_3_3 recalculation");
    }

    @Async
    public void checkRisk_1_4_1() {
        LOG.info("Start RISK_1_4_1 recalculation");
        risk_1_4_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_4_1 recalculation");
    }

    @Async
    public void checkRisk_1_4_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_4_1 recalculation");
        risk_1_4_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_4_1 recalculation");
    }

    @Async
    public void checkRisk_1_4_2() {
        LOG.info("Start RISK_1_4_2 recalculation");
        risk_1_4_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_4_2 recalculation");
    }

    @Async
    public void checkRisk_1_4_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_4_2 recalculation");
        risk_1_4_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_4_2 recalculation");
    }

    @Async
    public void checkRisk_1_5_1() {
        LOG.info("Start RISK_1_5_1 recalculation");
        risk_1_5_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_5_1 recalculation");
    }

    @Async
    public void checkRisk_1_5_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_5_1 recalculation");
        risk_1_5_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_5_1 recalculation");
    }

    @Async
    public void checkRisk_1_5_2() {
        LOG.info("Start RISK_1_5_2 recalculation");
        risk_1_5_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_5_2 recalculation");
    }

    @Async
    public void checkRisk_1_5_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_5_2 recalculation");
        risk_1_5_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_5_2 recalculation");
    }

    @Async
    public void checkRisk_1_6() {
        LOG.info("Start RISK_1_6 recalculation");
        risk_1_6_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_6 recalculation");
    }

    @Async
    public void checkRisk_1_6(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_6 recalculation");
        risk_1_6_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_6 recalculation");
    }

    @Async
    public void checkRisk_1_8_1() {
        LOG.info("Start RISK_1_8_1 recalculation");
        risk_1_8_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_8_1 recalculation");
    }

    @Async
    public void checkRisk_1_8_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_8_1 recalculation");
        risk_1_8_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_8_1 recalculation");
    }

    @Async
    public void checkRisk_1_8_2() {
        LOG.info("Start RISK_1_8_2 recalculation");
        risk_1_8_2_extractor.checkIndicator();
//        risk_1_8_2_extractorUpdated.extract();
        LOG.info("Finish RISK_1_8_2 recalculation");
    }

    @Async
    public void checkRisk_1_8_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_8_2 recalculation");
//        risk_1_8_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_8_2 recalculation");
    }

    @Async
    public void checkRisk_1_10_1() {
        LOG.info("Start RISK_1_10_1 recalculation");
        risk_1_10_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_1 recalculation");
    }

    @Async
    public void checkRisk_1_10_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_10_1 recalculation");
        risk_1_10_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_10_1 recalculation");
    }

    @Async
    public void checkRisk_1_10_2() {
        LOG.info("Start RISK_1_10_2 recalculation");
        risk_1_10_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk_1_10_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_10_2 recalculation");
        risk_1_10_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk1_10_3() {
        LOG.info("Start RISK_1_10_2 recalculation");
        risk_1_10_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk1_10_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_10_2 recalculation");
        risk_1_10_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk_1_10_4() {
        LOG.info("Start RISK_1_10_4 recalculation");
        risk_1_10_4_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_4 recalculation");
    }

    @Async
    public void checkRisk_1_10_4(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_10_4 recalculation");
        risk_1_10_4_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_10_4 recalculation");
    }

    @Async
    public void checkRisk_1_12() {
        LOG.info("Start RISK_1_12 recalculation");
        risk_1_12_extractor.checkIndicator();
        LOG.info("Finish RISK_1_12 recalculation");
    }

    @Async
    public void checkRisk_1_12(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_12 recalculation");
        risk_1_12_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_12 recalculation");
    }

    @Async
    public void checkRisk_1_13_1() {
        LOG.info("Start RISK_1_13_1 recalculation");
        risk_1_13_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_1 recalculation");
    }

    @Async
    public void checkRisk_1_13_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_13_1 recalculation");
        risk_1_13_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_13_1 recalculation");
    }

    @Async
    public void checkRisk_1_13_2() {
        LOG.info("Start RISK_1_13_2 recalculation");
        risk_1_13_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_2 recalculation");
    }

    @Async
    public void checkRisk_1_13_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_13_2 recalculation");
        risk_1_13_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_13_2 recalculation");
    }

    @Async
    public void checkRisk_1_13_3() {
        LOG.info("Start RISK_1_13_3 recalculation");
        risk_1_13_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_3 recalculation");
    }

    @Async
    public void checkRisk_1_13_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_13_2 recalculation");
        risk_1_13_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_13_3 recalculation");
    }


    @Async
    public void checkRisk_1_14() {
        LOG.info("Start RISK_1_14 recalculation");
        risk_1_14_extractor.checkIndicator();
        LOG.info("Finish RISK_1_14 recalculation");
    }

    @Async
    public void checkRisk_1_14(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_14 recalculation");
        risk_1_14_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_14 recalculation");
    }

    @Async
    public void checkRisk_2_1() {
        LOG.info("Start RISK_2_1 recalculation");
        risk_2_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_1 recalculation");
    }

    @Async
    public void checkRisk_2_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_1 recalculation");
        risk_2_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_1 recalculation");
    }

    @Async
    public void checkRisk_2_2() {
        LOG.info("Start RISK_2_2 recalculation");
        risk_2_2_extractor.checkIndicator();
        LOG.info("Finish RISK_2_2 recalculation");
    }

    @Async
    public void checkRisk_2_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_2 recalculation");
        risk_2_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_2 recalculation");
    }

    @Async
    public void checkRisk_2_2_1() {
        LOG.info("Start RISK_2_2_1 recalculation");
        risk_2_2_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_2_1 recalculation");
    }

    @Async
    public void checkRisk_2_2_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_2_1 recalculation");
        risk_2_2_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_2_1 recalculation");
    }

    @Async
    public void checkRisk_2_2_2() {
        LOG.info("Start RISK_2_2_2 recalculation");
        risk_2_2_2_extractor.checkIndicator();
        LOG.info("Finish RISK_2_2_2 recalculation");
    }

    @Async
    public void checkRisk_2_2_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_2_2 recalculation");
        risk_2_2_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_2_2 recalculation");
    }

    @Async
    public void checkRisk_2_3() {
        LOG.info("Start RISK_2_3 recalculation");
        risk_2_3_extractor.checkIndicator();
        LOG.info("Finish RISK_2_3 recalculation");
    }

    @Async
    public void checkRisk_2_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_3 recalculation");
        risk_2_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_3  recalculation");
    }

    @Async
    public void checkRisk_2_4() {
        LOG.info("Start RISK_2_4 recalculation");
        risk_2_4_extractor.checkIndicator();
        LOG.info("Finish RISK_2_4 recalculation");
    }

    @Async
    public void checkRisk_2_4(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_4 recalculation");
        risk_2_4_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_4  recalculation");
    }

    @Async
    public void checkRisk_2_5() {
        LOG.info("Start RISK_2_5 recalculation");
        risk_2_5_extractor.checkIndicator();
        LOG.info("Finish RISK_2_5 recalculation");
    }

    @Async
    public void checkRisk_2_5(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5 recalculation");
        risk_2_5_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_5 recalculation");
    }

    @Async
    public void checkRisk_2_5_1() {
        LOG.info("Start RISK_2_5_1 recalculation");
        risk_2_5_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_5_1  recalculation");
    }

    @Async
    public void checkRisk_2_5_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5_1 recalculation");
        risk_2_5_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_5_1  recalculation");
    }

    @Async
    public void checkRisk_2_5_2() {
        LOG.info("Start RISK_2_5_2 recalculation");
        risk_2_5_2_extractor.checkIndicator();
        LOG.info("Finish RISK_2_5_2  recalculation");
    }

    @Async
    public void checkRisk_2_5_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5_2 recalculation");
        risk_2_5_2_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_5_2  recalculation");
    }

    @Async
    public void checkRisk_2_5_3() {
        LOG.info("Start RISK_2_5_3 recalculation");
        risk_2_5_3_extractor.checkIndicator();
        LOG.info("Finish RISK_2_5_3  recalculation");
    }

    @Async
    public void checkRisk_2_5_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5_3 recalculation");
        risk_2_5_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_5_3  recalculation");
    }

    @Async
    public void checkRisk_2_6() {
        LOG.info("Start RISK_2_6 recalculation");
        risk_2_6_extractor.checkIndicator();
        LOG.info("Finish RISK_2_6 recalculation");
    }

    @Async
    public void checkRisk_2_6(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_6 recalculation");
        risk_2_6_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_6 recalculation");
    }

    @Async
    public void checkRisk_2_6_1() {
        LOG.info("Start RISK_2_6_1 recalculation");
        risk_2_6_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_6_1 recalculation");
    }

    @Async
    public void checkRisk_2_6_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_6_1 recalculation");
        risk_2_6_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_6_1 recalculation");
    }

    @Async
    public void checkRisk_2_6_2() {
        LOG.info("Start RISK_2_6_2 recalculation");
        risk_2_6_2Extractor.checkIndicator();
        LOG.info("Finish RISK_2_6_2 recalculation");
    }

    @Async
    public void checkRisk_2_6_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_6_2 recalculation");
        risk_2_6_2Extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_6_2 recalculation");
    }

    @Async
    public void checkRisk_2_6_3() {
        LOG.info("Start RISK_2_6_3 recalculation");
        risk_2_6_3_extractor.checkIndicator();
        LOG.info("Finish RISK_2_6_3 recalculation");
    }

    @Async
    public void checkRisk_2_6_3(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_6_3 recalculation");
        risk_2_6_3_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_6_3 recalculation");
    }

    @Async
    public void checkRisk_2_9() {
        LOG.info("Start RISK_2_9 recalculation");
        risk_2_9_extractor.checkIndicator();
        LOG.info("Finish RISK_2_9 recalculation");
    }

    @Async
    public void checkRisk_2_9(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_9 recalculation");
        risk_2_9_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_9 recalculation");
    }

    @Async
    public void checkRisk_2_10() {
        LOG.info("Start RISK_2_10 recalculation");
        risk_2_10_extractor.checkIndicator();
        LOG.info("Finish RISK_2_10 recalculation");
    }

    @Async
    public void checkRisk_2_10(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_10 recalculation");
        risk_2_10_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_10 recalculation");
    }

    @Async
    public void checkRisk_2_11() {
        LOG.info("Start RISK_2_11 recalculation");
        risk_2_11_extractor.checkIndicator();
        LOG.info("Finish RISK_2_11 recalculation");
    }

    @Async
    public void checkRisk_2_11(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_11 recalculation");
        risk_2_11_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_11 recalculation");
    }

    @Async
    public void checkRisk_2_12() {
        LOG.info("Start RISK_2_12 recalculation");
        risk_2_12_extractor.checkIndicator();
        LOG.info("Finish RISK_2_12 recalculation");
    }

    @Async
    public void checkRisk_2_12(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_12 recalculation");
        risk_2_12_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_12 recalculation");
    }

    @Async
    public void checkRisk_2_13() {
        LOG.info("Start RISK_2_13 recalculation");
        risk_2_13_extractor.checkIndicator();
        LOG.info("Finish RISK_2_13 recalculation");
    }

    @Async
    public void checkRisk_2_13(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_13 recalculation");
        risk_2_13_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_13 recalculation");
    }

    @Async
    public void checkRisk_2_13_1() {
        LOG.info("Start RISK_2_13_1 recalculation");
        risk_2_13_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_13_1 recalculation");
    }

    @Async
    public void checkRisk_2_13_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_13_1 recalculation");
        risk_2_13_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_13_1  recalculation");
    }

    @Async
    public void checkRisk_2_14() {
        LOG.info("Start RISK_2_14 recalculation");
        risk_2_14_extractor.checkIndicator();
        LOG.info("Finish RISK_2_14 recalculation");
    }

    @Async
    public void checkRisk_2_14(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_14 recalculation");
        risk_2_14_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_14 recalculation");
    }

    @Async
    public void checkRisk_2_14_1() {
        LOG.info("Start RISK_2_14_1 recalculation");
        risk_2_14_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_14_1 recalculation");
    }

    @Async
    public void checkRisk_2_14_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_14_1 recalculation");
        risk_2_14_1_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_14_1 recalculation");
    }

    @Async
    public void checkRisk_2_15() {
        LOG.info("Start RISK_2_15 recalculation");
        risk_2_15_extractor.checkIndicator();
        LOG.info("Finish RISK_2_15 recalculation");
    }

    @Async
    public void checkRisk_2_15(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_15 recalculation");
        risk_2_15_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_15 recalculation");
    }

    @Async
    public void checkRisk_2_15_1() {
        LOG.info("Start RISK_2_15_1recalculation");
        risk_2_15_1Extractor.checkIndicator();
        LOG.info("Finish RISK_2_15_1recalculation");
    }

    @Async
    public void checkRisk_2_15_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_15_1recalculation");
        risk_2_15_1Extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_15_1recalculation");
    }

    @Async
    public void checkRisk_2_16_2() {
        LOG.info("Start RISK_2_16_2recalculation");
        risk_2_16_2Extractor.checkIndicator();
        LOG.info("Finish RISK_2_16_2recalculation");
    }

    @Async
    public void checkRisk_2_16_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_16_2recalculation");
        risk_2_16_2Extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_16_2recalculation");
    }

    @Async
    public void checkRisk_2_17_2() {
        LOG.info("Start RISK_2_17_2recalculation");
        risk_2_17_2Extractor.checkIndicator();
        LOG.info("Finish RISK_2_17_2recalculation");
    }

    @Async
    public void checkRisk_2_17_2(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_17_2recalculation");
        risk_2_17_2Extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_17_2recalculation");
    }


    @Async
    public void updateUncheckedTendersIndicators() {
        LOG.info("Start daily recalculation");
        if (!tendersIndicatorsResolverAvailable) {
            LOG.info("Indicators resolver is not available");
            return;
        }
        try {
            LOG.info("Daily recalculation started");

            tendersIndicatorsResolverAvailable = false;
            ZonedDateTime monthAgo = ZonedDateTime.now().minus(Period.ofMonths(1)).with(LocalTime.now());
            ZonedDateTime twoDaysAgo = ZonedDateTime.now().minus(Period.ofDays(2)).withHour(0);

            List<Runnable> indicatorsToCheck = new ArrayList<>();
            indicatorsToCheck.add(() -> checkRisk_1_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_1_4_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_1_4_2(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_1_8_2(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_1_14(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2_2(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_9(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15_1(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_16_2(monthAgo));
            indicatorsToCheck.add(() -> checkRisk_2_17_2(monthAgo));

            indicatorsToCheck.add(() -> checkRisk_2_5_2(twoDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5_3(twoDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_2(twoDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_3(twoDaysAgo));

            ExecutorService executor = Executors.newFixedThreadPool(50);
            indicatorsToCheck.forEach(executor::execute);

            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            LOG.info("Daily recalculation finished");
        } catch (Exception ex) {
            LOG.error("Failed to execute recalculation", ex);
        }
        tendersIndicatorsResolverAvailable = true;
    }
}
