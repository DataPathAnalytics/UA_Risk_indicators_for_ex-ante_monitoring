package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionIndicatorsQueueItemHistoryRepository extends JpaRepository<RegionIndicatorsQueueItemHistory, String> {

    List<RegionIndicatorsQueueItemHistory> findByTenderIdIn(List<String> tenderIds);
}

