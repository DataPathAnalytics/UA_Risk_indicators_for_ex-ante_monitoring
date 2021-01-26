package com.datapath.integration.services.impl;

import com.datapath.integration.domain.AuctionDatabaseResponseEntity;
import com.datapath.integration.services.AuctionDatabaseLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AuctionDatabaseLoadServiceImpl implements AuctionDatabaseLoadService {

    private final RestTemplate restTemplate;

    @Autowired
    public AuctionDatabaseLoadServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AuctionDatabaseResponseEntity loadAuctionDatabaseResponse(String auctionUrl) {
        String url = auctionUrl.replaceAll("tenders", "database");
        log.info("Auction database loading: {}", url);
        return restTemplate.getForObject(url, AuctionDatabaseResponseEntity.class);
    }
}
