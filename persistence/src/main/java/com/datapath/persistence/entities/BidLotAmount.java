package com.datapath.persistence.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "bid_lot_amount",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"bid_id", "lot_id"})
        })
@NoArgsConstructor
public class BidLotAmount {

    @EmbeddedId
    private BidLotAmountId id;

    @Column(name = "initial_amount")
    private Double initialAmount;

    @Column(name = "result_amount")
    private Double resultAmount;

    public BidLotAmount(Bid bid, Lot lot) {
        this.id = new BidLotAmountId(bid, lot);
    }
}
