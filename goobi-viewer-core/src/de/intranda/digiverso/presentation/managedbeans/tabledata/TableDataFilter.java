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

import java.util.Optional;

public class TableDataFilter {
    
    private String joinTable = null;
    private String column;
    private String value;
    
    public TableDataFilter(String column, String value) {
        super();
        this.column = column;
        this.value = value;
    }
    public TableDataFilter(String joinTable, String column, String value) {
        super();
        this.joinTable = joinTable;
        this.column = column;
        this.value = value;
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
