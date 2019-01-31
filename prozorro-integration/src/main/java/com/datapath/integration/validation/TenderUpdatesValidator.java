package com.datapath.integration.validation;

import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponseEntity;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.services.impl.ProzorroTenderUpdatesManager;
import com.datapath.integration.services.impl.TenderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TenderUpdatesValidator {

    private static final int TENDERS_LIMIT = 1000;

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
        ZonedDateTime lastDateModified = tenderService
                .findLastModifiedEntry()
                .getDateModified()
                .minusDays(1);

        log.info("Start tender update validation. StartDate: {}, EndDate: {}", yearEarlier, lastDateModified);

        ZonedDateTime dateOffset = yearEarlier.withZoneSameInstant(ZoneId.of("Europe/Kiev"));

        String url = ProzorroRequestUrlCreator.createTendersUrl(
                ProzorroTenderUpdatesManager.TENDERS_SEARCH_URL, dateOffset, TENDERS_LIMIT);

        List<TenderUpdateInfo> tenderUpdateInfos = new ArrayList<>();

        while (true) {
            try {
                TendersPageResponseEntity tendersPageResponseEntity = tenderLoaderService.loadTendersPage(url);
                List<TenderUpdateInfo> items = tendersPageResponseEntity.getItems()
                        .stream().filter(item -> item.getDateModified().isBefore(lastDateModified))
                        .collect(Collectors.toList());

                tenderUpdateInfos.addAll(items);

                if (items.size() < TENDERS_LIMIT) {
                    break;
                }

                url = URLDecoder.decode(tendersPageResponseEntity.getNextPage().getUri(), "UTF-8");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        log.info("Tenders loading finished. Fetched {} items", tenderUpdateInfos.size());

        Set<String> tendersOuterIds = tenderUpdateInfos.stream()
                .map(TenderUpdateInfo::getId)
                .collect(Collectors.toSet());

        Map<String, ZonedDateTime> existingTendersOuterIds = fetchExistingTendersOuterIdsAndDateModified(tendersOuterIds);

        log.info("Existed tenders count: {}", existingTendersOuterIds.size());

        List<TenderUpdateInfo> infos = new ArrayList<>();
        tenderUpdateInfos.forEach(tenderUpdateInfo -> {
            ZonedDateTime dateModified = existingTendersOuterIds.get(tenderUpdateInfo.getId());
            if (dateModified != null) {
                if (!dateModified.isEqual(tenderUpdateInfo.getDateModified())) {
                    log.info("Tender is not up to date: {} Expected date: {} Actual date: {}",
                            tenderUpdateInfo.getId(), tenderUpdateInfo.getDateModified(), dateModified);

                    infos.add(tenderUpdateInfo);
                }
            }
        });

        log.info("There are {} not up to date tenders. Start updating...", infos.size());
        tenderUpdatesManager.saveTendersFromUpdateInfo(infos);
        log.info("Tenders updating finished.");
    }

    private Map<String, ZonedDateTime> fetchExistingTendersOuterIdsAndDateModified(Set<String> tendersOuterIds) {
        Map<String, ZonedDateTime> existingTendersOuterIdsAndDateModified = new TreeMap<>();
        int pageSize = 1000;
        int currentIndex = 0;

        while (currentIndex <= tendersOuterIds.size() - 1) {
            int nextIndex = currentIndex + pageSize < tendersOuterIds.size() - 1 ?
                    currentIndex + pageSize : tendersOuterIds.size();

            List<String> tenderIds = new ArrayList<>(tendersOuterIds).subList(currentIndex, nextIndex);
            Map<String, ZonedDateTime> existingTenderIds = tenderService.getExistingTenderOuterIdsAndDateModifiedByOuterIds(tenderIds);

            existingTendersOuterIdsAndDateModified.putAll(existingTenderIds);

            currentIndex += pageSize;
        }

        return existingTendersOuterIdsAndDateModified;
    }
}
