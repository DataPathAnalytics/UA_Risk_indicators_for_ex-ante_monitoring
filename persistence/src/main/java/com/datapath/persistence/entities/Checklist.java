package com.datapath.persistence.entities;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "checklist",
        indexes = {
                @Index(columnList = "tender_outer_id", name = "checklist_tender_outer_id_idx")
        })
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    @Column(name = "modify_date")
    private LocalDateTime modifyDate;

    @Column(name = "tender_outer_id", unique = true)
    private String tenderOuterId;

    @Column(name = "tender_id")
    private String tenderId;

    @Column(name = "reason")
    private String reason;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ChecklistIndicator> indicators;
}
