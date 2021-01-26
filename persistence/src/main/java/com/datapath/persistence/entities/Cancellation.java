package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity(name = "cancellation")
public class Cancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "reason_type")
    private String reasonType;

    private String status;

    @Column(name = "cancellation_of")
    private String cancellationOf;

    @Column(name = "related_lot")
    private String relatedLot;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
}
