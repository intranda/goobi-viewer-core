package io.goobi.viewer.controller;

import java.util.List;
import java.util.Map;

import io.goobi.viewer.exceptions.DAOException;

@FunctionalInterface
public interface DAOSearchFunction<T> {
    List<T> apply(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;
}
