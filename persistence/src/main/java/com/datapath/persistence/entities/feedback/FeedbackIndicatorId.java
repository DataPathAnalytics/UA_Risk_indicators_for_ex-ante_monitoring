package com.datapath.persistence.entities.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackIndicatorId implements Serializable {

    private String id;
    private String tenderOuterId;
}
