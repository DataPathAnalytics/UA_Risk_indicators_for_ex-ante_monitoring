package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.NearThresholdOneSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NearThresholdOneSupplierRepository extends JpaRepository<NearThresholdOneSupplier, Long> {

    Optional<NearThresholdOneSupplier> findFirstByProcuringEntityAndSupplierIn(String procuringEntity, List<String> suppliers);
}
