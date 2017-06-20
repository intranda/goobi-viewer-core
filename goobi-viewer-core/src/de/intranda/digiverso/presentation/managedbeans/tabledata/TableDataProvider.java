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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;

public class TableDataProvider<T> {

    private static final Logger logger = LoggerFactory.getLogger(TableDataProvider.class);

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

    public List<T> getPaginatorList() throws DAOException {
        List<T> ret = source.getEntries(currentPage * entriesPerPage, entriesPerPage, sortField, sortOrder, getAsMap(filters));
        return ret;
    }

    public Map<String, String> getFiltersAsMap() {
        return getAsMap(getFilters());
    }

    private static Map<String, String> getAsMap(List<TableDataFilter> filters) {
        Map<String, String> map = new HashMap<>();
        for (TableDataFilter filter : filters) {
            map.put(filter.getColumn(), filter.getValue());
        }
        return map;
    }

    public void sortBy(String sortField, String sortOrder) {
        logger.trace("sortBy: {} {}", sortField, sortOrder);
        setSortField(sortField);
        setSortOrder(SortOrder.valueOf(sortOrder));
    }

    public void sortBy(String sortField, SortOrder sortOrder) {
        logger.trace("sortBy: {} {}", sortField, sortOrder);
        setSortField(sortField);
        setSortOrder(sortOrder);
    }

    public String cmdMoveFirst() throws DAOException {
        if (this.currentPage != 0) {
            this.currentPage = 0;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMovePrevious() throws DAOException {
        if (!isFirstPage()) {
            this.currentPage--;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveNext() throws DAOException {
        if (!isLastPage()) {
            this.currentPage++;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveLast() throws DAOException {
        if (this.currentPage != getLastPageNumber()) {
            this.currentPage = getLastPageNumber();
            getPaginatorList();
        }
        return "";
    }

    public void setTxtMoveTo(int neueSeite) throws DAOException {
        if ((this.currentPage != neueSeite - 1) && neueSeite > 0 && neueSeite <= getLastPageNumber() + 1) {
            this.currentPage = neueSeite - 1;
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
        return source.getTotalNumberOfRecords();
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setEntriesPerPage(int entriesPerPage) {
        this.entriesPerPage = entriesPerPage;
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public List<TableDataFilter> getFilters() {
        return filters;
    }

    public void addFilter(TableDataFilter filter) {
        this.filters.add(filter);
    }

    public boolean addFilter(String column) {
        if (getFilter(column) == null) {
            addFilter(new TableDataFilter(column, ""));
            return true;
        }

        return false;
    }

    public TableDataFilter getFilter(String column) {
        for (TableDataFilter filter : filters) {
            if (filter.getColumn().equalsIgnoreCase(column)) {
                return filter;
            }
        }
        return null;
    }

    public void removeFilter(TableDataFilter filter) {
        this.filters.remove(filter);
    }

    public void removeFilter(String column) {
        TableDataFilter toRemove = getFilter(column);
        if (toRemove != null) {
            removeFilter(toRemove);
        }
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

}
