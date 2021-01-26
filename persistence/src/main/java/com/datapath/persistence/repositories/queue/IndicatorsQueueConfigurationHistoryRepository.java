package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.IndicatorsQueueConfigurationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorsQueueConfigurationHistoryRepository extends JpaRepository<IndicatorsQueueConfigurationHistory, Long> {

}
