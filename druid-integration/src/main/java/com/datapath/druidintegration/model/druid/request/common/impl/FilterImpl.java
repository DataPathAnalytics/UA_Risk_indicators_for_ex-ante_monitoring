package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Filter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class FilterImpl implements Filter {
    private String type;
    private String dimension;
    private List<Filter> fields;
    private Filter field;
    private String aggregation;
    private List<String> intervals;

    public FilterImpl(){}
}
