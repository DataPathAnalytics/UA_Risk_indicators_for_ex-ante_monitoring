package com.datapath.integration.validation;

import com.datapath.integration.domain.TenderResponse;
import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponse;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.impl.TenderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.JsonUtils;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.persistence.entities.validation.TenderValidationHistory;
import com.datapath.persistence.repositories.validation.TenderValidationHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TenderExistenceValidator {

    private static final int TENDERS_LIMIT = 1000;

    @Value("${prozorro.tenders.url}")
    private String tendersApiUrl;

    private TenderService tenderService;
    private TenderLoaderService tenderLoaderService;
    private TenderValidationHistoryRepository tenderValidationHistoryRepository;

    public TenderExistenceValidator(TenderService tenderService,
                                    TenderLoaderService tenderLoaderService,
                                    TenderValidationHistoryRepository tenderValidationHistoryRepository) {

        this.tenderService = tenderService;
        this.tenderLoaderService = tenderLoaderService;
        this.tenderValidationHistoryRepository = tenderValidationHistoryRepository;
    }

    @Async
    public void validate(ZonedDateTime startDate) {
        ZonedDateTime lastDateModified = tenderService.findLastModifiedEntry().getDateModified();

        log.trace("Start tenders existence validation. StartDate: {}, EndDate: {}", startDate, lastDateModified);

        ZonedDateTime dateOffset = startDate.withZoneSameInstant(ZoneId.of("Europe/Kiev"));

        String url = ProzorroRequestUrlCreator.createTendersUrl(tendersApiUrl, dateOffset, TENDERS_LIMIT);

        List<TenderUpdateInfo> tenderUpdateInfos = new ArrayList<>();

        while (true) {
            try {
                TendersPageResponse tendersPageResponse = tenderLoaderService.loadTendersPage(url);
                List<TenderUpdateInfo> items = tendersPageResponse.getItems()
                        .stream().filter(item -> item.getDateModified().isBefore(lastDateModified))
                        .collect(Collectors.toList());

                log.trace("Fetched {} items by url {}", items.size(), url);

                tenderUpdateInfos.addAll(items);

                if (items.size() < TENDERS_LIMIT) {
                    break;
                }

                url = URLDecoder.decode(tendersPageResponse.getNextPage().getUri(), "UTF-8");

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.trace("Tenders loading finished. Fetched {} items", tenderUpdateInfos.size());

        Set<String> tendersOuterIds = tenderUpdateInfos.stream()
                .map(TenderUpdateInfo::getId)
                .collect(Collectors.toSet());

        Set<String> existingTendersOuterIds = fetchExistingTendersOuterIds(tendersOuterIds);
        Set<String> notExistingTenderOuterIds = findNotExistingTendersOuterIds(
                tendersOuterIds, existingTendersOuterIds);

        log.trace("Existing tenders count: {}. Not existing tenders count: {}",
                existingTendersOuterIds.size(), notExistingTenderOuterIds.size());

        Set<String> testAndExpiredTendersOuterIds = findTestAndExpiredTendersOuterIds(notExistingTenderOuterIds);


        int missingTenderCount = tendersOuterIds.size() - existingTendersOuterIds.size()
                - testAndExpiredTendersOuterIds.size();

        int tendersCount = tendersOuterIds.size();

        log.trace("Found {} test or expired tenders.", testAndExpiredTendersOuterIds.size());
        log.trace("Totals | Tenders count: {}", tendersCount);
        log.trace("Totals | Existing tenders count: {}", existingTendersOuterIds.size());
        log.trace("Totals | Test or expired tenders count: {}", testAndExpiredTendersOuterIds.size());
        log.warn("Totals | Number of missing tenders: {}", missingTenderCount);

        tendersOuterIds.removeAll(existingTendersOuterIds);
        tendersOuterIds.removeAll(testAndExpiredTendersOuterIds);
        tendersOuterIds.forEach(outerId -> log.trace("{}", outerId));

        String[] missingTenders = tendersOuterIds.toArray(new String[tendersOuterIds.size()]);

        TenderValidationHistory history = new TenderValidationHistory();
        history.setTendersCount(tendersCount);
        history.setExistingTendersCount(existingTendersOuterIds.size());
        history.setTestOrExpiredTendersCount(testAndExpiredTendersOuterIds.size());
        history.setMissingTendersCount(missingTenderCount);
        history.setMissingTenders(missingTenders);
        history.setDate(ZonedDateTime.now());

        tenderValidationHistoryRepository.save(history);

        log.trace("Tenders validation result saved.");
    }

    private Set<String> fetchExistingTendersOuterIds(Set<String> tendersOuterIds) {
        Set<String> existingTendersOuterIds = new TreeSet<>();
        int pageSize = 1000;
        int currentIndex = 0;

        while (currentIndex <= tendersOuterIds.size() - 1) {
            int nextIndex = currentIndex + pageSize < tendersOuterIds.size() - 1 ?
                    currentIndex + pageSize : tendersOuterIds.size();
            List<String> tenderIds = new ArrayList<>(tendersOuterIds).subList(currentIndex, nextIndex);
            List<String> existingTenderIds = tenderService.getExistingTenderOuterIdsByOuterIds(tenderIds);
            existingTendersOuterIds.addAll(existingTenderIds);
            currentIndex += pageSize;
        }

        return existingTendersOuterIds;
    }

    private Set<String> findNotExistingTendersOuterIds(Set<String> tendersOuterIds,
                                                       Set<String> existingTendersOuterIds) {
        return tendersOuterIds.stream()
                .filter(item -> !existingTendersOuterIds.contains(item))
                .collect(Collectors.toSet());
    }

    private Set<String> findTestAndExpiredTendersOuterIds(Set<String> notExistingTendersOuterIds) {
        List<String> testOrExpiredTenders = new ArrayList<>();
        notExistingTendersOuterIds.forEach(outerId -> {
            TenderUpdateInfo updateInfo = new TenderUpdateInfo();
            updateInfo.setId(outerId);
            TenderResponse tenderResponse = tenderLoaderService.loadTender(updateInfo);
            if (isTestOrExpiredTender(tenderResponse)) {
                testOrExpiredTenders.add(tenderResponse.getId());
            }
        });
        return new HashSet<>(testOrExpiredTenders);
    }

    private boolean isTestOrExpiredTender(TenderResponse responseEntity) {
        try {
            JsonNode node = new ObjectMapper().readTree(responseEntity.getData());
            ZonedDateTime date = JsonUtils.getDate(node, "/data/date");

            if (date == null || date.isBefore(DateUtils.yearEarlierFromNow())) {
                return true;
            }

            if (node.at("/data/mode").asText().equals("test")) {
                return true;
            }

        } catch (Exception e) {
            log.error("Error while check tender {} for existence.", responseEntity.getId());
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
