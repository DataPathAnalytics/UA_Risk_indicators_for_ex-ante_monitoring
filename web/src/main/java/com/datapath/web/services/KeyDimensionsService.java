package com.datapath.web.services;

import com.datapath.druidintegration.service.ExtractKeyDimensionsService;
import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.KeyDimensions;
import com.datapath.web.domain.KeyDimensionsItem;
import com.datapath.web.mappers.KeyDimensionsItemMapper;
import com.datapath.web.providers.impl.InDatabaseIndicatorInfoProvider;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KeyDimensionsService {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX";

    private ExtractKeyDimensionsService extractKeyDimensionsService;
    private InDatabaseIndicatorInfoProvider inDatabaseIndicatorInfoProvider;

    public KeyDimensionsService(ExtractKeyDimensionsService extractKeyDimensionsService,
                                InDatabaseIndicatorInfoProvider inDatabaseIndicatorInfoProvider) {
        this.extractKeyDimensionsService = extractKeyDimensionsService;
        this.inDatabaseIndicatorInfoProvider = inDatabaseIndicatorInfoProvider;
    }

    public KeyDimensions getKeyDimensions(ZonedDateTime startDate, ZonedDateTime endDate) {
        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        List<KeyDimensionsItem> procedureTypes = extractKeyDimensionsService
                .getProcedureTypes(formattedStartDate, formattedEndDate)
                .stream()
                .map(KeyDimensionsItemMapper::mapToIndicatorInfo)
                .collect(Collectors.toList());

        List<KeyDimensionsItem> indicators = extractKeyDimensionsService
                .getIndicatorIds(formattedStartDate, formattedEndDate)
                .stream()
                .map(KeyDimensionsItemMapper::mapToIndicatorInfo)
                .peek(keyDimensionsItem -> {
                    IndicatorInfo indicator = inDatabaseIndicatorInfoProvider.getIndicatorById(keyDimensionsItem.getId());
                    keyDimensionsItem.setName(indicator.getIndicatorShortName());
                    keyDimensionsItem.setCode(indicator.getIndicatorCode());
                })
                .collect(Collectors.toList());

        KeyDimensions keyDimensions = new KeyDimensions();
        keyDimensions.setProcedureTypes(procedureTypes);
        keyDimensions.setIndicators(indicators);

        return keyDimensions;
    }

    public KeyDimensions getContractsKeyDimensions(ZonedDateTime startDate, ZonedDateTime endDate) {
        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        List<KeyDimensionsItem> indicators = extractKeyDimensionsService
                .getContractIndicatorIds(formattedStartDate, formattedEndDate)
                .stream()
                .map(KeyDimensionsItemMapper::mapToIndicatorInfo)
                .peek(keyDimensionsItem -> {
                            IndicatorInfo indicator = inDatabaseIndicatorInfoProvider.getIndicatorById(keyDimensionsItem.getId());
                            keyDimensionsItem.setName(indicator.getIndicatorShortName());
                            keyDimensionsItem.setCode(indicator.getIndicatorCode());
                        }
                )
                .collect(Collectors.toList());

        List<KeyDimensionsItem> procedureTypes = extractKeyDimensionsService
                .getContractProcedureTypes(formattedStartDate, formattedEndDate)
                .stream()
                .map(KeyDimensionsItemMapper::mapToIndicatorInfo)
                .collect(Collectors.toList());

        KeyDimensions keyDimensions = new KeyDimensions();
        keyDimensions.setIndicators(indicators);
        keyDimensions.setProcedureTypes(procedureTypes);

        return keyDimensions;
    }
}
