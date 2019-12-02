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
import java.util.Optional;

public class TableDataFilter implements Serializable {

    private static final long serialVersionUID = 9120268561536080056L;
    
    private String joinTable = null;
    private String column;
    private String value;
    private final TableDataProvider<?> owner;

    public TableDataFilter(String column, String value, TableDataProvider<?> owner) {
        super();
        this.column = column;
        this.value = value;
        this.owner = owner;
    }

    public TableDataFilter(String joinTable, String column, String value, TableDataProvider<?> owner) {
        super();
        this.joinTable = joinTable;
        this.column = column;
        this.value = value;
        this.owner = owner;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        // Reset number of records as soon as the value has changed so that the paginator etc. can be updated on time
        owner.resetTotalNumberOfRecords();
    }

    /**
     * @param joinTable the joinTable to set
     */
    public void setJoinTable(String joinTable) {
        this.joinTable = joinTable;
    }

    /**
     * @return the joinTable
     */
    public Optional<String> getJoinTable() {
        return Optional.ofNullable(joinTable);
    }
}
