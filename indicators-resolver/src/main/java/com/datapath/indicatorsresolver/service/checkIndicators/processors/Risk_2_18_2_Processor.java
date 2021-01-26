package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.request.RequestDTO;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.repositories.MonitoringRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class Risk_2_18_2_Processor extends BaseExtractor {

    private static final List<String> STATUSES = Arrays.asList("complete", "cancelled", "unsuccessful");

    @Autowired
    private MonitoringRepository monitoringRepository;

    public List<TenderIndicator> process(Indicator indicator, List<Tender> tenders, Map<String, RequestDTO> requestMap) {

        List<String> tenderOuterIds = tenders.stream().map(Tender::getOuterId).collect(toList());

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        Map<String, MonitoringEntity> monitoringMap = getMonitoringMap(tenderOuterIds);

        List<TenderIndicator> tenderIndicators = new LinkedList<>();


        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            if (STATUSES.contains(tender.getStatus()) && tender.getDate().isBefore(ZonedDateTime.now().minusDays(1))) {
                return;
            }

            int indicatorValue = NOT_RISK;

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());
            if (!requestMap.containsKey(tender.getOuterId())) {
                indicatorValue = CONDITIONS_NOT_MET;
            }

            MonitoringEntity tenderMonitoring = monitoringMap.get(tender.getOuterId());
            RequestDTO tenderRequest = requestMap.get(tender.getOuterId());

            if (tenderMonitoring != null && tenderRequest != null) {
                if (tenderMonitoring.getConclusionDate() != null && tenderRequest.getDateCreated().isBefore(tenderMonitoring.getConclusionDate())) {
                    indicatorValue = RISK;
                }
            }
            tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
        });

        return tenderIndicators;
    }

    private Map<String, MonitoringEntity> getMonitoringMap(List<String> tenderOuterIds) {
        Map<String, MonitoringEntity> tendersMonitoringMap = new HashMap<>();
        monitoringRepository
                .findAllByTenderIdIn(tenderOuterIds)
                .forEach(monitoring -> tendersMonitoringMap.put(monitoring.getTenderId(), monitoring));
        return tendersMonitoringMap;
    }
}
