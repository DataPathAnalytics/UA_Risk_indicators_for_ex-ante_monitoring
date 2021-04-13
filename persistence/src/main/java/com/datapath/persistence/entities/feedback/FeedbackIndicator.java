package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@IdClass(FeedbackIndicatorId.class)
@Table(name = "feedback_indicator",
        indexes = {
                @Index(columnList = "tender_id", name = "feedback_indicator_tender_id_idx")
        })
public class FeedbackIndicator {

    @Id
    private String id;

    @Id
    @Column(name = "tender_outer_id", nullable = false)
    private String tenderOuterId;

    @Column(name = "tender_id", nullable = false)
    private String tenderId;

    @Column(columnDefinition = "text")
    private String comment;

    @ManyToOne
    @JoinColumn(name = "indicator_response_id")
    private FeedbackIndicatorResponse indicatorResponse;
}
