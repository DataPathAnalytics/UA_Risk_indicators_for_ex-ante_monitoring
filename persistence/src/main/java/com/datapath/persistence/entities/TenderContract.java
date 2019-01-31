package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import com.datapath.persistence.type.StringArrayUserType;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "tender_contract",
        indexes = {
                @Index(columnList = "outer_id", name = "contract_outer_id_idx"),
                @Index(columnList = "tender_id", name = "tender_contract_tender_id_idx"),
                @Index(columnList = "supplier_id", name = "tender_contract_supplier_id_idx"),
                @Index(columnList = "award_id", name = "tender_contract_award_id_idx")
        })
@ToString(exclude = {"tender", "supplier", "award"})
@TypeDef(name = "array", typeClass = StringArrayUserType.class)
public class TenderContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "date_signed")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateSigned;

    @Column(name = "supplier_identifier_id", length = 1000)
    private String supplierIdentifierId;

    @Column(name = "supplier_identifier_scheme")
    private String supplierIdentifierScheme;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Column(name = "supplier_telephone")
    private String supplierTelephone;

    @Column(columnDefinition = "text[]", name = "contract_cpv_list")
    @Type(type = "array")
    private String[] contractCpvList;

    @ManyToOne
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
    })
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "award_id")
    private Award award;

    @OneToOne(mappedBy = "tenderContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private Contract contract;

    @OneToMany(mappedBy = "tenderContract", cascade = CascadeType.ALL)
    private List<Document> documents;

    @Transient
    private String awardId;
}
