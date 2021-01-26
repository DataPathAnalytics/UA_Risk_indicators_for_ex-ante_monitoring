package com.datapath.persistence.entities.queue;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name = "indicators_queue_configuration_history")
public class IndicatorsQueueConfigurationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_name")
    private String fieldName;

    @CreationTimestamp
    private LocalDateTime changed;

    private Double value;

    @Column(name = "previous_value")
    private Double previousValue;
}
