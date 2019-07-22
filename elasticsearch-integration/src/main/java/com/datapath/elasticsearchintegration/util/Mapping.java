package com.datapath.elasticsearchintegration.util;

import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.IndicatorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;

/**
 * @author vitalii
 */
@Component
@Slf4j
public class Mapping {

    private final IndicatorRepository indicatorRepository;

    public static final Map<String, KeyValueObject> GSW;
    public static final Map<String, KeyValueObject> MONITORING_CAUSE;
    public static final Map<String, KeyValueObject> MONITORING_STATUS;
    public static final Map<String, KeyValueObject> PROCEDURE_TYPES;
    public static final Map<String, KeyValueObject> RISKED_PROCEDURES;
    public static final Map<String, KeyValueObject> TENDER_STATUS;
    public static final Map<String, KeyValueObject> COMPLAINTS;
    public static final Map<String, KeyValueObject> APPEAL;
    public static final Map<String, KeyValueObject> PROCURING_ENTITY_KIND;
    public static final List<KeyValueObject> EXPORT_FIELD_MAPPING;
    public static Map<String, String> RISK_INDICATORS;
    public static Map<String, String> RISK_INDICATORS_ACTIVE;
    public static Map<String, List<String>> RISK_INDICATORS_PROCEDURES;

    private static Function<String, Map.Entry<String, String>> mapToItem = line -> {
        String[] p = line.split("\t");
        return new AbstractMap.SimpleEntry<>(p[0], p[1]);
    };

    static {
        GSW = initMapping("gsw");
        MONITORING_CAUSE = initMapping("monitoringCause");
        MONITORING_STATUS = initMapping("monitoringStatus");
        PROCEDURE_TYPES = initMapping("procedureTypes");
        RISKED_PROCEDURES = initMapping("riskedProcedures");
        TENDER_STATUS = initMapping("tenderStatus");
        COMPLAINTS = initMapping("complaints");
        APPEAL = initMapping("appeal");
        PROCURING_ENTITY_KIND = initMapping("procuringEntityKind");
    }

    @Autowired
    public Mapping(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
        RISK_INDICATORS = new HashMap<>();
        RISK_INDICATORS_PROCEDURES = new HashMap<>();
        RISK_INDICATORS_ACTIVE = new HashMap<>();
        for (Indicator indicator : this.indicatorRepository.findAll()) {
            RISK_INDICATORS.put(indicator.getId(), indicator.getName());
            RISK_INDICATORS_PROCEDURES.put(indicator.getId(), Arrays.asList(indicator.getProcedureTypes()));
            if (indicator.getIsActive()) {
                RISK_INDICATORS_ACTIVE.put(indicator.getId(), indicator.getName());
            }
        }

    }


    private static Map<String, KeyValueObject> initMapping(String inputFilePath) {
        Map<String, KeyValueObject> inputList = new HashMap<>(10);
        try {
            InputStream is = new ClassPathResource("mappings/" + inputFilePath + ".tsv").getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            br.lines().map(mapToItem).
                    forEach(item -> inputList.put(item.getKey(), new KeyValueObject(item.getKey(), item.getValue())));
            br.close();
        } catch (IOException e) {
            log.error("File not found", e);
        }
        return inputList;
    }

    static {
        EXPORT_FIELD_MAPPING = new ArrayList<>();
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("tenderId", "ID процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("expectedValue", "Очікувана вартість"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("tenderRiskScore", "Ризик бал процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("tenderRiskScoreRank", "Ранг ризику"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("region", "Регіон"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("indicatorsWithRisk", "Ризик"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("riskName", "Назва ризику"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("tenderStatus", "Статус процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("procedureType", "Метод закупівлі"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("procuringEntityEDRPOU", "ЄДР замовника"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("procuringEntityKind", "Вид замовника"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("cpv", "Код предмету закупівлі процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("cpvName", "Назва предмету закупівлі"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("cpv2", "Розділ Єдиного закупівельного словника"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("cpv2Name", "Назва розділу Єдиного закупівельного словника"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("gsw", "Товари, роботи, послуги"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("tenderName", "Назва процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("datePublished", "Дата процедури"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("procuringEntityName", "Назва замовника"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("monitoringStatus", "Статус моніторинга"));
        EXPORT_FIELD_MAPPING.add(new KeyValueObject("monitoringAppeal", "Наявність скарг"));
    }

    public static final List<String> SORT_FIELDS_WITH_KEYWORD = Arrays.asList(
            "tenderOuterId",
            "tenderId",
            "indicators",
            "indicatorsWithRisk",
            "indicatorsWithoutRisk",
            "tenderStatus",
            "procedureType",
            "monitoringStatus",
            "monitoringCause",
            "currency",
            "cpv",
            "cpv2",
            "cpvName",
            "cpv2Name",
            "gsw",
            "procuringEntityEDRPOU",
            "procuringEntityName",
            "procuringEntityKind",
            "region"
    );

    public static final List<String> SORT_FIELDS_WITHOUT_KEYWORD = Arrays.asList(
            "tenderRiskScore",
            "monitoringAppeal",
            "isInQueue",
            "hasPriorityStatus",
            "hasComplaints",
            "expectedValue",
            "datePublished"
    );
}
