package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    Agreement findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(String source);
}
