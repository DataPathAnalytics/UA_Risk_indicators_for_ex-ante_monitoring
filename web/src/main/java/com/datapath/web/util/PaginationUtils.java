package com.datapath.web.util;

import com.datapath.web.common.Parameters;
import com.datapath.web.common.SortOrder;
import com.datapath.web.domain.IndicatorsPage;
import com.datapath.web.domain.queue.IndicatorsQueuePage;
import com.datapath.web.domain.queue.IndicatorsQueuePagination;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public class PaginationUtils {

    public static IndicatorsPage createIndicatorsPage(ZonedDateTime startDate,
                                                      ZonedDateTime endDate,
                                                      ZonedDateTime nextPageStartDate,
                                                      ZonedDateTime nextPageEndDate,
                                                      String order,
                                                      Integer limit,
                                                      Boolean riskOnly,
                                                      String path,
                                                      List<String> procedureTypes,
                                                      List<String> indicatorIds) {
        IndicatorsPage indicatorsPage = new IndicatorsPage();
        if (order.equals(SortOrder.DESC)) {
            indicatorsPage.setEndDate(nextPageEndDate);
            indicatorsPage.setStartDate(startDate);
        } else {
            indicatorsPage.setStartDate(nextPageStartDate);
            indicatorsPage.setEndDate(endDate);
        }

        UriBuilder builder = UriComponentsBuilder.fromHttpUrl(path);
        if (order.equals(SortOrder.DESC)) {
            builder.queryParam(Parameters.END_DATE, nextPageEndDate);
            builder.queryParam(Parameters.START_DATE, startDate);
        } else {
            builder.queryParam(Parameters.START_DATE, nextPageStartDate);
            builder.queryParam(Parameters.END_DATE, endDate);
        }

        builder.queryParam(Parameters.LIMIT, limit);
        builder.queryParam(Parameters.ORDER, order);
        builder.queryParam(Parameters.RISK_ONLY, riskOnly);

        if (procedureTypes != null) {
            procedureTypes.forEach(procedureType -> builder.queryParam(
                    Parameters.PROCEDURE_TYPE, procedureType));
        }

        if (indicatorIds != null) {
            indicatorIds.forEach(indicatorId -> builder.queryParam(
                    Parameters.INDICATOR, indicatorId));
        }

        URI uri = builder.build();
        indicatorsPage.setPath(uri.getPath());
        indicatorsPage.setUrl(uri.toString());

        return indicatorsPage;
    }

    public static IndicatorsQueuePage createIndicatorsQueueNextPage(Integer page,
                                                                    Integer limit,
                                                                    String path,
                                                                    List<String> regions) {

        IndicatorsQueuePage queuePage = new IndicatorsQueuePage();

        Integer nextPage = page + 1;

        UriBuilder builder = UriComponentsBuilder.fromHttpUrl(path);
        builder.queryParam(Parameters.PAGE, nextPage);
        builder.queryParam(Parameters.LIMIT, limit);
        regions.forEach(region -> builder.queryParam(Parameters.REGION, region));

        URI uri = builder.build();
        queuePage.setPath(uri.getPath());
        queuePage.setUrl(uri.toString());

        return queuePage;
    }

    public static IndicatorsQueuePage createIndicatorsQueuePreviousPage(Integer page,
                                                                        Integer limit,
                                                                        String path,
                                                                        List<String> regions) {

        IndicatorsQueuePage queuePage = new IndicatorsQueuePage();

        Integer nextPage = page - 1;

        UriBuilder builder = UriComponentsBuilder.fromHttpUrl(path);

        builder.queryParam(Parameters.PAGE, nextPage);
        builder.queryParam(Parameters.LIMIT, limit);
        regions.forEach(region -> builder.queryParam(Parameters.REGION, region));


        URI uri = builder.build();
        queuePage.setPath(uri.getPath());
        queuePage.setUrl(uri.toString());

        return queuePage;
    }

    public static IndicatorsQueuePagination createIndicatorsQueuePagination(Integer page,
                                                                            Integer limit,
                                                                            String path,
                                                                            Integer totalPages,
                                                                            Long totalElements,
                                                                            List<String> regions) {

        IndicatorsQueuePage previousQueuePage = page > 0 ? createIndicatorsQueuePreviousPage(
                page, limit, path, regions) : null;

        IndicatorsQueuePage nextQueuePage = page < totalPages -1 ?  createIndicatorsQueueNextPage(
                page, limit, path, regions) : null;

        IndicatorsQueuePagination pagination = new IndicatorsQueuePagination();
        pagination.setNextPage(nextQueuePage);
        pagination.setPreviousPage(previousQueuePage);
        pagination.setTotalPages(totalPages);
        pagination.setTotalElements(totalElements);

        return pagination;
    }

}
