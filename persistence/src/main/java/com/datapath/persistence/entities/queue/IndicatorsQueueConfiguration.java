package com.datapath.persistence.entities.queue;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@ToString
@Table(name = "indicators_queue_configuration")
public class IndicatorsQueueConfiguration {

    @Id
    private Integer id;

    @Column
    private String title;

    @Column
    private Double mixedTopRiskPercentage;

    @Column
    private Double lowTopRiskPercentage;

    @Column
    private Double mediumTopRiskPercentage;

    @Column
    private Double highTopRiskPercentage;

    @Column
    private Double minMixedIndicatorImpactRange;

    @Column
    private Double maxMixedIndicatorImpactRange;

    @Column
    private Double minLowIndicatorImpactRange;

    @Column
    private Double maxLowIndicatorImpactRange;

    @Column
    private Double minMediumIndicatorImpactRange;

    @Column
    private Double maxMediumIndicatorImpactRange;

    @Column
    private Double minHighIndicatorImpactRange;

    @Column
    private Double maxHighIndicatorImpactRange;

    @Column
    private Double lowTopRiskProcuringEntityPercentage;

    @Column
    private Double mediumTopRiskProcuringEntityPercentage;

    @Column
    private Double highTopRiskProcuringEntityPercentage;
}
