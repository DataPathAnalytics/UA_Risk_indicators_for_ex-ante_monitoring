package com.datapath.druidintegration.service;

import com.datapath.druidintegration.DruidResolverTest;
import com.datapath.druidintegration.model.TendersFilter;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ExtractDataServiceTest extends DruidResolverTest {
    @Autowired
    private ExtractTenderDataService extractDataService;

    @Test
    public void test() {
        List<Event> desc = extractDataService.getTimePeriodTenderData("2017", "2019", 100, "desc", false, new TendersFilter());
        try {
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(desc));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}
