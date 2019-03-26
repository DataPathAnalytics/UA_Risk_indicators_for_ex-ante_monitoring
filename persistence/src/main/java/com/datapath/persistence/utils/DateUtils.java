package com.datapath.persistence.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
    }

    public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
        LocalDateTime ldt = LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
        return Timestamp.valueOf(ldt);
    }

    public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        return timestamp.toInstant().atZone(DEFAULT_TIMEZONE);
    }

    public static String formatToString(Date date, String formatter) {
        DateFormat dateFormat = new SimpleDateFormat(formatter);
        return dateFormat.format(date);
    }
}
