package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.AgreementSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementSupplierRepository extends JpaRepository<AgreementSupplier, Long> {

    AgreementSupplier findFirstByIdentifierIdAndIdentifierScheme(String identifierId, String identifierScheme);
}
