package com.datapath.web.services;

import com.datapath.persistence.repositories.TenderRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TenderService {

    private TenderRepository tenderRepository;

    public TenderService(TenderRepository tenderRepository) {
        this.tenderRepository = tenderRepository;
    }

    public Map<String, String> getTendersStatuses(List<String> tenderIds) {
        if (tenderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tenderRepository.findTenderOuterIdAndStatusByTendersOuterIdIn(tenderIds)
                .stream()
                .collect(Collectors.toMap(
                        outerIdObj -> outerIdObj[0].toString(),
                        outerIdObj -> outerIdObj[1].toString())
                );
    }

}
