package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.druid.request.TopNRequest;
import com.datapath.druidintegration.model.druid.request.common.impl.SimpleMetricImpl;
import com.datapath.druidintegration.model.druid.response.TopNResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
public class ExtractKeyDimensionsService {

    private static final String PROCEDURE_TYPE = "procedureType";
    private static final String STATUS = "status";
    private static final String INDICATOR_ID = "indicatorId";

    private String druidUrl;
    private String tendersIndex;
    private String contractsIndex;
    private RestTemplate restTemplate;

    @Value("${druid.url}")
    public void setDruidUrl(String url) {
        this.druidUrl = url;
    }

    @Value("${druid.tenders.index}")
    public void setDruidTendersIndex(String index) {
        this.tendersIndex = index;
    }

    @Value("${druid.contracts.index}")
    public void setDruidContractsIndex(String index) {
        this.contractsIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TopNResponse[] getTendersTopNKeyDimensions(String startDate, String endDate, String dimension) {
        return getTopNKeyDimensions(startDate, endDate, dimension, tendersIndex);
    }

    public TopNResponse[] getContractsTopNKeyDimensions(String startDate, String endDate, String dimension) {
        return getTopNKeyDimensions(startDate, endDate, dimension, contractsIndex);
    }

    public TopNResponse[] getTopNKeyDimensions(String startDate, String endDate, String dimension, String index) {
        TopNRequest topProcedureTypes = new TopNRequest(index);
        topProcedureTypes.setIntervals(startDate + "/" + endDate);
        topProcedureTypes.setDimension(dimension);

        SimpleMetricImpl metric = new SimpleMetricImpl();
        metric.setType("dimension");
        metric.setOrdering("lexicographic");
        topProcedureTypes.setMetric(metric);
        topProcedureTypes.setThreshold(100);

        return restTemplate.postForObject(druidUrl, topProcedureTypes, TopNResponse[].class);
    }

    public List<String> getProcedureTypes(String startDate, String endDate) {
        TopNResponse[] postForObject = getTendersTopNKeyDimensions(startDate, endDate, PROCEDURE_TYPE);
        return (nonNull(postForObject) && postForObject.length == 0) ?
                new ArrayList<>() :
                postForObject[0].getResult()
                        .stream()
                        .map(TopNResponse.Result::getProcedureType)
                        .collect(Collectors.toList());
    }

    public List<String> getStatuses(String startDate, String endDate) {
        TopNResponse[] postForObject = getTendersTopNKeyDimensions(startDate, endDate, STATUS);
        return (nonNull(postForObject) && postForObject.length == 0) ?
                new ArrayList<>() :
                postForObject[0].getResult()
                        .stream()
                        .map(TopNResponse.Result::getStatus)
                        .collect(Collectors.toList());
    }

    public List<String> getIndicatorIds(String startDate, String endDate) {
        TopNResponse[] postForObject = getTendersTopNKeyDimensions(startDate, endDate, INDICATOR_ID);
        return (nonNull(postForObject) && postForObject.length == 0) ?
                new ArrayList<>() :
                postForObject[0].getResult()
                        .stream()
                        .map(TopNResponse.Result::getIndicatorId)
                        .collect(Collectors.toList());
    }


    public List<String> getContractIndicatorIds(String startDate, String endDate) {
        TopNResponse[] postForObject = getContractsTopNKeyDimensions(startDate, endDate, INDICATOR_ID);
        return (nonNull(postForObject) && postForObject.length == 0) ?
                new ArrayList<>() :
                postForObject[0].getResult()
                        .stream()
                        .map(TopNResponse.Result::getIndicatorId)
                        .collect(Collectors.toList());
    }

    public List<String> getContractProcedureTypes(String startDate, String endDate) {
        TopNResponse[] postForObject = getContractsTopNKeyDimensions(startDate, endDate, PROCEDURE_TYPE);
        return (nonNull(postForObject) && postForObject.length == 0) ?
                new ArrayList<>() :
                postForObject[0].getResult()
                        .stream()
                        .map(TopNResponse.Result::getProcedureType)
                        .collect(Collectors.toList());
    }
}
