package com.datapath.integration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class AuctionDatabaseResponseEntity {
    @JsonProperty("initial_bids")
    private List<Bid> initialBids;
    private List<Bid> results;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Bid {
        private Double amount;

        @JsonProperty("bidder_id")
        private String bidderId;
    }
}
