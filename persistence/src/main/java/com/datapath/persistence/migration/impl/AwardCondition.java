package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.AwardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(AwardMigration.class)
public class AwardCondition implements ApplicationCondition {

    private AwardRepository awardRepository;

    public AwardCondition(AwardRepository awardRepository) {
        this.awardRepository = awardRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long nullableBidsCount = awardRepository.countAllByBidIsNull();
        Long awardsCount = awardRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), awardsCount.equals(nullableBidsCount));
        return awardsCount.equals(nullableBidsCount);
    }
}
