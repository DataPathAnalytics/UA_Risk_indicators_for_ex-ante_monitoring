package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.druidintegration.model.DruidTenderIndicator;
import com.datapath.druidintegration.service.ExtractContractDataService;
import com.datapath.druidintegration.service.ExtractTenderDataService;
import com.datapath.druidintegration.service.UploadDataService;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.DruidIndicatorMapper;
import com.datapath.nbu.service.ExchangeRateService;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;


@Service
@Slf4j
public class BaseExtractor {

    protected Integer AVAILABLE_HOURS_DIFF = 2;

    @Value("${prozorro.workdays-off.url}")
    private String workdaysOffUrl;
    @Value("${prozorro.weekends-on.url}")
    private String weekendsOnUrl;

    protected final String INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT = "%s Extractor is not available";
    protected final String UPDATE_MESSAGE_FORMAT = "Update indicator: %s";
    private final String PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT = "Previous equals current: %s";
    protected final String TENDER_INDICATOR_ERROR_MESSAGE = "ERROR while processing indicator: %s tender: %s";

    protected final String EUR_CURRENCY = "EUR";
    protected final String UAH_CURRENCY = "UAH";

    protected final Integer RISK = 1;
    protected final Integer NOT_RISK = 0;
    protected final Integer IMPOSSIBLE_TO_DETECT = -1;


    protected final String COMMA_SEPARATOR = ",";

    protected AwardRepository awardRepository;
    protected ContractRepository contractRepository;
    protected DocumentRepository documentRepository;
    protected DruidIndicatorMapper druidIndicatorMapper;
    protected ExchangeRateService exchangeRateService;
    protected ExtractContractDataService extractContractDataService;
    protected ExtractTenderDataService extractDataService;
    protected IndicatorRepository indicatorRepository;
    protected LotRepository lotRepository;
    protected QuestionRepository questionRepository;
    protected RestTemplate restTemplate;
    protected TenderDataRepository tenderDataRepository;
    protected TenderContractRepository tenderContractRepository;
    protected ContractDocumentRepository contractDocumentRepository;
    protected TenderRepository tenderRepository;
    protected ContractChangeRepository contractChangeRepository;
    protected TenderItemRepository tenderItemRepository;
    protected UploadDataService uploadDataService;
    protected QualificationRepository qualificationRepository;

    @Autowired
    public void setIndicatorRepository(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    @Autowired
    public void setTenderContractRepository(TenderContractRepository tenderContractRepository) {
        this.tenderContractRepository = tenderContractRepository;
    }

    @Autowired
    public void setTenderRepository(TenderRepository tenderRepository) {
        this.tenderRepository = tenderRepository;
    }

    @Autowired
    public void setTenderItemRepository(TenderItemRepository tenderItemRepository) {
        this.tenderItemRepository = tenderItemRepository;
    }

    @Autowired
    public void setContractRepository(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Autowired
    public void setContractChangeRepository(ContractChangeRepository contractChangeRepository) {
        this.contractChangeRepository = contractChangeRepository;
    }

    @Autowired
    public void setAwardRepository(AwardRepository awardRepository) {
        this.awardRepository = awardRepository;
    }

    @Autowired
    public void setExchangeRateService(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Autowired
    public void setLotRepository(LotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }


    @Autowired
    public void setQuestionRepository(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Autowired
    public void setTenderDataRepository(TenderDataRepository tenderDataRepository) {
        this.tenderDataRepository = tenderDataRepository;
    }

    @Autowired
    public void setDruidIndicatorMapper(DruidIndicatorMapper druidIndicatorMapper) {
        this.druidIndicatorMapper = druidIndicatorMapper;
    }

    @Autowired
    public void setExtractTenderDataService(ExtractTenderDataService extractDataService) {
        this.extractDataService = extractDataService;
    }

    @Autowired
    public void setUploadDataService(UploadDataService uploadDataService) {
        this.uploadDataService = uploadDataService;
    }

    @Autowired
    public void setExtractContractDataService(ExtractContractDataService extractContractDataService) {
        this.extractContractDataService = extractContractDataService;
    }

    @Autowired
    public void setDocumentRepository(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Autowired
    public void setContractDocumentRepository(ContractDocumentRepository contractDocumentRepository) {
        this.contractDocumentRepository = contractDocumentRepository;
    }

    @Autowired
    public void setQualificationRepository(QualificationRepository qualificationRepository) {
        this.qualificationRepository = qualificationRepository;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private List<ZonedDateTime> getListOfWorkingWeekEnds() {
        String getForObject = restTemplate.getForObject(weekendsOnUrl, String.class);
        return parseListOfDates(getForObject);
    }

    private List<ZonedDateTime> getListOfWeekEnds() {
        String getForObject = restTemplate.getForObject(workdaysOffUrl, String.class);
        return parseListOfDates(getForObject);
    }

    private List<ZonedDateTime> parseListOfDates(String forObject) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ZonedDateTime> dates = new ArrayList<>();
        try {
            List<Object> datesList = new ArrayList<>();
            datesList.addAll(objectMapper.readValue(forObject, List.class));
            datesList.forEach(date -> {
                        LocalDate parse = LocalDate.parse(date.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        dates.add(ZonedDateTime.of(parse, LocalTime.MIDNIGHT, ZoneId.of("Europe/Kiev")));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return dates;
        }
    }

    protected ZonedDateTime getDateOfCurrentDateMinusNWorkingDays(Integer daysAmount) {
        Integer count = 0;
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Kiev")).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<ZonedDateTime> listOfWorkingWeekEnds = getListOfWorkingWeekEnds();
        List<ZonedDateTime> listOfWeekEnds = getListOfWeekEnds();
        List<String> workingDays = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        List<String> weekEndDays = Arrays.asList("SATURDAY", "SUNDAY");

        while (count < daysAmount) {
            now = now.minusDays(1L);
            String dayName = now.getDayOfWeek().toString();
            if ((workingDays.contains(dayName) && !listOfWeekEnds.contains(now)) ||
                    (weekEndDays.contains(dayName) && listOfWorkingWeekEnds.contains(now))) {
                count++;
            }
        }
        return now;
    }

    public ZonedDateTime getDateOfDateMinusNWorkingDays(ZonedDateTime date, Integer daysAmount) {
        Integer count = 0;
        date = date.withZoneSameInstant(ZoneId.of("Europe/Kiev")).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<ZonedDateTime> listOfWorkingWeekEnds = getListOfWorkingWeekEnds();
        List<ZonedDateTime> listOfWeekEnds = getListOfWeekEnds();
        List<String> workingDays = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        List<String> weekEndDays = Arrays.asList("SATURDAY", "SUNDAY");

        while (count < daysAmount) {
            date = date.minusDays(1L);
            String dayName = date.getDayOfWeek().toString();
            if ((workingDays.contains(dayName) && !listOfWeekEnds.contains(date)) ||
                    (weekEndDays.contains(dayName) && listOfWorkingWeekEnds.contains(date))) {
                count++;
            }
        }
        return date;
    }


    List<String> findTenders(ZonedDateTime date,
                             List<String> procedureStatuses,
                             List<String> procedureTypes,
                             List<String> procuringEntityKind,
                             Integer page,
                             Integer size) {
        if (!procedureStatuses.isEmpty() && !procedureTypes.isEmpty()) {
            return tenderRepository.findTenderIdByProcedureStatusAndProcedureType(date, procedureStatuses,
                    procedureTypes, procuringEntityKind, PageRequest.of(page, size));
        }
        if (!procedureStatuses.isEmpty()) {
            return tenderRepository.findTenderIdByProcedureStatus(date, procedureStatuses,
                    procuringEntityKind, PageRequest.of(page, size));
        }
        return tenderRepository.findTenderIdByProcedureType(date, procedureTypes,
                procuringEntityKind, PageRequest.of(page, size));
    }

    Indicator getActiveIndicator(String indicatorId) {
        Optional<Indicator> indicatorOptional = indicatorRepository.findFirstByIdAndAndIsActive(indicatorId, true);
        return indicatorOptional.orElse(null);
    }

    void uploadIndicator(TenderIndicator tenderIndicator) {
        DruidTenderIndicator druidIndicator = druidIndicatorMapper.transformToDruidTenderIndicator(tenderIndicator);
        Long lastIterationForIndicatorValue = extractDataService.findLastIterationForTenderIndicatorsData(Collections.singletonList(druidIndicator));

        if (!Objects.equals(lastIterationForIndicatorValue, tenderIndicator.getTenderDimensions().getDruidCheckIteration())) {
            log.info(String.format(UPDATE_MESSAGE_FORMAT, druidIndicator));
            uploadDataService.uploadTenderIndicator(druidIndicator);
        } else {
            log.debug(String.format(PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT, druidIndicator.getTenderId()));
        }
    }

    void uploadIndicatorIfNotExists(String tenderId, String indicatorId, TenderIndicator tenderIndicator) {
        DruidTenderIndicator druidIndicator = druidIndicatorMapper.transformToDruidTenderIndicator(tenderIndicator);
        Boolean theLastTenderEqualsCurrent = extractDataService.theLastTenderEquals(tenderId,
                indicatorId,
                Collections.singletonList(druidIndicator));

        if (!theLastTenderEqualsCurrent) {
            log.info(String.format(UPDATE_MESSAGE_FORMAT, druidIndicator));
            uploadDataService.uploadTenderIndicator(druidIndicator);
        } else {
            log.debug(String.format(PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT, druidIndicator));
        }
    }

    void uploadIndicators(List<TenderIndicator> tenderIndicators, Long maxTenderIteration) {
        if (tenderIndicators.isEmpty()) return;

        List<DruidTenderIndicator> druidIndicators = druidIndicatorMapper.transformToDruidTenderIndicator(tenderIndicators);

        Long lastIterationByIndicators = extractDataService.findLastIterationForTenderIndicatorsData(druidIndicators);

        if (!Objects.equals(lastIterationByIndicators, maxTenderIteration)) {
            log.info(String.format(UPDATE_MESSAGE_FORMAT, druidIndicators));
            uploadDataService.uploadTenderIndicator(druidIndicators);
        } else {
            log.debug(String.format(PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT, druidIndicators));
        }
    }

    void uploadIndicatorIfNotExists(String tenderId, String indicatorId, List<TenderIndicator> tenderIndicators) {
        if (tenderIndicators.isEmpty()) {
            return;
        }
        List<DruidTenderIndicator> druidIndicators = druidIndicatorMapper.transformToDruidTenderIndicator(tenderIndicators);
        Boolean theLastTenderEqualsCurrent = extractDataService.theLastTenderEquals(tenderId,
                indicatorId,
                druidIndicators);

        if (!theLastTenderEqualsCurrent) {
            log.info(String.format(UPDATE_MESSAGE_FORMAT, druidIndicators));
            uploadDataService.uploadTenderIndicator(druidIndicators);
        } else {
            log.debug(String.format(PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT, druidIndicators));
        }
    }

    Map<String, TenderDimensions> getTenderDimensionsWithIndicatorLastIteration(Set<String> tenderIds, String indicatorId) {
        log.debug("Start receiving tender dimensions");
        Map<String, TenderDimensions> tenderDimensions = getTenderDimensionsMap(tenderIds);
        Map<String, Long> maxTenderIndicatorIteration = extractDataService.getMaxTenderIndicatorIteration(tenderIds, indicatorId);
        tenderDimensions.forEach((key, value) -> value.setDruidCheckIteration(maxTenderIndicatorIteration.get(key)));
        log.debug("Finish receiving tender dimensions");
        return tenderDimensions;
    }

    private Map<String, TenderDimensions> getTenderDimensionsMap(Set<String> tenderIds) {

        List<Object[]> tenders = tenderRepository.findAllByOuterIdIn(tenderIds.stream().collect(Collectors.joining(",")));

        Map<String, TenderDimensions> result = new HashMap<>();
        tenders.forEach(o -> {

            TenderDimensions tenderDimensions = new TenderDimensions();
            String outerId = o[0].toString();
            String tenderId = o[1].toString();
            Timestamp dateTimestamp = (Timestamp) o[2];
            Timestamp dateModifiedTimestamp = (Timestamp) o[3];
            String procedureType = o[4].toString();
            String status = o[5].toString();
            Timestamp dateCreatedTimestamp = (Timestamp) o[6];

            tenderDimensions.setId(outerId);
            tenderDimensions.setTenderId(tenderId);
            tenderDimensions.setDate(isNull(dateTimestamp) ? null : toZonedDateTime(dateTimestamp));
            tenderDimensions.setModifiedDate(isNull(dateModifiedTimestamp) ? null : toZonedDateTime(dateModifiedTimestamp));
            tenderDimensions.setProcedureType(procedureType);
            tenderDimensions.setStatus(status);
            tenderDimensions.setDateCreated(isNull(dateCreatedTimestamp) ? null : toZonedDateTime(dateCreatedTimestamp));
            result.put(outerId, tenderDimensions);
        });

        return result;
    }

    Map<String, TenderDimensions> getTenderDimensionsWithIndicatorLastIteration(List<Tender> tenders, String indicatorId) {
        Map<String, TenderDimensions> tenderDimensions = getTenderDimensionsMap(tenders);

        Set<String> tenderIds = tenders.stream().map(Tender::getOuterId).collect(Collectors.toSet());
        Map<String, Long> maxTenderIndicatorIteration = extractDataService.getMaxTenderIndicatorIteration(tenderIds, indicatorId);
        tenderDimensions.forEach((key, value) -> value.setDruidCheckIteration(maxTenderIndicatorIteration.get(key)));
        return tenderDimensions;
    }

    private Map<String, TenderDimensions> getTenderDimensionsMap(List<Tender> tenders) {

        Map<String, TenderDimensions> result = new HashMap<>();
        tenders.forEach(tender -> {

            TenderDimensions tenderDimensions = new TenderDimensions();

            tenderDimensions.setId(tender.getOuterId());
            tenderDimensions.setTenderId(tender.getTenderId());
            tenderDimensions.setDate(tender.getDate());
            tenderDimensions.setModifiedDate(tender.getDateModified());
            tenderDimensions.setProcedureType(tender.getProcurementMethodType());
            tenderDimensions.setStatus(tender.getStatus());
            tenderDimensions.setDateCreated(tender.getDateCreated());
            result.put(tender.getOuterId(), tenderDimensions);
        });

        return result;
    }


    Map<String, ContractDimensions> getContractDimensionsWithIndicatorLastIteration(Set<String> contractIds, String indicatorId) {
        Map<String, ContractDimensions> contractDimensions = getContractDimensionsMap(contractIds);
        contractDimensions.entrySet().stream().forEach(item -> {
            Long maxIteration = extractContractDataService.getMaxIndicatorIteration(item.getKey(), indicatorId);
            item.getValue().setDruidCheckIteration(maxIteration);
        });
        return contractDimensions;
    }

    private Map<String, ContractDimensions> getContractDimensionsMap(Set<String> contractIds) {
        return contractIds.stream().map(this::getContractDimensions)
                .collect(Collectors.toMap(ContractDimensions::getContractId, item -> item));
    }

    private ContractDimensions getContractDimensions(String contractId) {
        Object[] contractDimensionsObject = tenderContractRepository.getContractDimensions(contractId);
        Object[] contract = (Object[]) contractDimensionsObject[0];
        ContractDimensions contractDimensions = new ContractDimensions();
        contractDimensions.setContractIdHr((String) contract[0]);
        contractDimensions.setContractId((String) contract[1]);
        contractDimensions.setDateCreated(toZonedDateTime((Timestamp) contract[2]));
        contractDimensions.setTenderIdHr((String) contract[3]);
        contractDimensions.setTenderId((String) contract[4]);
        contractDimensions.setStatus((String) contract[5]);
        contractDimensions.setProcedureType((String) contract[6]);
        return contractDimensions;
    }

    ZonedDateTime getMaxTenderDateCreated(Map<String, TenderDimensions> dimensionsMap, ZonedDateTime defaultDateTime) {
        return dimensionsMap.values()
                .stream().map(TenderDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

    ZonedDateTime getMaxContractDateCreated(Map<String, ContractDimensions> dimensionsMap, ZonedDateTime defaultDateTime) {
        return dimensionsMap.values()
                .stream().map(ContractDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

}
