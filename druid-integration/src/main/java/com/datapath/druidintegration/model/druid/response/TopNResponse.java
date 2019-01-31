package com.datapath.druidintegration.model.druid.response;

import lombok.Data;

import java.util.List;

@Data
public class TopNResponse {
    private String timestamp;
    private List<Result> result;

    @Data
    public static class Result {
      private String tenderOuterId;
      private String procedureType;
      private String status;
      private String indicatorId;
      private String contractOuterId;
      private String tmax;
    }
}
