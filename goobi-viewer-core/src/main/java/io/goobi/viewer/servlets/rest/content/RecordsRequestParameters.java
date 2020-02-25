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
package io.goobi.viewer.servlets.rest.content;

/**
 * POST request parameters for RecordsResource.
 */
public class RecordsRequestParameters {

    private String query;
    private String sortFields;
    private String sortOrder;
    private String jsonFormat;
    private int count;
    private int offset;
    private boolean randomize;

    /**
     * <p>
     * Getter for the field <code>query</code>.
     * </p>
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * <p>
     * Setter for the field <code>query</code>.
     * </p>
     *
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * <p>
     * Getter for the field <code>sortFields</code>.
     * </p>
     *
     * @return the sortFields
     */
    public String getSortFields() {
        return sortFields;
    }

    /**
     * <p>
     * Setter for the field <code>sortFields</code>.
     * </p>
     *
     * @param sortFields the sortFields to set
     */
    public void setSortFields(String sortFields) {
        this.sortFields = sortFields;
    }

    /**
     * <p>
     * Getter for the field <code>sortOrder</code>.
     * </p>
     *
     * @return the sortOrder
     */
    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * <p>
     * Setter for the field <code>sortOrder</code>.
     * </p>
     *
     * @param sortOrder the sortOrder to set
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * <p>
     * Getter for the field <code>jsonFormat</code>.
     * </p>
     *
     * @return the jsonFormat
     */
    public String getJsonFormat() {
        return jsonFormat;
    }

    /**
     * <p>
     * Setter for the field <code>jsonFormat</code>.
     * </p>
     *
     * @param jsonFormat the jsonFormat to set
     */
    public void setJsonFormat(String jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    /**
     * <p>
     * Getter for the field <code>count</code>.
     * </p>
     *
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * <p>
     * Setter for the field <code>count</code>.
     * </p>
     *
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * <p>
     * Getter for the field <code>offset</code>.
     * </p>
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * <p>
     * Setter for the field <code>offset</code>.
     * </p>
     *
     * @param offset the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * <p>
     * isRandomize.
     * </p>
     *
     * @return the randomize
     */
    public boolean isRandomize() {
        return randomize;
    }

    /**
     * <p>
     * Setter for the field <code>randomize</code>.
     * </p>
     *
     * @param randomize the randomize to set
     */
    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }
}
