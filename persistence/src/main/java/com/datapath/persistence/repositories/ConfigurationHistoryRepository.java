package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.ConfigurationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationHistoryRepository extends JpaRepository<ConfigurationHistory, Long> {
}
