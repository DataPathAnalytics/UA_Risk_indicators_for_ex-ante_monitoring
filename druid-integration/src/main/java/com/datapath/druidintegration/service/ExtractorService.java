package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.druidintegration.model.DruidIndicator;
import com.datapath.druidintegration.model.druid.request.GroupByRequest;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.Metric;
import com.datapath.druidintegration.model.druid.request.common.impl.*;
import com.datapath.druidintegration.model.druid.response.GroupByResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

abstract class ExtractorService {

    static final String UTC_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    static final String DATE = "date";
    static final String TENDER_ID = "tenderId";
    static final String TENDER_OUTER_ID = "tenderOuterId";
    static final String CONTRACT_ID = "contractId";
    static final String CONTRACT_OUTER_ID = "contractOuterId";
    static final String INDICATOR_TYPE = "indicatorType";
    static final String INDICATOR_ID = "indicatorId";
    static final String INDICATOR_VALUE = "indicatorValue";
    static final String INDICATOR_IMPACT = "indicatorImpact";
    static final String ITERATION_ID = "iterationId";
    static final String PROCEDURE_TYPE = "procedureType";
    static final String STATUS = "status";
    static final String LOT_IDS = "lotIds";

    String druidUrl;
    RestTemplate restTemplate;

    @Value("${druid.url}")
    public void setDruidUrl(String url) {
        this.druidUrl = url;
    }

    Filter getFilterByIndicator(DruidIndicator druidIndicator) {

        FilterImpl filter = new FilterImpl();
        List<Filter> filters = new ArrayList<>();
        filter.setType("and");
        if (druidIndicator instanceof DruidContractIndicator) {
            filters.add(new StringFilter("selector", CONTRACT_ID, druidIndicator.getContractId()));
        } else {
            filters.add(new StringFilter("selector", TENDER_ID, druidIndicator.getTenderId()));
        }
        filters.add(new StringFilter("selector", INDICATOR_ID, druidIndicator.getIndicatorId()));
        filters.add(new IntFilter("selector", INDICATOR_VALUE, druidIndicator.getIndicatorValue()));

        if (!druidIndicator.getLotIds().isEmpty()) {
            ListStringFilter lotsFilter = new ListStringFilter();
            lotsFilter.setType("and");
            lotsFilter.setFields(druidIndicator.getLotIds().stream().map(item -> new StringFilter("selector", LOT_IDS, item)).collect(Collectors.toList()));
            filters.add(lotsFilter);
        }
        filter.setFields(filters);
        return filter;
    }

    Metric getTopNMetricByDate(String order) {
        if (order.equals("desc")) {
            SimpleMetricImpl metric = new SimpleMetricImpl();
            metric.setMetric("tmax");
            return metric;
        } else {
            MetricWithInnerImpl metric = new MetricWithInnerImpl();
            metric.setType("inverted");
            MetricWithInnerImpl.InnerMetric innerMetric = new MetricWithInnerImpl.InnerMetric();
            innerMetric.setMetric("tmax");
            metric.setMetric(innerMetric);
            return metric;
        }
    }

    public Long getMaxIndicatorIteration(StringFilter filterByTenderId, String indicatorId, String index) {
        GroupByRequest groupByRequest = new GroupByRequest(index);
        SimpleAggregationImpl aggregation = new SimpleAggregationImpl();
        aggregation.setType("longMax");
        aggregation.setName("maxIteration");
        aggregation.setFieldName(ITERATION_ID);

        groupByRequest.setAggregations(Collections.singletonList(aggregation));
        FilterImpl filter = new FilterImpl();
        filter.setType("and");
        filter.setFields(Arrays.asList(filterByTenderId, new StringFilter("selector", INDICATOR_ID, indicatorId)));
        groupByRequest.setFilter(filter);

        GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);
        if (postForObject != null) {
            return postForObject.length == 0 ? 0 : postForObject[0].getEvent().getMaxIteration();
        }
        return 0L;
    }
}
