package com.datapath.persistence.service;

import com.datapath.persistence.entities.Checklist;
import com.datapath.persistence.repositories.ChecklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChecklistDaoService {

    @Autowired
    private ChecklistRepository repository;

    public List<Checklist> findByTenderOuterIds(List<String> ids) {
        return repository.findByTenderOuterIdIn(ids);
    }

    public void removeByTenderOuterId(String id) {
        repository.removeByTenderOuterId(id);
    }

    public Checklist findByTenderOuterId(String id) {
        return repository.findByTenderOuterId(id);
    }

    public void save(Checklist checklist) {
        repository.save(checklist);
    }
}
