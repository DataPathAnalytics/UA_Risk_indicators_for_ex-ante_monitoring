package com.datapath.druidintegration.model.druid.request.common.impl;

import lombok.Data;

import java.util.List;

@Data
public class Having  {
    private String type;
    private String dimension;
    private String aggregation;
    private List<String> intervals;
}
