package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

//fixme rename to Cause table and entity
@Data
@Entity(name = "couse")
@Table(indexes = {
        @Index(columnList = "monitoring_id", name = "couse_monitoring_id_idx")
})
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
