package com.datapath.persistence.entities.feedback;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "feedback_result",
        indexes = {
                @Index(columnList = "tender_outer_id", name = "feedback_result_tender_outer_id_idx"),
                @Index(columnList = "tender_id", name = "feedback_result_tender_id_idx")
        })
public class FeedbackResult {

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "result")
    private List<FeedbackMaterial> materials;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "result")
    private List<FeedbackProtocol> protocols;
}
