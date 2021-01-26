package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.UnsuccessfulAbove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UnsuccessfulAboveRepository extends JpaRepository<UnsuccessfulAbove, Integer> {

    @Query(value = "select lotsCount from UnsuccessfulAbove where procuringEntity=?1 and cpv in ?2")
    List<Integer> getUnsuccessfulAboveCountByProcuringEntityAndCpv(String procuringEntity, List<String> cpv);

}
