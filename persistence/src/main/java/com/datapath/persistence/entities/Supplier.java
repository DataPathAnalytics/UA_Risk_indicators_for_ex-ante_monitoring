package com.datapath.persistence.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "supplier",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"identifier_id", "identifier_scheme"})
        })
@ToString(exclude = {"tenderContracts", "bids", "awards"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier_id", length = 1000)
    private String identifierId;

    @Column(name = "identifier_legal_name", length = 2000)
    private String identifierLegalName;

    @Column(name = "identifier_scheme")
    private String identifierScheme;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @OneToMany(mappedBy = "supplier")
    private List<TenderContract> tenderContracts;

    @OneToMany(mappedBy = "supplier")
    private List<Award> awards;

    @OneToMany(mappedBy = "supplier")
    private List<Bid> bids;
}
