package com.datapath.integration.services;

import com.datapath.integration.domain.AgreementResponseEntity;
import com.datapath.integration.domain.AgreementUpdateInfo;
import com.datapath.integration.domain.AgreementsPageResponse;
import com.datapath.persistence.entities.Agreement;
import org.springframework.retry.annotation.Retryable;

import java.time.ZonedDateTime;

public interface AgreementLoaderService {

    @Retryable(maxAttempts = 5)
    AgreementResponseEntity loadAgreement(AgreementUpdateInfo info);

    ZonedDateTime resolveDateOffset();

    AgreementsPageResponse loadAgreementPage(String url);

    void saveAgreement(Agreement agreement) throws Exception;
}
