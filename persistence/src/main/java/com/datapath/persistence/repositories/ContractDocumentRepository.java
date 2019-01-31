package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractDocumentRepository extends PagingAndSortingRepository<ContractDocument, Long>, JpaRepository<ContractDocument, Long> {
    @Query(value = "SELECT format FROM contract_document " +
            "JOIN  contract on contract_document.contract_id = contract.id " +
            "WHERE contract.outer_id=?1", nativeQuery = true)
    List<String> getFormatByContractOuterId(String contractId);


}