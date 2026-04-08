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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a single filter criterion applied to a data table column.
 */
public class TableDataFilter implements Serializable {

    private static final long serialVersionUID = 9120268561536080056L;

    private String joinTable;
    private final List<String> columns;
    private String value;
    private final TableDataProvider<?> owner;

    /**
     * Creates a new TableDataFilter instance.
     *
     * @param owner data provider notified when the filter value changes
     * @param columns database column names this filter applies to
     */
    public TableDataFilter(TableDataProvider<?> owner, String... columns) {
        this.columns = Arrays.asList(columns);
        this.value = "";
        this.owner = owner;
        this.joinTable = null;
    }

    /**
     * <p>Constructor for TableDataFilter.
     *
     * @param columns database column names this filter applies to
     */
    public TableDataFilter(String... columns) {
        this(null, columns);
    }

    /**
     * Getter for the field <code>column</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * <p>getName.
     *
     * @return a {@link java.lang.String} object
     */
    public String getName() {
        return this.columns.stream().collect(Collectors.joining());
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value filter string to match against the configured columns
     */
    public void setValue(String value) {
        this.value = value;
        // Reset number of records as soon as the value has changed so that the paginator etc. can be updated on time
        if (owner != null) {
            owner.resetTotalNumberOfRecords();
        }
    }

    /**
     * Getter for the field <code>joinTable</code>.
     *
     * @return an Optional containing the name of the table to join when filtering, or empty if no join is required
     */
    public Optional<String> getJoinTable() {
        return Optional.ofNullable(joinTable);
    }

    /**
     * <p>Setter for the field <code>joinTable</code>.
     *
     * @param joinTable name of the table to join when filtering
     */
    public void setJoinTable(String joinTable) {
        this.joinTable = joinTable;
    }

}
