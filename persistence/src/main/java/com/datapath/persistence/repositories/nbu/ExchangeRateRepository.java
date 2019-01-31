package com.datapath.persistence.repositories.nbu;

import com.datapath.persistence.entities.nbu.ExchangeRate;
import com.datapath.persistence.entities.nbu.ExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateId> {

    ExchangeRate findOneById(ExchangeRateId id);

    Optional<ExchangeRate> findFirstByOrderByDateDesc();
}
