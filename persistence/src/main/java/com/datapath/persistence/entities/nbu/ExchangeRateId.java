package com.datapath.persistence.entities.nbu;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class ExchangeRateId implements Serializable {

    @Column(name = "id")
    private Integer id;

    @Column(name = "code")
    private String code;

}
