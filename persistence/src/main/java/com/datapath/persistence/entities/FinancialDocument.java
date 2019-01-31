package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "financial_document",
        indexes = {
                @Index(columnList = "tender_id", name = "financial_document_tender_id_idx"),
                @Index(columnList = "bid_id", name = "financial_document_bid_id_idx")
        })
public class FinancialDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String outerId;

    @Column
    private String format;

    @Column
    private String author;

    @Column(name = "document_of")
    private String documentOf;

    @Column(name = "related_item")
    private String relatedItem;

    @Column(name = "date_modified")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateModified;

    @Column(name = "date_published")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime datePublished;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @Transient
    private String type;

    @Transient
    private String bidOuterId;
}
