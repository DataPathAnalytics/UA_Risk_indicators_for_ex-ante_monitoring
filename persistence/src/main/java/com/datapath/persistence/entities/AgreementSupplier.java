package com.datapath.persistence.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "agreement_supplier")
@EqualsAndHashCode(of = {"identifierId", "identifierScheme"})
public class AgreementSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telephone;
    private String email;
    private String identifierScheme;
    private String identifierLegalName;
    private String identifierId;

    @ManyToMany(mappedBy = "suppliers")
    private List<AgreementContract> contracts;
}
