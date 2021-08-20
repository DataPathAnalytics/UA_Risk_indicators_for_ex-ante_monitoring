package com.datapath.integration.services.impl;

import com.datapath.integration.domain.AuctionDatabaseResponseEntity;
import com.datapath.integration.services.AuctionDatabaseLoadService;
import com.datapath.persistence.entities.Bid;
import com.datapath.persistence.entities.BidLotAmount;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.service.BidLotAmountDaoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class TenderAuctionUpdateService {

    private static final String ACTIVE_BID_STATUS = "active";
    private static final String INVALID_AUCTION_URL_PART = "esco-tenders";

    private final BidLotAmountDaoService bidLotAmountDaoService;
    private final AuctionDatabaseLoadService auctionDatabaseLoadService;

    @Transactional
    public void persistBidLotAmountAuctionData(Long tenderId) {
        List<Bid> bids = bidLotAmountDaoService.findBidsByTenderId(tenderId);

        if (CollectionUtils.isEmpty(bids)) return;

        Map<String, AuctionDatabaseResponseEntity> responseMap = new HashMap<>();

        bids.stream()
                .filter(b -> ACTIVE_BID_STATUS.equalsIgnoreCase(b.getStatus()))
                .map(b -> handleBid(b, responseMap))
                .filter(list -> !CollectionUtils.isEmpty(list))
                .forEach(bidLotAmountDaoService::save);
    }

    private List<BidLotAmount> handleBid(Bid bid, Map<String, AuctionDatabaseResponseEntity> responseMap) {
        List<Lot> lots = bidLotAmountDaoService.findLotsByBidIdAndTenderId(bid.getId(), bid.getTender().getId());

        if (CollectionUtils.isEmpty(lots)) return Collections.emptyList();

        return lots.stream()
                .map(l -> handleLot(l, bid, responseMap))
                .collect(toList());
    }

    private BidLotAmount handleLot(Lot lot, Bid bid, Map<String, AuctionDatabaseResponseEntity> responseMap) {
        if (isEmpty(lot.getAuctionUrl()) || lot.getAuctionUrl().contains(INVALID_AUCTION_URL_PART))
            return emptyBidLotAmount(bid, lot);

        try {
            responseMap.computeIfAbsent(
                    lot.getAuctionUrl(),
                    auctionDatabaseLoadService::loadAuctionDatabaseResponse
            );

            return bidLotAmount(bid, lot, responseMap.get(lot.getAuctionUrl()));
        } catch (Exception e) {
            log.warn("Auction data not loaded ({}) [bid {} lot {}]", e.getMessage(), bid.getId(), lot.getId());
            return emptyBidLotAmount(bid, lot);
        }
    }

    private BidLotAmount emptyBidLotAmount(Bid bid, Lot lot) {
        BidLotAmount bidLotAmount = new BidLotAmount(bid, lot);
        bidLotAmount.setResultAmount(bid.getAmount());
        return bidLotAmount;
    }

    private BidLotAmount bidLotAmount(Bid bid, Lot lot, AuctionDatabaseResponseEntity auctionResponse) {
        BidLotAmount bidLotAmount = new BidLotAmount(bid, lot);

        auctionResponse.getInitialBids()
                .stream()
                .filter(init -> bid.getOuterId().equalsIgnoreCase(init.getBidderId()))
                .findFirst()
                .ifPresent(init -> bidLotAmount.setInitialAmount(init.getAmount()));

        Optional<AuctionDatabaseResponseEntity.Bid> resultBid = auctionResponse.getResults()
                .stream()
                .filter(result -> bid.getOuterId().equalsIgnoreCase(result.getBidderId()))
                .findFirst();

        if (resultBid.isPresent() && nonNull(resultBid.get().getAmount())) {
            bidLotAmount.setResultAmount(resultBid.get().getAmount());
        } else {
            bidLotAmount.setResultAmount(bid.getAmount());
        }

        return bidLotAmount;
    }
}
