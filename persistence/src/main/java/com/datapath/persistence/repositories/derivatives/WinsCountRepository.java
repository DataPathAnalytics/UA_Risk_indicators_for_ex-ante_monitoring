package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.WinsCount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WinsCountRepository extends JpaRepository<WinsCount, Integer> {

    WinsCount findByProcuringEntityAndSupplier(String procuringEntity, String supplier);
}
