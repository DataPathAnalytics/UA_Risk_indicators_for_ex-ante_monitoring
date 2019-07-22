package com.datapath.web.api.rest.controller;


import com.datapath.elasticsearchintegration.domain.FilterQuery;
import com.datapath.elasticsearchintegration.domain.FilteringDTO;
import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.MonitoringBucketItemDTO;
import com.datapath.elasticsearchintegration.services.ElasticsearchDataExtractorService;
import com.datapath.elasticsearchintegration.services.ExcelExportService;
import com.datapath.elasticsearchintegration.services.ExportService;
import com.datapath.elasticsearchintegration.services.MonitoringBucketService;
import com.datapath.elasticsearchintegration.util.Mapping;
import com.datapath.persistence.entities.monitoring.User;
import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.dto.bucket.TenderIdsWrapper;
import com.datapath.web.exceptions.BucketOverfullException;
import com.datapath.web.exceptions.ExportOverfullException;
import com.datapath.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringIndicatorsController {

    private final ElasticsearchDataExtractorService elasticService;
    private final ExportService exportService;
    private final MonitoringBucketService monitoringBucketService;
    private final UserService userService;

    private static final int EXPORT_MAX_QUANTITY = 60_000;

    @Autowired
    public MonitoringIndicatorsController(ElasticsearchDataExtractorService elasticService, ExcelExportService exportService, MonitoringBucketService monitoringBucketService, UserService userService) {
        this.elasticService = elasticService;
        this.exportService = exportService;
        this.monitoringBucketService = monitoringBucketService;
        this.userService = userService;
    }

    @ApiVersion({0.1})
    @PostMapping("/filter/all")
    public FilteringDTO filterEverything(@RequestBody @Valid FilterQuery filterQuery, BindingResult result, HttpServletResponse response) throws IOException {

        if (result.hasErrors()) {
            response.sendError(400, "Bad date range");
            return null;
        }

        FilteringDTO filteringDTO = elasticService.applyFilter(filterQuery);
        filteringDTO.setKpiInfo(elasticService.getKpiInfo());
        filteringDTO.setKpiInfoFiltered(elasticService.getKpiInfoFiltered(filterQuery));
        filteringDTO.setChartsDataWraper(elasticService.getChartsDataWrapper(filterQuery));
        return filteringDTO;
    }

    @ApiVersion({0.1})
    @PostMapping("/filter-data")
    public Object search(@RequestBody @Valid FilterQuery filterQuery, BindingResult result, HttpServletResponse response) throws IOException {
        if (result.hasErrors()) {
            response.sendError(400, "Bad date range");
            return Collections.emptyList();
        }
        return elasticService.getFilterData(filterQuery);
    }

    @ApiVersion({0.1})
    @PostMapping("/check-all")
    public Object checkAll(@RequestBody @Valid FilterQuery filterQuery, BindingResult result, HttpServletResponse response) throws IOException {
        if (result.hasErrors()) {
            response.sendError(400, "Bad date range");
            return Collections.emptyList();
        }
        filterQuery.setSize(50000);
        return elasticService.checkAll(filterQuery);
    }

    @ApiVersion({0.1})
    @PostMapping("/export")
    public ResponseEntity<Resource> exportToExcel(@RequestBody TenderIdsWrapper tenderIdsWrapper) {

        if (tenderIdsWrapper.getTenderIds().size() > EXPORT_MAX_QUANTITY) {
            throw new ExportOverfullException(EXPORT_MAX_QUANTITY);
        }
        Resource resource = new ByteArrayResource(exportService.export(tenderIdsWrapper.getTenderIds(), tenderIdsWrapper.getColumns()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Ternders_Export.xlsx\"")
                .body(resource);
    }

    @ApiVersion({0.1})
    @GetMapping("/export/fields")
    public List<KeyValueObject> exportMapping() {
        return Mapping.EXPORT_FIELD_MAPPING;
    }

    @ApiVersion({0.1})
    @GetMapping("/mapping/risks")
    public List<KeyValueObject> risksMapping() {
        return Mapping.RISK_INDICATORS.entrySet().stream().map(entry -> new KeyValueObject(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    @ApiVersion({0.1})
    @PutMapping("/bucket")
    public void addToBucket(@RequestBody TenderIdsWrapper tenderIdsWrapper) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userService.findDBUserByEmail(currentPrincipalName);
        if (monitoringBucketService.isTooMuchForBucket(tenderIdsWrapper.getTenderIds().size(), user)) {
            throw new BucketOverfullException(MonitoringBucketService.BUCKET_MAX_COUNT);
        }
        monitoringBucketService.add(tenderIdsWrapper.getTenderIds(), user);
    }

    @ApiVersion({0.1})
    @GetMapping("/bucket")
    public List<MonitoringBucketItemDTO> getBucket() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userService.findDBUserByEmail(currentPrincipalName);
        return monitoringBucketService.get(user);
    }

    @ApiVersion({0.1})
    @DeleteMapping("/bucket")
    public void deleteFromBucket(@RequestBody TenderIdsWrapper tenderIdsWrapper) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = userService.findDBUserByEmail(currentPrincipalName);
        monitoringBucketService.delete(tenderIdsWrapper.getTenderIds(), user);
    }
}
