package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.TenderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(TenderItemMigration.class)
public class TenderItemCondition implements ApplicationCondition {

    private TenderItemRepository tenderItemRepository;

    public TenderItemCondition(TenderItemRepository tenderItemRepository) {
        this.tenderItemRepository = tenderItemRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long numberOfItemsWithNullableQuantity = tenderItemRepository.countAllByQuantityIsNull();
        Long tendersCount = tenderItemRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), numberOfItemsWithNullableQuantity.equals(tendersCount));
        return numberOfItemsWithNullableQuantity.equals(tendersCount);
    }
}
