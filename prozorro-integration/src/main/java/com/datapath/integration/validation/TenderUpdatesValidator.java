package com.datapath.integration.validation;

import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponseEntity;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.services.impl.TenderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TenderUpdatesValidator {

    private static final int TENDERS_LIMIT = 1000;

    @Value("${prozorro.tenders.url}")
    private String tendersApiUrl;

    private TenderService tenderService;
    private TenderLoaderService tenderLoaderService;
    private TenderUpdatesManager tenderUpdatesManager;

    public TenderUpdatesValidator(TenderService tenderService,
                                  TenderLoaderService tenderLoaderService,
                                  TenderUpdatesManager tenderUpdatesManager) {

        this.tenderService = tenderService;
        this.tenderLoaderService = tenderLoaderService;
        this.tenderUpdatesManager = tenderUpdatesManager;
    }

    public void validate() {
        ZonedDateTime yearEarlier = DateUtils.yearEarlierFromNow();
        Tender lastModifiedTender = tenderService.findLastModifiedEntry();
        //Database is empty no need to validate smt
        if (lastModifiedTender == null) return;

        ZonedDateTime lastDateModified = lastModifiedTender.getDateModified().minusDays(1);
        log.trace("Start tender update validation. StartDate: {}, EndDate: {}", yearEarlier, lastDateModified);

        ZonedDateTime dateOffset = yearEarlier.withZoneSameInstant(ZoneId.of("Europe/Kiev"));

        String url = ProzorroRequestUrlCreator.createTendersUrl(
                tendersApiUrl, dateOffset, TENDERS_LIMIT);

        List<TenderUpdateInfo> tenderUpdateInfos = new ArrayList<>();

        while (true) {
            try {
                log.trace("Fetching tenders list for {}", url);
                TendersPageResponseEntity tendersPageResponseEntity = tenderLoaderService.loadTendersPage(url);
                List<TenderUpdateInfo> items = tendersPageResponseEntity.getItems()
                        .stream().filter(item -> item.getDateModified().isBefore(lastDateModified))
                        .collect(Collectors.toList());

                tenderUpdateInfos.addAll(items);
                log.trace("Fetching {} tenders", items.size());
                if (items.size() == 0) {
                    break;
                }

                url = URLDecoder.decode(tendersPageResponseEntity.getNextPage().getUri(), StandardCharsets.UTF_8.name());

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.trace("Tenders loading finished. Fetched {} items", tenderUpdateInfos.size());

        Set<String> tendersOuterIds = tenderUpdateInfos.stream()
                .map(TenderUpdateInfo::getId)
                .collect(Collectors.toSet());

        Map<String, ZonedDateTime> existingTendersOuterIds = fetchExistingTendersOuterIdsAndDateModified(tendersOuterIds);

        log.trace("Existed tenders count: {}", existingTendersOuterIds.size());

        List<TenderUpdateInfo> infos = new ArrayList<>();
        tenderUpdateInfos.forEach(tenderUpdateInfo -> {
            ZonedDateTime dateModified = existingTendersOuterIds.get(tenderUpdateInfo.getId());
            if (dateModified != null && !dateModified.isEqual(tenderUpdateInfo.getDateModified())) {
                log.warn("Tender is not up to date: {} Expected date: {} Actual date: {}",
                        tenderUpdateInfo.getId(), tenderUpdateInfo.getDateModified(), dateModified);

                infos.add(tenderUpdateInfo);
            }
        });

        log.warn("There are {} not up to date tenders. Start updating...", infos.size());
        tenderUpdatesManager.saveTendersFromUpdateInfo(infos);
        log.trace("Tenders updating finished.");
    }

    private Map<String, ZonedDateTime> fetchExistingTendersOuterIdsAndDateModified(Set<String> tendersOuterIds) {
        Map<String, ZonedDateTime> existingTendersOuterIdsAndDateModified = new TreeMap<>();
        int pageSize = 1000;
        int currentIndex = 0;
        log.trace("Start fetching existing tenders");
        while (currentIndex <= tendersOuterIds.size() - 1) {
            int nextIndex = currentIndex + pageSize < tendersOuterIds.size() - 1 ?
                    currentIndex + pageSize : tendersOuterIds.size();

            List<String> tenderIds = new ArrayList<>(tendersOuterIds).subList(currentIndex, nextIndex);
            Map<String, ZonedDateTime> existingTenderIds = tenderService.getExistingTenderOuterIdsAndDateModifiedByOuterIds(tenderIds);

            existingTendersOuterIdsAndDateModified.putAll(existingTenderIds);

            currentIndex += pageSize;
            log.trace("Checked {} from {} tenders", currentIndex, tendersOuterIds.size());
        }
        log.trace("Finished fetching existing tenders");
        return existingTendersOuterIdsAndDateModified;
    }
}
