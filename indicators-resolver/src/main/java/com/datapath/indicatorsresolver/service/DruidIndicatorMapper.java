package com.datapath.indicatorsresolver.service;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.druidintegration.model.DruidTenderIndicator;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class DruidIndicatorMapper {

    public DruidTenderIndicator transformToDruidTenderIndicator(TenderIndicator indicator) {
        DruidTenderIndicator druidIndicator = new DruidTenderIndicator();
        try {
            druidIndicator.setTenderOuterId(indicator.getTenderDimensions().getId());
            druidIndicator.setTenderId(indicator.getTenderDimensions().getTenderId());
            druidIndicator.setIndicatorId(indicator.getIndicator().getId());
            druidIndicator.setIndicatorType(indicator.getIndicator().getTenderLotType());
            if (!isNull(indicator.getLots())) {
                druidIndicator.setLotIds(indicator.getLots());
            }
            druidIndicator.setIndicatorValue(indicator.getValue());
            druidIndicator.setIndicatorImpact(indicator.getIndicator().getImpact());
            if (indicator.getTenderDimensions() != null) {
                druidIndicator.setIterationId(indicator.getTenderDimensions().getDruidCheckIteration() == null ? null : indicator.getTenderDimensions().getDruidCheckIteration() + 1);
                druidIndicator.setStatus(indicator.getTenderDimensions().getStatus());
                druidIndicator.setProcedureType(indicator.getTenderDimensions().getProcedureType());
            } else {
                druidIndicator.setIterationId(null);
                druidIndicator.setStatus(null);
                druidIndicator.setProcedureType(null);
            }
            String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
            druidIndicator.setDate(now);
            druidIndicator.setTime(now);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            log.info("Failed to fully setup indicator {}", indicator.toString());
        }
        return druidIndicator;
    }

    public List<DruidTenderIndicator> transformToDruidTenderIndicator(List<TenderIndicator> indicators) {
        if (indicators.isEmpty()) return new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<DruidTenderIndicator> druidTenderIndicators = indicators.stream().map(this::transformToDruidTenderIndicator).collect(Collectors.toList());
        return IntStream.range(0, druidTenderIndicators.size())
                .mapToObj(index -> {
                    DruidTenderIndicator druidTenderIndicator = druidTenderIndicators.get(index);
                    ZonedDateTime zonedDateTime = now.plusNanos(index * 1000000);
                    druidTenderIndicator.setTime(zonedDateTime.toString());
                    druidTenderIndicator.setDate(zonedDateTime.toString());
                    return druidTenderIndicator;
                })
                .collect(Collectors.toList());

    }

    public DruidContractIndicator transformToDruidContractIndicator(ContractIndicator indicator) {
        DruidContractIndicator druidIndicator = new DruidContractIndicator();
        String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
        druidIndicator.setDate(now);
        druidIndicator.setTime(now);
        druidIndicator.setTenderOuterId(indicator.getContractDimensions().getTenderId());
        druidIndicator.setTenderId(indicator.getContractDimensions().getTenderIdHr());
        druidIndicator.setContractOuterId(indicator.getContractDimensions().getContractId());
        druidIndicator.setContractId(indicator.getContractDimensions().getContractIdHr());
        druidIndicator.setIndicatorId(indicator.getIndicator().getId());
        druidIndicator.setIndicatorType(indicator.getIndicator().getTenderLotType());
        druidIndicator.setIndicatorImpact(indicator.getIndicator().getImpact());
        druidIndicator.setIndicatorValue(indicator.getValue());
        druidIndicator.setIterationId(indicator.getContractDimensions().getDruidCheckIteration() + 1);
        druidIndicator.setStatus(indicator.getContractDimensions().getStatus());
        druidIndicator.setProcedureType(indicator.getContractDimensions().getProcedureType());
        if (!isNull(indicator.getLots())) {
            druidIndicator.setLotIds(indicator.getLots());
        }
        return druidIndicator;
    }

    public List<DruidContractIndicator> transformToDruidContractIndicator(List<ContractIndicator> indicators) {
        return indicators.isEmpty() ? new ArrayList<>() : indicators.stream().map(this::transformToDruidContractIndicator).collect(Collectors.toList());
    }
}
