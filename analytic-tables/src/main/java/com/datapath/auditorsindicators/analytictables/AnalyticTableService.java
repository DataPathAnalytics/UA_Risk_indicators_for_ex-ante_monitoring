package com.datapath.auditorsindicators.analytictables;

import com.datapath.auditorsindicators.analytictables.providers.*;
import com.datapath.persistence.entities.derivatives.*;
import com.datapath.persistence.repositories.derivatives.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AnalyticTableService implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private MutualParticipationRepository participationRepository;
    private SuppliersSingleBuyerRepository suppliersSingleBuyerRepository;
    private ItemsAbnormalQuantityRepository itemsAbnormalQuantityRepository;
    private UnsuccessfulAboveRepository unsuccessfulAboveRepository;
    private ProcuredCPVRepository procuredCPVRepository;
    private GeneralSpecialRepository generalSpecialRepository;
    private BiddersForBuyersRepository biddersForBuyersRepository;
    private NearThresholdRepository nearThresholdRepository;
    private NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository;
    private EDRPOURepository edrpouRepository;
    private SupplierForPEWith3CPVRepository supplierForPEWith3CPVRepository;

    private MutualParticipationProvider participationProvider;
    private SuppliersSingleBuyerProvider suppliersSingleBuyerProvider;
    private ItemsAbnormalQuantityProvider itemsAbnormalQuantityProvider;
    private UnsuccessfulAboveProvider unsuccessfulAboveProvider;
    private ProcuredCPVProvider procuredCPVProvider;
    private GeneralSpecialProvider generalSpecialProvider;
    private BiddersForBuyersProvider biddersForBuyersProvider;
    private NearThresholdProvider nearThresholdProvider;
    private NearThresholdOneSupplierProvider nearThresholdOneSupplierProvider;
    private EDRPOUProvider edrpouProvider;
    private SupplierForPEWith3CPVProvider supplierForPEWith3CPVProvider;

    @Autowired
    public void setParticipationRepository(MutualParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    @Autowired
    public void setSuppliersSingleBuyerRepository(SuppliersSingleBuyerRepository suppliersSingleBuyerRepository) {
        this.suppliersSingleBuyerRepository = suppliersSingleBuyerRepository;
    }

    @Autowired
    public void setItemsAbnormalQuantityRepository(ItemsAbnormalQuantityRepository itemsAbnormalQuantityRepository) {
        this.itemsAbnormalQuantityRepository = itemsAbnormalQuantityRepository;
    }

    @Autowired
    public void setUnsuccessfulAboveRepository(UnsuccessfulAboveRepository unsuccessfulAboveRepository) {
        this.unsuccessfulAboveRepository = unsuccessfulAboveRepository;
    }

    @Autowired
    public void setProcuredCPVRepository(ProcuredCPVRepository procuredCPVRepository) {
        this.procuredCPVRepository = procuredCPVRepository;
    }

    @Autowired
    public void setGeneralSpecialRepository(GeneralSpecialRepository generalSpecialRepository) {
        this.generalSpecialRepository = generalSpecialRepository;
    }

    @Autowired
    public void setBiddersForBuyersRepository(BiddersForBuyersRepository biddersForBuyersRepository) {
        this.biddersForBuyersRepository = biddersForBuyersRepository;
    }

    @Autowired
    public void setNearThresholdRepository(NearThresholdRepository nearThresholdRepository) {
        this.nearThresholdRepository = nearThresholdRepository;
    }

    @Autowired
    public void setNearThresholdOneSupplierRepository(NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository) {
        this.nearThresholdOneSupplierRepository = nearThresholdOneSupplierRepository;
    }

    @Autowired
    public void setEdrpouRepository(EDRPOURepository edrpouRepository) {
        this.edrpouRepository = edrpouRepository;
    }

    @Autowired
    public void setSupplierForPEWith3CPVRepository(SupplierForPEWith3CPVRepository supplierForPEWith3CPVRepository) {
        this.supplierForPEWith3CPVRepository = supplierForPEWith3CPVRepository;
    }

    @Autowired
    public void setParticipationProvider(MutualParticipationProvider participationProvider) {
        this.participationProvider = participationProvider;
    }

    @Autowired
    public void setSuppliersSingleBuyerProvider(SuppliersSingleBuyerProvider suppliersSingleBuyerProvider) {
        this.suppliersSingleBuyerProvider = suppliersSingleBuyerProvider;
    }

    @Autowired
    public void setItemsAbnormalQuantityProvider(ItemsAbnormalQuantityProvider itemsAbnormalQuantityProvider) {
        this.itemsAbnormalQuantityProvider = itemsAbnormalQuantityProvider;
    }

    @Autowired
    public void setUnsuccessfulAboveProvider(UnsuccessfulAboveProvider unsuccessfulAboveProvider) {
        this.unsuccessfulAboveProvider = unsuccessfulAboveProvider;
    }

    @Autowired
    public void setProcuredCPVProvider(ProcuredCPVProvider procuredCPVProvider) {
        this.procuredCPVProvider = procuredCPVProvider;
    }

    @Autowired
    public void setGeneralSpecialProvider(GeneralSpecialProvider generalSpecialProvider) {
        this.generalSpecialProvider = generalSpecialProvider;
    }

    @Autowired
    public void setBiddersForBuyersProvider(BiddersForBuyersProvider biddersForBuyersProvider) {
        this.biddersForBuyersProvider = biddersForBuyersProvider;
    }

    @Autowired
    public void setNearThresholdProvider(NearThresholdProvider nearThresholdProvider) {
        this.nearThresholdProvider = nearThresholdProvider;
    }

    @Autowired
    public void setNearThresholdOneSupplierProvider(NearThresholdOneSupplierProvider nearThresholdOneSupplierProvider) {
        this.nearThresholdOneSupplierProvider = nearThresholdOneSupplierProvider;
    }

    @Autowired
    public void setEdrpouProvider(EDRPOUProvider edrpouProvider) {
        this.edrpouProvider = edrpouProvider;
    }


    @Autowired
    public void setSupplierForPEWith3CPVProvider(SupplierForPEWith3CPVProvider supplierForPEWith3CPVProvider) {
        this.supplierForPEWith3CPVProvider = supplierForPEWith3CPVProvider;
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            recalculate();
        }
    }

    public void recalculate() {
        log.info("ItemsAbnormalQuantity recalculations start");
        List<ItemsAbnormalQuantity> itemsAbnormalQuantities = itemsAbnormalQuantityProvider.provide();
        itemsAbnormalQuantityRepository.deleteAllInBatch();
        itemsAbnormalQuantityRepository.saveAll(itemsAbnormalQuantities);
        log.info("ItemsAbnormalQuantity recalculations finish");

        log.info("SuppliersSingleBuyer recalculations start");
        List<SuppliersSingleBuyer> suppliersSingleBuyers = suppliersSingleBuyerProvider.provide();
        suppliersSingleBuyerRepository.deleteAllInBatch();
        suppliersSingleBuyerRepository.saveAll(suppliersSingleBuyers);
        log.info("SuppliersSingleBuyer recalculations finish");

        log.info("MutualParticipation recalculations start");
        List<MutualParticipation> participations = participationProvider.provide();
        participationRepository.deleteAllInBatch();
        participationRepository.saveAll(participations);
        log.info("MutualParticipation recalculations finish");

        log.info("UnsuccessfulAbove recalculations start");
        List<UnsuccessfulAbove> unsuccessfulAboves = unsuccessfulAboveProvider.provide();
        unsuccessfulAboveRepository.deleteAllInBatch();
        unsuccessfulAboveRepository.saveAll(unsuccessfulAboves);
        log.info("UnsuccessfulAbove recalculations finish");

        log.info("ProcuredCPV recalculations start");
        List<ProcuredCPV> procuredCPVs = procuredCPVProvider.provide();
        procuredCPVRepository.deleteAllInBatch();
        procuredCPVRepository.saveAll(procuredCPVs);
        log.info("ProcuredCPV recalculations finish");

        log.info("GeneralSpecial recalculations start");
        List<GeneralSpecial> generalSpecials = generalSpecialProvider.provide();
        generalSpecialRepository.deleteAllInBatch();
        generalSpecialRepository.saveAll(generalSpecials);
        log.info("GeneralSpecial recalculations finish");

        log.info("BiddersForBuyers recalculations start");
        List<BiddersForBuyers> biddersForBuyers = biddersForBuyersProvider.provide();
        biddersForBuyersRepository.deleteAllInBatch();
        biddersForBuyersRepository.saveAll(biddersForBuyers);
        log.info("BiddersForBuyers recalculations finish");

        log.info("NearThreshold recalculations start");
        List<NearThreshold> nearThresholds = nearThresholdProvider.provide();
        nearThresholdRepository.deleteAllInBatch();
        nearThresholdRepository.saveAll(nearThresholds);
        log.info("NearThreshold recalculations finish");

        log.info("NearThresholdOneSupplier recalculations start");
        List<NearThresholdOneSupplier> nearThresholdOneSuppliers = nearThresholdOneSupplierProvider.provide();
        nearThresholdOneSupplierRepository.deleteAllInBatch();
        nearThresholdOneSupplierRepository.saveAll(nearThresholdOneSuppliers);
        log.info("NearThresholdOneSupplier recalculations finish");

        log.info("EDRPOU recalculations start");
        List<EDRPOU> edrpous = edrpouProvider.provide();
        edrpouRepository.deleteAllInBatch();
        edrpouRepository.saveAll(edrpous);
        log.info("EDRPOU recalculations finish");

        log.info("SupplierForPEWith3CPV recalculations start");
        List<SupplierForPEWith3CPV> supplierForPEWith3CPVS = supplierForPEWith3CPVProvider.provide();
        supplierForPEWith3CPVRepository.deleteAllInBatch();
        supplierForPEWith3CPVRepository.saveAll(supplierForPEWith3CPVS);
        log.info("SupplierForPEWith3CPV recalculations finish");
    }
}