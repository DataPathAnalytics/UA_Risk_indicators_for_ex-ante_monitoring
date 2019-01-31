package com.datapath.druidintegration.model;

import lombok.Data;


@Data
public class DruidUploadResponse {

    private Result result;

    @Data
    public static class Result {
        private int received;
        private int sent;
    }

}
