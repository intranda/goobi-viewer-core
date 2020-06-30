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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST request parameters for RecordsResource.
 */
@Schema(name="SOLR request parameters", description="SOLR query and additional parameters", requiredProperties= {"query"})
public class RecordsRequestParameters {

    private String query;
    private List<String> resultFields = new ArrayList<>();
    private List<String> sortFields = new ArrayList<>();
    private String sortOrder = "asc";
    private String jsonFormat = "";
    private int count = 0;
    private int offset = 0;
    private boolean randomize = false;
    private String translationLanguage = "";

    /**
     * <p>
     * Getter for the field <code>query</code>.
     * </p>
     *
     * @return the query
     */
    @Schema(description = "Raw SOLR query", example="ISWORK:true")
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
    @Schema(description = "A string list of SOLR fields used for sorting", example="[\"SORTNUM_YEAR\",\"LABEL\"]")
    public List<String> getSortFields() {
        return sortFields;
    }

    /**
     * <p>
     * Setter for the field <code>sortFields</code>.
     * </p>
     *
     * @param sortFields the sortFields to set
     */
    public void setSortFields(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    /**
     * <p>
     * Getter for the field <code>sortOrder</code>.
     * </p>
     *
     * @return the sortOrder
     */
    @Schema(description = "If this has the value 'desc', the results will be sorted by the given sortFields in descending order, otherwise ascending", example="asc")
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
    @Schema(description = "If this has the value 'datecentric', the results will bedelivered in a date centric format", example="recordcentric")
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
    @Schema(description = "The maximum number of results to return", example="10")
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
    @Schema(description = "The absolute index of the first result to return", example="0")
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
    @Schema(description = "Set to 'true' to randomize all results. If used in conjuction with sortFields, randomization only applies to results with identical values in the sortFields", example="false")
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
    
    /**
     * @return the translationLanguage
     */
    @Schema(description = "If this field is set, all SOLR field names and values will be translated into this language, if possible", example="en")
    public String getTranslationLanguage() {
        return translationLanguage;
    }
    
    /**
     * @param translationLanguage the translationLanguage to set
     */
    public void setTranslationLanguage(String translationLanguage) {
        this.translationLanguage = translationLanguage;
    }
    
    /**
     * @return the resultFields
     */
    @Schema(description = "A string list of SOLR field names which should be included in the response, allows wildcards", example="[\"PI*\",\"IDDOC\",\"DOCTYPE\",\"DOCSTRCT\",\"LABEL\"]")
    public List<String> getResultFields() {
        return resultFields;
    }
    
    /**
     * @param resultFields the resultFields to set
     */
    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }
}
