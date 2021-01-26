package com.datapath.persistence.repositories.queue;

import com.datapath.persistence.entities.queue.IndicatorsQueueItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorsQueueItemRepository extends JpaRepository<IndicatorsQueueItem, Long> {

    Page<IndicatorsQueueItem> findAllByRegionInOrderByMaterialityScoreDesc(List<String> regions, Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(Double maxImpact,
                                                                                                 List<String> regions,
                                                                                                 Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreGreaterThanEqualAndRegionInOrderByMaterialityScoreDesc(Double minImpact,
                                                                                                         List<String> regions,
                                                                                                         Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(Double minImpact,
                                                                                                                               Double maxImpact,
                                                                                                                               List<String> regions,
                                                                                                                               Pageable pageable);

    Page<IndicatorsQueueItem> findAllByOrderByMaterialityScoreDesc(Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreLessThanOrderByMaterialityScoreDesc(Double maxImpact,
                                                                                      Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreGreaterThanEqualOrderByMaterialityScoreDesc(Double minImpact,
                                                                                              Pageable pageable);

    Page<IndicatorsQueueItem> findAllByTenderScoreGreaterThanEqualAndTenderScoreLessThanOrderByMaterialityScoreDesc(Double minImpact,
                                                                                                                    Double maxImpact,
                                                                                                                    Pageable pageable);

    Integer countByTopRiskIsTrueAndTenderScoreLessThanAndRegionIn(Double maxImpact, List<String> regions);

    Integer countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndRegionIn(Double minImpact, List<String> regions);

    Integer countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionIn(Double minImpact,
                                                                                                Double maxImpact,
                                                                                                List<String> regions);


    Integer countByTopRiskIsTrueAndTenderScoreLessThan(Double maxImpact);

    Integer countByTopRiskIsTrueAndTenderScoreGreaterThanEqual(Double minImpact);

    Integer countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndTenderScoreLessThan(Double minImpact,
                                                                                     Double maxImpact);

    Integer countByTopRiskIsTrueAndRegionIn(List<String> regions);

    Integer countByTopRiskIsTrue();

    @Query("SELECT DISTINCT i.region FROM IndicatorsQueueItem i")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT i.region FROM IndicatorsQueueItem i WHERE i.tenderScore < ?1")
    List<String> findDistinctRegionsByTenderScoreLessThan(Double maxImpact);

    @Query("SELECT DISTINCT i.region FROM IndicatorsQueueItem i WHERE i.tenderScore > ?1")
    List<String> findDistinctRegionsByTenderScoreGreaterThan(Double minImpact);

    @Query("SELECT DISTINCT i.region FROM IndicatorsQueueItem i WHERE i.tenderScore >= ?1 AND i.tenderScore <= ?2")
    List<String> findDistinctRegionsByTenderScoreGreaterThanEqualAndTenderScoreLessThanEqual(Double minImpact, Double maxImpact);

}
