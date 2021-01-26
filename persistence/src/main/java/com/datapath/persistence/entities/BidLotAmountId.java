package com.datapath.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class BidLotAmountId implements Serializable {
    @JoinColumn(name = "bid_id", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Bid bid;

    @JoinColumn(name = "lot_id", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Lot lot;
}
