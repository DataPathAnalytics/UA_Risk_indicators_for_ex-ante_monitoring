package com.datapath.elasticsearchintegration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author vitalii
 */
@Data
@AllArgsConstructor
public class KeyValueObject {
    private Object key;
    private Object value;
}
