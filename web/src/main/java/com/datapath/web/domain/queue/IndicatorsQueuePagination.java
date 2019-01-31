package com.datapath.web.domain.queue;

import lombok.Data;

@Data
public class IndicatorsQueuePagination {

    private Integer totalPages;
    private Long totalElements;
    private IndicatorsQueuePage nextPage;
    private IndicatorsQueuePage previousPage;

}
