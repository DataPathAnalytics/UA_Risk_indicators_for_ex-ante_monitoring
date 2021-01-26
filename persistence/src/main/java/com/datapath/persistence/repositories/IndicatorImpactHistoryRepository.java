package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.IndicatorImpactHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndicatorImpactHistoryRepository extends JpaRepository<IndicatorImpactHistory, Long> {
}
