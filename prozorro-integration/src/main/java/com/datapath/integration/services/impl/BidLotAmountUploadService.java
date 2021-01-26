package com.datapath.integration.services.impl;

import com.datapath.persistence.entities.Bid;
import com.datapath.persistence.entities.BidLotAmount;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.repositories.BidLotAmountRepository;
import com.datapath.persistence.repositories.BidRepository;
import com.datapath.persistence.repositories.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BidLotAmountUploadService {

    @Autowired
    private BidLotAmountRepository bidLotAmountRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private LotRepository lotRepository;

    public List<Lot> findLotsByBidIdAndTenderId(Long bidId, Long tenderId) {
        return lotRepository.findByBidIdAndTenderId(bidId, tenderId);
    }

    public void save(List<BidLotAmount> entity) {
        bidLotAmountRepository.saveAll(entity);
    }

    public List<Bid> findBidsByTenderId(Long tenderId) {
        return bidRepository.findByTenderId(tenderId);
    }
}
