package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.IndicatorsQueueRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorsQueueRegionRepository extends JpaRepository<IndicatorsQueueRegion, Long> {

}
