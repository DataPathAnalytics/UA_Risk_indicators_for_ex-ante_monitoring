package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "supplier_for_pe_with_3cpv")
public class SupplierForPEWith3CPV {

    @Id
    private Integer id;

    @Column(name = "procuring_entity", length = 2000)
    private String procuringEntity;

    @Column(name = "supplier", length = 2000)
    private String supplier;

    @Column(name = "cpv_count")
    private Integer cpvCount;
}
