package com.datapath.persistence.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString
@Table(name = "indicator_evaluations")
public class IndicatorEvaluations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation", columnDefinition = "text")
    private String evaluation;

    @Column(name = "indicator_id")
    private String indicator;
}
