package com.datapath.web.mappers;

import com.datapath.druidintegration.model.druid.response.common.Event;
import com.datapath.web.domain.DruidIndicator;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DruidEventMapper {

    public static DruidIndicator mapToIndicator(Event event) {
        DruidIndicator indicator = new DruidIndicator();
        indicator.setDate(parseZonedDateTimeFromUTC(event.getTimestamp()));
        indicator.setLotIds(event.getLotIds());
        indicator.setTenderId(event.getTenderId());
        indicator.setTenderOuterId(event.getTenderOuterId());
        indicator.setIndicatorId(event.getIndicatorId());
        indicator.setIndicatorType(event.getIndicatorType());
        indicator.setIterationId(event.getIterationId());
        indicator.setIndicatorValue(event.getIndicatorValue().byteValue());
        indicator.setIndicatorImpact(event.getIndicatorImpact());
        indicator.setMaxIteration(event.getMaxIteration());
        indicator.setContractId(event.getContractId());
        indicator.setContractOuterId(event.getContractOuterId());
        indicator.setProcedureType(event.getProcedureType());
        indicator.setTenderStatus(event.getStatus());
        return indicator;
    }


    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Kiev");
    private static final String ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX";
    private static final String UTC_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private static ZonedDateTime parseZonedDateTime(String zonedDateTimeStr) {
        if (zonedDateTimeStr == null) {
            return null;
        }
        return ZonedDateTime.parse(zonedDateTimeStr, DateTimeFormatter.ofPattern(ZONED_DATE_TIME_FORMAT))
                .withZoneSameLocal(DEFAULT_TIMEZONE);
    }

    private static ZonedDateTime parseZonedDateTimeFromUTC(String zonedDateTimeStr) {
        if (zonedDateTimeStr == null) {
            return null;
        }
        return ZonedDateTime.parse(zonedDateTimeStr, DateTimeFormatter.ofPattern(UTC_DATE_TIME_FORMAT))
                .withZoneSameLocal(ZoneOffset.UTC);
    }
}
