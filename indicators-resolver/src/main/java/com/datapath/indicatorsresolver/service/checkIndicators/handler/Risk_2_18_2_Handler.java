package com.datapath.indicatorsresolver.service.checkIndicators.handler;

import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.request.RequestDTO;
import com.datapath.indicatorsresolver.request.RequestsService;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.indicatorsresolver.service.checkIndicators.processors.Risk_2_18_2_Processor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class Risk_2_18_2_Handler extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-2-18-2";
    private boolean indicatorAvailable;

    private Risk_2_18_2_Processor processor;
    private RequestsService requestsService;

    public Risk_2_18_2_Handler(Risk_2_18_2_Processor processor, RequestsService requestsService) {
        this.processor = processor;
        this.requestsService = requestsService;
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
    @Scheduled(cron = "${risk-2-18-2.cron}")
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

        Map<String, RequestDTO> requestMap = getRequestMap();

        while (true) {
            List<Tender> tenders = tenderRepository.findTenders(dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind())
            );

            if (isEmpty(tenders)) {
                break;
            }

            List<TenderIndicator> tenderIndicators = processor.process(indicator, tenders,requestMap);

            tenderIndicators.forEach(this::uploadIndicator);

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(tenders);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicator.setDateChecked(ZonedDateTime.now());
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        indicator.setDateChecked(ZonedDateTime.now());
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private Map<String, RequestDTO> getRequestMap() {
        Map<String, RequestDTO> result = new HashMap<>();
        requestsService.getRequests()
                .forEach(request -> result.put(request.getTenderId(), request));
        return result;
    }

    public ZonedDateTime getMaxTenderDateCreated(List<Tender> tenders) {
        return tenders
                .stream()
                .map(Tender::getDateCreated)
                .max(Comparator.comparing(item -> item)).get();
    }
}
