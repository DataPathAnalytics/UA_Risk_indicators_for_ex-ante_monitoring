package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "feedback_protocol")
public class FeedbackProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "document", length = 50)
    private String document;

    private LocalDate date;

    @Column(columnDefinition = "text")
    private String description;

    private Double amount;

    @Column(name = "paid_amount")
    private Double paidAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_result_id")
    private FeedbackResult result;
}
