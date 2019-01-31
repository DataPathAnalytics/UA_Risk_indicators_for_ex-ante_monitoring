package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mutual_participation")
@Data
public class MutualParticipation {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "supplier_1")
    private String supplier1;

    @Column(name = "supplier_2")
    private String supplier2;

    @Column(name = "participation")
    private Double participation;

}