package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.ContractsFilter;
import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.druidintegration.model.druid.request.GroupByRequest;
import com.datapath.druidintegration.model.druid.request.SelectRequest;
import com.datapath.druidintegration.model.druid.request.TopNRequest;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.impl.*;
import com.datapath.druidintegration.model.druid.response.GroupByResponse;
import com.datapath.druidintegration.model.druid.response.SelectResponse;
import com.datapath.druidintegration.model.druid.response.TopNResponse;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class ExtractContractDataService extends ExtractorService {

    private String contractsIndex;

    @Value("${druid.contracts.index}")
    public void setDruidContractIndex(String index) {
        this.contractsIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private StringFilter getFilterByContractId(String id) {
        StringFilter filter = new StringFilter();
        filter.setType("or");
        filter.setFields(Arrays.asList(new StringFilter("selector", CONTRACT_ID, id), new StringFilter("selector", CONTRACT_OUTER_ID, id)));
        return filter;
    }

    private StringFilter getFilterByContractIds(List<String> contractIds) {
        StringFilter filter = new StringFilter();
        filter.setType("or");
        filter.setFields(Arrays.asList(
                new ListStringFilter("in", CONTRACT_ID, contractIds),
                new ListStringFilter("in", CONTRACT_OUTER_ID, contractIds)
        ));
        return filter;
    }

    private Long findLastIterationForContractIndicatorsData(List<DruidContractIndicator> druidIndicator) {
        FilterImpl filter = new FilterImpl();
        filter.setType("and");

        List<Filter> collect = druidIndicator.stream().map(this::getFilterByIndicator).collect(Collectors.toList());
        filter.setFields(collect);

        GroupByRequest groupByRequest = new GroupByRequest(contractsIndex);
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
            log.error(e.getMessage(), e);
            return 1L;
        }
    }

    public Boolean theLastContractEquals(String contractId, String indicatorId, List<DruidContractIndicator> druidIndicator) {
        return Objects.equals(findLastIterationForContractIndicatorsData(druidIndicator), getMaxIndicatorIteration(getFilterByContractId(contractId), indicatorId, contractsIndex));
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

            if ((isNull(contractsFilter) || CollectionUtils.isEmpty(contractsFilter.getIndicatorIds())) &&
                    (isNull(contractsFilter) || CollectionUtils.isEmpty(contractsFilter.getProcedureTypes()))) {
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
                GroupByRequest contractsIndicatorsMaxIteration = new GroupByRequest(contractsIndex);
                contractsIndicatorsMaxIteration.setDimensions(Arrays.asList(CONTRACT_OUTER_ID, INDICATOR_ID));
                contractsIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                contractsIndicatorsMaxIteration.setFilter(new ListStringFilter("in", CONTRACT_OUTER_ID, topLastUpdatedContractsIds));
                GroupByResponse[] contractsIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, contractsIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(contractsIndicatorsMaxIterationResponse) && contractsIndicatorsMaxIterationResponse.length > 0) {
                    List<Filter> filterList = Arrays.stream(contractsIndicatorsMaxIterationResponse).map((GroupByResponse item) -> {
                        Event event = item.getEvent();
                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(
                                new StringFilter("selector", CONTRACT_OUTER_ID, event.getContractOuterId()),
                                new StringFilter("selector", INDICATOR_ID, event.getIndicatorId()),
                                new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue()),
                                new IntFilter("selector", INDICATOR_VALUE, 1)
                        ));
                        return filter;
                    }).collect(Collectors.toList());

                    TopNRequest topNRequest = new TopNRequest(contractsIndex);
                    topNRequest.setIntervals(startDate + "/" + endDate);
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

            if ((isNull(contractsFilter) || CollectionUtils.isEmpty(contractsFilter.getIndicatorIds())) &&
                    (isNull(contractsFilter) || CollectionUtils.isEmpty(contractsFilter.getProcedureTypes()))) {
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
                GroupByRequest contractsIndicatorsMaxIteration = new GroupByRequest(contractsIndex);
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

                    TopNRequest topNRequest = new TopNRequest(contractsIndex);
                    topNRequest.setDimension(CONTRACT_OUTER_ID);
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
                        log.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contractsIndicatorsMaxIteration));
                    } catch (JsonProcessingException e) {
                        log.error(e.getMessage(), e);
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

    public List<Event> getContractsHistoryByIds(Set<String> contractIds) {
        List<Event> resultList = new ArrayList<>();
        if (!contractIds.isEmpty()) {
            SelectRequest selectRequest = new SelectRequest(contractsIndex);
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

    private ZonedDateTime convertStringDate(String date) {
        return ZonedDateTime.parse(date, DateTimeFormatter.ofPattern(UTC_DATE_TIME_FORMAT)).withZoneSameLocal(ZoneOffset.UTC);
    }

    public Long getMaxIndicatorIteration(String tenderId, String indicatorId) {
        return super.getMaxIndicatorIteration(getFilterByContractId(tenderId), indicatorId, contractsIndex);
    }
}
