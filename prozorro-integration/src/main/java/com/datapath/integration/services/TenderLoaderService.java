package com.datapath.integration.services;

import com.datapath.integration.domain.TenderResponse;
import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponse;
import com.datapath.persistence.entities.Tender;
import org.springframework.retry.annotation.Retryable;

import java.time.ZonedDateTime;

public interface TenderLoaderService {

    TendersPageResponse loadTendersPage(String url);

    @Retryable(maxAttempts = 5)
    TenderResponse loadTender(TenderUpdateInfo tenderUpdateInfo);

    ZonedDateTime resolveDateOffset();

    ZonedDateTime getYearEarlierDate();

    ZonedDateTime getLastModifiedDate();

    Tender saveTender(Tender tender);

    Tender getTenderByOuterId(String outerId);
}