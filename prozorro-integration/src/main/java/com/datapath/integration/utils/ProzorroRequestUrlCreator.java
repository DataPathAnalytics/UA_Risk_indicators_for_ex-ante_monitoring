package com.datapath.integration.utils;

import java.time.ZonedDateTime;

public class ProzorroRequestUrlCreator {

    public static String createTendersUrl(String baseUrl, ZonedDateTime offsetDate) {
        return createTendersUrl(baseUrl, offsetDate, 100);
    }

    public static String createTendersUrl(String baseUrl, ZonedDateTime offsetDate, int pageLimit) {
        return offsetDate != null ?
                String.format("%s?limit=%d&mode=_all_&offset=%s", baseUrl, pageLimit, formatOffsetDate(offsetDate)) :
                String.format("%s?limit=%d&mode=_all_&offset=%s", baseUrl, pageLimit, "");
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

    public static String createAgreementsUrl(String baseUrl, ZonedDateTime offsetDate) {
        return offsetDate != null ?
                String.format("%s?limit=100&offset=%s", baseUrl, formatOffsetDate(offsetDate)) :
                String.format("%s?limit=100&offset=%s", baseUrl, "");
    }

    public static String createAgreementUrl(String baseUrl, String agreementId) {
        return baseUrl + "/" + agreementId;
    }
}
