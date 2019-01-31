package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.SupplierRepository;
import com.datapath.persistence.repositories.TenderDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class SupplierMigration extends Migration {

    private SupplierRepository supplierRepository;
    private TenderDataRepository tenderDataRepository;

    public SupplierMigration(List<? extends ApplicationCondition> conditions,
                             SupplierRepository supplierRepository, TenderDataRepository tenderDataRepository) {
        super(conditions);
        this.supplierRepository = supplierRepository;
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
            supplierRepository.updateEmailFromTenderData(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} suppliers", currentId);
        }
        log.info("Suppliers updated", this.getClass().getSimpleName());
    }
}
