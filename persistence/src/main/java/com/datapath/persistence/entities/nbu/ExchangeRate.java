package com.datapath.persistence.entities.nbu;

import com.datapath.persistence.converters.ZonedDateTimeConverter;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Entity
@ToString
@Table(name = "exchange_rate")
public class ExchangeRate {

    @EmbeddedId
    private ExchangeRateId id;

    @Column(name = "currency")
    private String currency;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "date")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime date;
}
