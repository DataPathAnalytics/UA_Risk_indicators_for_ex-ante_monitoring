package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.parsers.impl.AgreementParser;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.AgreementLoaderService;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.Agreement;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProzorroContractLoaderService implements ContractLoaderService {

    @Value("${prozorro.contracts.url}")
    private String apiUrl;

    private RestTemplate restTemplate;
    private ContractService contractService;
    private TenderContractService tenderContractService;
    private TenderLoaderService tenderLoaderService;
    private TransactionVariablesResolver tvResolver;
    private TenderDataValidator tenderDataValidator;
    private AgreementLoaderService agreementLoaderService;
    private TenderAuctionUpdateService tenderAuctionService;

    public ProzorroContractLoaderService(RestTemplate restTemplate,
                                         ContractService integrationContractService,
                                         TenderContractService tenderContractService,
                                         TenderLoaderService tenderLoaderService,
                                         TransactionVariablesResolver tvResolver,
                                         TenderDataValidator tenderDataValidator,
                                         AgreementLoaderService agreementLoaderService,
                                         TenderAuctionUpdateService tenderAuctionService) {
        this.restTemplate = restTemplate;
        this.contractService = integrationContractService;
        this.tenderContractService = tenderContractService;
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.tenderDataValidator = tenderDataValidator;
        this.agreementLoaderService = agreementLoaderService;
        this.tenderAuctionService = tenderAuctionService;
    }

    @Override
    public ContractsPageResponseEntity loadContractsPage(String url) {
        return restTemplate.getForObject(url, ContractsPageResponseEntity.class);
    }

    @Override
    @Retryable(maxAttempts = 5)
    public ContractResponseEntity loadContract(ContractUpdateInfo contractUpdateInfo) {
        final String contractUrl = ProzorroRequestUrlCreator.createContractUrl(
                apiUrl, contractUpdateInfo.getId());

        String responseData = restTemplate.getForObject(contractUrl, String.class);
        ContractResponseEntity contract = new ContractResponseEntity();
        contract.setData(responseData);
        contract.setId(contractUpdateInfo.getId());
        contract.setDateModified(contractUpdateInfo.getDateModified());
        return contract;
    }

    @Override
    public ZonedDateTime resolveDateOffset() {
        ZonedDateTime lastModifiedDate = getLastModifiedDate();
        return lastModifiedDate != null ? lastModifiedDate.withZoneSameInstant(ZoneId.of("Europe/Kiev")) :
                getYearEarlierDate().withZoneSameInstant(ZoneId.of("Europe/Kiev"));
    }

    @Override
    public ZonedDateTime getYearEarlierDate() {
        return DateUtils.yearEarlierFromNow();
    }

    @Override
    public ZonedDateTime getLastModifiedDate() {
        Contract lastModifiedTender = contractService.findLastModifiedEntry();
        return lastModifiedTender != null && lastModifiedTender.getDateModified() != null ?
                lastModifiedTender.getDateModified().plusNanos(1000) : null;
    }

    @Override
    @Transactional
    public synchronized Contract saveContract(Contract contract) {
        TenderContract tenderContract = tenderContractService.findByOuterId(contract.getOuterId());
        Contract existingContract = contractService.findByOuterId(contract.getOuterId());

        if (existingContract != null) {
            contract.setId(existingContract.getId());
            updateContractChanges(contract, existingContract);
            updateContractDocuments(contract, existingContract);
        }

        if (tenderContract != null) {
            contract.setTender(tenderContract.getTender());
            contract.setTenderContract(tenderContract);
            tenderContract.setContract(contract);
        } else {
            TenderUpdateInfo tenderUpdateInfo = new TenderUpdateInfo();
            tenderUpdateInfo.setId(contract.getTenderOuterId());
            try {
                TenderResponse tenderResponse = tenderLoaderService.loadTender(tenderUpdateInfo);
                TenderParser tenderParser = TenderParser.create(tenderResponse.getData());
                Tender tender = tenderParser.buildTenderEntity();
                tender.setSource(EntitySource.CONTRACTING.toString());

                if (!tenderDataValidator.isProcessable(tender)) {
                    log.info("Tender [{}[ is not processable", tender.getOuterId());
                    return null;
                }

                if (!tenderDataValidator.isValidTender(tender)) {
                    log.error("Tender validation failed while contract loading.");
                    return null;
                }

                String subjectOfProcurement = tvResolver.getSubjectOfProcurement(tender);
                tender.setTvSubjectOfProcurement(subjectOfProcurement);

                String tenderCPV = tvResolver.getTenderCPV(tender);
                tender.setTvTenderCPV(tenderCPV);

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

                Tender existingTender = tenderLoaderService.saveTender(tender);
                tenderAuctionService.persistBidLotAmountAuctionData(existingTender.getId());

                Optional<TenderContract> optionalTenderContract = existingTender.getTenderContracts().stream()
                        .filter(tc -> tc.getOuterId().equals(contract.getOuterId())).findFirst();

                optionalTenderContract.ifPresent(contract::setTenderContract);
                contract.setTender(tender);

            } catch (TenderValidationException | IOException e) {
                log.error("Contract not saved. Tender is invalid.", e);
                return null;
            }
        }

        if (contract.getTenderContract() != null) {
            return contractService.save(contract);
        }

        return null;
    }

    private void updateContractDocuments(Contract contract, Contract existingContract) {
        contract.getDocuments().forEach(doc -> existingContract.getDocuments()
                .stream()
                .filter(exDoc -> exDoc.getOuterId().equalsIgnoreCase(doc.getOuterId()))
                .findFirst()
                .ifPresent(exDoc -> doc.setId(exDoc.getId())));
    }

    private void updateContractChanges(Contract contract, Contract existingContract) {
        contract.getChanges().forEach(change -> existingContract.getChanges()
                .stream()
                .filter(exChange -> exChange.getOuterId().equalsIgnoreCase(change.getOuterId()))
                .findFirst()
                .ifPresent(exChange -> change.setId(exChange.getId())));
    }
}
