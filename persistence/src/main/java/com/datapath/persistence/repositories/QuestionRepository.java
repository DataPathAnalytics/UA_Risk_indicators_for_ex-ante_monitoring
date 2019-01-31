package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Lot, Long> {

    @Query(value = "" +
            "SELECT\n" +
            "  tender.outer_id,\n" +
            "  q.id,\n" +
            "  q.date_answered,\n" +
            "  q.answer,\n" +
            "  q.date,\n" +
            "    abs(date_part('day', tender.end_date - q.date))\n" +
            "FROM tender\n" +
            "  LEFT JOIN question q ON tender.id = q.tender_id\n" +
            "WHERE  tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))", nativeQuery = true)
    List<Object> getQuestionDateDateAnswerAndAnswerOrderByTenderIdIn(String tenderIds);

}
