package com.datapath.integration.services;

import com.datapath.integration.domain.ContractResponseEntity;
import com.datapath.integration.domain.ContractUpdateInfo;
import com.datapath.integration.domain.ContractsPageResponseEntity;
import com.datapath.persistence.entities.Contract;

import java.time.ZonedDateTime;

public interface ContractLoaderService {

    ContractsPageResponseEntity loadContractsPage(String url);

    ContractResponseEntity loadContract(ContractUpdateInfo contractUpdateInfo);

    ZonedDateTime resolveDateOffset();

    ZonedDateTime getYearEarlierDate();

    ZonedDateTime getLastModifiedDate();

    Contract saveContract(Contract contract);
}
