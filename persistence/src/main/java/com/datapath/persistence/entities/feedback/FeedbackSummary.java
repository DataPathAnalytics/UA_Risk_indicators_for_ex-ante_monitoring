package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "feedback_summary",
        indexes = {
                @Index(columnList = "tender_outer_id", name = "feedback_summary_tender_outer_id_idx"),
                @Index(columnList = "tender_id", name = "feedback_summary_tender_id_idx")
        })
public class FeedbackSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_outer_id", unique = true, nullable = false)
    private String tenderOuterId;

    @Column(name = "tender_id", unique = true, nullable = false)
    private String tenderId;

    @Column(name = "number", length = 50)
    private String number;

    private LocalDate date;
}
