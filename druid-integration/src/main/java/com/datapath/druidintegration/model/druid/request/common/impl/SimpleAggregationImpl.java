package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Aggregation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SimpleAggregationImpl implements Aggregation {
    private String type;
    private String name;
    private String fieldName;

    public SimpleAggregationImpl(){}
}
