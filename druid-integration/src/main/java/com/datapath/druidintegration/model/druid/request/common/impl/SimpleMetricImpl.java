package com.datapath.druidintegration.model.druid.request.common.impl;

import com.datapath.druidintegration.model.druid.request.common.Metric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleMetricImpl implements Metric {
    String type;
    String metric;
    String ordering;
    String previousStop;
}
