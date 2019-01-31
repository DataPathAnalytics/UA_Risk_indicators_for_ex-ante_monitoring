package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.NearThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NearThresholdRepository extends JpaRepository<NearThreshold, Long> {
    Optional<NearThreshold> findFirstByProcuringEntity(String procuringEntity);
    Optional<NearThreshold> findFirstByProcuringEntityAndCpv(String procuringEntity, String cpv);
}
