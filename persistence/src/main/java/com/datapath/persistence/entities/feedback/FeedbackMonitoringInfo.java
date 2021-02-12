package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "feedback_monitoring_info",
        indexes = {
                @Index(columnList = "tender_outer_id", name = "feedback_monitoring_info_tender_outer_id_idx"),
                @Index(columnList = "tender_id", name = "feedback_monitoring_info_tender_id_idx")
        })
public class FeedbackMonitoringInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_outer_id", unique = true, nullable = false)
    private String tenderOuterId;

    @Column(name = "tender_id", unique = true, nullable = false)
    private String tenderId;

    @Column(name = "monitoring_number", length = 50)
    private String number;

    @Column(name = "monitoring_date")
    private LocalDate date;

    @Column(name = "monitoring_stop_number", length = 50)
    private String stopNumber;

    @Column(name = "monitoring_stop_date")
    private LocalDate stopDate;
}
