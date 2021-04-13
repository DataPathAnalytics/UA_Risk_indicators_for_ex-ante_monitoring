package com.datapath.indicatorsresolver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IndicatorLogService {

    private static final String TENDER_INDICATOR_CALCULATION_FAILED_MESSAGE = "Indicator {} calculation failed for tender {}";
    private static final String LOT_INDICATOR_CALCULATION_FAILED_MESSAGE = "Indicator {} calculation failed in tender {} for lot {}";
    private static final String CONTRACT_INDICATOR_CALCULATION_FAILED_MESSAGE = "Indicator {} calculation failed for contract {}";

    public void tenderIndicatorFailed(String code, String tenderOuterId, Exception e) {
        log.error(TENDER_INDICATOR_CALCULATION_FAILED_MESSAGE, code, tenderOuterId);
        log.error("Reason:", e);
    }

    public void contractIndicatorFailed(String code, String contractOuterId, Exception e) {
        log.error(CONTRACT_INDICATOR_CALCULATION_FAILED_MESSAGE, code, contractOuterId);
        log.error("Reason:", e);
    }

    public void lotIndicatorFailed(String code, String tenderOuterId, String lotOuterId, Exception e) {
        log.error(LOT_INDICATOR_CALCULATION_FAILED_MESSAGE, code, tenderOuterId, lotOuterId);
        log.error("Reason:", e);
    }
}
