package com.datapath.web.domain.queue;

import lombok.Data;

import java.util.List;

@Data
public class IndicatorsQueueDataPage<T> {

    private IndicatorsQueuePagination pagination;
    private IndicatorsQueueInfo queueInfo;
    private List<T> data;

}
