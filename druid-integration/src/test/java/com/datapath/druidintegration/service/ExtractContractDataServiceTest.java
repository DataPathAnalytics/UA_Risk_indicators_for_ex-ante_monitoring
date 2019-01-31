package com.datapath.druidintegration.service;

import com.datapath.druidintegration.DruidResolverTest;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ExtractContractDataServiceTest extends DruidResolverTest {
    @Autowired
    private ExtractContractDataService extractDataService;

    @Test
    public void test() {


        List<Event> desc = extractDataService.getTimePeriodContractData("2017-04-20T16:55:14.665+03:00", "2020-04-26T16:55:14.665+03:00", 50, "asc", true, null);
        try {
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(desc));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}