package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.BidLotAmount;
import com.datapath.persistence.entities.BidLotAmountId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidLotAmountRepository extends JpaRepository<BidLotAmount, BidLotAmountId> {

}
