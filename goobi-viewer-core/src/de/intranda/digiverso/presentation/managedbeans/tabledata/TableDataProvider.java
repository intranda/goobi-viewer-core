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
package de.intranda.digiverso.presentation.managedbeans.tabledata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;

public class TableDataProvider<T> {

    private int currentPage = 0;
    private int entriesPerPage;
    private TableDataSource<T> source;
    private String sortField = "";
    private SortOrder sortOrder = SortOrder.ASCENDING;
    private List<TableDataFilter> filters = new ArrayList<>();

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

    public TableDataProvider(TableDataSource<T> source) {
        this.source = source;
    }

    public List<T> getPaginatorList() throws TableDataSourceException {
        return loadList().orElse(Collections.EMPTY_LIST);
    }

    /**
     * 
     */
    protected Optional<List<T>> loadList() {
        return Optional
                .ofNullable(this.source.getEntries(currentPage * entriesPerPage, entriesPerPage, sortField, sortOrder, getAsMap(filters)));
    }

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
     * Called ony any changes to the currently listed objects
     * noop - may be implemented by inheriting classes
     */
    protected void resetCurrentList() {
    }

    public void sortBy(String sortField, String sortOrder) {
        setSortField(sortField);
        setSortOrder(SortOrder.valueOf(sortOrder));
    }

    public void sortBy(String sortField, SortOrder sortOrder) {
        setSortField(sortField);
        setSortOrder(sortOrder);
    }

    public String cmdMoveFirst() throws TableDataSourceException {
        if (this.currentPage != 0) {
            this.currentPage = 0;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    public String cmdMovePrevious() throws TableDataSourceException {
        if (!isFirstPage()) {
            this.currentPage--;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveNext() throws TableDataSourceException {
        if (!isLastPage()) {
            this.currentPage++;
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveLast() throws TableDataSourceException {
        if (this.currentPage != getLastPageNumber()) {
            this.currentPage = getLastPageNumber();
            resetCurrentList();
            getPaginatorList();
        }
        return "";
    }

    public void setTxtMoveTo(int neueSeite) throws TableDataSourceException {
        if ((this.currentPage != neueSeite - 1) && neueSeite > 0 && neueSeite <= getLastPageNumber() + 1) {
            this.currentPage = neueSeite - 1;
            resetCurrentList();
            getPaginatorList();
        }
    }

    public int getTxtMoveTo() {
        return this.currentPage + 1;
    }

    public int getLastPageNumber() {
        int ret = new Double(Math.floor(getSizeOfDataList() / entriesPerPage)).intValue();
        if (ret > 0 && getSizeOfDataList() % entriesPerPage == 0) {
            ret--;
        }
        return ret;
    }

    public boolean isFirstPage() {
        return this.currentPage == 0;
    }

    public boolean isLastPage() {
        return this.currentPage >= getLastPageNumber();
    }

    public boolean hasNextPage() {
        return this.currentPage == getLastPageNumber();
    }

    public boolean hasPreviousPage() {
        return this.currentPage > 0;
    }

    public Long getPageNumberCurrent() {
        int totalPages = getLastPageNumber();
        if (this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }
        return Long.valueOf(this.currentPage + 1);
    }

    public Long getPageNumberLast() {
        return Long.valueOf(getLastPageNumber() + 1);
    }

    public long getSizeOfDataList() {
        return source.getTotalNumberOfRecords(getAsMap(getFilters()));
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        if (!this.sortField.equals(sortField))
            this.sortField = sortField;
        resetCurrentList();
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        resetCurrentList();
    }

    public void setEntriesPerPage(int entriesPerPage) {
        this.entriesPerPage = entriesPerPage;
        resetCurrentList();
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public List<TableDataFilter> getFilters() {
        return filters;
    }

    public void addFilter(TableDataFilter filter) {
        this.filters.add(filter);
        resetCurrentList();
    }

    public boolean addFilter(String joinTable, String column) {
        if (!getFilterAsOptional(joinTable, column).isPresent()) {
            addFilter(new TableDataFilter(joinTable, column, ""));
            return true;
        }

        return false;
    }
    
    public boolean addFilter(String column) {
        if (!getFilterAsOptional(column).isPresent()) {
            addFilter(new TableDataFilter(column, ""));
            return true;
        }

        return false;
    }

    public Optional<TableDataFilter> getFilterAsOptional(String column) {
        for (TableDataFilter filter : filters) {
            if (filter.getColumn()
                    .equalsIgnoreCase(column) && !filter.getJoinTable().isPresent()) {
                return Optional.of(filter);
            }
        }
        return Optional.empty();
    }
    
    public Optional<TableDataFilter> getFilterAsOptional(String joinTable, String column) {
        for (TableDataFilter filter : filters) {
            if (filter.getColumn()
                    .equalsIgnoreCase(column) && filter.getJoinTable().equals(Optional.ofNullable(joinTable))) {
                return Optional.of(filter);
            }
        }
        return Optional.empty();
    }
    
    public TableDataFilter getFilter(String column) {
        return getFilterAsOptional(column).orElse(null);
    }
    
    public TableDataFilter getFilter(String joinTable, String column) {
        return getFilterAsOptional(joinTable, column).orElse(null);
    }

    public void removeFilter(TableDataFilter filter) {
        this.filters.remove(filter);
        resetCurrentList();
    }

    public void removeFilter(String column) {
        getFilterAsOptional(column).ifPresent(filter -> removeFilter(filter));
    }
    
    public void removeFilter(String joinTable, String column) {
        getFilterAsOptional(joinTable, column).ifPresent(filter -> removeFilter(filter));
    }

    public void resetFilters() {
        this.filters = new ArrayList<>();
    }

    public void setFilters(String... columns) {
        resetFilters();
        for (String column : columns) {
            addFilter(column);
        }
    }

    /**
     * 
     */
    public void resetAll() {
        currentPage = 0;
        sortField = "";
        sortOrder = SortOrder.ASCENDING;
        filters.forEach(filter -> filter.setValue(""));
        resetCurrentList();
        source.resetTotalNumberOfRecords();
        
        
    }

    /**
     * 
     */
    public void update() {
        resetCurrentList();
        source.resetTotalNumberOfRecords();
    }

}
