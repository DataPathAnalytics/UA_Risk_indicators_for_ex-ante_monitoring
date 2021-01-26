package com.datapath.integration.scheduling;

import com.datapath.persistence.entities.WeekendOn;
import com.datapath.persistence.entities.WorkdayOff;
import com.datapath.persistence.repositories.WeekendOnRepository;
import com.datapath.persistence.repositories.WorkdayOffRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class CalendarUpdateScheduled {

    @Value("${prozorro.workdays-off.url}")
    private String workdaysOffUrl;
    @Value("${prozorro.weekends-on.url}")
    private String weekendsOnUrl;

    private WorkdayOffRepository workdayOffRepository;
    private WeekendOnRepository weekendOnRepository;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public CalendarUpdateScheduled(WorkdayOffRepository workdayOffRepository, WeekendOnRepository weekendOnRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.workdayOffRepository = workdayOffRepository;
        this.weekendOnRepository = weekendOnRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(fixedDelay = 1_000 * 60 * 60 * 24, initialDelay = 1_000)
    public void updateWeekendsOn() throws IOException {
        log.info("Update weekends on started");
        String response = restTemplate.getForObject(weekendsOnUrl, String.class);
        JsonNode data = objectMapper.readTree(response);

        Set<WeekendOn> weekendOns = new HashSet<>();
        for (JsonNode date : data) {
            WeekendOn weekendOn = new WeekendOn();
            weekendOn.setDate(LocalDate.parse(date.asText()));
            weekendOns.add(weekendOn);
        }
        if (!isEmpty(weekendOns)) {
            weekendOnRepository.deleteAll();
            weekendOnRepository.saveAll(weekendOns);
        }
        log.info("Update weekends on finished");
    }

    @Transactional
    @Scheduled(fixedDelay = 1_000 * 60 * 60 * 24, initialDelay = 1_000)
    public void updateWorkdaysOff() throws IOException {
        log.info("Update workdays off started");
        String response = restTemplate.getForObject(workdaysOffUrl, String.class);
        JsonNode data = objectMapper.readTree(response);

        Set<WorkdayOff> workdayOffs = new HashSet<>();
        for (JsonNode date : data) {
            WorkdayOff workdayOff = new WorkdayOff();
            workdayOff.setDate(LocalDate.parse(date.asText()));
            workdayOffs.add(workdayOff);
        }
        if (!isEmpty(workdayOffs)) {
            workdayOffRepository.deleteAll();
            workdayOffRepository.saveAll(workdayOffs);
        }
        log.info("Update workdays off finished");
    }
}
