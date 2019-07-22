package com.datapath.persistence.repositories.monitoring;

import com.datapath.persistence.entities.monitoring.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRole(String role);
}
