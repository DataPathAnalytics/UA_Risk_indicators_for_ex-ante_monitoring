package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Table(name = "tender_data")
public class TenderData {

    @Id
    private Long id;

    @Column(name = "data", columnDefinition = "text")
    private String data;

    @OneToOne(fetch = LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Tender tender;
}
