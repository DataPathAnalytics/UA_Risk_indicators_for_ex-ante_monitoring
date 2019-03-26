package com.datapath.indicatorsqueue.services;

import com.datapath.persistence.entities.queue.IndicatorsQueueRegion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IndicatorsQueueRegionProvider implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private static final long REGIONS_LOADING_RATE = 60_000;

    private List<IndicatorsQueueRegion> regions;
    private Set<String> regionNames;
    private Map<String, String> regionMapping;

    private IndicatorsQueueRegionService indicatorsQueueRegionService;

    public IndicatorsQueueRegionProvider(IndicatorsQueueRegionService indicatorsQueueRegionService) {
        this.indicatorsQueueRegionService = indicatorsQueueRegionService;
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            init();
        }
    }

    @Scheduled(fixedRate = REGIONS_LOADING_RATE)
    private void init() {
        regions = indicatorsQueueRegionService.getAllRegions();
        regionMapping = new HashMap<>();
        regions.forEach(region -> regionMapping.put(
                region.getOriginalName(), region.getCorrectName()));
        regionNames = regions.stream()
                .map(IndicatorsQueueRegion::getCorrectName)
                .collect(Collectors.toSet());
    }

    public Map<String, String> getRegionMapping() {
        return this.regionMapping;
    }

    public String getRegionCorrectName(String originalName) {
        return this.regionMapping.get(originalName);
    }

    public Set<String> getRegions() {
        return this.regionNames;
    }
}
