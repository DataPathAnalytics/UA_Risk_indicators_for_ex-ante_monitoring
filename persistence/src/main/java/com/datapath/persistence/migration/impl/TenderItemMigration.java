package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.TenderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class TenderItemMigration extends Migration {

    private TenderItemRepository tenderItemRepository;

    public TenderItemMigration(List<? extends ApplicationCondition> conditions,
                               TenderItemRepository tenderItemRepository) {
        super(conditions);
        this.tenderItemRepository = tenderItemRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = tenderItemRepository.getMaxId();
        long minId = tenderItemRepository.getMinId();
        long step = 10000;

        log.info("Tender maxId = {}, minId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            tenderItemRepository.updateQuantityFromTenderData(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} tender items", currentId);
        }

        log.info("Tender items quantity updated", this.getClass().getSimpleName());
    }
}
