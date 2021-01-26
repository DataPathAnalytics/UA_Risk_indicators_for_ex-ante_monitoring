package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tender_plan",
        indexes = {
                @Index(columnList = "tender_id", name = "tender_plan_tender_id_idx")
        })
public class TenderPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String outerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;
}
