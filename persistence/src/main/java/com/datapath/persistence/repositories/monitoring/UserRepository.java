package com.datapath.persistence.repositories.monitoring;

import com.datapath.persistence.entities.monitoring.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    User findOneById(Long id);
}
