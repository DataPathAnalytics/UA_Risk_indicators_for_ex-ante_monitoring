package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.parsers.impl.AgreementParser;
import com.datapath.integration.parsers.impl.ContractParser;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.*;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.util.StringUtils.isEmpty;

@Slf4j
@Service
public class ProzorroTenderUpdatesManager implements TenderUpdatesManager {

    @Value("${prozorro.tenders.url}")
    private String apiUrl;

    private TenderLoaderService tenderLoaderService;
    private ContractLoaderService contractLoaderService;
    private TransactionVariablesResolver tvResolver;
    private TenderDataValidator tenderDataValidator;
    private AuctionDatabaseLoadService auctionDatabaseLoadService;
    private BidLotAmountUploadService bidLotAmountUploadService;
    private AgreementLoaderService agreementLoaderService;

    public ProzorroTenderUpdatesManager(TenderLoaderService tenderLoaderService,
                                        TransactionVariablesResolver tvResolver,
                                        ContractLoaderService contractLoaderService,
                                        TenderDataValidator tenderDataValidator,
                                        AuctionDatabaseLoadService auctionDatabaseLoadService,
                                        BidLotAmountUploadService bidLotAmountUploadService,
                                        AgreementLoaderService agreementLoaderService) {
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.contractLoaderService = contractLoaderService;
        this.tenderDataValidator = tenderDataValidator;
        this.auctionDatabaseLoadService = auctionDatabaseLoadService;
        this.bidLotAmountUploadService = bidLotAmountUploadService;
        this.agreementLoaderService = agreementLoaderService;
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
                    loadBidLotAmountData(savedTender.getId());
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

    private void loadBidLotAmountData(Long tenderId) {
        List<BidLotAmount> bidLots = new LinkedList<>();

        List<Bid> bids = bidLotAmountUploadService.findBidsByTenderId(tenderId);

        Map<String, AuctionDatabaseResponseEntity> urlAuctionResponseMap = new HashMap<>();

        if (!CollectionUtils.isEmpty(bids)) {
            bids.stream()
                    .filter(b -> "active".equalsIgnoreCase(b.getStatus()))
                    .forEach(b -> {
                                List<Lot> lots = bidLotAmountUploadService.findLotsByBidIdAndTenderId(b.getId(), tenderId);

                                if (!CollectionUtils.isEmpty(lots)) {
                                    lots.forEach(l -> {
                                        if (!isEmpty(l.getAuctionUrl()) && !l.getAuctionUrl().contains("esco-tenders")) {

                                            AuctionDatabaseResponseEntity auctionDatabaseResponse;
                                            if (urlAuctionResponseMap.containsKey(l.getAuctionUrl())) {
                                                auctionDatabaseResponse = urlAuctionResponseMap.get(l.getAuctionUrl());
                                            } else {
                                                auctionDatabaseResponse = auctionDatabaseLoadService
                                                        .loadAuctionDatabaseResponse(l.getAuctionUrl());
                                                urlAuctionResponseMap.put(l.getAuctionUrl(), auctionDatabaseResponse);
                                            }

                                            BidLotAmount bidLotAmount = new BidLotAmount(b, l);

                                            auctionDatabaseResponse.getInitialBids()
                                                    .stream()
                                                    .filter(bid -> bid.getBidderId().equalsIgnoreCase(b.getOuterId()))
                                                    .findFirst()
                                                    .ifPresent(bid -> bidLotAmount.setInitialAmount(bid.getAmount()));

                                            auctionDatabaseResponse.getResults()
                                                    .stream()
                                                    .filter(bid -> bid.getBidderId().equalsIgnoreCase(b.getOuterId()))
                                                    .findFirst()
                                                    .ifPresent(bid -> bidLotAmount.setResultAmount(bid.getAmount()));

                                            bidLots.add(bidLotAmount);
                                        }
                                    });
                                }
                            }
                    );

            bidLotAmountUploadService.save(bidLots);
        }
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
                loadBidLotAmountData(savedTender.getId());
                log.info("Tender saved {} ", savedTender.getId());
            } catch (ResourceAccessException e) {
                log.error("Error in loading tender {}", tenderUpdateInfo.getId(), e);
            } catch (Exception ex) {
                log.error("Failed to load tender {}", tenderUpdateInfo.getId(), ex);
            }
        }
    }
}
