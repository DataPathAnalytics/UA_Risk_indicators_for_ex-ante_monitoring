package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "couse")
public class Couse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "text")
    private String reason;
    @ManyToOne
    @JoinColumn(name = "monitoring_id")
    private MonitoringEntity monitoring;
}
