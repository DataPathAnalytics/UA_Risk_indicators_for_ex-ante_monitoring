package com.datapath.indicatorsresolver.service.checkIndicators.handler;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.indicatorsresolver.service.checkIndicators.processors.Risk_1_14_Processor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_14_Handler extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-14";
    private boolean indicatorAvailable;

    private Risk_1_14_Processor processor;

    public Risk_1_14_Handler(Risk_1_14_Processor processor) {
        this.processor = processor;
        indicatorAvailable = true;
    }

    @Async
    @Scheduled(cron = "${risk-1-14.cron}")
    public void handle() {
        if (!indicatorAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = getIndicatorLastCheckedDate(indicator);
                handle(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorAvailable = true;
        }
    }

    private void handle(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);

        while (true) {
            List<Long> contractIds = contractRepository.findContractIds(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcuringEntityKind())
            );

            if (isEmpty(contractIds)) {
                break;
            }

            List<ContractIndicator> contractIndicators = processor.process(indicator, contractIds);

            contractIndicators.forEach(contractIndicator -> {
                String contractId = contractIndicator.getContractDimensions().getContractId();
                DruidContractIndicator druidIndicators = druidIndicatorMapper.transformToDruidContractIndicator(contractIndicator);

                if (!extractContractDataService.theLastContractEquals(contractId, INDICATOR_CODE, singletonList(druidIndicators))) {
                    uploadDataService.uploadContractIndicator(druidIndicators);
                }
            });

            ZonedDateTime maxTenderDateCreated = getMaxContractDateCreated(contractIndicators, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicator.setDateChecked(ZonedDateTime.now());
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        indicator.setDateChecked(ZonedDateTime.now());
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

}
