package com.datapath.persistence.service;

import com.datapath.persistence.entities.Bid;
import com.datapath.persistence.entities.BidLotAmount;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.repositories.BidLotAmountRepository;
import com.datapath.persistence.repositories.BidRepository;
import com.datapath.persistence.repositories.LotRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class BidLotAmountDaoService {


    private final BidLotAmountRepository bidLotAmountRepository;
    private final BidRepository bidRepository;
    private final LotRepository lotRepository;

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
