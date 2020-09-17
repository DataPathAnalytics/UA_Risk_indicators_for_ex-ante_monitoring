package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.parsers.impl.ContractParser;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class ProzorroTenderUpdatesManager implements TenderUpdatesManager {

    @Value("${prozorro.tenders.url}")
    private String apiUrl;

    @Value("${prozorro.tenders.skip-test}")
    private boolean skipTestTenders;

    private TenderLoaderService tenderLoaderService;
    private ContractLoaderService contractLoaderService;
    private TransactionVariablesResolver tvResolver;
    private TenderDataValidator tenderDataValidator;

    public ProzorroTenderUpdatesManager(TenderLoaderService tenderLoaderService,
                                        TransactionVariablesResolver tvResolver,
                                        ContractLoaderService contractLoaderService,
                                        TenderDataValidator tenderDataValidator) {
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.contractLoaderService = contractLoaderService;
        this.tenderDataValidator = tenderDataValidator;
    }

    @Override
    public void loadLastModifiedTenders() {
        log.info("Started updating tenders");
        try {
            ZonedDateTime dateOffset = tenderLoaderService.resolveDateOffset()
                    .withZoneSameInstant(ZoneId.of("Europe/Kiev"));

            String url = ProzorroRequestUrlCreator.createTendersUrl(apiUrl, dateOffset);
            while (true) {
                log.info("Fetching tenders from [{}]", url);

                TendersPageResponseEntity tendersPageResponseEntity = tenderLoaderService.loadTendersPage(url);
                String nextPageUrl = URLDecoder.decode(tendersPageResponseEntity.getNextPage().getUri(), StandardCharsets.UTF_8.name());
                log.info("Fetched {} items", tendersPageResponseEntity.getItems().size());
                log.info("Next page url [{}]", nextPageUrl);
                List<TenderUpdateInfo> items = tendersPageResponseEntity.getItems();
                for (TenderUpdateInfo tenderUpdateInfo : items) {
                    try {
                        if (tenderUpdateInfo.getDateModified().isAfter(ZonedDateTime.now().minusMinutes(1))) {
                            log.info("Tenders loading paused");
                            return;
                        }
                        TenderResponseEntity tenderResponseEntity = tenderLoaderService.loadTender(tenderUpdateInfo);
                        log.info("Fetching tender: id = {}", tenderResponseEntity.getId());

                        TenderParser tenderParser = new TenderParser();
                        tenderParser.setRawData(tenderResponseEntity.getData());
                        tenderParser.setSkipTestTenders(skipTestTenders);
                        tenderParser.parseRawData();
                        Tender tender = tenderParser.buildTender();
                        tender.setSource(EntitySource.TENDERING.toString());

                        if (tenderUpdateInfo.getDateModified().isBefore(tender.getDateModified())) {
                            log.info("Newest version of tender {} exists.", tenderUpdateInfo.getId());
                            continue;
                        }

                        String subjectOfProcurement = tvResolver.getSubjectOfProcurement(tender);
                        tender.setTvSubjectOfProcurement(subjectOfProcurement);

                        String tenderCPV = tvResolver.getTenderCPV(tender);
                        tender.setTvTenderCPV(tenderCPV);

                        // Contracts loading
                        for (TenderContract tenderContract : tender.getTenderContracts()) {
                            ContractUpdateInfo contractUpdateInfo = new ContractUpdateInfo();
                            contractUpdateInfo.setId(tenderContract.getOuterId());
                            try {
                                ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
                                Contract contract = ContractParser.create(contractResponseEntity.getData()).buildContractEntity();
                                contract.setSource(EntitySource.TENDERING.toString());
                                contract.setTenderContract(tenderContract);
                                tenderContract.setContract(contract);
                            } catch (Exception ex) {
                                tenderContract.setContract(null);
                                log.warn("Contract with outer id {} not found. {}", tenderContract.getOuterId(), ex.getMessage());
                            }
                        }

                        if (!tenderDataValidator.isValidTender(tender)) {
                            log.error("Tender validation failed. Tender outer id = {}", tender.getOuterId());
                            continue;
                        }

                        Tender savedTender = tenderLoaderService.saveTender(tender);
                        log.info("Tender saved, id = {}", savedTender.getId());
                    } catch (TenderValidationException e) {
                        log.warn("Tender expired or it is test tender: outerId = {}", tenderUpdateInfo.getId(), e);
                    } catch (ConstraintViolationException e) {
                        log.error("Error while processing the tender: outerId = {} ", tenderUpdateInfo.getId(), e);
                        removeTender(tenderUpdateInfo.getId());
                        return;
                    } catch (ResourceAccessException e) {
                        log.error("Error in loading tenders: outerId = {}", e.getMessage(), e);
                        return;
                    } catch (Exception e) {
                        log.error("Error while processing the tender: outerId = {}", tenderUpdateInfo.getId(), e);
                        return;
                    }
                }

                if (items.isEmpty()) {
                    log.info("No items found on page. Tenders loading break");
                    break;
                }

                if (url.equalsIgnoreCase(nextPageUrl)) {
                    break;
                }
                log.info("All tenders from page {} saved.", url);
                url = nextPageUrl;

            }
            log.info("All updated tenders loaded");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void saveTendersFromUpdateInfo(List<TenderUpdateInfo> tenderUpdateInfos) {
        log.info("Save tenders from UpdateInfo");
        for (TenderUpdateInfo tenderUpdateInfo : tenderUpdateInfos) {
            try {
                TenderResponseEntity tenderResponseEntity = tenderLoaderService.loadTender(tenderUpdateInfo);
                log.info("Fetching tender: id = {}", tenderResponseEntity.getId());

                TenderParser tenderParser = new TenderParser();
                tenderParser.setRawData(tenderResponseEntity.getData());
                tenderParser.setSkipTestTenders(skipTestTenders);
                tenderParser.parseRawData();
                Tender tender = tenderParser.buildTender();

                tender.setSource(EntitySource.TENDERING.toString());

                if (tenderUpdateInfo.getDateModified().isBefore(tender.getDateModified())) {
                    log.info("Newest version of tender {} exists.", tenderUpdateInfo.getId());
                    continue;
                }

                // Contracts loading
                for (TenderContract tenderContract : tender.getTenderContracts()) {
                    ContractUpdateInfo contractUpdateInfo = new ContractUpdateInfo();
                    contractUpdateInfo.setId(tenderContract.getOuterId());
                    try {
                        ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
                        Contract contract = ContractParser.create(contractResponseEntity.getData()).buildContractEntity();
                        contract.setSource(EntitySource.TENDERING.toString());
                        contract.setTenderContract(tenderContract);
                        tenderContract.setContract(contract);
                    } catch (ResourceAccessException e) {
                        log.error("Error while loading contract - {}", tenderContract.getOuterId());
                        log.error(e.getMessage(), e);
                        return;
                    }
                }

                if (!tenderDataValidator.isValidTender(tender)) {
                    log.error("Tender validation failed. Tender outer id = {}", tender.getOuterId());
                    continue;
                }

                String subjectOfProcurement = tvResolver.getSubjectOfProcurement(tender);
                tender.setTvSubjectOfProcurement(subjectOfProcurement);

                String tenderCPV = tvResolver.getTenderCPV(tender);
                tender.setTvTenderCPV(tenderCPV);

                Tender savedTender = tenderLoaderService.saveTender(tender);
                log.info("Tender saved {} ", savedTender.getId());
            } catch (ResourceAccessException e) {
                log.error("Error in loading tender {}", tenderUpdateInfo.getId(), e);
            } catch (Exception ex) {
                log.info("Failed to load tender {}", tenderUpdateInfo.getId(), ex);
            }
        }
    }

    private void removeTender(String outerId) {
        try {
            tenderLoaderService.removeTenderByOuterId(outerId);
            log.info("Tender {} deleted", outerId);
        } catch (Exception ex) {
            log.info("Tender delete error. Tender outer id: {}", outerId, ex);
        }
    }
}
