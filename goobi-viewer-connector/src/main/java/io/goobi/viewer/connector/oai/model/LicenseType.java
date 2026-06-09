/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai.model;

import java.time.LocalDate;

/**
 * <p>
 * LicenseType class.
 * </p>
 *
 */
public class LicenseType {

    private final String field;
    private final String value;
    private final String conditions;

    /**
     * <p>
     * Constructor for LicenseType.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param conditions a {@link java.lang.String} object.
     */
    public LicenseType(String field, String value, String conditions) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }
        this.field = field;
        this.value = value;
        this.conditions = conditions;
    }

    /**
     * <p>
     * Getter for the field <code>field</code>.
     * </p>
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Getter for the field <code>conditions</code>.
     * </p>
     *
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }

    /**
     * <p>
     * getProcessedConditions.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getProcessedConditions() {
        String conditions = this.conditions;
        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(LocalDate.now().getYear()));

        }

        return conditions;
    }

}
