/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans.tabledata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DAOSearchFunction;
import io.goobi.viewer.exceptions.DAOException;

/**
 * Generic data provider for paginated, filterable, and sortable admin data tables.
 *
 * @param <T>
 */
public class TableDataProvider<T> implements Serializable {

    private static final long serialVersionUID = 6109453168491579420L;

    private static final Logger logger = LogManager.getLogger(TableDataProvider.class);

    private int currentPage = 0;
    private int entriesPerPage;
    private TableDataSource<T> source;
    private String sortField = "";
    private SortOrder sortOrder = SortOrder.ASCENDING;
    private List<TableDataFilter> filters = new ArrayList<>();
    private String lastFilterString = "";

    public enum SortOrder {
        ASCENDING,
        DESCENDING;

        public boolean asBoolean() {
            switch (this) {
                case DESCENDING:
                    return true;
                case ASCENDING:
                default:
                    return false;
            }
        }
    }

    /**
     * <p>initDataProvider.
     *
     * @param itemsPerPage number of items displayed per page
     * @param defaultSortField field name used when no explicit sort is set
     * @param defaultSortOrder sort direction applied with the default sort field
     * @param search DAO function used to fetch and count records
     * @param <T> a T class
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider} object
     */
    public static <T> TableDataProvider<T> initDataProvider(int itemsPerPage, String defaultSortField, SortOrder defaultSortOrder,
            DAOSearchFunction<T> search) {
        return new TableDataProvider<>(itemsPerPage, defaultSortOrder, new TableDataSource<T>() {

            private Optional<Long> numItems = Optional.empty();

            @Override
            public List<T> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder, Map<String, String> filters) {
                try {
                    String useSortField = sortField;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = defaultSortField;
                    }

                    return search.apply(first, pageSize, useSortField, sortOrder.asBoolean(), filters)
                            .stream()
                            .collect(Collectors.toList());
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numItems.isPresent()) {
                    try {
                        numItems = Optional.of(search.apply(0, Integer.MAX_VALUE, null, false, filters)
                                .stream()
                                .count());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of objects", e);
                    }
                }
                return numItems.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numItems = Optional.empty();
            }
        });
    }

    /**
     * Creates a new TableDataProvider instance.
     *
     * @param source data source that supplies and counts table entries
     */
    public TableDataProvider(TableDataSource<T> source) {
        this.source = source;
    }

    /**
     * Creates a new TableDataProvider instance.
     *
     * @param entriesPerPage the number of entries per page
     * @param sortOrder initial sort direction for the table
     * @param source data source that supplies and counts table entries
     */
    public TableDataProvider(int entriesPerPage, SortOrder sortOrder, TableDataSource<T> source) {
        this.source = source;
        this.sortOrder = sortOrder;
        this.entriesPerPage = entriesPerPage;
    }

    /**
     * getPaginatorList.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public List<T> getPaginatorList() throws TableDataSourceException {
        return loadList().orElse(Collections.emptyList());
    }

    /**
     * loadList.
     *
     * @return a {@link java.util.Optional} object.
     */
    protected Optional<List<T>> loadList() {
        String filterString = getFilterString(filters);
        if (!filterString.equals(this.lastFilterString)) {
            this.source.resetTotalNumberOfRecords();
            this.lastFilterString = filterString;
        }
        return Optional.ofNullable(this.source.getEntries(currentPage * entriesPerPage, entriesPerPage, sortField, sortOrder, getAsMap(filters)));
    }

    /**
     * @param filters list of table data filters to serialize
     * @return {@link String}
     */
    private static String getFilterString(List<TableDataFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        return filters.stream()
                .flatMap(filter -> filter.getColumns().stream().map(column -> column + "::" + filter.getValue()))
                .collect(Collectors.joining(";"));
    }

    /**
     * getFiltersAsMap.
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> getFiltersAsMap() {
        return getAsMap(getFilters());
    }

    private static Map<String, String> getAsMap(List<TableDataFilter> filters) {
        Map<String, String> map = new HashMap<>();
        for (TableDataFilter filter : filters) {
            for (String column : filter.getColumns()) {
                map.put(filter.getJoinTable().map(table -> table + "::").orElse("") + column, filter.getValue());
            }
        }
        return map;
    }

    /**
     * Called ony any changes to the currently listed objects noop - may be implemented by inheriting classes.
     */
    protected void resetCurrentList() {
        //
    }

    /**
     * sortBy.
     *
     * @param sortField field name to sort the table by
     * @param sortOrder sort direction name, parsed via SortOrder.valueOf
     */
    public void sortBy(String sortField, String sortOrder) {
        setSortField(sortField);
        setSortOrder(SortOrder.valueOf(sortOrder));
    }

    /**
     * sortBy.
     *
     * @param sortField field name to sort the table by
     * @param sortOrder ascending or descending sort direction to apply
     */
    public void sortBy(String sortField, SortOrder sortOrder) {
        setSortField(sortField);
        setSortOrder(sortOrder);
    }

    /**
     * cmdMoveFirst.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public String cmdMoveFirst() throws TableDataSourceException {
        if (this.currentPage != 0) {
            this.currentPage = 0;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    /**
     * cmdMovePrevious.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public String cmdMovePrevious() throws TableDataSourceException {
        if (!isFirstPage()) {
            this.currentPage--;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    /**
     * cmdMoveNext.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public String cmdMoveNext() throws TableDataSourceException {
        if (!isLastPage()) {
            this.currentPage++;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    /**
     * cmdMoveLast.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public String cmdMoveLast() throws TableDataSourceException {
        if (this.currentPage != getLastPageNumber()) {
            this.currentPage = getLastPageNumber();
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    /**
     * setTxtMoveTo.
     *
     * @param neueSeite 1-based target page number to navigate to
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public void setTxtMoveTo(int neueSeite) throws TableDataSourceException {
        if ((this.currentPage != neueSeite - 1) && neueSeite > 0 && neueSeite <= getLastPageNumber() + 1) {
            this.currentPage = neueSeite - 1;
            resetCurrentList();
            getPaginatorList();
        }
    }

    /**
     * getTxtMoveTo.
     *
     * @return a int.
     */
    public int getTxtMoveTo() {
        return this.currentPage + 1;
    }

    /**
     * getLastPageNumber.
     *
     * @return a int.
     */
    public int getLastPageNumber() {
        int ret = (int) Math.floor((double) getSizeOfDataList() / entriesPerPage);
        if (ret > 0 && getSizeOfDataList() % entriesPerPage == 0) {
            ret--;
        }
        return ret;
    }

    /**
     * isFirstPage.
     *
     * @return true if the current page is the first page of the data table, false otherwise
     */
    public boolean isFirstPage() {
        return this.currentPage == 0;
    }

    /**
     * isLastPage.
     *
     * @return true if the current page is the last page of the data table, false otherwise
     */
    public boolean isLastPage() {
        return this.currentPage >= getLastPageNumber();
    }

    /**
     * hasNextPage.
     *
     * @return true if there is a next page available in the data table, false otherwise
     */
    public boolean hasNextPage() {
        return this.currentPage == getLastPageNumber();
    }

    /**
     * hasPreviousPage.
     *
     * @return true if there is a previous page available in the data table, false otherwise
     */
    public boolean hasPreviousPage() {
        return this.currentPage > 0;
    }

    /**
     * getPageNumberCurrent.
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getPageNumberCurrent() {
        int totalPages = getLastPageNumber();
        if (this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }
        return Long.valueOf((long) this.currentPage + 1);
    }

    /**
     * getPageNumberLast.
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getPageNumberLast() {
        return Long.valueOf((long) getLastPageNumber() + 1);
    }

    /**
     * getSizeOfDataList.
     *
     * @return a long.
     */
    public long getSizeOfDataList() {
        return source.getTotalNumberOfRecords(getAsMap(getFilters()));
    }

    /**
     * Getter for the field <code>sortField</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Setter for the field <code>sortField</code>.
     *
     * @param sortField field name to sort the table by
     */
    public void setSortField(String sortField) {
        if (!this.sortField.equals(sortField)) {
            this.sortField = sortField;
        }
        resetCurrentList();
    }

    /**
     * Getter for the field <code>sortOrder</code>.
     *
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder} object.
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Setter for the field <code>sortOrder</code>.
     *
     * @param sortOrder ascending or descending sort direction to apply
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        resetCurrentList();
    }

    /**
     * Setter for the field <code>entriesPerPage</code>.
     *
     * @param entriesPerPage number of rows to display per page
     */
    public void setEntriesPerPage(int entriesPerPage) {
        this.entriesPerPage = entriesPerPage;
        resetCurrentList();
    }

    /**
     * Getter for the field <code>entriesPerPage</code>.
     *
     * @return a int.
     */
    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    /**
     * Getter for the field <code>filters</code>.
     *
     * @return a {@link java.util.List} object.
     */
    public List<TableDataFilter> getFilters() {
        return filters;
    }

    /**
     * removeFilter.
     *
     * @param filter filter instance to remove from the active filters
     */
    public void removeFilter(TableDataFilter filter) {
        this.filters.remove(filter);
        update();
    }

    /**
     * resetFilters.
     */
    public void resetFilters() {
        this.filters = new ArrayList<>();
    }

    /**
     * Method for filter objects being able to reset the number of records as soon as the filter value has changed.
     */
    void resetTotalNumberOfRecords() {
        source.resetTotalNumberOfRecords();
        resetCurrentList();
    }

    /**
     * resetAll.
     */
    public void resetAll() {
        currentPage = 0;
        sortField = "";
        sortOrder = SortOrder.ASCENDING;
        filters.forEach(filter -> filter.setValue(""));
        resetCurrentList();
        resetTotalNumberOfRecords();

    }

    /**
     * update.
     */
    public void update() {
        resetCurrentList();
        resetTotalNumberOfRecords();
    }

    /**
     * <p>getFilter.
     *
     * @param columns one or more column names the filter applies to
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataFilter} object
     */
    public TableDataFilter getFilter(String... columns) {
        return getFilterIfPresent(columns).orElseGet(() -> addFilter(columns));
    }

    private TableDataFilter addFilter(String... columns) {
        TableDataFilter filter = new TableDataFilter(this, columns);
        this.filters.add(filter);
        return filter;
    }

    /**
     * <p>addFilter.
     *
     * @param filter pre-built filter instance to append to the active filters
     */
    public void addFilter(TableDataFilter filter) {
        this.filters.add(filter);

    }

    /**
     * <p>getFilterIfPresent.
     *
     * @param columns one or more column names to match against existing filters
     * @return a {@link java.util.Optional} object
     */
    public Optional<TableDataFilter> getFilterIfPresent(String... columns) {
        for (TableDataFilter filter : this.getFilters()) {
            if (CollectionUtils.isEqualCollection(Arrays.asList(columns), filter.getColumns())) {
                return Optional.of(filter);
            }
        }
        return Optional.empty();
    }

}
