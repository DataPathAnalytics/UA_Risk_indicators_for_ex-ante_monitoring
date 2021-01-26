package com.datapath.indicatorsresolver;

import lombok.NoArgsConstructor;

import java.time.ZoneId;

@NoArgsConstructor
public final class IndicatorConstants {

    public static final ZoneId UA_ZONE = ZoneId.of("Europe/Kiev");
    public static final String CANCELLED = "cancelled";
    public static final String UNSUCCESSFUL = "unsuccessful";
    public static final String ACTIVE = "active";
    public static final String COMPLAINT = "complaint";
    public static final String PKCS7_SIGNATURE = "application/pkcs7-signature";
    public static final String CONTRACT = "contract";
    public static final String SATISFIED = "satisfied";
    public static final String CLAIM = "claim";
    public static final String DRAFT = "draft";
    public static final String ITEM_PRICE_VARIATION = "itemPriceVariation";
    public static final String ADDITIONAL_PURCHASE = "additionalPurchase";


}
