package com.datapath.druidintegration.model.druid.request;

import com.datapath.druidintegration.model.druid.request.common.Aggregation;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.Metric;
import com.datapath.druidintegration.model.druid.request.common.impl.Having;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.datapath.druidintegration.DruidConstants.DEFAULT_INTERVAL;

@Data
@Builder
@AllArgsConstructor
public class TopNRequest {
    private final static String TOP_N = "topN";
    private final static String GRANULARITY_ALL = "all";

    private String queryType;
    private String dataSource;
    private String granularity;
    private String intervals;
    private String dimension;
    private Integer threshold;
    private List<Aggregation> aggregations;
    private Filter filter;
    private Having having;
    private Metric metric;

    public TopNRequest(String dataSource) {
        this.queryType = TOP_N;
        this.granularity = GRANULARITY_ALL;
        this.dataSource = dataSource;
        this.intervals = DEFAULT_INTERVAL;
    }
}
