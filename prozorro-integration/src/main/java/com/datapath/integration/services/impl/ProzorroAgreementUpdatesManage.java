package com.datapath.integration.services.impl;

import com.datapath.integration.domain.AgreementResponseEntity;
import com.datapath.integration.domain.AgreementUpdateInfo;
import com.datapath.integration.domain.AgreementsPageResponse;
import com.datapath.integration.parsers.impl.AgreementParser;
import com.datapath.integration.services.AgreementLoaderService;
import com.datapath.integration.services.AgreementUpdatesManager;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.persistence.entities.Agreement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ProzorroAgreementUpdatesManage implements AgreementUpdatesManager {

    @Value("${prozorro.agreements.url}")
    private String apiUrl;

    private final AgreementLoaderService agreementLoaderService;

    public ProzorroAgreementUpdatesManage(ProzorroAgreementLoaderService agreementLoaderService) {
        this.agreementLoaderService = agreementLoaderService;
    }

    @Override
    public void loadLastModifiedAgreements() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            ZonedDateTime dateOffset = agreementLoaderService.resolveDateOffset();

            String url = ProzorroRequestUrlCreator.createAgreementsUrl(apiUrl, dateOffset);

            while (true) {
                log.info("Fetch agreements from [{}]", url);

                AgreementsPageResponse response = agreementLoaderService.loadAgreementPage(url);
                String nextPageUrl = URLDecoder.decode(response.getNextPage().getUri(), StandardCharsets.UTF_8.name());

                log.info("Fetched {} items", response.getItems().size());
                List<AgreementUpdateInfo> items = response.getItems();

                if (items.isEmpty()) {
                    log.info("No items found on page. Agreements loading break");
                    break;
                }

                List<Future<AgreementResponseEntity>> futures = new ArrayList<>();
                for (AgreementUpdateInfo info : items) {
                    Future<AgreementResponseEntity> future = executor.submit(() -> agreementLoaderService.loadAgreement(info));
                    futures.add(future);
                }

                for (Future<AgreementResponseEntity> future : futures) {
                    AgreementResponseEntity entity = future.get();
                    Agreement agreement = AgreementParser.parse(entity);
                    agreement.setSource(EntitySource.AGREEMENT.toString());

                    try {
                        agreementLoaderService.saveAgreement(agreement);
                    } catch (Exception e) {
                        log.error("Agreement {} not saved. Reason {}", agreement.getOuterId(), e.getMessage());
                    }
                }

                log.info("All agreements from page {} saved.", url);

                if (url.equalsIgnoreCase(nextPageUrl)) break;
                url = nextPageUrl;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            executor.shutdown();
        }

        log.info("All updated agreements loaded");
    }

}
