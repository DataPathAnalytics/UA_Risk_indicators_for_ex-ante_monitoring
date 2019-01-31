package com.datapath.druidintegration.service;

import com.datapath.druidintegration.DruidResolverTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExtractKeyDimensionsServiceTest extends DruidResolverTest {
    @Autowired
    private ExtractKeyDimensionsService extractKeyDimensionsService;

    @Test
    public void test() {
        System.out.println(extractKeyDimensionsService.getIndicatorIds("2018", "2020"));
        System.out.println(extractKeyDimensionsService.getProcedureTypes("2018", "2020"));
        System.out.println(extractKeyDimensionsService.getStatuses("2018", "2020"));
    }

}