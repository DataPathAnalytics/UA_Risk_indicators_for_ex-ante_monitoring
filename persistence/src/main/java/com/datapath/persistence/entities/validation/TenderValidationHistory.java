package com.datapath.persistence.entities.validation;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import com.datapath.persistence.type.StringArrayUserType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "tender_validation_history")
@TypeDef(name = "array", typeClass = StringArrayUserType.class)
public class TenderValidationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private Integer tendersCount;

    @Column
    private Integer existingTendersCount;

    @Column
    private Integer testOrExpiredTendersCount;

    @Column
    private Integer missingTendersCount;

    @Type(type = "array")
    @Column(columnDefinition = "text[]")
    private String[] missingTenders;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;
}
