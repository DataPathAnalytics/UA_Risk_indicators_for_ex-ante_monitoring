package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.derivatives.NearThresholdOneSupplier;
import com.datapath.persistence.repositories.derivatives.NearThresholdOneSupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;


@Service
@Slf4j
public class Risk_2_7_1_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-2-7-1";
    private boolean indicatorsResolverAvailable;
    private NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository;

    public Risk_2_7_1_AprilExtractor(NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository) {
        this.nearThresholdOneSupplierRepository = nearThresholdOneSupplierRepository;
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    @Async
    @Scheduled(cron = "${risk-2-7-1.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkIndicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = tenderRepository.findGoodsServicesTenderIdByProcedureStatusAndProcedureTypeByMainProcurementCategory(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    Arrays.asList("goods", "services")
            );

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> tenderDimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            String tenderIdsStr = String.join(",", tenders);
            List<Object[]> tendersInfo = tenderRepository.getTenderIdPendingContractsCountProcuringEnrityIdKindSupplierAndAmount(tenderIdsStr);

            tendersInfo.forEach(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();
                TenderDimensions tenderDimensions = tenderDimensionsMap.get(tenderId);

                log.info("Process tender {}", tenderId);

                int indicatorValue = NOT_RISK;
                try {
                    int pendingContractsCount = Integer.parseInt(tenderInfo[1].toString());
                    List<String> suppliers = isNull(tenderInfo[2])
                            ? null : Arrays.asList(tenderInfo[2].toString().split(","));
                    Double amount = isNull(tenderInfo[3]) ? null : Double.parseDouble(tenderInfo[3].toString());
                    String procuringEntity = tenderInfo[4].toString();

                    if (pendingContractsCount == 0) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {
                        if (amount > 950000 && amount < 1000000) {
                            indicatorValue = RISK;
                        }
                        if (indicatorValue == RISK) {
                            Optional<NearThresholdOneSupplier> nearThreshold = nearThresholdOneSupplierRepository
                                    .findFirstByProcuringEntityAndSupplierIn(procuringEntity, suppliers);
                            if (!nearThreshold.isPresent()) {
                                indicatorValue = NOT_RISK;
                            }
                        }
                    }
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, indicatorValue);
                uploadIndicatorIfNotExists(tenderIndicator.getTenderDimensions().getId(), INDICATOR_CODE, tenderIndicator);

            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(tenderDimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }
        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }
}
