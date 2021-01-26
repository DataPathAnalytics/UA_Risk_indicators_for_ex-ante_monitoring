package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class WinsCount {

    @Id
    private Integer id;

    @Column(name = "procuring_entity")
    private String procuringEntity;

    private String supplier;

    @Column(columnDefinition = "text", name = "cpv_list")
    private String cpvList;

    @Column(name = "cpv_count")
    private Integer cpvCount;
}
