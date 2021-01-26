package com.datapath.persistence.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "lot",
        indexes = {
                @Index(columnList = "tender_id", name = "lot_tender_id_idx")
        })
@ToString(exclude = {"tender", "items", "bids"})
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outer_id")
    private String outerId;

    @Column(name = "status")
    private String status;

    @Column(name = "cpv2")
    private String cpv2;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "guarantee_amount")
    private Double guaranteeAmount;

    @Column(name = "guarantee_currency")
    private String guaranteeCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @OneToMany(mappedBy = "lot")
    private List<TenderItem> items;

    @ManyToMany(mappedBy = "lots")
    private List<Bid> bids;

    @OneToMany(mappedBy = "lot")
    private List<Award> awards;

    @OneToMany(mappedBy = "lot")
    private List<Qualification> qualifications;

    @Column(name = "auction_url")
    private String auctionUrl;

    @OneToMany(mappedBy = "id.lot", cascade = CascadeType.ALL)
    private List<BidLotAmount> bidLotAmounts;
}
