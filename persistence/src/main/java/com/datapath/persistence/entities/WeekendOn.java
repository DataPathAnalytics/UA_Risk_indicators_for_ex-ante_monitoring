package com.datapath.persistence.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Data
@Entity(name = "weekend_on")
public class WeekendOn {

    @Id
    @Column(name = "date")
    private LocalDate date;
}
