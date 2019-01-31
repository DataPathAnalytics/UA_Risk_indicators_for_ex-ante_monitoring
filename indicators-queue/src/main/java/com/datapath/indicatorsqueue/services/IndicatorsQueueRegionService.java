package com.datapath.indicatorsqueue.services;

import com.datapath.persistence.entities.queue.IndicatorsQueueRegion;
import com.datapath.persistence.repositories.queue.IndicatorsQueueRegionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class IndicatorsQueueRegionService {

    private IndicatorsQueueRegionRepository indicatorsQueueRegionRepository;

    public IndicatorsQueueRegionService(IndicatorsQueueRegionRepository indicatorsQueueRegionRepository) {
        this.indicatorsQueueRegionRepository = indicatorsQueueRegionRepository;
    }

    public List<IndicatorsQueueRegion> getAllRegions() {
        return indicatorsQueueRegionRepository.findAll();
    }

    public IndicatorsQueueRegion save(IndicatorsQueueRegion indicatorsQueueRegion) {
        return indicatorsQueueRegionRepository.save(indicatorsQueueRegion);
    }

}
