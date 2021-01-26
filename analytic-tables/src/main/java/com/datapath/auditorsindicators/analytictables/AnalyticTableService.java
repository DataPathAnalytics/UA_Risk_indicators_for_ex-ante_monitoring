package com.datapath.auditorsindicators.analytictables;

import com.datapath.auditorsindicators.analytictables.providers.*;
import com.datapath.persistence.entities.derivatives.*;
import com.datapath.persistence.repositories.derivatives.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AnalyticTableService implements InitializingBean {

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
    private Contracts3YearsRepository contracts3YearsRepository;
    private WinsCountRepository winsCountRepository;
    private NoNeedRepository noNeedRepository;
    private NoMoneyRepository noMoneyRepository;

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
    private Contracts3YearsProvider contracts3YearsProvider;
    private WinsCountProvider winsCountProvider;
    private NoNeedProvider noNeedProvider;
    private NoMoneyProvider noMoneyProvider;

    @Autowired
    public void setNoMoneyRepository(NoMoneyRepository noMoneyRepository) {
        this.noMoneyRepository = noMoneyRepository;
    }

    @Autowired
    public void setNoMoneyProvider(NoMoneyProvider noMoneyProvider) {
        this.noMoneyProvider = noMoneyProvider;
    }

    @Autowired
    public void setNoNeedRepository(NoNeedRepository noNeedRepository) {
        this.noNeedRepository = noNeedRepository;
    }

    @Autowired
    public void setNoNeedProvider(NoNeedProvider noNeedProvider) {
        this.noNeedProvider = noNeedProvider;
    }

    @Autowired
    public void setContracts3YearsRepository(Contracts3YearsRepository contracts3YearsRepository) {
        this.contracts3YearsRepository = contracts3YearsRepository;
    }

    @Autowired
    public void setWinsCountRepository(WinsCountRepository winsCountRepository) {
        this.winsCountRepository = winsCountRepository;
    }

    @Autowired
    public void setWinsCountProvider(WinsCountProvider winsCountProvider) {
        this.winsCountProvider = winsCountProvider;
    }

    @Autowired
    public void setContracts3YearsProvider(Contracts3YearsProvider contracts3YearsProvider) {
        this.contracts3YearsProvider = contracts3YearsProvider;
    }

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
        recalculate();
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

        log.info("Contracts3Years recalculations start");
        List<Contracts3Years> contracts3Years = contracts3YearsProvider.provide();
        contracts3YearsRepository.deleteAllInBatch();
        contracts3YearsRepository.saveAll(contracts3Years);
        log.info("Contracts3Years recalculations finish");

        log.info("WinsCount recalculations start");
        List<WinsCount> winsCounts = winsCountProvider.provide();
        winsCounts.forEach(w -> w.setCpvCount(w.getCpvList().split(",").length));
        winsCountRepository.deleteAllInBatch();
        winsCountRepository.saveAll(winsCounts);
        log.info("WinsCount recalculations finish");

        log.info("NoNeed recalculations start");
        List<NoNeed> noNeeds = noNeedProvider.provide();
        noNeedRepository.deleteAllInBatch();
        noNeedRepository.saveAll(noNeeds);
        log.info("NoNeed recalculations finish");

        log.info("NoMoney recalculations start");
        List<NoMoney> noMoneyList = noMoneyProvider.provide();
        noMoneyRepository.deleteAllInBatch();
        noMoneyRepository.saveAll(noMoneyList);
        log.info("NoMoney recalculations finish");
    }
}