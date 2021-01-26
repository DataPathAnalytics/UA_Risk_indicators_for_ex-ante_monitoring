package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "monitoring")
public class MonitoringEntity {

    @Id
    private String id;
    private String status;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "monitoring", fetch = FetchType.EAGER)
    private List<Couse> causes = new ArrayList<>();

    private boolean appeal;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private String modifiedDate;
    private String tenderId;
    @Column(columnDefinition = "text")
    private String office;

    private ZonedDateTime conclusionDate;
}
