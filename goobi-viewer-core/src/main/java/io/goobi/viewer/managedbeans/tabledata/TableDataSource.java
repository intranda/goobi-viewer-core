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

import java.util.List;
import java.util.Map;

import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;

/**
 * Data-source interface for paginated, sortable, and filterable table data used by {@link TableDataProvider}.
 * Implementors supply a page of entries for a given offset and page size as well as the total record count,
 * both taking an arbitrary map of filter criteria into account.
 *
 * @param <T> the type of entities returned by this data source
 */
public interface TableDataSource<T> {

    /**
     * getEntries.
     *
     * @param first zero-based index of the first result to return.
     * @param pageSize maximum number of results to return.
     * @param sortField field name to sort by; null for default order.
     * @param sortOrder ascending or descending sort direction.
     * @param filters map of field names to filter values.
     * @return a list of entries for the requested page, sorted and filtered according to the given parameters
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public List<T> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters)
            throws TableDataSourceException;

    /**
     * getTotalNumberOfRecords.
     *
     * @param filters map of field names to filter values applied to the count.
     * @return a long.
     */
    long getTotalNumberOfRecords(Map<String, String> filters);

    /**
     * resetTotalNumberOfRecords.
     */
    void resetTotalNumberOfRecords();

}
