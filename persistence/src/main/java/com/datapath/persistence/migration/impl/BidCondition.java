package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.BidRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(BidMigration.class)
public class BidCondition implements ApplicationCondition {

    private BidRepository bidRepository;

    public BidCondition(BidRepository bidRepository) {
        this.bidRepository = bidRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long nullableSupplierNameCount = bidRepository.countAllBySupplierIdentifierLegalNameIsNull();
        Long bidsCount = bidRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), bidsCount.equals(nullableSupplierNameCount));
        return bidsCount.equals(nullableSupplierNameCount);
    }
}
