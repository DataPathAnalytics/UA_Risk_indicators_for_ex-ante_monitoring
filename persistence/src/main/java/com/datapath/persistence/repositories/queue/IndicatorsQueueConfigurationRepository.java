package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.IndicatorsQueueConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorsQueueConfigurationRepository extends JpaRepository<IndicatorsQueueConfiguration, Integer> {

    IndicatorsQueueConfiguration findOneById(Integer id);

}
