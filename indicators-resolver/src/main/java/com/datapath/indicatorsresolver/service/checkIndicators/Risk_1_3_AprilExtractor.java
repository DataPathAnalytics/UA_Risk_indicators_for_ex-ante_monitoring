package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_3_AprilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-1-3";

    private static final Map<String, Pattern> CAUSE_PATTERN = new HashMap<String, Pattern>() {{
        put("technicalReasons", Pattern.compile(".*онопол.*|.*ідсутн.*конкур.*|.*постач.*територ.*"));
        put("twiceUnsuccessful", Pattern.compile(".*двічі відмінен.*|.*відсутн.*кільк.*учасник.*|.*ідсутн.*кільк.*пропозиц.*|.*вічі.*не відбулис.*"));
        put("additionalPurchase", Pattern.compile(".*додатк.*закупівл.*"));
        put("additionalConstruction", Pattern.compile(".*додатк.*робіт.*"));
        put("intProperty", Pattern.compile(".*мистецтв*|.*інтелектуаль.*власн.*|.*авторськ.*прав.*"));
        put("emergency", Pattern.compile(".*агальн.*потреб*"));
        put("stateLegalServices", Pattern.compile(".*юрид.*|.*юризд.*"));
        put("resolvingInsolvency", Pattern.compile(".*платоспром.*"));
        put("artPurchase", Pattern.compile(".*витв.*мист.*"));
        put("lastHope", Pattern.compile(".*останн.*наді.*"));
        put("contestWinner", Pattern.compile(".*переможц.*"));
        put("humanitarianAid", Pattern.compile(".*гуманіт.*"));
        put("contractCancelled", Pattern.compile(".*розірв.*"));
        put("activeComplaint", Pattern.compile(".*скарг.*"));
    }};

    private boolean indicatorsResolverAvailable;

    public Risk_1_3_AprilExtractor() {
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
    @Scheduled(cron = "${risk-1-3.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getDateChecked())
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
            List<Tender> tenders = tenderRepository.findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind())
            );

            if (isEmpty(tenders)) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    tenders, INDICATOR_CODE);

            List<TenderIndicator> tenderIndicators = new LinkedList<>();

            tenders.forEach(tender -> {
                log.info("Process tender {}", tender.getOuterId());

                TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());

                int indicatorValue = IMPOSSIBLE_TO_DETECT;

                try {
                    if (tender.getCause() != null && tender.getCauseDescription() != null) {
                        for (Map.Entry<String, Pattern> causePattern : CAUSE_PATTERN.entrySet()) {
                            if (causePattern.getValue().matcher(tender.getCauseDescription()).find()) {
                                if (tender.getCause().equals(causePattern.getKey())) {
                                    indicatorValue = NOT_RISK;
                                    break;
                                } else {
                                    indicatorValue = RISK;
                                }
                            }
                        }
                    } else {
                        indicatorValue = CONDITIONS_NOT_MET;
                    }
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tender.getOuterId(), e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
            });

            tenderIndicators.forEach(this::uploadIndicator);

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicator.setDateChecked(ZonedDateTime.now());
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        log.info("{} indicator finished", INDICATOR_CODE);
    }

}
