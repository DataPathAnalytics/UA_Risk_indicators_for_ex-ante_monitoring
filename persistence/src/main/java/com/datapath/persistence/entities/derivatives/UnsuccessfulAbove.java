package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class UnsuccessfulAbove {

    @Id
    private Integer id;
    @Column(name = "procuring_entity")
    private String procuringEntity;
    @Column(name = "tender_cpv")
    private String tenderCpv;
    @Column(name = "unsuccessful_above_procedures_count")
    private Integer unsuccessfulAboveProceduresCount;

}
