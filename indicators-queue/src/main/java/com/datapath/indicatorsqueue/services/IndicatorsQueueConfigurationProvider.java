package com.datapath.indicatorsqueue.services;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.datapath.persistence.entities.queue.IndicatorsQueueConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndicatorsQueueConfigurationProvider {

    private static final int CONFIGURATION_ID = 1;

    @Getter
    private Double mixedTopRiskPercentage;
    @Getter
    private Double lowTopRiskPercentage;
    @Getter
    private Double mediumTopRiskPercentage;
    @Getter
    private Double highTopRiskPercentage;

    @Getter
    private IndicatorsImpactRange mixedIndicatorImpactRange;
    @Getter
    private IndicatorsImpactRange lowIndicatorImpactRange;
    @Getter
    private IndicatorsImpactRange mediumIndicatorImpactRange;
    @Getter
    private IndicatorsImpactRange highIndicatorImpactRange;

    @Getter
    private Double lowTopRiskProcuringEntityPercentage;
    @Getter
    private Double mediumTopRiskProcuringEntityPercentage;
    @Getter
    private Double highTopRiskProcuringEntityPercentage;

    private IndicatorsQueueConfigurationService indicatorsQueueConfigurationService;

    public IndicatorsQueueConfigurationProvider(IndicatorsQueueConfigurationService indicatorsQueueConfigurationService) {
        this.indicatorsQueueConfigurationService = indicatorsQueueConfigurationService;
    }

    public void init() {
        IndicatorsQueueConfiguration config = indicatorsQueueConfigurationService.getConfigurationById(CONFIGURATION_ID);

        mixedTopRiskPercentage = config.getMixedTopRiskPercentage();
        lowTopRiskPercentage = config.getLowTopRiskPercentage();
        mediumTopRiskPercentage = config.getMediumTopRiskPercentage();
        highTopRiskPercentage = config.getHighTopRiskPercentage();

        mixedIndicatorImpactRange = new IndicatorsImpactRange(
                config.getMinMixedIndicatorImpactRange(),
                config.getMaxMixedIndicatorImpactRange()
        );
        lowIndicatorImpactRange = new IndicatorsImpactRange(
                config.getMinLowIndicatorImpactRange(),
                config.getMaxLowIndicatorImpactRange()
        );
        mediumIndicatorImpactRange = new IndicatorsImpactRange(
                config.getMinMediumIndicatorImpactRange(),
                config.getMaxMediumIndicatorImpactRange()
        );
        highIndicatorImpactRange= new IndicatorsImpactRange(
                config.getMinHighIndicatorImpactRange(),
                config.getMaxHighIndicatorImpactRange()
        );

        lowTopRiskProcuringEntityPercentage = config.getLowTopRiskProcuringEntityPercentage();
        mediumTopRiskProcuringEntityPercentage = config.getMediumTopRiskProcuringEntityPercentage();
        highTopRiskProcuringEntityPercentage = config.getHighTopRiskProcuringEntityPercentage();
    }
}
