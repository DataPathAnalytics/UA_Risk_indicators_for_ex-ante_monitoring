package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "procured_cpv")
public class ProcuredCPV {

    @Id
    private Integer id;
    @Column(name = "cpv")
    private String cpv;

}
