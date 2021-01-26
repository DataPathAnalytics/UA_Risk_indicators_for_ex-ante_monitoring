package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.request.RequestDTO;
import com.datapath.indicatorsresolver.request.RequestsService;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.repositories.MonitoringRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class Risk_2_18_3_Processor extends BaseExtractor {

    @Autowired
    private MonitoringRepository monitoringRepository;
    @Autowired
    private RequestsService requestsService;

    public List<TenderIndicator> process(Indicator indicator, List<Long> tenderIds,Map<String, RequestDTO> requestMap) {
        List<Tender> tenders = tenderRepository.findByIdIn(tenderIds);

        List<String> tenderOuterIds = tenders.stream().map(Tender::getOuterId).collect(toList());

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        Map<String, MonitoringEntity> monitoringMap = getMonitoringMap(tenderOuterIds);

        List<TenderIndicator> tenderIndicators = new LinkedList<>();


        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            int indicatorValue = RISK;

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());
            if (!requestMap.containsKey(tender.getOuterId())) {
                indicatorValue = CONDITIONS_NOT_MET;
            }

            MonitoringEntity tenderMonitoring = monitoringMap.get(tender.getOuterId());
            RequestDTO tenderRequest = requestMap.get(tender.getOuterId());

            if (tenderMonitoring != null && tenderRequest != null) {
                if (tenderMonitoring.getConclusionDate() != null && tenderRequest.getDateCreated().isBefore(tenderMonitoring.getConclusionDate())) {
                    indicatorValue = NOT_RISK;
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
