package com.datapath.elasticsearchintegration.domain;

import com.datapath.elasticsearchintegration.constants.RiskedProcedure;
import com.datapath.elasticsearchintegration.constants.TenderScoreRank;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author vitalii
 */
@Data
@ApiModel(description = "Class representing filtering model.")
public class FilterQuery {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date startDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date endDate;

    @ApiModelProperty(value = "Page number of returned elements")
    private int page = 0;

    @ApiModelProperty(value = "Quantity of returned elements")
    private int size = 10;

    private String procedureId;
    private List<String> riskedIndicators = new ArrayList<>();
    private RiskedProcedure riskedProcedures;
    private List<String> regions = new ArrayList<>();
    private List<String> cpv2Names = new ArrayList<>();
    private List<String> cpvNames = new ArrayList<>();
    private List<String> procedureTypes = new ArrayList<>();
    private List<String> currency = new ArrayList<>();
    private List<TenderScoreRank> tenderRank = new ArrayList<>();
    private List<String> procuringEntities = new ArrayList<>();
    private List<String> tenderStatuses = new ArrayList<>();
    private List<String> monitoringCause = new ArrayList<>();
    private List<String> monitoringOffices = new ArrayList<>();
    private String procuringEntityKind;
    private String gsw;
    private List<String> monitoringStatus = new ArrayList<>();
    private Boolean complaints;
    private Boolean monitoringAppeal;
    private Long minExpectedValue;
    private Long maxExpectedValue;
    @ApiModelProperty(value = "Search field name for getting available data for filter", allowableValues = "spvNames,spv2Names,procuringEntities")
    private String searchField;
    @ApiModelProperty(value = "Value for search in search field")
    private String searchValue;
    @ApiModelProperty(value = "Number of dropdown elements. Default = 10")
    private int searchCount;

    @ApiModelProperty(value = "field used for sorting (from procedure's entity), Default = datePublished")
    private String sortField = "datePublished";
    @ApiModelProperty(value = "Sort direction: ASC, DESC. Default = DESC")
    private String sortDirection = "DESC";
}