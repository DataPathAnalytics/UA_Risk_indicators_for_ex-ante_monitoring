package com.datapath.persistence.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "procuring_entity",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"identifier_id", "identifier_scheme"})
        })
@ToString(exclude = {"tenders"})
public class ProcuringEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier_id")
    private String identifierId;

    @Column(name = "identifier_legal_name", length = 2000)
    private String identifierLegalName;

    @Column(name = "identifier_scheme")
    private String identifierScheme;

    @Column(name = "kind")
    private String kind;

    @Column(name = "region")
    private String region;

    @OneToMany(mappedBy = "procuringEntity")
    private List<Tender> tenders;
}

