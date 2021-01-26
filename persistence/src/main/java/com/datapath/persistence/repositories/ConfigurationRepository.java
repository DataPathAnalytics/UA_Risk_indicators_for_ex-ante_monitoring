package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationRepository extends JpaRepository<Configuration, String> {

    Configuration findByKey(String key);
}
