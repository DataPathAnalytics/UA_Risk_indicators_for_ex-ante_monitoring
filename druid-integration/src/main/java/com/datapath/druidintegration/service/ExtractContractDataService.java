package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.ContractsFilter;
import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.druidintegration.model.druid.request.GroupByRequest;
import com.datapath.druidintegration.model.druid.request.SelectRequest;
import com.datapath.druidintegration.model.druid.request.TopNRequest;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.Metric;
import com.datapath.druidintegration.model.druid.request.common.impl.*;
import com.datapath.druidintegration.model.druid.response.GroupByResponse;
import com.datapath.druidintegration.model.druid.response.SelectResponse;
import com.datapath.druidintegration.model.druid.response.TopNResponse;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class ExtractContractDataService {

    private static final String UTC_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private final static String DATE = "date";
    private final static String TENDER_ID = "tenderId";
    private final static String TENDER_OUTER_ID = "tenderOuterId";
    private final static String CONTRACT_ID = "contractId";
    private final static String CONTRACT_OUTER_ID = "contractOuterId";
    private final static String INDICATOR_TYPE = "indicatorType";
    private final static String INDICATOR_ID = "indicatorId";
    private final static String INDICATOR_VALUE = "indicatorValue";
    private final static String INDICATOR_IMPACT = "indicatorImpact";
    private final static String ITERATION_ID = "iterationId";
    private final static String PROCEDURE_TYPE = "procedureType";
    private final static String LOT_IDS = "lotIds";

    private String druidUrl;
    private String contractsIndex;
    private RestTemplate restTemplate;

    @Value("${druid.url}")
    public void setDruidUrl(String url) {
        this.druidUrl = url;
    }

    @Value("${druid.contracts.index}")
    public void setDruidContractIndex(String index) {
        this.contractsIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private StringFilter getFilterByContractId(String contarctId) {
        StringFilter filter = new StringFilter();
        filter.setType("or");

        StringFilter tenderIdFilter = new StringFilter();
        tenderIdFilter.setType("selector");
        tenderIdFilter.setDimension(CONTRACT_ID);
        tenderIdFilter.setValue(contarctId);

        StringFilter outerIdFilter = new StringFilter();
        outerIdFilter.setType("selector");
        outerIdFilter.setDimension(CONTRACT_OUTER_ID);
        outerIdFilter.setValue(contarctId);

        filter.setFields(Arrays.asList(tenderIdFilter, outerIdFilter));

        return filter;
    }

    private StringFilter getFilterByTenderId(String tenderId) {
        StringFilter filter = new StringFilter();
        filter.setType("or");

        StringFilter tenderIdFilter = new StringFilter();
        tenderIdFilter.setType("selector");
        tenderIdFilter.setDimension(TENDER_ID);
        tenderIdFilter.setValue(tenderId);

        StringFilter outerIdFilter = new StringFilter();
        outerIdFilter.setType("selector");
        outerIdFilter.setDimension(TENDER_OUTER_ID);
        outerIdFilter.setValue(tenderId);

        filter.setFields(Arrays.asList(tenderIdFilter, outerIdFilter));

        return filter;
    }

    private StringFilter getFilterByContractIds(List<String> contractIds) {
        StringFilter filter = new StringFilter();
        filter.setType("or");

        ListStringFilter contractIdsFilter = new ListStringFilter();
        contractIdsFilter.setType("in");
        contractIdsFilter.setDimension(CONTRACT_ID);
        contractIdsFilter.setValues(contractIds);

        ListStringFilter outerIdsFilter = new ListStringFilter();
        outerIdsFilter.setType("in");
        outerIdsFilter.setDimension(CONTRACT_OUTER_ID);
        outerIdsFilter.setValues(contractIds);

        filter.setFields(Arrays.asList(contractIdsFilter, outerIdsFilter));

        return filter;
    }

    private StringFilter getFilterByIndicatorId(String indicatorId) {
        StringFilter indicatorIdFilter = new StringFilter();
        indicatorIdFilter.setType("selector");
        indicatorIdFilter.setDimension(INDICATOR_ID);
        indicatorIdFilter.setValue(indicatorId);

        return indicatorIdFilter;
    }

    private Filter getFilterByContractIndicator(DruidContractIndicator druidIndicator) {

        FilterImpl filter = new FilterImpl();
        List<Filter> filters = new ArrayList<>();
        filter.setType("and");

        StringFilter tenderIdFilter = new StringFilter();
        tenderIdFilter.setType("selector");
        tenderIdFilter.setDimension(CONTRACT_ID);
        tenderIdFilter.setValue(druidIndicator.getContractId());
        filters.add(tenderIdFilter);

        StringFilter indicatorIdFilter = new StringFilter();
        indicatorIdFilter.setType("selector");
        indicatorIdFilter.setDimension(INDICATOR_ID);
        indicatorIdFilter.setValue(druidIndicator.getIndicatorId());
        filters.add(indicatorIdFilter);

        IntFilter indicatorValueFilter = new IntFilter();
        indicatorValueFilter.setType("selector");
        indicatorValueFilter.setDimension(INDICATOR_VALUE);
        indicatorValueFilter.setValue(druidIndicator.getIndicatorValue());
        filters.add(indicatorValueFilter);

        if (!druidIndicator.getLotIds().isEmpty()) {
            ListStringFilter lotsFilter = new ListStringFilter();
            lotsFilter.setType("and");
            lotsFilter.setFields(druidIndicator.getLotIds().stream().map(item -> {
                StringFilter lotFilter = new StringFilter();
                lotFilter.setType("selector");
                lotFilter.setDimension(LOT_IDS);
                lotFilter.setValue(item);
                return lotFilter;
            }).collect(Collectors.toList()));
            filters.add(lotsFilter);
        }
        filter.setFields(filters);
        return filter;
    }


    public Long findLastIterationForContractIndicatorsData(List<DruidContractIndicator> druidIndicator) {
        FilterImpl filter = new FilterImpl();
        filter.setType("and");

        List<Filter> collect = druidIndicator.stream().map(this::getFilterByContractIndicator).collect(Collectors.toList());
        filter.setFields(collect);

        GroupByRequest groupByRequest = new GroupByRequest(contractsIndex, "2015/2020");
        SimpleAggregationImpl aggregation = new SimpleAggregationImpl();
        aggregation.setType("longMax");
        aggregation.setName("maxIteration");
        aggregation.setFieldName(ITERATION_ID);

        groupByRequest.setAggregations(Collections.singletonList(aggregation));
        groupByRequest.setFilter(filter);

        try {
            GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);
            return isNull(postForObject) || postForObject.length == 0 ? null : postForObject[0].getEvent().getMaxIteration();
        } catch (Exception e) {
            System.out.println("EXCEPTION");
            return 1L;
        }

    }

    public Boolean theLastContractEquals(String contractId, String indicatorId, List<DruidContractIndicator> druidIndicator) {
        return Objects.equals(findLastIterationForContractIndicatorsData(druidIndicator), getMaxContractIndicatorIteration(contractId, indicatorId));
    }

    public Metric getTopNMetricByDate(String order) {
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

    public TopNResponse[] getTopNLastContracts(String startDate, String endDate, Integer limit, String order) {
        TopNRequest topTenders = TopNRequest.builder()
                .dataSource(contractsIndex)
                .queryType("topN")
                .granularity("all")
                .intervals(startDate + "/" + endDate)
                .dimension(CONTRACT_OUTER_ID)
                .threshold(limit * 10)
                .metric(getTopNMetricByDate(order))
                .aggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)))
                .build();

        return restTemplate.postForObject(druidUrl, topTenders, TopNResponse[].class);
    }

    public GroupByResponse[] getGroupByLastContracts(String startDate, String endDate, Integer limit, String order, ContractsFilter contractsFilter) {

        List<Filter> filterList = new ArrayList<>();
        if (nonNull(contractsFilter.getIndicatorIds()))
            filterList.add(new ListStringFilter("in", INDICATOR_ID, contractsFilter.getIndicatorIds()));
        if (nonNull(contractsFilter.getProcedureTypes()))
            filterList.add(new ListStringFilter("in", PROCEDURE_TYPE, contractsFilter.getProcedureTypes()));

        GroupByRequest topTenders = GroupByRequest.builder()
                .queryType("groupBy")
                .granularity("all")
                .dataSource(GroupByRequest.DataSource.builder().type("table").name(contractsIndex).build())
                .intervals(startDate + "/" + endDate)
                .dimensions(Collections.singletonList(CONTRACT_OUTER_ID))
                .limitSpec(GroupByRequest.LimitSpec.builder()
                        .type("default")
                        .limit(limit * 10)
                        .columns(Collections.singletonList(GroupByRequest.LimitSpec.Column.builder()
                                .dimension("tmax")
                                .direction(order)
                                .dimensionOrder("numeric")
                                .build()))
                        .build())
                .aggregations(Arrays.asList(
                        new SimpleAggregationImpl("timeMax", "tmax", DATE),
                        FilteredAggregationImpl.builder()
                                .type("filtered")
                                .filter(FilterImpl.builder().type("and").fields(filterList).build())
                                .aggregator(SimpleAggregationImpl.builder().type("count").name("count").build())
                                .build()
                ))
                .having(GroupByRequest.Having.builder().type("greaterThan").aggregation("count").value(0).build())
                .build();

        return restTemplate.postForObject(druidUrl, topTenders, GroupByResponse[].class);

    }



    public List<Event> getTimePeriodRiskContractsData(String startDate, String endDate, Integer limit, String order, ContractsFilter contractsFilter) {
        Set<String> contractIds = new HashSet<>();
        while (contractIds.size() < limit) {
            List<String> topLastUpdatedContractsIds = new ArrayList<>();
            List<String> topLastUpdatedContractsTmax = new ArrayList<>();

            if ((isNull(contractsFilter.getIndicatorIds()) || contractsFilter.getIndicatorIds().isEmpty()) &&
                    (isNull(contractsFilter.getProcedureTypes()) || contractsFilter.getProcedureTypes().isEmpty())) {
                TopNResponse[] topLastUpdatedTenders = getTopNLastContracts(startDate, endDate, limit, order);

                if (nonNull(topLastUpdatedTenders) && topLastUpdatedTenders.length > 0 && !topLastUpdatedTenders[0].getResult().isEmpty()) {
                    topLastUpdatedContractsIds = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getContractOuterId).collect(Collectors.toList());
                    topLastUpdatedContractsTmax = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTmax).collect(Collectors.toList());
                }
            } else {
                GroupByResponse[] topLastUpdatedTenders = getGroupByLastContracts(startDate, endDate, limit, order, contractsFilter);
                topLastUpdatedContractsIds = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getContractOuterId()).collect(Collectors.toList());
                topLastUpdatedContractsTmax = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTmax()).collect(Collectors.toList());
            }

            if (!topLastUpdatedContractsIds.isEmpty()) {
                GroupByRequest contractsIndicatorsMaxIteration = new GroupByRequest(contractsIndex, "2017/2020");
                contractsIndicatorsMaxIteration.setDimensions(Arrays.asList(CONTRACT_OUTER_ID, INDICATOR_ID));
                contractsIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                contractsIndicatorsMaxIteration.setFilter(new ListStringFilter("in", CONTRACT_OUTER_ID, topLastUpdatedContractsIds));
                GroupByResponse[] contractsIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, contractsIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(contractsIndicatorsMaxIterationResponse) && contractsIndicatorsMaxIterationResponse.length > 0) {
                    List<Filter> filterList = Arrays.stream(contractsIndicatorsMaxIterationResponse).map((GroupByResponse item) -> {
                        Event event = item.getEvent();
                        StringFilter contractIdFilter = new StringFilter("selector", CONTRACT_OUTER_ID, event.getContractOuterId());
                        StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, event.getIndicatorId());
                        IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue());
                        IntFilter indicatorValueFilter = new IntFilter("selector", INDICATOR_VALUE, 1);

                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(contractIdFilter, indicatorIdFilter, iterationIdFilter, indicatorValueFilter));
                        return filter;
                    }).collect(Collectors.toList());

                    TopNRequest topNRequest = new TopNRequest(contractsIndex, startDate + "/" + endDate);
                    topNRequest.setDimension(CONTRACT_OUTER_ID);
                    topNRequest.setThreshold(limit);

                    topNRequest.setAggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)));
                    topNRequest.setMetric(getTopNMetricByDate(order));


                    FilterImpl filter = new FilterImpl();
                    filter.setType("or");
                    filter.setFields(filterList);
                    topNRequest.setFilter(filter);
                    TopNResponse[] postForObject = restTemplate.postForObject(druidUrl, topNRequest, TopNResponse[].class);
                    if (postForObject != null && postForObject.length > 0 && !postForObject[0].getResult().isEmpty()) {
                        List<String> tenderIDs = postForObject[0].getResult().stream().map(TopNResponse.Result::getContractOuterId).collect(Collectors.toList());
                        for (String tenderID : tenderIDs) {
                            if (contractIds.size() < limit) {
                                contractIds.add(tenderID);
                            }
                        }
                        if (tenderIDs.size() < limit) {
                            int size = postForObject[0].getResult().size();
                            if (order.equals("desc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(postForObject[0].getResult().get(size - 1).getTmax());
                                if (convertStringDate(startDate).isBefore(zonedDateTime.minusNanos(1000000))) {
                                    endDate = zonedDateTime.minusNanos(1).toString();
                                } else {
                                    break;
                                }
                            }
                            if (order.equals("asc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(postForObject[0].getResult().get(size - 1).getTmax());
                                if (zonedDateTime.plusNanos(1).isBefore(convertStringDate(endDate))) {
                                    startDate = zonedDateTime.plusNanos(1000000).toString();
                                } else {
                                    break;
                                }
                            }
                        }
                    } else {
                        if (topLastUpdatedContractsIds.size() > 0 && contractIds.size() < limit) {
                            int size = topLastUpdatedContractsTmax.size();
                            if (order.equals("desc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedContractsTmax.get(size - 1));
                                if (convertStringDate(startDate).isBefore(zonedDateTime.minusNanos(1))) {
                                    endDate = zonedDateTime.minusNanos(1000000).toString();
                                } else {
                                    break;
                                }
                            }
                            if (order.equals("asc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedContractsTmax.get(0));
                                if (zonedDateTime.plusNanos(1).isBefore(convertStringDate(endDate))) {
                                    startDate = zonedDateTime.plusNanos(1000000).toString();
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                break;
            }
        }
        return getContractsHistoryByIds(contractIds);
    }

    public List<Event> getTimePeriodAllContractsData(String startDate, String endDate, Integer limit, String order, ContractsFilter contractsFilter) {
        Set<String> contractIds = new HashSet<>();
        while (contractIds.size() < limit) {
            List<String> topLastUpdatedContractsIds = new ArrayList<>();
            List<String> topLastUpdatedContractsTmax = new ArrayList<>();

            if ((isNull(contractsFilter.getIndicatorIds()) || contractsFilter.getIndicatorIds().isEmpty()) &&
                    (isNull(contractsFilter.getProcedureTypes()) || contractsFilter.getProcedureTypes().isEmpty())) {
                TopNResponse[] topLastUpdatedTenders = getTopNLastContracts(startDate, endDate, limit, order);

                if (nonNull(topLastUpdatedTenders) && topLastUpdatedTenders.length > 0 && !topLastUpdatedTenders[0].getResult().isEmpty()) {
                    topLastUpdatedContractsIds = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getContractOuterId).collect(Collectors.toList());
                    topLastUpdatedContractsTmax = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTmax).collect(Collectors.toList());
                }
            } else {
                GroupByResponse[] topLastUpdatedTenders = getGroupByLastContracts(startDate, endDate, limit, order, contractsFilter);
                topLastUpdatedContractsIds = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getContractOuterId()).collect(Collectors.toList());
                topLastUpdatedContractsTmax = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTmax()).collect(Collectors.toList());
            }

            if (!topLastUpdatedContractsIds.isEmpty()) {
                GroupByRequest contractsIndicatorsMaxIteration = new GroupByRequest(contractsIndex, "2017/2020");
                contractsIndicatorsMaxIteration.setDimensions(Collections.singletonList(CONTRACT_OUTER_ID));
                contractsIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)));
                contractsIndicatorsMaxIteration.setFilter(new ListStringFilter("in", CONTRACT_OUTER_ID, topLastUpdatedContractsIds));

                GroupByResponse[] contractsIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, contractsIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(contractsIndicatorsMaxIterationResponse) && contractsIndicatorsMaxIterationResponse.length > 0) {
                    List<Filter> filterList = Arrays.stream(contractsIndicatorsMaxIterationResponse).map((GroupByResponse item) -> {
                        Event event = item.getEvent();
                        StringFilter contractIdFilter = new StringFilter("selector", CONTRACT_OUTER_ID, event.getContractOuterId());
                        StringFilter dateFilter = new StringFilter("selector", DATE, event.getTmax());
                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(contractIdFilter, dateFilter));
                        return filter;
                    }).collect(Collectors.toList());

                    TopNRequest topNRequest = new TopNRequest(contractsIndex, startDate + "/" + endDate);
                    topNRequest.setDimension(CONTRACT_OUTER_ID);
                    topNRequest.setThreshold(limit);
                    topNRequest.setAggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)));
                    topNRequest.setMetric(getTopNMetricByDate(order));

                    FilterImpl filter = new FilterImpl();
                    filter.setType("or");
                    filter.setFields(filterList);
                    topNRequest.setFilter(filter);

                    TopNResponse[] postForObject = restTemplate.postForObject(druidUrl, topNRequest, TopNResponse[].class);


                    if (postForObject != null && postForObject.length > 0 && !postForObject[0].getResult().isEmpty()) {
                        List<String> contractIDs = postForObject[0].getResult().stream().map(TopNResponse.Result::getContractOuterId).collect(Collectors.toList());
                        for (String contractID : contractIDs) {
                            if (contractIds.size() < limit) {
                                contractIds.add(contractID);
                            }
                        }
                    }
                    if (topLastUpdatedContractsIds.size() > 0 && contractIds.size() < limit) {
                        int size = topLastUpdatedContractsTmax.size();
                        if (order.equals("desc")) {
                            ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedContractsTmax.get(size - 1));
                            if (convertStringDate(startDate).isBefore(zonedDateTime.minusNanos(1))) {
                                endDate = zonedDateTime.minusNanos(1000000).toString();
                            } else {
                                break;
                            }
                        }
                        if (order.equals("asc")) {
                            ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedContractsTmax.get(0));
                            if (zonedDateTime.plusNanos(1).isBefore(convertStringDate(endDate))) {
                                startDate = zonedDateTime.plusNanos(1000000).toString();
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                } else {
                    try {
                        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contractsIndicatorsMaxIteration));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                break;
            }
        }
        return getContractsHistoryByIds(contractIds);
    }

    public List<Event> getTimePeriodContractData(String startDate, String endDate, Integer limit,
                                                 String order, Boolean riskOnly, ContractsFilter contractsFilter) {
        return riskOnly ?
                getTimePeriodRiskContractsData(startDate, endDate, limit, order, contractsFilter) :
                getTimePeriodAllContractsData(startDate, endDate, limit, order, contractsFilter);

    }

    public List<Event> getContractDataByContractId(ContractsFilter contractsFilter) {
        return getContractsHistoryByIds(new HashSet<>(Collections.singletonList(contractsFilter.getContractId())));
    }

    public List<Event> getContracstDataByTenderId(String tenderId) {
        List<Event> resultList = new ArrayList<>();
        if (!tenderId.isEmpty()) {
            SelectRequest selectRequest = new SelectRequest(contractsIndex, "2017/2020");
            SelectRequest.PagingSpec pagingSpec = new SelectRequest.PagingSpec();
            pagingSpec.setThreshold(1000000L);
            selectRequest.setPagingSpec(pagingSpec);
            selectRequest.setDimensions(Arrays.asList(DATE, CONTRACT_ID, CONTRACT_OUTER_ID, TENDER_ID, TENDER_OUTER_ID, INDICATOR_IMPACT, INDICATOR_TYPE, INDICATOR_ID, INDICATOR_VALUE, LOT_IDS, ITERATION_ID, PROCEDURE_TYPE));
            selectRequest.setFilter(getFilterByTenderId(tenderId));
            SelectResponse[] selectResponses = restTemplate.postForObject(druidUrl, selectRequest, SelectResponse[].class);

            if (selectResponses != null && selectResponses.length > 0) {
                for (SelectResponse.Result.Events item : selectResponses[0].getResult().getEvents()) {
                    Event event = item.getEvent();
                    resultList.add(event);
                }
            }
        }
        return resultList;
    }


    public List<Event> getContractsHistoryByIds(Set<String> contractIds) {
        List<Event> resultList = new ArrayList<>();
        if (!contractIds.isEmpty()) {
            SelectRequest selectRequest = new SelectRequest(contractsIndex, "2017/2020");
            SelectRequest.PagingSpec pagingSpec = new SelectRequest.PagingSpec();
            pagingSpec.setThreshold(1000000L);
            selectRequest.setPagingSpec(pagingSpec);
            selectRequest.setDimensions(Arrays.asList(DATE, TENDER_ID, TENDER_OUTER_ID, INDICATOR_IMPACT, INDICATOR_TYPE, INDICATOR_ID, INDICATOR_VALUE, LOT_IDS, ITERATION_ID, CONTRACT_ID, CONTRACT_OUTER_ID, PROCEDURE_TYPE));
            selectRequest.setFilter(getFilterByContractIds(new ArrayList<>(contractIds)));
            SelectResponse[] selectResponses = restTemplate.postForObject(druidUrl, selectRequest, SelectResponse[].class);

            if (selectResponses != null && selectResponses.length > 0) {
                for (SelectResponse.Result.Events item : selectResponses[0].getResult().getEvents()) {
                    Event event = item.getEvent();
                    resultList.add(event);
                }
            }
        }
        return resultList;
    }


    public ZonedDateTime convertStringDate(String date) {
        return ZonedDateTime.parse(date, DateTimeFormatter.ofPattern(UTC_DATE_TIME_FORMAT)).withZoneSameLocal(ZoneOffset.UTC);
    }


    public Long getMaxContractIndicatorIteration(String tenderId, String indicatorId) {
        GroupByRequest groupByRequest = new GroupByRequest(contractsIndex, "2015/2020");
        SimpleAggregationImpl aggregation = new SimpleAggregationImpl();
        aggregation.setType("longMax");
        aggregation.setName("maxIteration");
        aggregation.setFieldName(ITERATION_ID);

        groupByRequest.setAggregations(Collections.singletonList(aggregation));
        FilterImpl filter = new FilterImpl();
        filter.setType("and");
        filter.setFields(Arrays.asList(getFilterByContractId(tenderId), getFilterByIndicatorId(indicatorId)));
        groupByRequest.setFilter(filter);

        GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);
        if (postForObject != null) {
            return postForObject.length == 0 ? 0 : postForObject[0].getEvent().getMaxIteration();
        } return 0L;
    }
}
