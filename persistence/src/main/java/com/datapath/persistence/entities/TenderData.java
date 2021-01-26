package com.datapath.persistence.entities;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tender_data",
        indexes = {
                @Index(columnList = "tender_id", name = "tender_data_tender_id_idx"),
        })
public class TenderData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data")
    @Type(type = "org.hibernate.type.TextType")
    private String data;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;
}
