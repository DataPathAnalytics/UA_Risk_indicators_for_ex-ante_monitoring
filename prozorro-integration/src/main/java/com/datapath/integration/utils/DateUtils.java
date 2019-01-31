package com.datapath.integration.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");
    private static final String ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX";

    public static ZonedDateTime yearEarlierFromNow() {
        return ZonedDateTime.now(DEFAULT_TIMEZONE)
                .minusYears(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(DEFAULT_TIMEZONE);
    }

    public static ZonedDateTime parseZonedDateTime(String zonedDateTimeStr) {
        return ZonedDateTime.parse(zonedDateTimeStr, DateTimeFormatter.ofPattern(ZONED_DATE_TIME_FORMAT));
    }

    public static String formatToZonedDateTime(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(DateTimeFormatter.ofPattern(ZONED_DATE_TIME_FORMAT));
    }
}
