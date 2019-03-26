package com.datapath.web.services.impl;

import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.IndicatorRepository;
import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.exceptions.IndicatorNotFountException;
import com.datapath.web.mappers.IndicatorInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IndicatorInfoService {

    private IndicatorRepository indicatorRepository;

    public IndicatorInfoService(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    public IndicatorInfo getIndicatorInfo(String id) throws IndicatorNotFountException {
        Optional<Indicator> optionalIndicator = indicatorRepository.findById(id);
        if (optionalIndicator.isPresent()) {
            return IndicatorInfoMapper.mapToIndicatorInfo(optionalIndicator.get());
        }

        throw new IndicatorNotFountException(id);
    }

    public List<IndicatorInfo> getIndicatorsInfo() {
        return indicatorRepository.findAll().stream()
                .map(IndicatorInfoMapper::mapToIndicatorInfo)
                .collect(Collectors.toList());
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
