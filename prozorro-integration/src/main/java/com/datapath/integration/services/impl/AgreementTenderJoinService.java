package com.datapath.integration.services.impl;

import com.datapath.persistence.entities.*;

public class AgreementTenderJoinService {

    public static void joinInnerElements(Agreement agreement, Tender tender) {
        agreement.getContracts().forEach(c -> {
            Bid bid = tender.getBids().stream().filter(b -> c.getBidId().equals(b.getOuterId())).findFirst().orElse(null);
            c.setBid(bid);

            Award award = tender.getAwards().stream().filter(a -> c.getAwardId().equals(a.getOuterId())).findFirst().orElse(null);
            c.setAward(award);
        });

        agreement.getItems().forEach(i -> {
            Lot lot = tender.getLots().stream().filter(l -> i.getRelatedLotId().equals(l.getOuterId())).findFirst().orElse(null);
            i.setLot(lot);
        });

        agreement.setTender(tender);
        agreement.setProcuringEntity(tender.getProcuringEntity());
    }
}
