package com.datapath.persistence.entities;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name = "indicator_impact_history")
public class IndicatorImpactHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_id")
    private String indicatorId;

    @CreationTimestamp
    private LocalDateTime changed;

    @Column(name = "previous_value")
    private Double previousValue;
    private Double value;
}
