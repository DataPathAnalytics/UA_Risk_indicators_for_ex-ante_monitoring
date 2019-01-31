package com.datapath.druidintegration.service;

import com.datapath.druidintegration.DruidResolverTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TenderRateServiceTest extends DruidResolverTest {
    @Autowired
    private TenderRateService tenderRateService;

    @Test
    public void test() {
        tenderRateService.getResult();
    }


}
