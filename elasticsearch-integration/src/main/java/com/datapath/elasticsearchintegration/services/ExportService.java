package com.datapath.elasticsearchintegration.services;

import java.util.List;

public interface ExportService {

    byte[] export(List<String> tenderIds, List<String> columns);
}
