package com.datapath.persistence.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@ToString(exclude = "tender")
@Entity
@Table(name = "tender_item",
        indexes = {
                @Index(columnList = "lot_id", name = "tender_item_lot_id_idx"),
                @Index(columnList = "tender_id", name = "tender_item_tender_id_idx"),
        })
public class TenderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "classification_id")
    private String classificationId;

    @Column(name = "quantity")
    private Double quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Transient
    private String relatedLotId;
}
