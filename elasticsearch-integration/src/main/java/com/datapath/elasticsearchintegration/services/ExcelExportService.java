package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.TenderIndicatorsCommonInfo;
import com.datapath.elasticsearchintegration.util.Mapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExcelExportService implements ExportService {

    private final ProcedureFilterService filterService;

    private List<String> defaultHeaders = Arrays.asList(
            "ID процедури", "Очікувана вартість", "Ризик бал процедури", "Ранг ризику", "Регіон", "Ризик", "Назва ризику", "Статус процедури",
            "Метод закупівлі", "ЄДР замовника", "Вид замовника", "Код предмету закупівлі процедури", "Назва предмету закупівлі",
            "Розділ Єдиного закупівельного словника", "Назва розділу Єдиного закупівельного словника", "Товари, роботи, послуги",
            "Назва процедури", "Дата процедури", "Назва замовника", "Статус моніторинга", "Наявність скарг");
    private static Map<String, KeyValueObject> exportFieldMapping;

    static {
        exportFieldMapping = new HashMap<>();
        exportFieldMapping.put("ID процедури", new KeyValueObject("tenderId", String.class));
        exportFieldMapping.put("Очікувана вартість", new KeyValueObject("expectedValue", String.class));
        exportFieldMapping.put("Ризик бал процедури", new KeyValueObject("tenderRiskScore", Double.class));
        exportFieldMapping.put("Ранг ризику", new KeyValueObject("tenderRiskScoreRank", String.class));
        exportFieldMapping.put("Регіон", new KeyValueObject("region", String.class));
        exportFieldMapping.put("Ризик", new KeyValueObject("indicatorsWithRisk", List.class));
        exportFieldMapping.put("Назва ризику", new KeyValueObject("riskName", List.class));
        exportFieldMapping.put("Статус процедури", new KeyValueObject("tenderStatus", String.class));
        exportFieldMapping.put("Метод закупівлі", new KeyValueObject("procedureType", String.class));
        exportFieldMapping.put("ЄДР замовника", new KeyValueObject("procuringEntityEDRPOU", String.class));
        exportFieldMapping.put("Вид замовника", new KeyValueObject("procuringEntityKind", String.class));
        exportFieldMapping.put("Код предмету закупівлі процедури", new KeyValueObject("cpv", String.class));
        exportFieldMapping.put("Назва предмету закупівлі", new KeyValueObject("cpvName", String.class));
        exportFieldMapping.put("Розділ Єдиного закупівельного словника", new KeyValueObject("cpv2", String.class));
        exportFieldMapping.put("Назва розділу Єдиного закупівельного словника", new KeyValueObject("cpv2Name", String.class));
        exportFieldMapping.put("Товари, роботи, послуги", new KeyValueObject("gsw", String.class));
        exportFieldMapping.put("Назва процедури", new KeyValueObject("tenderName", String.class));
        exportFieldMapping.put("Дата процедури", new KeyValueObject("datePublished", String.class));
        exportFieldMapping.put("Назва замовника", new KeyValueObject("procuringEntityName", String.class));
        exportFieldMapping.put("Статус моніторинга", new KeyValueObject("monitoringStatus", String.class));
        exportFieldMapping.put("Наявність скарг", new KeyValueObject("monitoringAppeal", Boolean.class));
    }

    @Autowired
    public ExcelExportService(ProcedureFilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public byte[] export(List<String> tenderIds, List<String> columns) {

        if (columns == null) {
            columns = defaultHeaders;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XSSFWorkbook wb = new XSSFWorkbook();
        writeHeaders(wb, columns);
        List<TenderIndicatorsCommonInfo> forExport = new ArrayList<>();
        int count = 0;
        do {
            forExport.addAll(filterService.getForExport(tenderIds.stream().skip(count).limit(1000).collect(Collectors.toList())));
            count = count + 1000;
        } while (count < tenderIds.size());
        writeData(wb, forExport, columns);
        try {
            wb.write(bos);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return bos.toByteArray();
    }


    private void writeHeaders(XSSFWorkbook wb, List<String> columns) {
        String sheetName = "tenders";
        XSSFSheet sheet = wb.createSheet(sheetName);
        XSSFRow row = sheet.createRow(0);
        int cellNumber = 0;

        for (String column : columns) {
            row.createCell(cellNumber++).setCellValue(column);
        }
    }

    private String getValueByFieldName(TenderIndicatorsCommonInfo info, String fieldName, Class fieldClassName) {

        Class<TenderIndicatorsCommonInfo> className = TenderIndicatorsCommonInfo.class;
        try {
            Field field;
            if (fieldName.equals("riskName")) {
                field = className.getField("indicatorsWithRisk");
            } else {
                field = className.getField(fieldName);
            }
            if (fieldClassName == Boolean.class) {
                return Mapping.APPEAL.get(String.valueOf(field.getBoolean(info))).getValue().toString();
            }
            if (fieldClassName == Double.class) {
                return String.valueOf(field.get(info));
            }
            if (fieldClassName == List.class) {
                Set<String> list = (Set<String>) field.get(info);
                if (list != null) {
                    if (fieldName.equals("riskName")) {
                        return String.join(",", mapIndicatorsName(list));
                    } else {
                        return String.join(",", list);
                    }
                }
            }
            if (fieldClassName == String.class) {
                String stringValue = String.valueOf(field.get(info));

                switch (fieldName) {
                    case "tenderStatus":
                        return Mapping.TENDER_STATUS.get(stringValue).getValue().toString();
                    case "procuringEntityKind":
                        return Mapping.PROCURING_ENTITY_KIND.get(stringValue).getValue().toString();
                    case "procedureType":
                        if (stringValue.contains("negotiation")) {
                            return Mapping.PROCEDURE_TYPES.get("negotiation").getValue().toString();
                        } else {
                            return Mapping.PROCEDURE_TYPES.get(stringValue).getValue().toString();
                        }
                    case "gsw":
                        return Mapping.GSW.get(stringValue).getValue().toString();
                    case "monitoringStatus":
                        return Mapping.MONITORING_STATUS.get(stringValue).getValue().toString();
                    default:
                        return stringValue;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return null;

    }

    private void writeData(XSSFWorkbook wb, List<TenderIndicatorsCommonInfo> tenders, List<String> columns) {
        String sheetName = "tenders";
        XSSFSheet sheet = wb.getSheet(sheetName);

        for (TenderIndicatorsCommonInfo tender : tenders) {
            XSSFRow row = sheet.createRow(tenders.indexOf(tender) + 1);
            int cellNumber = 0;
            for (String column : columns) {
                String stringExportValue = getValueByFieldName(tender, (String) exportFieldMapping.get(column).getKey(), (Class) exportFieldMapping.get(column).getValue());
                if (column.equals("Дата процедури")) {
                    try {
                        CellStyle cellStyle = wb.createCellStyle();
                        CreationHelper createHelper = wb.getCreationHelper();
                        short dateFormat = createHelper.createDataFormat().getFormat("yyyy-dd-MM");
                        cellStyle.setDataFormat(dateFormat);
                        XSSFCell cell = row.createCell(cellNumber++);
                        cell.setCellValue(castToDateCellValue(stringExportValue));
                        cell.setCellStyle(cellStyle);
                    } catch (ParseException e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    row.createCell(cellNumber++).setCellValue(castToCellValue(stringExportValue));
                }
            }
        }
    }

    private String castToCellValue(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private Date castToDateCellValue(String value) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (value == null) {
            return new Date();
        }
        Date parse = format.parse(value);
        return parse;
    }

    private List<String> mapIndicatorsName(Collection<String> list) {
        return list.stream().map(item -> Mapping.RISK_INDICATORS.get(item)).collect(Collectors.toList());
    }
}
