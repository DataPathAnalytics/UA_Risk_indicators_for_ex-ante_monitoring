package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "agreement_document",
        indexes = {
                @Index(columnList = "agreement_id", name = "agreement_document_agreement_id_idx")
        })
public class AgreementDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateModified;
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime datePublished;

    private String documentOf;
    private String documentType;
    private String format;
    private String outerId;
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;
}
