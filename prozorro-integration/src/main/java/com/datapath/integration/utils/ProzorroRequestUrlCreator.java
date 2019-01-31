package com.datapath.integration.utils;

import java.time.ZonedDateTime;

public class ProzorroRequestUrlCreator {

    public static String createTendersUrl(String baseUrl, ZonedDateTime offsetDate) {
        return createTendersUrl(baseUrl, offsetDate, 50);
    }

    public static String createTendersUrl(String baseUrl, ZonedDateTime offsetDate, int pageLimit) {
        return String.format("%s?limit=%d&mode=_all_&offset=%s", baseUrl, pageLimit, formatOffsetDate(offsetDate));
    }

    public static String createTenderUrl(String baseUrl, String tenderId) {
        return baseUrl + '/' + tenderId;
    }

    public static String createContractUrl(String baseUrl, String contractId) {
        return baseUrl + '/' + contractId;
    }

    private static String formatOffsetDate(ZonedDateTime zonedDateTime) {
        return DateUtils.formatToZonedDateTime(zonedDateTime);
    }

}
