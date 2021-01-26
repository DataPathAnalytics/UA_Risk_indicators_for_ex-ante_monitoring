package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "agreement_item",
        indexes = {
                @Index(columnList = "agreement_id", name = "agreement_item_agreement_id_idx"),
                @Index(columnList = "lot_id", name = "agreement_item_lot_id_idx")
        })
public class AgreementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String classificationId;
    private String classificationScheme;
    private String classificationDescription;
    private String outerId;
    private String unitCode;
    private String unitName;
    private Double quantity;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime deliveryEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Transient
    private String relatedLotId;
}
