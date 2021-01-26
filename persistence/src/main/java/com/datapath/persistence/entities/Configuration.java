package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity(name = "configuration")
public class Configuration {

    @Id
    private String key;
    private String value;
}
