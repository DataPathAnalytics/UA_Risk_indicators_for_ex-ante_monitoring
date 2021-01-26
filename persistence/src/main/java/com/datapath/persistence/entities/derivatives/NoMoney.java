package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity
public class NoMoney {

    @Id
    private Integer id;

    @Column(name = "procuring_entity")
    private String procuringEntity;

    private String cpv;

    private LocalDateTime date;
}
