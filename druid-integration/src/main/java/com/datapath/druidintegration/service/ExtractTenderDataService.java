package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.DruidTenderIndicator;
import com.datapath.druidintegration.model.TendersFilter;
import com.datapath.druidintegration.model.druid.request.GroupByRequest;
import com.datapath.druidintegration.model.druid.request.SelectRequest;
import com.datapath.druidintegration.model.druid.request.TopNRequest;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.impl.*;
import com.datapath.druidintegration.model.druid.response.GroupByResponse;
import com.datapath.druidintegration.model.druid.response.SelectResponse;
import com.datapath.druidintegration.model.druid.response.TopNResponse;
import com.datapath.druidintegration.model.druid.response.common.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.druidintegration.DruidConstants.DEFAULT_INTERVAL;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class ExtractTenderDataService extends ExtractorService {

    private String tenderIndex;

    @Value("${druid.url}")
    public void setDruidUrl(String url) {
        this.druidUrl = url;
    }

    @Value("${druid.tenders.index}")
    public void setDruidTenderIndex(String index) {
        this.tenderIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private StringFilter getFilterByTenderId(String tenderId) {
        return getFilterByTenderIds(Collections.singletonList(tenderId));
    }

    private StringFilter getFilterByTenderIds(List<String> tenderIds) {
        StringFilter filter = new StringFilter();
        filter.setType("or");
        ListStringFilter tenderIdsFilter = new ListStringFilter("in", TENDER_ID, tenderIds);
        ListStringFilter outerIdsFilter = new ListStringFilter("in", TENDER_OUTER_ID, tenderIds);
        filter.setFields(Arrays.asList(tenderIdsFilter, outerIdsFilter));
        return filter;
    }

    public List<Event> getLastIterationData(String tenderId, String indicatorId, Long iterationId) {
        SelectRequest selectRequest = new SelectRequest(tenderIndex);
        selectRequest.setPagingSpec(new SelectRequest.PagingSpec(3L));
        selectRequest.setDimensions(new ArrayList<>());

        StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, tenderId);
        StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, indicatorId);
        IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, iterationId.intValue());

        FilterImpl filter = FilterImpl.builder().type("and").fields(Arrays.asList(tenderIdFilter, indicatorIdFilter, iterationIdFilter)).build();
        selectRequest.setFilter(filter);

        SelectResponse[] postForObject = restTemplate.postForObject(druidUrl, selectRequest, SelectResponse[].class);
        return isNull(postForObject) || postForObject.length == 0 ?
                new ArrayList<>() :
                postForObject[0].getResult().getEvents().stream()
                        .map(SelectResponse.Result.Events::getEvent)
                        .collect(Collectors.toList());

    }

    public Long findLastIterationForTenderIndicatorsData(List<DruidTenderIndicator> druidIndicator) {
        List<Integer> maxIterations = druidIndicator.stream().map(indicator -> {
            GroupByRequest groupByRequest = new GroupByRequest(tenderIndex);
            SimpleAggregationImpl aggregation = new SimpleAggregationImpl();
            aggregation.setType("longMax");
            aggregation.setName("maxIteration");
            aggregation.setFieldName(ITERATION_ID);

            groupByRequest.setAggregations(Collections.singletonList(aggregation));
            groupByRequest.setFilter(getFilterByIndicator(indicator));
            GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);
            return isNull(postForObject) || postForObject.length == 0 ? null : postForObject[0].getEvent().getMaxIteration().intValue();
        }).collect(Collectors.toList());
        List<Integer> nonNullMaxIterations = maxIterations.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return (nonNullMaxIterations.size() > 0 && nonNullMaxIterations.size() == druidIndicator.size()) ?
                Collections.max(nonNullMaxIterations).longValue() : null;

    }

    public Boolean theLastTenderEquals(String tenderId, String indicatorId, List<DruidTenderIndicator> druidIndicator) {
        return Objects.equals(findLastIterationForTenderIndicatorsData(druidIndicator), getMaxIndicatorIteration(getFilterByTenderId(tenderId), indicatorId, tenderIndex));
    }

    public List<Event> getTenderDataByTenderId(TendersFilter tendersFilter) {
        if (isNull(tendersFilter.getTenderId())) {
            return new ArrayList<>();
        }
        SelectRequest selectRequest = new SelectRequest(tenderIndex);
        SelectRequest.PagingSpec pagingSpec = new SelectRequest.PagingSpec();
        pagingSpec.setThreshold(10000L);
        selectRequest.setPagingSpec(pagingSpec);
        selectRequest.setDimensions(Arrays.asList(DATE, TENDER_ID, TENDER_OUTER_ID, INDICATOR_IMPACT, INDICATOR_TYPE, INDICATOR_ID, INDICATOR_VALUE, LOT_IDS, ITERATION_ID, STATUS, PROCEDURE_TYPE));

        FilterImpl filter = new FilterImpl();
        filter.setType("and");
        List<Filter> filters = new ArrayList<>();
        filters.add(getFilterByTenderId(tendersFilter.getTenderId()));
        if (nonNull(tendersFilter.getProcedureTypes()))
            filters.add(new ListStringFilter("in", PROCEDURE_TYPE, tendersFilter.getProcedureTypes()));
        if (nonNull(tendersFilter.getIndicatorIds()))
            filters.add(new ListStringFilter("in", INDICATOR_ID, tendersFilter.getIndicatorIds()));
        filter.setFields(filters);

        selectRequest.setFilter(filter);

        SelectResponse[] postForObject = restTemplate.postForObject(druidUrl, selectRequest, SelectResponse[].class);

        return isNull(postForObject)
                ? null
                : postForObject[0].getResult().getEvents().stream().map(SelectResponse.Result.Events::getEvent).collect(Collectors.toList());
    }


    private List<Event> getTimePeriodRiskTenderData(String startDate, String endDate, Integer limit, String order, TendersFilter tendersFilter) {
        Set<String> tenderIds = new HashSet<>();
        while (tenderIds.size() < limit) {
            List<String> topLastUpdatedTendersIds = new ArrayList<>();
            List<String> topLastUpdatedTendersTmax = new ArrayList<>();

            if ((isNull(tendersFilter.getIndicatorIds()) || tendersFilter.getIndicatorIds().isEmpty()) &&
                    (isNull(tendersFilter.getProcedureTypes()) || tendersFilter.getProcedureTypes().isEmpty())) {
                TopNResponse[] topLastUpdatedTenders = getTopNLastRiskTenders(startDate, endDate, limit, order);

                if (nonNull(topLastUpdatedTenders) && topLastUpdatedTenders.length > 0 && !topLastUpdatedTenders[0].getResult().isEmpty()) {
                    topLastUpdatedTendersIds = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTenderOuterId).collect(Collectors.toList());
                    topLastUpdatedTendersTmax = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTmax).collect(Collectors.toList());
                }
            } else {
                GroupByResponse[] topLastUpdatedTenders = getGroupByLastRiskTenders(startDate, endDate, limit, order, tendersFilter);
                topLastUpdatedTendersIds = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTenderOuterId()).collect(Collectors.toList());
                topLastUpdatedTendersTmax = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTmax()).collect(Collectors.toList());
            }
            if (!topLastUpdatedTendersIds.isEmpty()) {
                GroupByRequest tendersIndicatorsMaxIteration = new GroupByRequest(tenderIndex);
                tendersIndicatorsMaxIteration.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID));
                tendersIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                tendersIndicatorsMaxIteration.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topLastUpdatedTendersIds));
                GroupByResponse[] tendersIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, tendersIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(tendersIndicatorsMaxIterationResponse) && tendersIndicatorsMaxIterationResponse.length > 0) {
                    List<Filter> filterList = Arrays.stream(tendersIndicatorsMaxIterationResponse).map((GroupByResponse item) -> {
                        Event event = item.getEvent();
                        StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, event.getTenderOuterId());
                        StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, event.getIndicatorId());
                        IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue());
                        IntFilter indicatorValueFilter = new IntFilter("selector", INDICATOR_VALUE, 1);
                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(tenderIdFilter, indicatorIdFilter, iterationIdFilter, indicatorValueFilter));
                        return filter;
                    }).collect(Collectors.toList());

                    TopNRequest topNRequest = new TopNRequest(tenderIndex);
                    topNRequest.setDimension(TENDER_OUTER_ID);
                    topNRequest.setIntervals(startDate + "/" + endDate);
                    topNRequest.setThreshold(limit);

                    topNRequest.setAggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)));
                    topNRequest.setMetric(getTopNMetricByDate(order));

                    FilterImpl filter = new FilterImpl();
                    filter.setType("or");
                    filter.setFields(filterList);
                    topNRequest.setFilter(filter);
                    TopNResponse[] postForObject = restTemplate.postForObject(druidUrl, topNRequest, TopNResponse[].class);
                    if (postForObject != null && postForObject.length > 0 && !postForObject[0].getResult().isEmpty()) {
                        List<String> tenderIDs = postForObject[0].getResult().stream().map(TopNResponse.Result::getTenderOuterId).collect(Collectors.toList());
                        for (String tenderID : tenderIDs) {
                            if (tenderIds.size() < limit) {
                                tenderIds.add(tenderID);
                            }
                        }
                        if (tenderIDs.size() < limit) {
                            int size = postForObject[0].getResult().size();
                            if (order.equals("desc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(postForObject[0].getResult().get(size - 1).getTmax());
                                endDate = zonedDateTime.minusNanos(1000000).toString();
                            }
                            if (order.equals("asc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(postForObject[0].getResult().get(0).getTmax());
                                startDate = zonedDateTime.plusNanos(1000000).toString();
                            }
                        }
                    } else {
                        if (topLastUpdatedTendersIds.size() > 0 && tenderIds.size() < limit) {
                            int size = topLastUpdatedTendersTmax.size();
                            if (order.equals("desc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedTendersTmax.get(size - 1));
                                if (convertStringDate(startDate).isBefore(zonedDateTime.minusNanos(1))) {
                                    endDate = zonedDateTime.minusNanos(1000000).toString();
                                } else {
                                    break;
                                }
                            }
                            if (order.equals("asc")) {
                                ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedTendersTmax.get(0));
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
        return getTendersHistoryByIds(tenderIds);
    }

    private TopNResponse[] getTopNLastTenders(String startDate, String endDate, Integer limit, String order) {
        TopNRequest topTenders = TopNRequest.builder()
                .dataSource(tenderIndex)
                .queryType("topN")
                .granularity("all")
                .intervals(startDate + "/" + endDate)
                .dimension(TENDER_OUTER_ID)
                .threshold(limit * 10)
                .metric(getTopNMetricByDate(order))
                .aggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)))
                .build();

        return restTemplate.postForObject(druidUrl, topTenders, TopNResponse[].class);
    }

    private TopNResponse[] getTopNLastRiskTenders(String startDate, String endDate, Integer limit, String order) {
        TopNRequest topTenders = TopNRequest.builder()
                .dataSource(tenderIndex)
                .queryType("topN")
                .granularity("all")
                .intervals(startDate + "/" + endDate)
                .dimension(TENDER_OUTER_ID)
                .threshold(limit * 10)
                .filter(new IntFilter("selector", INDICATOR_VALUE, 1))
                .metric(getTopNMetricByDate(order))
                .aggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)))
                .build();

        return restTemplate.postForObject(druidUrl, topTenders, TopNResponse[].class);
    }

    private GroupByResponse[] getGroupByLastTenders(String startDate, String endDate, Integer limit, String order, TendersFilter tendersFilter) {

        List<Filter> filterList = new ArrayList<>();
        if (nonNull(tendersFilter.getIndicatorIds()))
            filterList.add(new ListStringFilter("in", INDICATOR_ID, tendersFilter.getIndicatorIds()));
        if (nonNull(tendersFilter.getProcedureTypes()))
            filterList.add(new ListStringFilter("in", PROCEDURE_TYPE, tendersFilter.getProcedureTypes()));

        GroupByRequest topTenders = GroupByRequest.builder()
                .queryType("groupBy")
                .granularity("all")
                .dataSource(GroupByRequest.DataSource.builder().type("table").name(tenderIndex).build())
                .intervals(startDate + "/" + endDate)
                .dimensions(Collections.singletonList(TENDER_OUTER_ID))
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

    private GroupByResponse[] getGroupByLastRiskTenders(String startDate, String endDate, Integer limit, String order, TendersFilter tendersFilter) {
        List<Filter> filterList = new ArrayList<>();
        if (nonNull(tendersFilter.getIndicatorIds()))
            filterList.add(new ListStringFilter("in", INDICATOR_ID, tendersFilter.getIndicatorIds()));
        if (nonNull(tendersFilter.getProcedureTypes()))
            filterList.add(new ListStringFilter("in", PROCEDURE_TYPE, tendersFilter.getProcedureTypes()));

        GroupByRequest topTenders = GroupByRequest.builder()
                .queryType("groupBy")
                .granularity("all")
                .dataSource(GroupByRequest.DataSource.builder().type("table").name(tenderIndex).build())
                .intervals(startDate + "/" + endDate)
                .filter(new IntFilter("selector", INDICATOR_VALUE, 1))
                .dimensions(Collections.singletonList(TENDER_OUTER_ID))
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

    private List<Event> getTimePeriodAllTenderData(String startDate, String endDate, Integer limit, String order, TendersFilter tendersFilter) {
        Set<String> tenderIds = new HashSet<>();
        while (tenderIds.size() < limit) {

            List<String> topLastUpdatedTendersIds = new ArrayList<>();
            List<String> topLastUpdatedTendersTmax = new ArrayList<>();


            if ((isNull(tendersFilter.getIndicatorIds()) || tendersFilter.getIndicatorIds().isEmpty()) &&
                    (isNull(tendersFilter.getProcedureTypes()) || tendersFilter.getProcedureTypes().isEmpty())) {
                TopNResponse[] topLastUpdatedTenders = getTopNLastTenders(startDate, endDate, limit, order);

                if (nonNull(topLastUpdatedTenders) && topLastUpdatedTenders.length > 0 && !topLastUpdatedTenders[0].getResult().isEmpty()) {
                    topLastUpdatedTendersIds = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTenderOuterId).collect(Collectors.toList());
                    topLastUpdatedTendersTmax = topLastUpdatedTenders[0].getResult().stream().map(TopNResponse.Result::getTmax).collect(Collectors.toList());
                }
            } else {
                GroupByResponse[] topLastUpdatedTenders = getGroupByLastTenders(startDate, endDate, limit, order, tendersFilter);
                topLastUpdatedTendersIds = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTenderOuterId()).collect(Collectors.toList());
                topLastUpdatedTendersTmax = Arrays.stream(topLastUpdatedTenders).map(item -> item.getEvent().getTmax()).collect(Collectors.toList());
            }

            if (!topLastUpdatedTendersIds.isEmpty()) {
//                GroupByRequest tendersIndicatorsMaxIteration = new GroupByRequest(tenderIndex);
//                tendersIndicatorsMaxIteration.setDimensions(Collections.singletonList(TENDER_OUTER_ID));
//                tendersIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)));
//                tendersIndicatorsMaxIteration.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topLastUpdatedTendersIds));
//
//                GroupByResponse[] tendersIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, tendersIndicatorsMaxIteration, GroupByResponse[].class);
//
//                if (nonNull(tendersIndicatorsMaxIterationResponse) && tendersIndicatorsMaxIterationResponse.length > 0) {
//                    List<Filter> filterList = Arrays.stream(tendersIndicatorsMaxIterationResponse).map((GroupByResponse item) -> {
//                        Event event = item.getEvent();
//                        FilterImpl filter = new FilterImpl();
//                        List<Filter> fields = new ArrayList<>();
//                        fields.add(new StringFilter("selector", TENDER_OUTER_ID, event.getTenderOuterId()));
//                        fields.add(new StringFilter("selector", DATE, event.getTmax()));
//                        filter.setType("and");
//                        filter.setFields(fields);
//                        return filter;
//                    }).collect(Collectors.toList());
//
//                    FilterImpl filter = new FilterImpl();
//                    filter.setType("or");
//                    filter.setFields(filterList);
//
//                    TopNRequest topNRequest = TopNRequest.builder()
//                            .dataSource(tenderIndex)
//                            .queryType("topN")
//                            .granularity("all")
//                            .intervals(startDate + "/" + endDate)
//                            .dimension(TENDER_OUTER_ID)
//                            .threshold(limit)
//                            .aggregations(Collections.singletonList(new SimpleAggregationImpl("timeMax", "tmax", DATE)))
//                            .metric(getTopNMetricByDate(order))
//                            .filter(filter)
//                            .build();
//
//                    TopNResponse[] postForObject = restTemplate.postForObject(druidUrl, topNRequest, TopNResponse[].class);
//                    if (postForObject != null && postForObject.length > 0 && !postForObject[0].getResult().isEmpty()) {
//                        postForObject[0].getResult().forEach(item -> {
//                            if (tenderIds.size() < limit) {
//                                tenderIds.add(item.getTenderOuterId());
//                            }
//                        });
//                    }
//                }
                tenderIds.addAll(topLastUpdatedTendersIds);
            }

            if (tenderIds.size() < limit && !topLastUpdatedTendersIds.isEmpty()) {
                int size = topLastUpdatedTendersIds.size();
                if (order.equals("desc")) {
                    ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedTendersTmax.get(size - 1));
                    if (convertStringDate(startDate).isBefore(zonedDateTime.minusNanos(1000000))) {
                        endDate = zonedDateTime.minusNanos(1).toString();
                    } else {
                        break;
                    }
                }
                if (order.equals("asc")) {
                    ZonedDateTime zonedDateTime = convertStringDate(topLastUpdatedTendersTmax.get(0));
                    if (zonedDateTime.plusNanos(1).isBefore(convertStringDate(endDate))) {
                        startDate = zonedDateTime.plusNanos(1000000).toString();
                    } else {
                        break;
                    }
                }
            } else {
                break;
            }

        }
        return getTendersHistoryByIds(tenderIds);

    }

    public List<Event> getTimePeriodTenderData(String startDate, String endDate, Integer limit, String order, Boolean riskOnly, TendersFilter tendersFilter) {
        return riskOnly ?
                getTimePeriodRiskTenderData(startDate, endDate, limit, order, tendersFilter) :
                getTimePeriodAllTenderData(startDate, endDate, limit, order, tendersFilter);

    }

    private List<Event> getTendersHistoryByIds(Set<String> tenderIds) {
        List<Event> resultList = new ArrayList<>();
        if (!tenderIds.isEmpty()) {
            SelectRequest selectRequest = new SelectRequest(tenderIndex);
            SelectRequest.PagingSpec pagingSpec = new SelectRequest.PagingSpec();
            pagingSpec.setThreshold(1000000L);
            selectRequest.setPagingSpec(pagingSpec);
            selectRequest.setDimensions(Arrays.asList(DATE, TENDER_ID, TENDER_OUTER_ID, CONTRACT_ID, CONTRACT_OUTER_ID, INDICATOR_IMPACT, INDICATOR_TYPE, INDICATOR_ID, INDICATOR_VALUE, LOT_IDS, ITERATION_ID, PROCEDURE_TYPE, STATUS));
            selectRequest.setFilter(getFilterByTenderIds(new ArrayList<>(tenderIds)));
            SelectResponse[] selectResponses = restTemplate.postForObject(druidUrl, selectRequest, SelectResponse[].class);

            if (selectResponses != null && selectResponses.length > 0) {
                return selectResponses[0].getResult().getEvents()
                        .stream()
                        .map(SelectResponse.Result.Events::getEvent)
                        .collect(Collectors.toList());

            }
        }
        return resultList;
    }

    private ZonedDateTime convertStringDate(String date) {
        return ZonedDateTime.parse(date, DateTimeFormatter.ofPattern(UTC_DATE_TIME_FORMAT)).withZoneSameLocal(ZoneOffset.UTC);
    }


    public Map<String, Long> getMaxTenderIndicatorIteration(Set<String> tenderIds, String indicatorId) {

        Map<String, Long> result = new HashMap<>();
        GroupByRequest groupByRequest = new GroupByRequest(tenderIndex);
        SimpleAggregationImpl aggregation = new SimpleAggregationImpl();
        aggregation.setType("longMax");
        aggregation.setName("maxIteration");
        aggregation.setFieldName(ITERATION_ID);
        groupByRequest.setAggregations(Collections.singletonList(aggregation));
        groupByRequest.setDimensions(Collections.singletonList(TENDER_OUTER_ID));

        List<Filter> filterList = tenderIds.stream().map(tenderId -> {
            FilterImpl filter = new FilterImpl();
            filter.setType("and");
            filter.setFields(Arrays.asList(getFilterByTenderId(tenderId), new StringFilter("selector", INDICATOR_ID, indicatorId)));
            return filter;
        }).collect(Collectors.toList());

        FilterImpl resultFilter = new FilterImpl();
        resultFilter.setType("or");
        resultFilter.setFields(filterList);
        groupByRequest.setFilter(resultFilter);

        try {
            GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);
            if (postForObject != null && postForObject.length != 0) {
                Arrays.stream(postForObject).forEach(item -> {
                    Event event = item.getEvent();
                    result.put(event.getTenderOuterId(), event.getMaxIteration());
                });
            }
            tenderIds.forEach(tenderId -> {
                if (!result.containsKey(tenderId)) {
                    result.put(tenderId, 0L);
                }
            });
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return result;
    }

    public Map<String, Integer> getMaxTendersIterationData(Map<String, Long> tenderIterations, String indicatorId) {

        Map<String, Integer> result = new HashMap<>();

        GroupByRequest groupByRequest = new GroupByRequest(tenderIndex);
        groupByRequest.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_VALUE));
        FilterImpl filter = new FilterImpl();
        filter.setType("or");
        filter.setFields(getFilters(tenderIterations, indicatorId));
        groupByRequest.setFilter(filter);
        GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);

        if (postForObject != null && postForObject.length != 0) {
            Arrays.stream(postForObject).forEach(item -> {
                Event event = item.getEvent();
                result.put(event.getTenderOuterId(), event.getIndicatorValue());
            });
        }
        return result;
    }

    private List<Filter> getFilters(Map<String, Long> tenderIterations, String indicatorId) {
        return tenderIterations.entrySet().stream().map(item -> {
            String tenderId = item.getKey();
            Long iteration = item.getValue();
            FilterImpl filter = new FilterImpl();
            filter.setType("and");
            StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, tenderId);
            IntFilter iterationFilter = new IntFilter("selector", ITERATION_ID, iteration.intValue());
            StringFilter indicatorFilter = new StringFilter("selector", INDICATOR_ID, indicatorId);
            filter.setFields(Arrays.asList(tenderIdFilter, iterationFilter, indicatorFilter));
            return filter;
        }).collect(Collectors.toList());
    }

    public Map<String, Map<String, Integer>> getMaxTendersLotIterationData(Map<String, Long> tenderIterations, String indicatorId) {

        Map<String, Map<String, Integer>> result = new HashMap<>();
        tenderIterations.keySet().forEach(tenderId -> {
            result.put(tenderId, new HashMap<>());
        });
        GroupByRequest groupByRequest = new GroupByRequest(tenderIndex);
        groupByRequest.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_VALUE, LOT_IDS));
        FilterImpl filter = new FilterImpl();
        filter.setType("or");
        filter.setFields(getFilters(tenderIterations, indicatorId));
        groupByRequest.setFilter(filter);

        GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, groupByRequest, GroupByResponse[].class);

        if (postForObject != null && postForObject.length != 0) {
            Arrays.stream(postForObject).forEach(item -> {
                Event event = item.getEvent();
                String tenderOuterId = event.getTenderOuterId();
                event.getLotIds().forEach(lot -> result.get(tenderOuterId).put(lot, event.getIndicatorValue()));
            });
        }
        return result;
    }

    public List<String> getListTenders(String previousStop, String interval) {

        TopNRequest request = TopNRequest.builder()
                .queryType("topN")
                .dataSource(tenderIndex)
                .granularity("all")
                .intervals(interval)
                .dimension(TENDER_OUTER_ID)
                .threshold(1000)
                .aggregations(new ArrayList<>())
                .metric(SimpleMetricImpl.builder().type("dimension").ordering("lexicographic").previousStop(previousStop).build())
                .build();
        log.info("before request");
        TopNResponse[] response = restTemplate.postForObject(druidUrl, request, TopNResponse[].class);
        log.info("after request");
        return response[0].getResult().stream().map(TopNResponse.Result::getTenderOuterId).collect(Collectors.toList());
    }

    public List<String> getListTenders(String previousStop) {
        return getListTenders(previousStop, DEFAULT_INTERVAL);
    }

    public List<Event> getLastTendersData(List<String> tenderIds) {

        GroupByRequest tendersIndicatorsMaxIteration = new GroupByRequest(tenderIndex);
        tendersIndicatorsMaxIteration.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID));
        tendersIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
        tendersIndicatorsMaxIteration.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, tenderIds));
        GroupByResponse[] tendersIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, tendersIndicatorsMaxIteration, GroupByResponse[].class);

        if (nonNull(tendersIndicatorsMaxIterationResponse) && tendersIndicatorsMaxIterationResponse.length > 0) {

            List<Event> response = Arrays.stream(tendersIndicatorsMaxIterationResponse).map(GroupByResponse::getEvent).collect(Collectors.toList());

            List<Filter> filterList = response.stream().map((event) -> {
                StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, event.getTenderOuterId());
                StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, event.getIndicatorId());
                IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue());
                FilterImpl filter = new FilterImpl();
                filter.setType("and");
                filter.setFields(Arrays.asList(tenderIdFilter, indicatorIdFilter, iterationIdFilter));
                return filter;
            }).collect(Collectors.toList());

            GroupByRequest filteredByTendersGroupBy = new GroupByRequest(tenderIndex);
            filteredByTendersGroupBy.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID, ITERATION_ID, INDICATOR_VALUE, INDICATOR_IMPACT));
            filteredByTendersGroupBy.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, tenderIds));

            GroupByRequest resultRequest = GroupByRequest
                    .builder()
                    .queryType("groupBy")
                    .granularity("all")
                    .intervals(DEFAULT_INTERVAL)
                    .dataSource(GroupByRequest.DataSource.builder().type("query").query(filteredByTendersGroupBy).build())
                    .dimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID, INDICATOR_VALUE, INDICATOR_IMPACT))
                    .aggregations(new ArrayList<>())
                    .filter(FilterImpl.builder().type("or").fields(filterList).build())
                    .build();

            GroupByResponse[] tenderWithLastIndicatorResult = restTemplate.postForObject(druidUrl, resultRequest, GroupByResponse[].class);
            return Arrays.stream(tenderWithLastIndicatorResult).map(GroupByResponse::getEvent).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public Long getMaxIndicatorIteration(String tenderId, String indicatorId) {
        return super.getMaxIndicatorIteration(getFilterByTenderId(tenderId), indicatorId, tenderIndex);
    }
}
