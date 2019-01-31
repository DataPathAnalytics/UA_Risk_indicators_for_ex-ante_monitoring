package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.TenderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(TenderMigration.class)
public class TenderTitleCondition implements ApplicationCondition {

    private TenderRepository tenderRepository;

    public TenderTitleCondition(TenderRepository tenderRepository) {
        this.tenderRepository = tenderRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long numberOfTendersWithNullableTitle = tenderRepository.countAllByTitleIsNull();
        Long tendersCount = tenderRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), numberOfTendersWithNullableTitle.equals(tendersCount));
        return numberOfTendersWithNullableTitle.equals(tendersCount);
    }
}
