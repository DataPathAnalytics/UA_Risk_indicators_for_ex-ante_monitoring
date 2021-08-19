package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.parsers.impl.AgreementParser;
import com.datapath.integration.parsers.impl.ContractParser;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.AgreementLoaderService;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.Agreement;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ProzorroTenderUpdatesManager implements TenderUpdatesManager {

    @Value("${prozorro.tenders.url}")
    private String apiUrl;

    private TenderLoaderService tenderLoaderService;
    private ContractLoaderService contractLoaderService;
    private TransactionVariablesResolver tvResolver;
    private TenderDataValidator tenderDataValidator;
    private AgreementLoaderService agreementLoaderService;
    private TenderAuctionUpdateService tenderAuctionService;

    public ProzorroTenderUpdatesManager(TenderLoaderService tenderLoaderService,
                                        TransactionVariablesResolver tvResolver,
                                        ContractLoaderService contractLoaderService,
                                        TenderDataValidator tenderDataValidator,
                                        AgreementLoaderService agreementLoaderService,
                                        TenderAuctionUpdateService tenderAuctionService) {
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.contractLoaderService = contractLoaderService;
        this.tenderDataValidator = tenderDataValidator;
        this.agreementLoaderService = agreementLoaderService;
        this.tenderAuctionService = tenderAuctionService;
    }

    @Override
    public void loadLastModifiedTenders() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            ZonedDateTime dateOffset = tenderLoaderService.resolveDateOffset();

            String url = ProzorroRequestUrlCreator.createTendersUrl(apiUrl, dateOffset);

            while (true) {
                log.info("Fetch tenders from [{}]", url);

                TendersPageResponse tendersPageResponse = tenderLoaderService.loadTendersPage(url);
                String nextPageUrl = URLDecoder.decode(tendersPageResponse.getNextPage().getUri(), StandardCharsets.UTF_8.name());
                log.info("Next page url {}", nextPageUrl);
                log.info("Fetched {} items", tendersPageResponse.getItems().size());
                List<TenderUpdateInfo> items = tendersPageResponse.getItems();

                if (items.isEmpty()) {
                    log.info("No items found on page. Tenders loading break");
                    break;
                }

                List<Future<Tender>> futures = new ArrayList<>();
                for (TenderUpdateInfo tenderInfo : items) {

                    //fixme check on LocalTime. Maybe get time once before for cycle
                    if (tenderInfo.getDateModified().isAfter(ZonedDateTime.now().minusHours(2))) {
                        log.info("Tenders loading paused");
                        break;
                    }

                    Future<Tender> future = executor.submit(() -> loadTender(tenderInfo));
                    futures.add(future);
                }

                for (Future<Tender> future : futures) {

                    Tender tender = future.get();

                    if (!tenderDataValidator.isProcessable(tender)) {
                        log.info("Tender [{}[ is not processable", tender.getOuterId());
                        continue;
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
                    tenderAuctionService.persistBidLotAmountAuctionData(savedTender.getId());
                }

                log.info("All tenders from page {} saved.", url);

                if (url.equalsIgnoreCase(nextPageUrl)) break;
                url = nextPageUrl;

            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            executor.shutdown();
        }

        log.info("All updated tenders loaded");
    }

    private Tender loadTender(TenderUpdateInfo tenderUpdateInfo) throws Exception {
        TenderResponse tenderResponse = tenderLoaderService.loadTender(tenderUpdateInfo);
        log.info("Fetching tender by id = [{}]", tenderResponse.getId());
        TenderParser tenderParser = TenderParser.create(tenderResponse.getData());
        Tender tender = tenderParser.buildTenderEntity();
        tender.setSource(EntitySource.TENDERING.toString());

        for (TenderContract tenderContract : tender.getTenderContracts()) {
            ContractUpdateInfo contractUpdateInfo = new ContractUpdateInfo();
            contractUpdateInfo.setId(tenderContract.getOuterId());
            try {
                ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
                Contract contract = ContractParser.create(contractResponseEntity.getData()).buildContractEntity();
                contract.setSource(EntitySource.TENDERING.toString());
                contract.setTender(tender);
                contract.setTenderContract(tenderContract);
                tenderContract.setContract(contract);
            } catch (Exception ex) {
                tenderContract.setContract(null);
                log.warn("Contract with outer id {} not found. {}", tenderContract.getOuterId(), ex.getMessage());
            }
        }

        List<Agreement> agreements = new LinkedList<>();
        for (String agreementOuterId : tender.getAgreementOuterIds()) {
            AgreementUpdateInfo updateInfo = new AgreementUpdateInfo();
            updateInfo.setId(agreementOuterId);
            try {
                AgreementResponseEntity responseEntity = agreementLoaderService.loadAgreement(updateInfo);
                Agreement agreement = AgreementParser.parse(responseEntity);
                agreement.setSource(EntitySource.TENDERING.toString());
                AgreementTenderJoinService.joinInnerElements(agreement, tender);
                agreements.add(agreement);
            } catch (Exception ex) {
                log.warn("Agreement with outer id {} not found. {}", agreementOuterId, ex.getMessage());
            }
        }
        tender.setAgreements(agreements);

        return tender;
    }

    @Override
    public void saveTendersFromUpdateInfo(List<TenderUpdateInfo> tenderUpdateInfos) {
        log.info("Save tenders from UpdateInfo");
        for (TenderUpdateInfo tenderUpdateInfo : tenderUpdateInfos) {
            try {
                TenderResponse tenderResponse = tenderLoaderService.loadTender(tenderUpdateInfo);
                log.info("Fetching tender: id = {}", tenderResponse.getId());
                TenderParser tenderParser = TenderParser.create(tenderResponse.getData());
                Tender tender = tenderParser.buildTenderEntity();
                tender.setSource(EntitySource.TENDERING.toString());

                if (tenderUpdateInfo.getDateModified().isBefore(tender.getDateModified())) {
                    log.info("Newest version of tender {} exists.", tenderUpdateInfo.getId());
                    continue;
                }

                for (TenderContract tenderContract : tender.getTenderContracts()) {
                    ContractUpdateInfo contractUpdateInfo = new ContractUpdateInfo();
                    contractUpdateInfo.setId(tenderContract.getOuterId());
                    try {
                        ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
                        Contract contract = ContractParser.create(contractResponseEntity.getData()).buildContractEntity();
                        contract.setSource(EntitySource.TENDERING.toString());
                        contract.setTender(tender);
                        contract.setTenderContract(tenderContract);
                        tenderContract.setContract(contract);
                    } catch (Exception e) {
                        tenderContract.setContract(null);
                        log.warn("Contract with outer id {} not found. {}", tenderContract.getOuterId(), e.getMessage());
                    }
                }

                List<Agreement> agreements = new LinkedList<>();
                for (String agreementOuterId : tender.getAgreementOuterIds()) {
                    AgreementUpdateInfo updateInfo = new AgreementUpdateInfo();
                    updateInfo.setId(agreementOuterId);
                    try {
                        AgreementResponseEntity responseEntity = agreementLoaderService.loadAgreement(updateInfo);
                        Agreement agreement = AgreementParser.parse(responseEntity);
                        agreement.setSource(EntitySource.TENDERING.toString());
                        AgreementTenderJoinService.joinInnerElements(agreement, tender);
                        agreements.add(agreement);
                    } catch (Exception ex) {
                        log.warn("Agreement with outer id {} not found. {}", agreementOuterId, ex.getMessage());
                    }
                }
                tender.setAgreements(agreements);

                if (!tenderDataValidator.isProcessable(tender)) {
                    log.info("Tender [{}[ is not processable", tender.getOuterId());
                    continue;
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
                tenderAuctionService.persistBidLotAmountAuctionData(savedTender.getId());
                log.info("Tender saved {} ", savedTender.getId());
            } catch (ResourceAccessException e) {
                log.error("Error in loading tender {}", tenderUpdateInfo.getId(), e);
            } catch (Exception ex) {
                log.error("Failed to load tender {}", tenderUpdateInfo.getId(), ex);
            }
        }
    }
}
