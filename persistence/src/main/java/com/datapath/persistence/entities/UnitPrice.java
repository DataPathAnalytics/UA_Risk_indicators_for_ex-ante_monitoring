package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "unit_price",
        indexes = {
                @Index(columnList = "agreement_contract_id", name = "unit_price_agreement_contract_id_idx")
        })
public class UnitPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String currency;
    private Double amount;
    private Boolean valueAddedTaxIncluded;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "agreement_item_id")
    private AgreementItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_contract_id")
    private AgreementContract contract;
}
