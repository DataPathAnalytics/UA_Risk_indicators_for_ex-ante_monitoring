package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_1_3_1_Extractor extends BaseExtractor {

    /*
    Документи тендерної документації не позначені електронним підписом
    */
    private final String INDICATOR_CODE = "RISK1-3_1";
    private final String PKCS7_SIGNATURE_FORMAT = "application/pkcs7-signature";
    private boolean indicatorsResolverAvailable;


    public Risk_1_3_1_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_3_1_Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_1_3_1_Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk_1_3_1_Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tendersWithDocuments = tenderRepository.getTendersWithDocumentTypes(
                    dateTime,
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    PageRequest.of(page, size));
            if (tendersWithDocuments.isEmpty()) {
                break;
            }
            Set<String> tenders = new HashSet<>();

            List<TenderIndicator> tenderIndicators = new ArrayList<>();

            ZonedDateTime maxTenderDateCreated = ZonedDateTime.now();

            for (Object[] tendersWithDocument : tendersWithDocuments) {
                String tenderId = tendersWithDocument[0].toString();
                maxTenderDateCreated = toZonedDateTime((Timestamp) tendersWithDocument[1]);
                List<String> documentFormats = isNull(tendersWithDocument[2])
                        ? null
                        : Arrays.asList(tendersWithDocument[2].toString().split(COMMA_SEPARATOR));

                tenders.add(tenderId);
                int indicatorValue;
                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

                if (nonNull(documentFormats)) {
                    indicatorValue = documentFormats.contains(PKCS7_SIGNATURE_FORMAT) ? NOT_RISK : RISK;
                } else {
                    indicatorValue = -2;
                }

                tenderIndicators.add(new TenderIndicator(tenderDimensions,
                        indicator,
                        indicatorValue,
                        new ArrayList<>()));
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenders, INDICATOR_CODE);

            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicator(tenderIndicator);
            });

            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);
            dateTime = maxTenderDateCreated;
        }
        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);
    }
}
