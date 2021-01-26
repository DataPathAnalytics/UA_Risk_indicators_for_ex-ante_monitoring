package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import com.datapath.persistence.type.StringArrayUserType;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "contract_change",
        indexes = {
                @Index(columnList = "contract_id", name = "contract_change_contract_id_idx")
        })
@ToString(exclude = {"contract"})
@TypeDef(name = "array", typeClass = StringArrayUserType.class)
public class ContractChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "status")
    private String status;

    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "date_signed")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateSigned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(columnDefinition = "text[]", name = "rationale_types")
    @Type(type = "array")
    private String[] rationaleTypes;

    @OneToMany(mappedBy = "change", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ContractDocument> documents;
}
