package com.datapath.integration.services;

import com.datapath.integration.domain.AuctionDatabaseResponseEntity;

public interface AuctionDatabaseLoadService {
    AuctionDatabaseResponseEntity loadAuctionDatabaseResponse(String auctionUrl);
}
