package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import com.datapath.persistence.type.StringArrayUserType;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@ToString
@Table(name = "indicator")
@TypeDef(name = "array", typeClass = StringArrayUserType.class)
public class Indicator {

    @Id
    private String id;

    @Column
    private String code;

    @Column(length = 1000)
    private String name;

    @Column(name = "short_name", length = 1000)
    private String shortName;

    @Column
    private Double impact;

    @Column
    private String tenderLotType;

    @Column
    private String impactType;

    @Column(columnDefinition = "text[]", name = "procedure_types")
    @Type(type = "array")
    private String[] procedureTypes;

    @Column(columnDefinition = "text[]", name = "procedure_statuses")
    @Type(type = "array")
    private String[] procedureStatuses;

    @Column(columnDefinition = "text[]", name = "procuring_entity_kind")
    @Type(type = "array")
    private String[] procuringEntityKind;

    @Column
    private String stage;

    @Column
    private String risk;

    @Column(name = "active")
    private Boolean isActive;

    @Column(name = "last_checked_date_created")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime lastCheckedDateCreated;

    @Column(name = "date_checked")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateChecked;

    @Column(name = "checking_frequency")
    private Integer checkingFrequency;
}
