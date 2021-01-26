package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.WorkdayOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface WorkdayOffRepository extends JpaRepository<WorkdayOff, LocalDate> {
}
