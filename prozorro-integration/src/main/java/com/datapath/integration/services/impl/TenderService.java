package com.datapath.integration.services.impl;

import com.datapath.integration.services.EntityService;
import com.datapath.integration.utils.EntitySource;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.repositories.TenderRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("integrationTenderService")
public class TenderService implements EntityService<Tender> {

    private TenderRepository repository;

    public TenderService(TenderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tender getById(Long id) {
        return repository.getOne(id);
    }

    @Override
    public Tender findById(Long id) {
        final Optional<Tender> optionalTender = repository.findById(id);
        return optionalTender.orElseGet(optionalTender::get);
    }

    @Override
    public List<Tender> findAll() {
        return repository.findAll();
    }

    @Override
    public Tender save(Tender entity) {
        return repository.save(entity);
    }

    @Override
    public List<Tender> save(List<Tender> entities) {
        return repository.saveAll(entities);
    }

    public Tender findLastModifiedEntry() {
        return repository.findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(EntitySource.TENDERING.toString());
    }

    public Tender findByOuterId(String outerId) {
        return repository.findFirstByOuterId(outerId);
    }

    public void removeByDate(ZonedDateTime date) {
        repository.deleteAllByDateBefore(date);
    }

    public List<String> getExistingTenderOuterIdsByOuterIds(List<String> outerIds) {
        return repository.findAllOuterIdsByOuterIdIn(outerIds);
    }

    public Map<String, ZonedDateTime> getExistingTenderOuterIdsAndDateModifiedByOuterIds(List<String> outerIds) {
        return repository.findAllOuterIdsAndDateModifiedByOuterIdIn(outerIds)
                .stream()
                .collect(Collectors.toMap(
                        (Object[] objArr) -> objArr[0].toString(),
                        (Object[] objArr) -> (ZonedDateTime) objArr[1])
                );
    }

    public Long removeByOuterId(String outerId) {
        return repository.deleteAllByOuterId(outerId);
    }
}
