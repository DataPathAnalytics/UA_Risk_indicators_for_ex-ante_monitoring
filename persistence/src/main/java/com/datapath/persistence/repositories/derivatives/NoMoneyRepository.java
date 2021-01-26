package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.NoMoney;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoMoneyRepository extends JpaRepository<NoMoney, Integer> {

    List<NoMoney> findByProcuringEntity(String procuringEntity);
}
