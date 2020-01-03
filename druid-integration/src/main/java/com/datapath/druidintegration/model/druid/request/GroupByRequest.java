package com.datapath.druidintegration.model.druid.request;

import com.datapath.druidintegration.model.druid.request.common.Aggregation;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.datapath.druidintegration.DruidConstants.DEFAULT_INTERVAL;

@Data
@Builder
@AllArgsConstructor
public class GroupByRequest {
    private final static String GROUP_BY = "groupBy";
    private final static String GRANULARITY_ALL = "all";

    private String queryType;
    private DataSource dataSource;
    private String granularity;
    private String intervals;
    private List<String> dimensions;
    private List<Aggregation> aggregations;
    private Filter filter;
    private Having having;
    private LimitSpec limitSpec;

    public GroupByRequest(String dataSource) {
        this.queryType = GROUP_BY;
        this.granularity = GRANULARITY_ALL;
        this.dataSource = new DataSource("table", dataSource);
        this.intervals = DEFAULT_INTERVAL;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DataSource {
        String type;
        String name;
        GroupByRequest query;

        public DataSource() {
        }

        public DataSource(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    @Data
    @Builder
    public static class Having {
        String type;
        String aggregation;
        Integer value;
    }

    @Data
    @Builder
    public static class LimitSpec {
        String type;
        Integer limit;
        List<Column> columns;

        @Data
        @Builder
        public static class Column {
            String dimension;
            String direction;
            String dimensionOrder;
        }
    }
}
