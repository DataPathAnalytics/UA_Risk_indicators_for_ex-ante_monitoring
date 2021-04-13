package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity(name = "feedback_indicator_response")
public class FeedbackIndicatorResponse {

    @Id
    private Integer id;
    @Column(columnDefinition = "text")
    private String value;
}
