package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "award",
        indexes = {
                @Index(columnList = "outer_id", name = "award_outer_id_idx"),
                @Index(columnList = "tender_id", name = "award_tender_id_idx"),
                @Index(columnList = "supplier_id", name = "award_supplier_id_idx"),
                @Index(columnList = "lot_id", name = "award_lot_id_idx"),
                @Index(columnList = "bid_id", name = "award_bid_id_idx"),
        })
@ToString(exclude = {"tender", "tenderContract", "lot", "complaints", "documents"})
public class Award {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "supplier_identifier_id", length = 1000)
    private String supplierIdentifierId;

    @Column(name = "supplier_identifier_scheme")
    private String supplierIdentifierScheme;

    @Column(name = "supplier_identifier_legal_name", length = 2000)
    private String supplierIdentifierLegalName;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Column(name = "supplier_telephone")
    private String supplierTelephone;

    @OneToOne(mappedBy = "award")
    private TenderContract tenderContract;

    @OneToMany(mappedBy = "award", cascade = CascadeType.ALL)
    private List<Document> documents;

    @OneToMany(mappedBy = "award", cascade = CascadeType.ALL)
    private List<Complaint> complaints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
    }, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
    })
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
    }, fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @Transient
    private String lotId;

    @Transient
    private String bidId;
}
