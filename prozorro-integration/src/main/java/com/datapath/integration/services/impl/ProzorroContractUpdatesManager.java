package com.datapath.integration.services.impl;

import com.datapath.integration.domain.ContractResponseEntity;
import com.datapath.integration.domain.ContractUpdateInfo;
import com.datapath.integration.domain.ContractsPageResponseEntity;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.parsers.impl.ContractParser;
import com.datapath.integration.services.ContractLoaderService;
import com.datapath.integration.services.ContractUpdatesManager;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.integration.utils.ServiceStatus;
import com.datapath.persistence.entities.Contract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLDecoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class ProzorroContractUpdatesManager implements ContractUpdatesManager {


    @Value("${prozorro.contracts.url}")
    private String apiUrl;

    private ContractLoaderService contractLoaderService;
    private TenderLoaderService tenderLoaderService;
    private ServiceStatus serviceStatus;

    public ProzorroContractUpdatesManager(ContractLoaderService contractLoaderService,
                                          TenderLoaderService tenderLoaderService) {
        this.contractLoaderService = contractLoaderService;
        this.tenderLoaderService = tenderLoaderService;
        this.serviceStatus = ServiceStatus.ENABLED;
    }

    @Override
    public void loadLastModifiedContracts() {
        changeServiceStatus(ServiceStatus.DISABLED);
        ZonedDateTime dateOffset = contractLoaderService.resolveDateOffset()
                .withZoneSameInstant(ZoneId.of("Europe/Kiev"));
        String url = ProzorroRequestUrlCreator.createTendersUrl(apiUrl, dateOffset);
        ZonedDateTime nextDateOffset = dateOffset;
        while (true) {
            ZonedDateTime tenderDateOffset = tenderLoaderService.resolveDateOffset();
            if (nextDateOffset.isAfter(tenderDateOffset)) {
                log.info("Contracts date offset is after tender newest tender was " +
                        "modified. Next date offset {}, newest tender date {}", nextDateOffset, tenderDateOffset);
                break;
            }
            try {
                log.info("Fetch contracts data from Prozorro: url = {}", url);
                ContractsPageResponseEntity contractsPageResponseEntity = contractLoaderService.loadContractsPage(url);
                String nextPageUrl = URLDecoder.decode(contractsPageResponseEntity.getNextPage().getUri(), "UTF-8");
                List<ContractUpdateInfo> items = contractsPageResponseEntity.getItems();
                log.info("Next page url {}", nextPageUrl);
                log.info("Fetched {} contract items", items.size());
                for (ContractUpdateInfo contractUpdateInfo : items) {
                    try {
                        ContractResponseEntity contractResponseEntity = contractLoaderService.loadContract(contractUpdateInfo);
                        log.info("Fetching contract: id = {}", contractResponseEntity.getId());

                        ContractParser contractParser = ContractParser.create(contractResponseEntity.getData());
                        Contract contract = contractParser.buildContractEntity();
                        contract.setSource(EntitySource.CONTRACTING.toString());

                        if (contractUpdateInfo.getDateModified().isBefore(contract.getDateModified())) {
                            log.info("Newest version of contract {} exists. Current modified date {}, expected version at {}",
                                    contractUpdateInfo.getId(), contractUpdateInfo.getDateModified(), contract.getDateModified());
                            continue;
                        }

                        if (contractUpdateInfo.getDateModified().isAfter(tenderDateOffset)) {
                            log.info("Contracts date modified is after newest tender was " +
                                            "modified. Contract date modified {}, newest tender date {}",
                                    contractUpdateInfo.getDateModified(), tenderDateOffset);

                            Thread.sleep(60000);
                            changeServiceStatus(ServiceStatus.ENABLED);
                            return;
                        }

                        Contract savedContract = contractLoaderService.saveContract(contract);
                        if (savedContract != null) {
                            log.info("Contract saved, id = {}", savedContract.getId());
                        }

                    } catch (TenderValidationException ex) {
                        log.error("Contract not saved. Tender is invalid", ex);
                    } catch (ResourceAccessException e) {
                        changeServiceStatus(ServiceStatus.ENABLED);
                        log.error("Error in loading tender {}", contractUpdateInfo.getId(), e);
                        return;
                    } catch (Exception ex) {
                        log.error("Error in processing the contract: outerId = {} {}",
                                contractUpdateInfo.getId(), ex.getMessage(), ex);
                        changeServiceStatus(ServiceStatus.ENABLED);
                        return;
                    }
                }

                if (items.isEmpty()) {
                    log.info("No items found on page. Contracts loading break");
                    break;
                }

                if (url.equalsIgnoreCase(nextPageUrl)) {
                    break;
                }

                url = nextPageUrl;
                nextDateOffset = DateUtils.parseZonedDateTime(contractsPageResponseEntity.getNextPage().getOffset());
            } catch (ResourceAccessException e) {
                log.error("Error in loading contracts {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("Error in processing contracts {}", e.getMessage(), e);
                changeServiceStatus(ServiceStatus.ENABLED);
                return;
            }
        }

        changeServiceStatus(ServiceStatus.ENABLED);
    }

    @Override
    public void changeServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

}
