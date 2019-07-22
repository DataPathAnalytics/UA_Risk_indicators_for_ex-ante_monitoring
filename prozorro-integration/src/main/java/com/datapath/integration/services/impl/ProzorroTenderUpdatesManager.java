package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.email.EmailSender;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.parsers.impl.ContractParser;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.utils.ServiceStatus;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProzorroTenderUpdatesManager implements TenderUpdatesManager {

    public static final String TENDERS_SEARCH_URL = "https://public.api.openprocurement.org/api/2.4/tenders";

    private TenderLoaderService tenderLoaderService;
    private ContractLoaderService contractLoaderService;
    private TransactionVariablesResolver tvResolver;
    private Map<String, ZonedDateTime> failedTenders;
    private TenderDataValidator tenderDataValidator;

    private boolean updatesAvailable;
    private ServiceStatus serviceStatus;

    public ProzorroTenderUpdatesManager(TenderLoaderService tenderLoaderService,
                                        TransactionVariablesResolver tvResolver,
                                        ContractLoaderService contractLoaderService,
                                        TenderDataValidator tenderDataValidator) {
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.contractLoaderService = contractLoaderService;
        this.tenderDataValidator = tenderDataValidator;
        this.serviceStatus = ServiceStatus.ENABLED;
        this.failedTenders = new HashMap<>();
    }

    @Async
    @Override
    public void loadLastModifiedTenders() {
        changeServiceStatus(ServiceStatus.DISABLED);
        try {
            ZonedDateTime dateOffset = tenderLoaderService.resolveDateOffset()
                    .withZoneSameInstant(ZoneId.of("Europe/Kiev"));

            String url = ProzorroRequestUrlCreator.createTendersUrl(TENDERS_SEARCH_URL, dateOffset);
            while (true) {
                log.info("Fetch tenders from Prozorro: url = {}", url);
                try {
                    TendersPageResponseEntity tendersPageResponseEntity = tenderLoaderService.loadTendersPage(url);
                    String nextPageUrl = URLDecoder.decode(tendersPageResponseEntity.getNextPage().getUri(), StandardCharsets.UTF_8.name());
                    log.info("Next page url {}", nextPageUrl);
                    log.info("Fetched {} items", tendersPageResponseEntity.getItems().size());
                    List<TenderUpdateInfo> items = tendersPageResponseEntity.getItems();
                    for (TenderUpdateInfo tenderUpdateInfo : items) {
                        try {
                            if (tenderUpdateInfo.getDateModified().isAfter(ZonedDateTime.now().minusMinutes(1))) {
                                log.info("Tenders loading paused");
                                changeServiceStatus(ServiceStatus.ENABLED);
                                return;
                            }
                            TenderResponseEntity tenderResponseEntity = tenderLoaderService.loadTender(tenderUpdateInfo);
                            log.info("Fetching tender: id = {}", tenderResponseEntity.getId());
                            TenderParser tenderParser = TenderParser.create(tenderResponseEntity.getData());
                            Tender tender = tenderParser.buildTenderEntity();
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
                                sendValidationFailedReport(tender);
                                continue;
                            }

                                Tender savedTender = tenderLoaderService.saveTender(tender);
                                setUpdatesAvailability(true);
                                log.info("Tender saved, id = {}", savedTender.getId());
                        } catch (TenderValidationException e) {
                            log.warn("Tender expired or it is test tender: outerId = {}", tenderUpdateInfo.getId(), e);
                        } catch (ConstraintViolationException e) {
                            log.error("Error while processing the tender: outerId = {} ", tenderUpdateInfo.getId(), e);
                            removeTender(tenderUpdateInfo.getId());
                            changeServiceStatus(ServiceStatus.ENABLED);
                            return;
                        } catch (ResourceAccessException e) {
                            log.error("Error in loading tenders: outerId = {}", e.getMessage(), e);
                            changeServiceStatus(ServiceStatus.ENABLED);
                            return;
                        } catch (Exception e) {
                            log.error("Error while processing the tender: outerId = {}", tenderUpdateInfo.getId(), e);
                            changeServiceStatus(ServiceStatus.ENABLED);
                            sendNotification(tenderUpdateInfo);
                            return;
                        }
                    }

                    if (items.isEmpty()) {
                        log.info("No items found on page. Tenders loading break");
                        break;
                    }
                    log.info("All tenders from page {} saved.", url);
                    url = nextPageUrl;
                } catch (ResourceAccessException e) {
                    log.error("Error in loading tenders. {}", e.getMessage(), e);
                    changeServiceStatus(ServiceStatus.ENABLED);
                    return;
                }
            }
        } catch (Exception e) {
            changeServiceStatus(ServiceStatus.ENABLED);
            log.error("Error in processing tenders. {}", e.getMessage(), e);
        }

        changeServiceStatus(ServiceStatus.ENABLED);
        log.info("All updated tenders loaded");
    }

    @Override
    public void saveTendersFromUpdateInfo(List<TenderUpdateInfo> tenderUpdateInfos) {
        log.info("Save tenders from UpdateInfo");
        for (TenderUpdateInfo tenderUpdateInfo : tenderUpdateInfos) {
            try {
                TenderResponseEntity tenderResponseEntity = tenderLoaderService.loadTender(tenderUpdateInfo);
                log.info("Fetching tender: id = {}", tenderResponseEntity.getId());
                TenderParser tenderParser = TenderParser.create(tenderResponseEntity.getData());
                Tender tender = tenderParser.buildTenderEntity();
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
                    } catch (ResourceAccessException e) {
                        log.error("Error in loading contract. {}", tenderContract.getOuterId());
                        e.printStackTrace();
                        changeServiceStatus(ServiceStatus.ENABLED);
                        return;
                    }
                }

                if (!tenderDataValidator.isValidTender(tender)) {
                    log.error("Tender validation failed. Tender outer id = {}", tender.getOuterId());
                    sendValidationFailedReport(tender);
                    continue;
                }

                Tender savedTender = tenderLoaderService.saveTender(tender);
                log.info("Tender saved {} ", savedTender.getId());
            } catch (ResourceAccessException e) {
                log.error("Error in loading tender {}", tenderUpdateInfo.getId(), e);
            } catch (Exception ex) {
                log.info("Failed to load tender {}", tenderUpdateInfo.getId(), ex);
            }
    }
    }

    private boolean newTendersVersionExists(TenderUpdateInfo tenderUpdateInfo, Tender tender) {
        if (tenderUpdateInfo.getDateModified().isBefore(tender.getDateModified())) {
            log.info("Newest version of tender {} exists.", tenderUpdateInfo.getId());
            return true;
        }
        return false;
    }

    private void updateTenderContract(TenderContract tenderContract, ContractUpdateInfo contractUpdateInfo) throws IOException {
        ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
        Contract contract = ContractParser.create(contractResponseEntity.getData()).buildContractEntity();
        contract.setSource(EntitySource.TENDERING.toString());
        contract.setTenderContract(tenderContract);
        tenderContract.setContract(contract);
    }


    @Async
    @Override
    public void removeExpiredTenders() {
        tenderLoaderService.removeTendersByDate(DateUtils.yearEarlierFromNow());
    }

    @Override
    public void changeServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    @Override
    public boolean isUpdatesAvailable() {
        boolean updatesAvailable = this.updatesAvailable;
        setUpdatesAvailability(false);
        return updatesAvailable;
    }

    @Override
    public void setUpdatesAvailability(boolean availability) {
        this.updatesAvailable = availability;
    }

    private void removeTender(String outerId) {
        try {
            tenderLoaderService.removeTenderByOuterId(outerId);
            log.info("Tender {} deleted", outerId);
        } catch (Exception ex) {
            log.info("Tender delete error. Tender outer id: {}", outerId, ex);
        }
    }

    private void sendNotification(TenderUpdateInfo tenderUpdateInfo) {
        ZonedDateTime failedTenderTime = failedTenders.get(tenderUpdateInfo.getId());
        if (failedTenderTime != null) {
            if (ZonedDateTime.now().minusHours(1).isAfter(failedTenderTime)) {
                log.info("Tender {} loading failed. Send email notification...");
                boolean isSent = EmailSender.sendFailedTenderNotification(tenderUpdateInfo.getId());
                log.info("Notification {} sent", isSent ? "" : "not");
                failedTenders.remove(tenderUpdateInfo.getId());
            }
        } else {
            failedTenders.put(tenderUpdateInfo.getId(), ZonedDateTime.now());
        }
    }

    private void sendValidationFailedReport(Tender tender) {
        log.info("Tender {} validation failed. Send email notification...");
        boolean isSent = EmailSender.sendTenderValidationFailedNotification(tender.getOuterId());
        log.info("Notification {} sent", isSent ? "" : "not");
    }
}
