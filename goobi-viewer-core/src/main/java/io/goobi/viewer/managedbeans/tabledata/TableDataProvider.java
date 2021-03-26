/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans.tabledata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * TableDataProvider class.
 * </p>
 */
public class TableDataProvider<T> implements Serializable {

    private static final long serialVersionUID = 6109453168491579420L;

    private static final Logger logger = LoggerFactory.getLogger(TableDataProvider.class);

    private int currentPage = 0;
    private int entriesPerPage;
    private TableDataSource<T> source;
    private String sortField = "";
    private SortOrder sortOrder = SortOrder.ASCENDING;
    private List<TableDataFilter> filters = new ArrayList<>();
    String lastFilterString = "";

    public static enum SortOrder {
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
     * <p>
     * Constructor for TableDataProvider.
     * </p>
     *
     * @param source a {@link io.goobi.viewer.managedbeans.tabledata.TableDataSource} object.
     */
    public TableDataProvider(TableDataSource<T> source) {
        this.source = source;
    }

    /**
     * <p>
     * getPaginatorList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public List<T> getPaginatorList() throws TableDataSourceException {
        return loadList().orElse(Collections.emptyList());
    }

    /**
     * <p>
     * loadList.
     * </p>
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
     * @param filters2
     * @return
     */
    private static String getFilterString(List<TableDataFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        return filters.stream().map(filter -> filter.getColumn() + "::" + filter.getValue()).collect(Collectors.joining(";"));
    }

    /**
     * <p>
     * getFiltersAsMap.
     * </p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> getFiltersAsMap() {
        return getAsMap(getFilters());
    }

    private static Map<String, String> getAsMap(List<TableDataFilter> filters) {
        Map<String, String> map = new HashMap<>();
        for (TableDataFilter filter : filters) {
            map.put(filter.getJoinTable().map(table -> table + "::").orElse("") + filter.getColumn(), filter.getValue());
        }
        return map;
    }

    /**
     * Called ony any changes to the currently listed objects noop - may be implemented by inheriting classes
     */
    protected void resetCurrentList() {
    }

    /**
     * <p>
     * sortBy.
     * </p>
     *
     * @param sortField a {@link java.lang.String} object.
     * @param sortOrder a {@link java.lang.String} object.
     */
    public void sortBy(String sortField, String sortOrder) {
        setSortField(sortField);
        setSortOrder(SortOrder.valueOf(sortOrder));
    }

    /**
     * <p>
     * sortBy.
     * </p>
     *
     * @param sortField a {@link java.lang.String} object.
     * @param sortOrder a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder} object.
     */
    public void sortBy(String sortField, SortOrder sortOrder) {
        setSortField(sortField);
        setSortOrder(sortOrder);
    }

    /**
     * <p>
     * cmdMoveFirst.
     * </p>
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
     * <p>
     * cmdMovePrevious.
     * </p>
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
     * <p>
     * cmdMoveNext.
     * </p>
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
     * <p>
     * cmdMoveLast.
     * </p>
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
     * <p>
     * setTxtMoveTo.
     * </p>
     *
     * @param neueSeite a int.
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
     * <p>
     * getTxtMoveTo.
     * </p>
     *
     * @return a int.
     */
    public int getTxtMoveTo() {
        return this.currentPage + 1;
    }

    /**
     * <p>
     * getLastPageNumber.
     * </p>
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
     * <p>
     * isFirstPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isFirstPage() {
        return this.currentPage == 0;
    }

    /**
     * <p>
     * isLastPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLastPage() {
        return this.currentPage >= getLastPageNumber();
    }

    /**
     * <p>
     * hasNextPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasNextPage() {
        return this.currentPage == getLastPageNumber();
    }

    /**
     * <p>
     * hasPreviousPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPreviousPage() {
        return this.currentPage > 0;
    }

    /**
     * <p>
     * getPageNumberCurrent.
     * </p>
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
     * <p>
     * getPageNumberLast.
     * </p>
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getPageNumberLast() {
        return Long.valueOf((long) getLastPageNumber() + 1);
    }

    /**
     * <p>
     * getSizeOfDataList.
     * </p>
     *
     * @return a long.
     */
    public long getSizeOfDataList() {
        return source.getTotalNumberOfRecords(getAsMap(getFilters()));
    }

    /**
     * <p>
     * Getter for the field <code>sortField</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * <p>
     * Setter for the field <code>sortField</code>.
     * </p>
     *
     * @param sortField a {@link java.lang.String} object.
     */
    public void setSortField(String sortField) {
        if (!this.sortField.equals(sortField))
            this.sortField = sortField;
        resetCurrentList();
    }

    /**
     * <p>
     * Getter for the field <code>sortOrder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder} object.
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * <p>
     * Setter for the field <code>sortOrder</code>.
     * </p>
     *
     * @param sortOrder a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder} object.
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        resetCurrentList();
    }

    /**
     * <p>
     * Setter for the field <code>entriesPerPage</code>.
     * </p>
     *
     * @param entriesPerPage a int.
     */
    public void setEntriesPerPage(int entriesPerPage) {
        this.entriesPerPage = entriesPerPage;
        resetCurrentList();
    }

    /**
     * <p>
     * Getter for the field <code>entriesPerPage</code>.
     * </p>
     *
     * @return a int.
     */
    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    /**
     * <p>
     * Getter for the field <code>filters</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<TableDataFilter> getFilters() {
        return filters;
    }

    /**
     * <p>
     * addFilter.
     * </p>
     *
     * @param filter a {@link io.goobi.viewer.managedbeans.tabledata.TableDataFilter} object.
     */
    public void addFilter(TableDataFilter filter) {
        this.filters.add(filter);
        resetCurrentList();
    }

    /**
     * <p>
     * addFilter.
     * </p>
     *
     * @param joinTable a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean addFilter(String joinTable, String column) {
        if (!getFilterAsOptional(joinTable, column).isPresent()) {
            addFilter(new TableDataFilter(joinTable, column, "", this));
            return true;
        }

        return false;
    }

    /**
     * <p>
     * addFilter.
     * </p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean addFilter(String column) {
        if (!getFilterAsOptional(column).isPresent()) {
            addFilter(new TableDataFilter(column, "", this));
            return true;
        }

        return false;
    }

    /**
     * <p>
     * getFilterAsOptional.
     * </p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<TableDataFilter> getFilterAsOptional(String column) {
        for (TableDataFilter filter : filters) {
            if (filter.getColumn().equalsIgnoreCase(column) && !filter.getJoinTable().isPresent()) {
                return Optional.of(filter);
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getFilterAsOptional.
     * </p>
     *
     * @param joinTable a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<TableDataFilter> getFilterAsOptional(String joinTable, String column) {
        for (TableDataFilter filter : filters) {
            if (filter.getColumn().equalsIgnoreCase(column) && filter.getJoinTable().equals(Optional.ofNullable(joinTable))) {
                return Optional.of(filter);
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getFilter.
     * </p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataFilter} object.
     */
    public TableDataFilter getFilter(String column) {
        return getFilterAsOptional(column).orElse(null);
    }

    /**
     * <p>
     * getFilter.
     * </p>
     *
     * @param joinTable a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataFilter} object.
     */
    public TableDataFilter getFilter(String joinTable, String column) {
        return getFilterAsOptional(joinTable, column).orElse(null);
    }

    /**
     * <p>
     * removeFilter.
     * </p>
     *
     * @param filter a {@link io.goobi.viewer.managedbeans.tabledata.TableDataFilter} object.
     */
    public void removeFilter(TableDataFilter filter) {
        this.filters.remove(filter);
        update();
    }

    /**
     * <p>
     * removeFilter.
     * </p>
     *
     * @param column a {@link java.lang.String} object.
     */
    public void removeFilter(String column) {
        getFilterAsOptional(column).ifPresent(filter -> removeFilter(filter));
    }

    /**
     * <p>
     * removeFilter.
     * </p>
     *
     * @param joinTable a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     */
    public void removeFilter(String joinTable, String column) {
        getFilterAsOptional(joinTable, column).ifPresent(filter -> removeFilter(filter));
    }

    /**
     * <p>
     * resetFilters.
     * </p>
     */
    public void resetFilters() {
        this.filters = new ArrayList<>();
    }

    /**
     * <p>
     * Setter for the field <code>filters</code>.
     * </p>
     *
     * @param columns a {@link java.lang.String} object.
     */
    public void setFilters(String... columns) {
        resetFilters();
        for (String column : columns) {
            addFilter(column);
        }
    }

    /**
     * Method for filter objects being able to reset the number of records as soon as the filter value has changed.
     */
    void resetTotalNumberOfRecords() {
        source.resetTotalNumberOfRecords();
        resetCurrentList();
    }

    /**
     * <p>
     * resetAll.
     * </p>
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
     * <p>
     * update.
     * </p>
     */
    public void update() {
        resetCurrentList();
        resetTotalNumberOfRecords();
    }

}
