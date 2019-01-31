package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "edrpou")
public class EDRPOU {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "count")
    private Integer count;
}
