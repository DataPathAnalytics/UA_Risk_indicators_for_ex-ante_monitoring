package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Contracts3Years {

    @Id
    private Integer id;

    @Column(name = "procuring_entity")
    private String procuringEntity;

    private String supplier;
    private String cpv;
    private Double amount;
}
