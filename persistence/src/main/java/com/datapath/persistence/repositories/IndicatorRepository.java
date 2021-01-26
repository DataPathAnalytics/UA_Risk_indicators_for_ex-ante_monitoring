package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, String> {

    List<Indicator> findAllByActiveTrue();

    List<Indicator> findAllByActiveTrueOrderById();
}
