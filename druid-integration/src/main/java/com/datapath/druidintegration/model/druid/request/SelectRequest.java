package com.datapath.druidintegration.model.druid.request;

import com.datapath.druidintegration.model.druid.request.common.Filter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

import static com.datapath.druidintegration.DruidConstants.DEFAULT_INTERVAL;

@Data
public class SelectRequest {
    private final static String SELECT = "select";
    private final static String GRANULARITY_ALL = "all";

    private Filter filter;
    private String queryType;
    private String intervals;
    private String dataSource;
    private String granularity;
    private List<String> dimensions;
    private PagingSpec pagingSpec;

    @Data
    @AllArgsConstructor
    public static class PagingSpec {
        private Long threshold;

        public PagingSpec() {
        }
    }

    public SelectRequest(String dataSource) {
        this.queryType = SELECT;
        this.granularity = GRANULARITY_ALL;
        this.dataSource = dataSource;
        this.intervals = DEFAULT_INTERVAL;
    }
}
