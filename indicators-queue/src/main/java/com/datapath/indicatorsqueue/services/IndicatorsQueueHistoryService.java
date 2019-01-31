package com.datapath.indicatorsqueue.services;

import com.datapath.persistence.entities.queue.IndicatorsQueueHistory;
import com.datapath.persistence.repositories.queue.IndicatorsQueueHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndicatorsQueueHistoryService {

    private IndicatorsQueueHistoryRepository indicatorsQueueHistoryRepository;

    public IndicatorsQueueHistoryService(IndicatorsQueueHistoryRepository indicatorsQueueHistoryRepository) {
        this.indicatorsQueueHistoryRepository = indicatorsQueueHistoryRepository;
    }

    public Long getMaxId() {
        return indicatorsQueueHistoryRepository.getMaxId();
    }

    public IndicatorsQueueHistory save(IndicatorsQueueHistory history) {
        return indicatorsQueueHistoryRepository.save(history);
    }

}
