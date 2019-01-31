package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.CpvCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CpvCatalogueRepository extends JpaRepository<CpvCatalogue, Integer> {

    List<CpvCatalogue> findByCpvCodeIn(Collection<String> codes);

    CpvCatalogue findByCpv(String cpv);
}
