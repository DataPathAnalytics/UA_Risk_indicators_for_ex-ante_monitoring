package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "checklist_indicator")
public class ChecklistIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator")
    private String indicator;

    private String evaluation;

    private Boolean answer;

    @ManyToOne
    @JoinColumn(name = "checklist_id", referencedColumnName = "id")
    private Checklist checklist;
}
