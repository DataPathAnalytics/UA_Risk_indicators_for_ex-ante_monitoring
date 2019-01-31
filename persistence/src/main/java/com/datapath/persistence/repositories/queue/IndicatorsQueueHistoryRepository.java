package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.IndicatorsQueueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorsQueueHistoryRepository extends JpaRepository<IndicatorsQueueHistory, Long> {

    @Query("SELECT coalesce(max(i.id), 0) FROM IndicatorsQueueHistory i")
    Long getMaxId();

}
