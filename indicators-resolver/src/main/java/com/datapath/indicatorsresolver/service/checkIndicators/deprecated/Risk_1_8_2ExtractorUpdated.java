package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;


import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Document;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Deprecated
public class Risk_1_8_2ExtractorUpdated extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK1-8_2";
    private static final String ACTIVE_STATUS = "ACTIVE";

    @Transactional
    public void extract() {
        Indicator indicator = getIndicator(INDICATOR_CODE);
        if (!indicator.isActive()) return;

        ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                ? ZonedDateTime.now().minusYears(1).withHour(0)
                : indicator.getLastCheckedDateCreated();

        Pageable pageRequest = PageRequest.of(0, 100);
        Page<Tender> tendersPage;
        do {
            tendersPage = tenderRepository.getRisk1_8_2Tenders(dateTime, pageRequest);
            pageRequest = tendersPage.nextPageable();

            List<Tender> tenders = tendersPage.getContent();
            processBatch(tenders, indicator);
        } while (tendersPage.hasNext());
    }

    private void processBatch(List<Tender> tenders, Indicator indicator) {
        if (tenders.isEmpty()) return;

        Set<String> tenderIds = tenders.stream().map(Tender::getOuterId).collect(Collectors.toSet());

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenders, INDICATOR_CODE);

        Map<String, Long> maxTendersIndicatorIteration = extractDataService
                .getMaxTenderIndicatorIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

        Map<String, Map<String, Integer>> maxTendersLotIterationData = extractDataService
                .getMaxTendersLotIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);


        Map<String, List<TenderIndicator>> indicatorsMap = new HashMap<>();

        for (Tender tender : tenders) {

            Map<String, Integer> lotIndicators = new HashMap<>();
            TenderDimensions tenderDimensions = new TenderDimensions(tender.getOuterId());

            for (Lot lot : tender.getLots()) {
                int indicatorValue;

                if (maxTendersLotIterationData.get(tender.getOuterId()).containsKey(lot.getOuterId()) && maxTendersLotIterationData.get(tender.getOuterId()).get(lot.getOuterId()).equals(1)) {
                    indicatorValue = RISK;
                } else if (Arrays.asList("cancelled", "unsuccessful").contains(lot.getStatus())) {
                    indicatorValue = CONDITIONS_NOT_MET;
                } else {
                    indicatorValue = calcLotIndicatorValue(lot);
                }

                lotIndicators.put(lot.getOuterId(), indicatorValue);
            }

            List<TenderIndicator> tenderIndicators = new ArrayList<>();
            for (Integer value : new HashSet<>(lotIndicators.values())) {
                List<String> lotIds = new ArrayList<>();
                lotIndicators.forEach((lotId, val) -> {
                    if (value.equals(val)) {
                        lotIds.add(lotId);
                    }
                });
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, value, lotIds));
            }
            indicatorsMap.put(tender.getOuterId(), tenderIndicators);
        }


        indicatorsMap.forEach((tenderId, tenderIndicators) -> {
            tenderIndicators.forEach(tenderIndicator -> tenderIndicator
                    .setTenderDimensions(dimensionsMap.get(tenderId)));
            uploadIndicatorIfNotExists(tenderId, INDICATOR_CODE, tenderIndicators);
        });

        Optional<ZonedDateTime> max = tenders.stream().map(Tender::getDateCreated).max(ZonedDateTime::compareTo);
        max.ifPresent(dateChecked -> {
            indicator.setLastCheckedDateCreated(dateChecked);
            indicatorRepository.save(indicator);
        });
    }

    private Integer calcLotIndicatorValue(Lot lot) {
        int indicatorValue = NOT_RISK;

        boolean hasManyContractsToAward = lot.getAwards()
                .stream()
                .anyMatch(a -> a.getTenderContracts().size() > 1);

        if (hasManyContractsToAward) return CONDITIONS_NOT_MET;

        boolean hasActiveContractsWithoutFormat = lot.getAwards()
                .stream()
                .filter(award -> !isEmpty(award.getTenderContracts()))
                .flatMap(award -> award.getTenderContracts().stream())
                .filter(c -> ACTIVE_STATUS.equalsIgnoreCase(c.getStatus()))
                .flatMap(c -> c.getDocuments().stream())
                .anyMatch(document -> !"application/pkcs7-signature".equalsIgnoreCase(document.getFormat()));

        List<Document> contractDocuments = lot.getAwards().stream()
                .filter(award -> !isEmpty(award.getTenderContracts()))
                .flatMap(award -> award.getTenderContracts().stream())
                .flatMap(c -> c.getDocuments().stream())
                .filter(doc -> "contract".equalsIgnoreCase(doc.getDocumentOf()))
                .collect(toList());

        boolean hasAnotherFormat = contractDocuments.stream().anyMatch(doc -> !"application/pkcs7-signature".equalsIgnoreCase(doc.getFormat()));

        boolean expiredAwardDate = lot.getAwards().stream()
                .filter(award -> ACTIVE_STATUS.equalsIgnoreCase(award.getStatus()))
                .anyMatch(award ->
                        (isEmpty(award.getComplaints()) && Duration.between(award.getDate(), ZonedDateTime.now()).toDays() > 22)
                                || (!isEmpty(award.getComplaints()) && Duration.between(award.getDate(), ZonedDateTime.now()).toDays() > 37)
                );

        if (!hasActiveContractsWithoutFormat
                && (isEmpty(contractDocuments) || hasAnotherFormat)
                && expiredAwardDate) {
            indicatorValue = RISK;
        }

        return indicatorValue;
    }
}
