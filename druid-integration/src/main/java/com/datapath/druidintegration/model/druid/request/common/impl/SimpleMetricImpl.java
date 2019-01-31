package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Metric;
import lombok.Data;

@Data
public class SimpleMetricImpl implements Metric {
    String type;
    String metric;
    String ordering;
}
