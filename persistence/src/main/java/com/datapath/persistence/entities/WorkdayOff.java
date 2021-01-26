package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Data
@Entity(name = "workday_off")
public class WorkdayOff {

    @Id
    @Column(name = "date")
    private LocalDate date;
}
