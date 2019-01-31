package com.datapath.web.services.impl;

import com.datapath.persistence.repositories.IndicatorRepository;
import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.mappers.IndicatorInfoMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IndicatorInfoService {

    private IndicatorRepository indicatorRepository;

    public IndicatorInfoService(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    public Map<String, IndicatorInfo> getIndicatorsInfoMap() {
        return indicatorRepository.findAll().stream()
                .map(IndicatorInfoMapper::mapToIndicatorInfo)
                .collect(Collectors.toMap(
                        IndicatorInfo::getIndicatorId,
                        indicatorInfo -> indicatorInfo
                ));
    }

}
