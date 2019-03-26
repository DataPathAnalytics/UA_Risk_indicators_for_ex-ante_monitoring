package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
import com.datapath.integration.email.EmailSender;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.parsers.impl.TenderParser;
import com.datapath.integration.resolvers.TransactionVariablesResolver;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.validation.TenderDataValidator;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ProzorroContractLoaderService implements ContractLoaderService {

    private static final String CONTRACTS_SEARCH_URL = "https://public.api.openprocurement.org/api/2.4/contracts";

    private RestTemplate restTemplate;
    private ContractService contractService;
    private TenderContractService tenderContractService;
    private TenderLoaderService tenderLoaderService;
    private TransactionVariablesResolver tvResolver;
    private TenderDataValidator tenderDataValidator;

    public ProzorroContractLoaderService(RestTemplate restTemplate,
                                         ContractService integrationContractService,
                                         TenderContractService tenderContractService,
                                         TenderLoaderService tenderLoaderService,
                                         TransactionVariablesResolver tvResolver,
                                         TenderDataValidator tenderDataValidator) {
        this.restTemplate = restTemplate;
        this.contractService = integrationContractService;
        this.tenderContractService = tenderContractService;
        this.tenderLoaderService = tenderLoaderService;
        this.tvResolver = tvResolver;
        this.tenderDataValidator = tenderDataValidator;
    }

    @Override
    public ContractsPageResponseEntity loadContractsPage(String url) {
        return restTemplate.getForObject(url, ContractsPageResponseEntity.class);
    }

    @Override
    public ContractResponseEntity loadContract(ContractUpdateInfo contractUpdateInfo) {
        final String contractUrl = ProzorroRequestUrlCreator.createContractUrl(
                CONTRACTS_SEARCH_URL, contractUpdateInfo.getId());

        String responseData = restTemplate.getForObject(contractUrl, String.class);
        ContractResponseEntity contract = new ContractResponseEntity();
        contract.setData(responseData);
        contract.setId(contractUpdateInfo.getId());
        contract.setDateModified(contractUpdateInfo.getDateModified());
        return contract;
    }

    @Override
    public ZonedDateTime resolveDateOffset() {
        final ZonedDateTime lastModifiedDate = this.getLastModifiedDate();
        if (lastModifiedDate != null)
            return lastModifiedDate;
        else
            return getYearEarlierDate();
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
        }

        if (tenderContract != null) {
            contract.setTenderContract(tenderContract);
            tenderContract.setContract(contract);
        } else {
            TenderUpdateInfo tenderUpdateInfo = new TenderUpdateInfo();
            tenderUpdateInfo.setId(contract.getTenderOuterId());
            try {
                TenderResponseEntity tenderResponseEntity = tenderLoaderService.loadTender(tenderUpdateInfo);
                TenderParser tenderParser = TenderParser.create(tenderResponseEntity.getData(), true);
                Tender tender = tenderParser.buildTenderEntity();

                if (!tenderDataValidator.isValidTender(tender)) {
                    log.error("Tender validation failed while contract loading.");
                    sendValidationFailedReport(tender);
                    return null;
                }

                tender.setSource(EntitySource.CONTRACTING.toString());

                String subjectOfProcurement = tvResolver.getSubjectOfProcurement(tender);
                tender.setTvSubjectOfProcurement(subjectOfProcurement);

                String tenderCPV = tvResolver.getTenderCPV(tender);
                tender.setTvTenderCPV(tenderCPV);

//                if (tenderDataValidator.isTenderFromFinanceCategory(tender)) {
//                    return null;
//                }

                Tender existingTender = tenderLoaderService.saveTender(tender);

                Optional<TenderContract> optionalTenderContract = existingTender.getTenderContracts().stream()
                        .filter(tc -> tc.getOuterId().equals(contract.getOuterId())).findFirst();

                optionalTenderContract.ifPresent(contract::setTenderContract);

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

    private void sendValidationFailedReport(Tender tender) {
        log.info("Tender {} validation failed while contract loading. Send email notification...", tender.getOuterId());
        boolean isSent = EmailSender.sendTenderValidationFailedNotification(tender.getOuterId());
        log.warn("Notification {} sent", isSent ? "" : "not");
    }
}
