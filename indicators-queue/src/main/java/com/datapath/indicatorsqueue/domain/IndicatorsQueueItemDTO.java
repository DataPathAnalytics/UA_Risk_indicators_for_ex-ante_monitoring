package com.datapath.indicatorsqueue.domain;

import lombok.Data;

@Data
public class IndicatorsQueueItemDTO {

    private Long id;
    private String tenderOuterId;
    private String tenderId;
    private Double expectedValue;
    private Double materialityScore;
    private Double tenderScore;
    private String procuringEntityId;
    private Boolean topRisk;
    private String region;
    private String impactCategory;
    private Boolean monitoring;
    private String riskStage;
}
