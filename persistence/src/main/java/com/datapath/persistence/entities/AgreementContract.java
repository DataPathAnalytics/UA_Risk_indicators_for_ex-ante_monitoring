package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "agreement_contract",
        indexes = {
                @Index(columnList = "agreement_id", name = "agreement_contract_agreement_id_idx")
        })
public class AgreementContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String outerId;
    private String status;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private Award award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "agreement_contract_supplier",
            joinColumns = @JoinColumn(name = "agreement_contract_id"),
            inverseJoinColumns = @JoinColumn(name = "agreement_supplier_id"))
    private List<AgreementSupplier> suppliers;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitPrice> unitPrices;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Transient
    private String awardId;

    @Transient
    private String bidId;
}
