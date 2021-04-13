package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.druidintegration.model.DruidTenderIndicator;
import com.datapath.druidintegration.service.ExtractContractDataService;
import com.datapath.druidintegration.service.ExtractTenderDataService;
import com.datapath.druidintegration.service.UploadDataService;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.DruidIndicatorMapper;
import com.datapath.indicatorsresolver.service.IndicatorLogService;
import com.datapath.nbu.service.ExchangeRateService;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


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

    protected static final Integer RISK = 1;
    protected static final Integer NOT_RISK = 0;
    protected static final Integer IMPOSSIBLE_TO_DETECT = -1;
    protected static final Integer CONDITIONS_NOT_MET = -2;

    protected static final List<String> workingDays = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    protected static final List<String> weekEndDays = Arrays.asList("SATURDAY", "SUNDAY");

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
    protected IndicatorLogService logService;

    @Autowired
    public void setLogService(IndicatorLogService logService) {
        this.logService = logService;
    }

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
                        dates.add(ZonedDateTime.of(parse, LocalTime.MIDNIGHT, UA_ZONE));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dates;
    }

    public long getDaysBetween(ZonedDateTime startDate, ZonedDateTime endDate) {
        startDate = toUaMidnight(startDate);
        endDate = toUaMidnight(endDate);
        return Duration.between(startDate, endDate).toDays();
    }

    public int getWorkingDaysBetween(ZonedDateTime startDate, ZonedDateTime endDate) {
        List<ZonedDateTime> listOfWorkingWeekEnds = getListOfWorkingWeekEnds();
        List<ZonedDateTime> listOfWeekEnds = getListOfWeekEnds();

        int count = 0;

        startDate = toUaMidnight(startDate).plusDays(1);

        endDate = toUaMidnight(endDate);

        ZonedDateTime current = startDate;

        while (current.isBefore(endDate) || current.equals(endDate)) {
            String dayName = current.getDayOfWeek().toString();
            if ((workingDays.contains(dayName) && !listOfWeekEnds.contains(current)) ||
                    (weekEndDays.contains(dayName) && listOfWorkingWeekEnds.contains(current))) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }

    protected ZonedDateTime getDateOfCurrentDateMinusNWorkingDays(Integer daysAmount) {
        int count = 0;
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(UA_ZONE).withHour(0).withMinute(0).withSecond(0).withNano(0);
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
        int count = 0;
        date = date.withZoneSameInstant(UA_ZONE).withHour(0).withMinute(0).withSecond(0).withNano(0);
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


    protected List<String> findTenders(ZonedDateTime date,
                             List<String> procedureStatuses,
                             List<String> procedureTypes,
                             List<String> procuringEntityKind) {
        if (!procedureStatuses.isEmpty() && !procedureTypes.isEmpty()) {
            return tenderRepository.findTenderIdByProcedureStatusAndProcedureType(date, procedureStatuses,
                    procedureTypes, procuringEntityKind);
        }
        if (!procedureStatuses.isEmpty()) {
            return tenderRepository.findTenderIdByProcedureStatus(date, procedureStatuses,
                    procuringEntityKind);
        }
        return tenderRepository.findTenderIdByProcedureType(date, procedureTypes,
                procuringEntityKind);
    }

    List<Tender> findTenders(ZonedDateTime dateTime, Indicator indicator) {
        return tenderRepository.findTenders(dateTime,
                Arrays.asList(indicator.getProcedureStatuses()),
                Arrays.asList(indicator.getProcedureTypes()),
                Arrays.asList(indicator.getProcuringEntityKind()));
    }

    protected Indicator getIndicator(String indicatorId) {
        return indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new RuntimeException("Can't find indicator with id " + indicatorId));
    }

    protected void uploadIndicator(TenderIndicator tenderIndicator) {
        DruidTenderIndicator druidIndicator = druidIndicatorMapper.transformToDruidTenderIndicator(tenderIndicator);
        Long lastIterationForIndicatorValue = extractDataService.findLastIterationForTenderIndicatorsData(Collections.singletonList(druidIndicator));

        if (!Objects.equals(lastIterationForIndicatorValue, tenderIndicator.getTenderDimensions().getDruidCheckIteration())) {
            log.info(String.format(UPDATE_MESSAGE_FORMAT, druidIndicator));
            uploadDataService.uploadTenderIndicator(druidIndicator);
        } else {
            log.debug(String.format(PREVIOUS_EQUALS_CURRENT_MESSAGE_FORMAT, druidIndicator.getTenderId()));
        }
    }

    protected void uploadIndicatorIfNotExists(String tenderId, String indicatorId, TenderIndicator tenderIndicator) {
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

    protected void uploadIndicators(List<TenderIndicator> tenderIndicators, Long maxTenderIteration) {
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

    protected void uploadIndicatorIfNotExists(String tenderId, String indicatorId, List<TenderIndicator> tenderIndicators) {
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

    protected Map<String, TenderDimensions> getTenderDimensionsWithIndicatorLastIteration(Set<String> tenderIds, String indicatorId) {
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

    protected Map<String, TenderDimensions> getTenderDimensionsWithIndicatorLastIteration(List<Tender> tenders, String indicatorId) {
        Map<String, TenderDimensions> tenderDimensions = getTenderDimensionsMap(tenders);

        Set<String> tenderIds = tenders.stream().map(Tender::getOuterId).collect(toSet());
        Map<String, Long> maxTenderIndicatorIteration = extractDataService.getMaxTenderIndicatorIteration(tenderIds, indicatorId);
        tenderDimensions.forEach((key, value) -> value.setDruidCheckIteration(maxTenderIndicatorIteration.get(key)));
        return tenderDimensions;
    }

    protected Map<String, TenderDimensions> getTenderDimensionsMap(List<Tender> tenders) {

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


    protected Map<String, ContractDimensions> getContractDimensionsWithIndicatorLastIteration(Set<String> contractIds, String indicatorId) {
        Map<String, ContractDimensions> contractDimensions = getContractDimensionsMap(contractIds);
        contractDimensions.forEach((key, value) -> {
            Long maxIteration = extractContractDataService.getMaxIndicatorIteration(key, indicatorId);
            value.setDruidCheckIteration(maxIteration);
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

    protected ZonedDateTime getMaxTenderDateCreated(Map<String, TenderDimensions> dimensionsMap, ZonedDateTime defaultDateTime) {
        return dimensionsMap.values()
                .stream().map(TenderDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

    protected ZonedDateTime getMaxTenderDateCreated(List<TenderIndicator> tenderIndicators, ZonedDateTime defaultDateTime) {
        return tenderIndicators
                .stream()
                .map(TenderIndicator::getTenderDimensions)
                .map(TenderDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

    protected ZonedDateTime getMaxContractDateCreated(Map<String, ContractDimensions> dimensionsMap, ZonedDateTime defaultDateTime) {
        return dimensionsMap.values()
                .stream().map(ContractDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

    protected ZonedDateTime getMaxContractDateCreated(List<ContractIndicator> contractIndicators, ZonedDateTime defaultDateTime) {
        return contractIndicators
                .stream()
                .map(ContractIndicator::getContractDimensions)
                .map(ContractDimensions::getDateCreated)
                .max(Comparator.comparing(item -> item)).orElse(defaultDateTime);
    }

    protected List<LocalDate> dateBetween(LocalDate start, LocalDate end) {
        return LongStream.range(start.toEpochDay(), end.toEpochDay()).mapToObj(LocalDate::ofEpochDay).collect(toList());
    }

    protected ZonedDateTime toUaMidnight(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(UA_ZONE).with(LocalTime.MIDNIGHT);
    }

    protected ZonedDateTime getIndicatorLastCheckedDate(Indicator indicator) {
        return isNull(indicator.getLastCheckedDateCreated())
                ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
                : indicator.getLastCheckedDateCreated();
    }

    protected Set<String> getOuterIds(List<Tender> tenders) {
        return tenders.stream().map(Tender::getOuterId).collect(toSet());
    }
}
