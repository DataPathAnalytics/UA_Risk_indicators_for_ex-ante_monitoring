package com.datapath.persistence.entities;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import com.datapath.persistence.type.StringArrayUserType;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "contract_document",
        indexes = {
                @Index(columnList = "contract_id", name = "contract_document_contract_id_idx")
        })
@ToString(exclude = {"contract"})
@TypeDef(name = "array", typeClass = StringArrayUserType.class)
public class ContractDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "format")
    private String format;

    @Column(name = "title", length = 2000)
    private String title;

    @Column(name = "document_of")
    private String documentOf;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "date_published")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime datePublished;

    @Column(name = "date_modified")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime dateModified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

}
