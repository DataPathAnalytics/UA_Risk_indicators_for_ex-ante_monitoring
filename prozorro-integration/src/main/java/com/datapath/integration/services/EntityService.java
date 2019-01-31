package com.datapath.integration.services;

import java.util.List;

public interface EntityService<T> {

    T getById(Long id);

    T findById(Long id);

    List<T> findAll();

    T save(T entity);

    List<T> save(List<T> entities);

}
