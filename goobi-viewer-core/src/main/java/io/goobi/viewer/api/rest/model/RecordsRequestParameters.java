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
package io.goobi.viewer.api.rest.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST request parameters for RecordsResource.
 */
@Schema(name = "SolrRequestParameters", description = "SOLR query and additional parameters", requiredProperties = { "query" })
public class RecordsRequestParameters {

    @Schema(description = "Raw SOLR query", example = "+ISWORK:true +DOCSTRCT:monograph +(FACET_PLACEPUBLISH:Berlin FACET_PLACEPUBLISH:'New York')")
    private String query;
    @Schema(description = "A string list of SOLR field names which should be included in the response, allows wildcards",
            example = "[\"PI*\",\"IDDOC\",\"DOCTYPE\",\"DOCSTRCT\",\"LABEL\"]")
    private List<String> resultFields = new ArrayList<>();
    @Schema(description = "A string list of SOLR fields used for sorting", example = "[\"SORTNUM_YEAR\",\"LABEL\"]")
    private List<String> sortFields = new ArrayList<>();
    @Schema(description = "If this has the value 'desc', the results will be sorted by the given sortFields in descending order, otherwise ascending",
            example = "asc")
    private String sortOrder = "asc";
    @Schema(description = "If this has the value 'datecentric', the results will be grouped by their import date in their JSON representation",
            example = "recordcentric")
    private String jsonFormat = "";
    @Schema(description = "The maximum number of results to return. Negative values don't set a limit", example = "10")
    private int count = -1;
    @Schema(description = "The absolute index of the first result to return", example = "0")
    private int offset = 0;
    @Schema(description = "Set to 'true' to randomize all results. If used in conjuction with sortFields,"
            + " randomization only applies to results with identical values in the sortFields",
            example = "false")
    private boolean randomize = false;
    @Schema(description = "If this field is set, all SOLR field names and values will be translated into this language if possible."
            + " If no language parameter is given, no fields will be translated",
            example = "en")
    private String language = "";
    @Schema(description = "Set to 'true' to include all child documents (sections, pages) that match the query."
            + " Child documents are appended in the 'children' property",
            example = "false")
    private boolean includeChildHits = false;
    @Schema(description = "Set to 'true' to place main record that contain the search terms in the title on top", example = "false")
    private boolean boostTopLevelDocstructs = false;
    @Schema(description = "A list of SOLR field names to get facet results for", example = "[\"DC\",\"DOCSTRCT\"]")
    private List<String> facetFields = new ArrayList<>();

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the resultFields
     */
    public List<String> getResultFields() {
        return resultFields;
    }

    /**
     * @param resultFields the resultFields to set
     */
    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }

    /**
     * @return the sortFields
     */
    public List<String> getSortFields() {
        return sortFields;
    }

    /**
     * @param sortFields the sortFields to set
     */
    public void setSortFields(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    /**
     * @return the sortOrder
     */
    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * @param sortOrder the sortOrder to set
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * @return the jsonFormat
     */
    public String getJsonFormat() {
        return jsonFormat;
    }

    /**
     * @param jsonFormat the jsonFormat to set
     */
    public void setJsonFormat(String jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return the randomize
     */
    public boolean isRandomize() {
        return randomize;
    }

    /**
     * @param randomize the randomize to set
     */
    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the includeChildHits
     */
    public boolean isIncludeChildHits() {
        return includeChildHits;
    }

    /**
     * @param includeChildHits the includeChildHits to set
     */
    public void setIncludeChildHits(boolean includeChildHits) {
        this.includeChildHits = includeChildHits;
    }

    /**
     * @return the boostTopLevelDocstructs
     */
    public boolean isBoostTopLevelDocstructs() {
        return boostTopLevelDocstructs;
    }

    /**
     * @param boostTopLevelDocstructs the boostTopLevelDocstructs to set
     */
    public void setBoostTopLevelDocstructs(boolean boostTopLevelDocstructs) {
        this.boostTopLevelDocstructs = boostTopLevelDocstructs;
    }

    /**
     * @return the facetFields
     */
    public List<String> getFacetFields() {
        return facetFields;
    }

    /**
     * @param facetFields the facetFields to set
     */
    public void setFacetFields(List<String> facetFields) {
        this.facetFields = facetFields;
    }
}
