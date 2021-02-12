package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "feedback_violation",
        indexes = {
                @Index(columnList = "tender_outer_id", name = "feedback_violation_tender_outer_id_idx"),
                @Index(columnList = "tender_id", name = "feedback_violation_tender_id_idx")
        })
public class FeedbackViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_outer_id", unique = true, nullable = false)
    private String tenderOuterId;

    @Column(name = "tender_id", unique = true, nullable = false)
    private String tenderId;

    private Double amount;

    @Column(name = "canceled_amount")
    private Double canceledAmount;

    @Column(name = "terminated_amount")
    private Double terminatedAmount;

    @Column(name = "returned_amount")
    private Double returnedAmount;
}
