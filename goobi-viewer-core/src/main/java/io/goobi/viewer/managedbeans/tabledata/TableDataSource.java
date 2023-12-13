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
 * <p>
 * TableDataSource interface.
 * </p>
 * 
 * @param <T>
 */
public interface TableDataSource<T> {

    /**
     * <p>
     * getEntries.
     * </p>
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param sortOrder a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder} object.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.managedbeans.tabledata.TableDataSourceException if any.
     */
    public List<T> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters)
            throws TableDataSourceException;

    /**
     * <p>
     * getTotalNumberOfRecords.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     */
    long getTotalNumberOfRecords(Map<String, String> filters);

    /**
     * <p>
     * resetTotalNumberOfRecords.
     * </p>
     */
    void resetTotalNumberOfRecords();

}
