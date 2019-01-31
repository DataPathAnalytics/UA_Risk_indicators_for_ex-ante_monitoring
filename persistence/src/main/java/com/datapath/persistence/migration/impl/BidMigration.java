package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.BidRepository;
import com.datapath.persistence.repositories.TenderDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class BidMigration extends Migration {

    private BidRepository bidRepository;
    private TenderDataRepository tenderDataRepository;

    public BidMigration(List<? extends ApplicationCondition> conditions,
                        BidRepository bidRepository, TenderDataRepository tenderDataRepository) {
        super(conditions);
        this.bidRepository = bidRepository;
        this.tenderDataRepository = tenderDataRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = tenderDataRepository.getMaxId();
        long minId = tenderDataRepository.getMinId();
        long step = 20000;

        log.info("Supplier maxId = {}, minId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            bidRepository.updateSupplierNameFromTenderData(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} suppliers", currentId);
        }
        log.info("Suppliers updated", this.getClass().getSimpleName());
    }
}
