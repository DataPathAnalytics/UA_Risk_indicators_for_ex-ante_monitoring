package com.datapath.indicatorsresolver.service;

import com.datapath.indicatorsresolver.service.checkIndicators.*;
import com.datapath.indicatorsresolver.service.checkIndicators.handler.*;
import com.datapath.persistence.service.ConfigurationDaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.LocalTime.MIDNIGHT;


@Service
public class IndicatorsResolver {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorsResolver.class);
    public static final long THREE_WEEK_DAYS = 21L;

    @Autowired
    private ConfigurationDaoService configurationService;
    @Autowired
    private Risk_1_1_AprilExtractor risk_1_1_April_extractor;
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
    private Risk_1_6_Extractor risk_1_6_extractor;
    @Autowired
    private Risk_1_8_1_Extractor risk_1_8_1_extractor;
    @Autowired
    private Risk_1_8_2_Extractor risk_1_8_2_extractor;
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
    private Risk_2_5_1_AprilExtractor risk_2_5_1_aprilExtractor;
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
    @Autowired
    private Risk_1_10_Handler risk_1_10_handler;
    @Autowired
    private Risk_1_21_AprilExtractor risk_1_21_aprilExtractor;
    @Autowired
    private Risk_1_21_1_AprilExtractor risk_1_21_1_aprilExtractor;
    @Autowired
    private Risk_2_5_AprilExtractor risk_2_5_aprilExtractor;
    @Autowired
    private Risk_2_6_AprilExtractor risk_2_6_aprilExtractor;
    @Autowired
    private Risk_2_3_AprilExtractor risk_2_3_aprilExtractor;
    @Autowired
    private Risk_2_7_AprilExtractor risk_2_7_aprilExtractor;
    @Autowired
    private Risk_2_8_AprilExtractor risk_2_8_aprilExtractor;
    @Autowired
    private Risk_2_6_1_AprilExtractor risk_2_6_1_aprilExtractor;
    @Autowired
    private Risk_2_7_1_AprilExtractor risk_2_7_1_aprilExtractor;
    @Autowired
    private Risk_2_8_1_AprilExtractor risk_2_8_1_aprilExtractor;
    @Autowired
    private Risk_2_9AprilExtractor risk_2_9_aprilExtractor;
    @Autowired
    private Risk_1_7_1_AprilExtractor risk_1_7_1_aprilExtractor;
    @Autowired
    private Risk_1_13_Handler risk_1_13_handler;
    @Autowired
    private Risk_2_15_1_AprilExtractor risk_2_15_1_aprilExtractor;
    @Autowired
    private Risk_1_2_AprilExtractor risk_1_2_aprilExtractor;
    @Autowired
    private Risk_1_2_1_AprilExtractor risk_1_2_1_aprilExtractor;
    @Autowired
    private Risk_1_11_AprilExtractor risk_1_11_aprilExtractor;
    @Autowired
    private Risk_1_3_AprilExtractor risk_1_3_aprilExtractor;
    @Autowired
    private Risk_2_14_Handler risk_2_14_handler;
    @Autowired
    private Risk_2_15_Handler risk_2_15_handler;
    @Autowired
    private Risk_2_16_Handler risk_2_16_handler;

    private boolean resolverAvailable;

    public IndicatorsResolver() {
        resolverAvailable = true;
    }

    @Async
    public void checkRisk_1_1() {
        LOG.info("Start RISK_1_1 recalculation");
        risk_1_1_April_extractor.checkIndicator();
        LOG.info("Finish RISK_1_1 recalculation");
    }

    @Async
    public void checkRisk_1_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_1 recalculation");
        risk_1_1_April_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_1 recalculation");
    }

    @Async
    public void checkRisk_1_2_1() {
        LOG.info("Start RISK_1_2_1 recalculation");
        risk_1_2_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_2_1 recalculation");
    }

    @Async
    public void checkRisk_1_2_2() {
        LOG.info("Start RISK_1_2_2 recalculation");
        risk_1_2_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_2_2 recalculation");
    }

    @Async
    public void checkRisk_1_3_1() {
        LOG.info("Start RISK_1_3_1 recalculation");
        risk_1_3_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_1 recalculation");
    }

    @Async
    public void checkRisk_1_3_2() {
        LOG.info("Start RISK_1_3_2 recalculation");
        risk_1_3_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_2 recalculation");
    }

    @Async
    public void checkRisk_1_3_3() {
        LOG.info("Start RISK_1_3_3 recalculation");
        risk_1_3_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_3_3 recalculation");
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
    public void checkRisk_1_5_2() {
        LOG.info("Start RISK_1_5_2 recalculation");
        risk_1_5_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_5_2 recalculation");
    }

    @Async
    public void checkRisk_1_6() {
        LOG.info("Start RISK_1_6 recalculation");
        risk_1_6_extractor.checkIndicator();
        LOG.info("Finish RISK_1_6 recalculation");
    }

    @Async
    public void checkRisk_1_8_1() {
        LOG.info("Start RISK_1_8_1 recalculation");
        risk_1_8_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_8_1 recalculation");
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
    public void checkRisk_1_10_2() {
        LOG.info("Start RISK_1_10_2 recalculation");
        risk_1_10_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk1_10_3() {
        LOG.info("Start RISK_1_10_2 recalculation");
        risk_1_10_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_2 recalculation");
    }

    @Async
    public void checkRisk_1_10_4() {
        LOG.info("Start RISK_1_10_4 recalculation");
        risk_1_10_4_extractor.checkIndicator();
        LOG.info("Finish RISK_1_10_4 recalculation");
    }

    @Async
    public void checkRisk_1_12() {
        LOG.info("Start RISK_1_12 recalculation");
        risk_1_12_extractor.checkIndicator();
        LOG.info("Finish RISK_1_12 recalculation");
    }

    @Async
    public void checkRisk_1_13_1() {
        LOG.info("Start RISK_1_13_1 recalculation");
        risk_1_13_1_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_1 recalculation");
    }

    @Async
    public void checkRisk_1_13_2() {
        LOG.info("Start RISK_1_13_2 recalculation");
        risk_1_13_2_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_2 recalculation");
    }

    @Async
    public void checkRisk_1_13_3() {
        LOG.info("Start RISK_1_13_3 recalculation");
        risk_1_13_3_extractor.checkIndicator();
        LOG.info("Finish RISK_1_13_3 recalculation");
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
    public void checkRisk_2_4() {
        LOG.info("Start RISK_2_4 recalculation");
        risk_2_4_extractor.checkIndicator();
        LOG.info("Finish RISK_2_4 recalculation");
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
    public void checkRisk_2_5_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5_1 recalculation");
        risk_2_5_1_aprilExtractor.checkIndicator(date);
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
    public void checkRisk_2_11() {
        LOG.info("Start RISK_2_11 recalculation");
        risk_2_11_extractor.checkIndicator();
        LOG.info("Finish RISK_2_11 recalculation");
    }

    @Async
    public void checkRisk_2_12() {
        LOG.info("Start RISK_2_12 recalculation");
        risk_2_12_extractor.checkIndicator();
        LOG.info("Finish RISK_2_12 recalculation");
    }

    @Async
    public void checkRisk_2_13() {
        LOG.info("Start RISK_2_13 recalculation");
        risk_2_13_extractor.checkIndicator();
        LOG.info("Finish RISK_2_13 recalculation");
    }

    @Async
    public void checkRisk_2_13_1() {
        LOG.info("Start RISK_2_13_1 recalculation");
        risk_2_13_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_13_1 recalculation");
    }

    @Async
    public void checkRisk_2_14() {
        LOG.info("Start RISK_2_14 recalculation");
        risk_2_14_extractor.checkIndicator();
        LOG.info("Finish RISK_2_14 recalculation");
    }

    @Async
    public void checkRisk_2_14_1() {
        LOG.info("Start RISK_2_14_1 recalculation");
        risk_2_14_1_extractor.checkIndicator();
        LOG.info("Finish RISK_2_14_1 recalculation");
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
    public void checkRisk_1_10_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-1-10 recalculation");
        risk_1_10_handler.handle(date);
        LOG.info("Finish daily RISK-1-10 recalculation");
    }

    @Async
    public void checkRisk_1_21_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-1-21 recalculation");
        risk_1_21_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-1-21 recalculation");
    }

    @Async
    public void checkRisk_1_21_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-1-21-1 recalculation");
        risk_1_21_1_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-1-21-1 recalculation");
    }

    @Async
    public void checkRisk_2_5_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-5 recalculation");
        risk_2_5_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-5 recalculation");
    }

    @Async
    public void checkRisk_2_6_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-6 recalculation");
        risk_2_6_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-6 recalculation");
    }

    @Async
    public void checkRisk_2_3_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-3 recalculation");
        risk_2_3_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-3 recalculation");
    }

    @Async
    public void checkRisk_2_7_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-7 recalculation");
        risk_2_7_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-7 recalculation");
    }

    @Async
    public void checkRisk_2_8_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-8 recalculation");
        risk_2_8_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-8 recalculation");
    }

    @Async
    public void checkRisk_2_6_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-6-1 recalculation");
        risk_2_6_1_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-6-1 recalculation");
    }

    @Async
    public void checkRisk_2_7_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-7-1 recalculation");
        risk_2_7_1_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-7-1 recalculation");
    }

    @Async
    public void checkRisk_2_8_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-2-8-1 recalculation");
        risk_2_8_1_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK-2-8-1 recalculation");
    }

    @Async
    public void checkRisk_1_13_april(ZonedDateTime date) {
        LOG.info("Start daily RISK-1-13 recalculation");
        risk_1_13_handler.handle(date);
        LOG.info("Finish daily RISK-1-13 recalculation");
    }

    @Async
    public void checkRisk_2_9_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-9 recalculation");
        risk_2_9_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-2-9 recalculation");
    }

    @Async
    public void checkRisk_1_7_1_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-9 recalculation");
        risk_1_7_1_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-2-9 recalculation");
    }

    @Async
    public void checkRisk_2_15_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-15 recalculation");
        risk_2_15_handler.handle(yearAgo);
        LOG.info("Finish daily RISK-2-15 recalculation");
    }

    @Async
    public void checkRisk_2_15_1_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-15-1 recalculation");
        risk_2_15_1_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-2-15-1 recalculation");
    }

    @Async
    public void checkRisk_1_2_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-1-2 recalculation");
        risk_1_2_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-1-2 recalculation");
    }

    @Async
    public void checkRisk_1_2_1_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-1-2-1 recalculation");
        risk_1_2_1_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-1-2-1 recalculation");
    }

    @Async
    public void checkRisk_1_11_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-1-11 recalculation");
        risk_1_11_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-1-11 recalculation");
    }

    @Async
    public void checkRisk_1_3_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-1-3 recalculation");
        risk_1_3_aprilExtractor.checkIndicator(yearAgo);
        LOG.info("Finish daily RISK-1-3 recalculation");
    }

    @Async
    public void checkRisk_2_14_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-14 recalculation");
        risk_2_14_handler.handle(yearAgo);
        LOG.info("Finish daily RISK-2-14 recalculation");
    }

    @Async
    public void checkRisk_2_16_april(ZonedDateTime yearAgo) {
        LOG.info("Start daily RISK-2-16 recalculation");
        risk_2_16_handler.handle(yearAgo);
        LOG.info("Finish daily RISK-2-16 recalculation");
    }

    @Async
    public void updateUncheckedTendersIndicators() {
        LOG.info("Start daily recalculation");
        if (!resolverAvailable) {
            LOG.info("Indicators resolver is not available");
            return;
        }
        try {
            LOG.info("Daily recalculation started");

            Long tendersCompletedDays = configurationService.getConfiguration().getTendersCompletedDays();
            if (tendersCompletedDays == null) {
                tendersCompletedDays = THREE_WEEK_DAYS;
            }

            resolverAvailable = false;
            ZonedDateTime tendersCompletedDaysAgo = ZonedDateTime.now().with(MIDNIGHT).minusDays(tendersCompletedDays);
            ZonedDateTime twoDaysAgo = ZonedDateTime.now().with(MIDNIGHT).minusDays(2);

            List<Runnable> indicatorsToCheck = new ArrayList<>();
            indicatorsToCheck.add(() -> checkRisk_1_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_4_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_4_2(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_8_2(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_14(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_2_2(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_9(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15_1(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_16_2(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_17_2(tendersCompletedDaysAgo));

            indicatorsToCheck.add(() -> checkRisk_2_9_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_10_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_21_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_21_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_5_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_3_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_7_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_8_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_6_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_7_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_8_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_13_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_7_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_15_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_2_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_2_1_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_11_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_1_3_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_14_april(tendersCompletedDaysAgo));
            indicatorsToCheck.add(() -> checkRisk_2_16_april(tendersCompletedDaysAgo));

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
        resolverAvailable = true;
    }

}
