package com.datapath.persistence.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "bid",
        indexes = {
                @Index(columnList = "tender_id", name = "bid_tender_id_idx"),
                @Index(columnList = "supplier_id", name = "bid_supplier_id_idx")
        })
@ToString(exclude = {"tender", "supplier", "lots"})
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "status")
    private String status;

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

    @ManyToOne
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

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "bid_lot",
            joinColumns = @JoinColumn(name = "bid_id"),
            inverseJoinColumns = @JoinColumn(name = "lot_id")
    )
    private List<Lot> lots;

    @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL)
    private List<Document> documents;

    @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL)
    private List<EligibilityDocument> eligibilityDocument;

    @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL)
    private List<FinancialDocument> financialDocuments;

    @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL)
    private List<Award> awards;

    @Transient
    private List<String> relatedLots;
}
