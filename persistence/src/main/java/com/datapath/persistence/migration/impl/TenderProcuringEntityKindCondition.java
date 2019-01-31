package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.TenderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(TenderMigration.class)
public class TenderProcuringEntityKindCondition implements ApplicationCondition {

    private TenderRepository tenderRepository;

    public TenderProcuringEntityKindCondition(TenderRepository tenderRepository) {
        this.tenderRepository = tenderRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long numberOfTendersWithNullableProcuringEntityKind = tenderRepository.countAllByProcuringEntityKindIsNull();
        Long tendersCount = tenderRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), numberOfTendersWithNullableProcuringEntityKind.equals(tendersCount));
        return numberOfTendersWithNullableProcuringEntityKind.equals(tendersCount);
    }
}
