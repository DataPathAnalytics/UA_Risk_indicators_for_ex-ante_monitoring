package com.datapath.web.domain;

import lombok.Data;

@Data
public class IndicatorsSummary {

    protected Integer numberOfEligibleIndicators;
    protected Integer numberOfIndicatorsWithRisk;
    protected Integer numberOfFailedIndicators;

}
