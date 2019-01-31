package com.datapath.persistence.entities.queue;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@ToString
@Table(name = "indicators_queue_history")
public class IndicatorsQueueHistory {

    @Id
    private Long id;

    @Column(name = "date_created")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateCreated;
}

