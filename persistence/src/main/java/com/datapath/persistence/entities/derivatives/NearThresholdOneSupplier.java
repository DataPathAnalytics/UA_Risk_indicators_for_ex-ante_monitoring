package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class NearThresholdOneSupplier {

    @Id
    private Integer id;
    @Column(name = "procuring_entity")
    private String procuringEntity;
    @Column(name = "supplier")
    private String supplier;
}
