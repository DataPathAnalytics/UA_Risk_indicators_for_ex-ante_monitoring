package com.datapath.indicatorsresolver.service.checkIndicators.handler;

import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.indicatorsresolver.service.checkIndicators.processors.Risk_1_13_Processor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_13_Handler extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-13";
    private boolean indicatorAvailable;

    private Risk_1_13_Processor processor;

    public Risk_1_13_Handler(Risk_1_13_Processor processor) {
        this.processor = processor;
        indicatorAvailable = true;
    }

    public void handle(ZonedDateTime dateTime) {
        try {
            indicatorAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                handle(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorAvailable = true;
        }
    }

    @Async
    @Scheduled(cron = "${risk-1-13.cron}")
    public void handle() {
        if (!indicatorAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = getIndicatorLastCheckedDate(indicator);
                handle(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorAvailable = true;
        }
    }

    private void handle(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);

        while (true) {
            List<Long> tenderIds = tenderRepository.findTenderIds(dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind())
            );

            if (isEmpty(tenderIds)) {
                break;
            }

            List<TenderIndicator> tenderIndicators = processor.process(indicator, tenderIds);

            tenderIndicators.forEach(this::uploadIndicator);

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(tenderIndicators, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicator.setDateChecked(ZonedDateTime.now());
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        indicator.setDateChecked(ZonedDateTime.now());
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }
}
