package com.datapath.web.services.impl;

import com.datapath.persistence.repositories.IndicatorRepository;
import com.datapath.web.domain.Indicator;
import com.datapath.web.mappers.GeneralBeanMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IndicatorService {

    private IndicatorRepository indicatorRepository;

    public IndicatorService(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    public List<Indicator> getIndicators() {
        return indicatorRepository.findAll().stream()
                .map(item -> (Indicator) GeneralBeanMapper.map(item, Indicator.class))
                .collect(Collectors.toList());
    }
}
