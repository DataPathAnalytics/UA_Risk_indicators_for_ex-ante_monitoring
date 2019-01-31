package com.datapath.indicatorsqueue.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageableResource<T> {

    private List<T>  content;
    private Integer totalPages;
    private Long totalElements;
    private Integer numberOfTopRiskedTenders;

}
