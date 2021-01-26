package com.datapath.persistence.entities;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name = "configuration_history")
public class ConfigurationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String key;

    @CreationTimestamp
    private LocalDateTime changed;

    @Column(name = "previous_value")
    private String previousValue;
    private String value;
}
