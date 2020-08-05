package com.datapath.integration.services.impl;

import com.datapath.integration.domain.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ProzorroContractLoaderService implements ContractLoaderService {

    @Value("${prozorro.contracts.url}")
    private String apiUrl;

    @Value("${prozorro.tenders.skip-test}")
    private boolean skipTestTenders;

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

                TenderParser tenderParser = new TenderParser();
                tenderParser.setRawData(tenderResponseEntity.getData());
                tenderParser.setSkipExpiredTenders(false);
                tenderParser.setSkipTestTenders(skipTestTenders);
                tenderParser.parseRawData();

                Tender tender = tenderParser.buildTender();

                if (!tenderDataValidator.isValidTender(tender)) {
                    log.error("Tender [{}] validation failed while contract loading.", tender.getOuterId());
                    return null;
                }

                tender.setSource(EntitySource.CONTRACTING.toString());

                String subjectOfProcurement = tvResolver.getSubjectOfProcurement(tender);
                tender.setTvSubjectOfProcurement(subjectOfProcurement);

                String tenderCPV = tvResolver.getTenderCPV(tender);
                tender.setTvTenderCPV(tenderCPV);

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
}
