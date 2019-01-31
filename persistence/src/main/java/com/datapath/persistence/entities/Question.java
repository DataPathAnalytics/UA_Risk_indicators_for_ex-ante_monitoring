package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "question",
        indexes = {
                @Index(columnList = "tender_id", name = "question_tender_id_idx")
        })
@ToString
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "date_answered")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateAnswered;

    @Column(name = "answer")
    @Type(type = "org.hibernate.type.TextType")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;
}
