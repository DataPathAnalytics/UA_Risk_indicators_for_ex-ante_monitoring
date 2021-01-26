package com.datapath.persistence.entities.queue;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Data
@Entity(name = "region_indicators_queue_item_history")
public class RegionIndicatorsQueueItemHistory {

    @Id
    @Column(name = "tender_id")
    private String tenderId;

    @Column(name = "tender_outer_id")
    private String tenderOuterId;

    @Column(name = "left_queue_date")
    private ZonedDateTime leftQueueDate;

    @Column(name = "present_in_queue_date")
    private ZonedDateTime presentInQueueDate;
}

