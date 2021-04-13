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
    private Risk_2_5_1_AprilExtractor risk_2_5_1_aprilExtractor;
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
    public void checkRisk_1_1(ZonedDateTime date) {
        LOG.info("Start daily RISK_1_1 recalculation");
        risk_1_1_April_extractor.checkIndicator(date);
        LOG.info("Finish daily RISK_1_1 recalculation");
    }

    @Async
    public void checkRisk_2_5_1_april(ZonedDateTime date) {
        LOG.info("Start daily RISK_2_5_1 recalculation");
        risk_2_5_1_aprilExtractor.checkIndicator(date);
        LOG.info("Finish daily RISK_2_5_1  recalculation");
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

            List<Runnable> indicatorsToCheck = new ArrayList<>();
            indicatorsToCheck.add(() -> checkRisk_1_1(tendersCompletedDaysAgo));

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
