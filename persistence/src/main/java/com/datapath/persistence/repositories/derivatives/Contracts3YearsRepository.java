package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.Contracts3Years;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Contracts3YearsRepository extends JpaRepository<Contracts3Years, Integer> {

    Optional<Contracts3Years> findByProcuringEntityAndSupplierAndCpv(String procuringEntity, String supplier, String cpv);
}
