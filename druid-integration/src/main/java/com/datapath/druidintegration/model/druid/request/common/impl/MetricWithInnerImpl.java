package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Metric;
import lombok.Data;

@Data
public class MetricWithInnerImpl implements Metric {
    String type;
    InnerMetric metric;

    @Data
    public static class InnerMetric{
        String type;
        String metric;
    }
}
