package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity(name = "cpv_catalogue")
public class CpvCatalogue {

    @Id
    private Integer id;
    @Column(name = "cpv_code")
    private String cpvCode;
    @Column(name = "name", length = 300)
    private String name;
    @Column(name = "cpv")
    private String cpv;
    @Column(name = "cpv2")
    private String cpv2;
    @Column(name = "cpv3")
    private String cpv3;
    @Column(name = "cpv4")
    private String cpv4;
    @Column(name = "cpv5")
    private String cpv5;
    @Column(name = "cpv6")
    private String cpv6;
    @Column(name = "cpv7")
    private String cpv7;
    @Column(name = "cpv8")
    private String cpv8;
    @Column(name = "parent_cpv")
    private String parentCpv;

}