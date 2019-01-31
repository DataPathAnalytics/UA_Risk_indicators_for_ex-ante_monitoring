package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tender_contract_id")
    private TenderContract tenderContract;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<ContractChange> changes;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<ContractDocument> documents;

}
