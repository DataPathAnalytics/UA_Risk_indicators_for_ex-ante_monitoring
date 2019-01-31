package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.UnsuccessfulAbove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UnsuccessfulAboveRepository extends JpaRepository<UnsuccessfulAbove, Integer> {

    @Query(value = "select ua.unsuccessfulAboveProceduresCount from UnsuccessfulAbove ua where ua.procuringEntity=?1 and ua.tenderCpv in ?2")
    List<Integer> getUnsuccessfulAboveCountByProcuringEntityAndTenderCpv(String procuringEntity, List<String> tenderCpv);

}
