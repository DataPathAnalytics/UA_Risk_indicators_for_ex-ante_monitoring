package com.datapath.web.domain.queue;

import lombok.Data;

@Data
public class RegionIndicatorsQueueItem {

    private String tenderOuterId;
    private String tenderId;
    private Double expectedValue;
    private Double materialityScore;
    private Double tenderScore;
    private String procuringEntityId;
    private String region;
    private Boolean topRisk;
    private String impactCategory;
    private Boolean monitoring;
}
