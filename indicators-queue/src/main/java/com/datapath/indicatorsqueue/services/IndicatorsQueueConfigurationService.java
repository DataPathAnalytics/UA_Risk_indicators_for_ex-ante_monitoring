package com.datapath.indicatorsqueue.services;

import com.datapath.persistence.entities.queue.IndicatorsQueueConfiguration;
import com.datapath.persistence.repositories.queue.IndicatorsQueueConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndicatorsQueueConfigurationService {

    private IndicatorsQueueConfigurationRepository repository;

    public IndicatorsQueueConfigurationService(IndicatorsQueueConfigurationRepository repository) {
        this.repository = repository;
    }

    public IndicatorsQueueConfiguration getConfigurationById(Integer id) {
        return repository.findOneById(id);
    }

    public IndicatorsQueueConfiguration save(IndicatorsQueueConfiguration indicatorsQueueConfiguration) {
        return repository.save(indicatorsQueueConfiguration);
    }
}
