package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "document",
        indexes = {
                @Index(columnList = "tender_id", name = "document_tender_id_idx"),
                @Index(columnList = "award_id", name = "document_award_id_idx"),
                @Index(columnList = "contract_id", name = "document_tender_contract_idx"),
                @Index(columnList = "bid_id", name = "document_bid_id_idx")
        })
public class Document {

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
    @JoinColumn(name = "award_id")
    private Award award;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private TenderContract tenderContract;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @Transient
    private String type;

    @Transient
    private String awardOuterId;

    @Transient
    private String tenderContractOuterId;

    @Transient
    private String bidOuterId;
}
