package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@Table(name = "contract",
        indexes = {
                @Index(columnList = "tender_contract_id", name = "contract_tender_contract_id_idx")
        })
@ToString(exclude = {"tenderContract"})
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id", unique = true)
    private String outerId;

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "tender_outer_id")
    private String tenderOuterId;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "source")
    private String source;

    @Column(name = "date_modified")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateModified;

    @Column(name = "date_created")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateCreated;

    @Column(name = "date_signed")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateSigned;

    @OneToOne(cascade = CascadeType.ALL, fetch = LAZY)
    @JoinColumn(name = "tender_contract_id")
    private TenderContract tenderContract;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<ContractChange> changes;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<ContractDocument> documents;

    @Column(name = "currency")
    private String currency;

    @Column(name = "amount_net")
    private Double amountNet;

    @Column(name = "value_added_tax_included")
    private Boolean valueAddedTaxIncluded;

    @Column(name = "paid_amount")
    private Double paidAmount;

    @Column(name = "paid_currency")
    private String paidCurrency;

    @Column(name = "paid_amount_net")
    private Double paidAmountNet;

    @Column(name = "paid_value_added_tax_included")
    private Boolean paidValueAddedTaxIncluded;

    @Column(name = "period_start_date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime periodStartDate;

    @Column(name = "period_end_date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime periodEndDate;

    @Column(name = "contract_number", columnDefinition = "text")
    private String contractNumber;

    @Column(columnDefinition = "text")
    private String terminationDetails;
}