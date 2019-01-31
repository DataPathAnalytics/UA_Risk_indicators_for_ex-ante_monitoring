package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, String> {
    Optional<Indicator> findFirstByIdAndAndIsActive(String id, Boolean active);
    List<Indicator> findAllByIsActiveTrue();
}
