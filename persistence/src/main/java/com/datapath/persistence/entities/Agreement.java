package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "agreement",
        indexes = {
                @Index(columnList = "tender_id", name = "agreement_tender_id_idx")
        })
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agreementId;
    private String outerId;
    private String number;
    private String status;
    private String owner;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime startDate;
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime endDate;
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateSigned;
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateModified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procuring_entity_id")
    private ProcuringEntity procuringEntity;

    private String tenderOuterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgreementDocument> documents;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgreementContract> contracts;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgreementItem> items;

    private String source;
}
