package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Aggregation;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FilteredAggregationImpl implements Aggregation {
    private String type;
    private Filter filter;
    private Aggregation aggregator;


    public FilteredAggregationImpl(){}
}
