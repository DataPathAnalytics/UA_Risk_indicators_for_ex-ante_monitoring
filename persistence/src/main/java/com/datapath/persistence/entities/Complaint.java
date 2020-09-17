package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "complaint",
        indexes = {
                @Index(columnList = "award_id", name = "complaint_award_id_idx"),
                @Index(columnList = "tender_id", name = "complaint_tender_id_idx")
        })
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String outerId;

    @Column
    private String complaintId;

    @Column
    private String status;

    @Column
    private String complaintType;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private Award award;
}
