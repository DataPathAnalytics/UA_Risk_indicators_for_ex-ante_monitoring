package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class GeneralSpecial {

    @Id
    private Integer id;
    @Column(name = "procuring_entity_id")
    private String procuringEntityId;
    @Column(name = "procuring_entity")
    private String procuringEntity;
}
