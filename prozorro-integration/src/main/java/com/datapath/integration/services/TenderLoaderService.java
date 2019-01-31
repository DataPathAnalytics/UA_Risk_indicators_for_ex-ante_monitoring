package com.datapath.integration.services;

import com.datapath.integration.domain.TenderResponseEntity;
import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponseEntity;
import com.datapath.persistence.entities.Tender;

import java.time.ZonedDateTime;

public interface TenderLoaderService {

    TendersPageResponseEntity loadTendersPage(String url);

    TenderResponseEntity loadTender(TenderUpdateInfo tenderUpdateInfo);

    ZonedDateTime resolveDateOffset();

    ZonedDateTime getYearEarlierDate();

    ZonedDateTime getLastModifiedDate();

    Tender saveTender(Tender tender);

    void removeTendersByDate(ZonedDateTime date);

    boolean newestTenderVersionExists(String outerId, ZonedDateTime dateModified);

    long removeTenderByOuterId(String outerId);
}
