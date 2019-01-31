package com.datapath.persistence.entities.queue;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString
@Table(name = "indicators_queue_item")
public class IndicatorsQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_outer_id")
    private String tenderOuterId;

    @Column(name = "tender_id")
    private String tenderId;

    @Column(name = "expected_value")
    private Double expectedValue;

    @Column(name = "materiality_score")
    private Double materialityScore;

    @Column(name = "tender_score")
    private Double tenderScore;

    @Column(name = "procuring_entity_id")
    private String procuringEntityId;

    @Column(name = "region")
    private String region;

    @Column(name = "top_risk")
    private Boolean topRisk;

    @Column(name = "monitoring")
    private Boolean monitoring;

    @Column(name = "risk_stage")
    private String riskStage;
}

