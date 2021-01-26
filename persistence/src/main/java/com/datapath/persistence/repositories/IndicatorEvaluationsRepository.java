package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.IndicatorEvaluations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndicatorEvaluationsRepository extends JpaRepository<IndicatorEvaluations, Long> {

    List<IndicatorEvaluations> findAllByIndicator(String indicator);
}
