package com.datapath.indicatorsresolver.service;

import com.datapath.indicatorsresolver.App;
import com.datapath.indicatorsresolver.service.checkIndicators.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class IndicatorsResolverTest {

    @Autowired
    private IndicatorsResolver indicatorsResolver;

    @Autowired
    private Risk_1_2_2_Extractor risk_1_2_2_extractor;
    @Autowired
    private Risk_1_3_2_Extractor risk_1_3_2_extractor;
    @Autowired
    private Risk_1_5_1_Extractor risk_1_5_1_extractor;
    @Autowired
    private Risk_1_6_1_Extractor risk_1_6_1_extractor;
    @Autowired
    private Risk_1_8_1_Extractor risk_1_8_1_extractor;
    @Autowired
    private Risk_1_10_1_Extractor risk_1_10_1_extractor;
    @Autowired
    private Risk_2_2_2_Extractor risk_2_2_2_extractor;
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

    @Test
    public void updateUncheckedContractsIndicators() throws Exception {
        indicatorsResolver.updateUncheckedTendersIndicators();
    }

    @Test
    public void checkIndicator_1_3_2() {
        risk_1_3_2_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_1_8_1() {
        risk_1_8_1_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_2_4() {
        risk_2_4_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_1_6() {
        risk_1_6_1_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_1_2_2() {
        risk_1_2_2_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_1_5_1() {
        risk_1_5_1_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_2_6() {
        risk_2_6_extractor.checkIndicator(ZonedDateTime.now().minusYears(1));
    }

    @Test
    public void checkIndicator_1_10_1() {
        risk_1_10_1_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

    @Test
    public void checkIndicator_2_5() {
        risk_2_5_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

    @Test
    public void checkIndicator_2_5_1() {
        risk_2_5_1_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

    @Test
    public void checkIndicator_2_5_2() {
        risk_2_5_2_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

    @Test
    public void checkIndicator_2_5_3() {
        risk_2_5_3_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

    @Test
    public void checkIndicator_2_2_2() {
        risk_2_2_2_extractor.checkIndicator(ZonedDateTime.now().minusYears(1).withHour(0));
    }

}