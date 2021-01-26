package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.WeekendOn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface WeekendOnRepository extends JpaRepository<WeekendOn, LocalDate> {
}
