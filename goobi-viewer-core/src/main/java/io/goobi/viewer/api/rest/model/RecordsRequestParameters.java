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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST request parameters for RecordsResource.
 */
@Schema(name = "SolrRequestParameters", description = "SOLR query and additional parameters", requiredProperties = { "query" })
// Ignore unknown JSON properties so schemathesis-generated requests with extra fields are accepted
// instead of causing Jackson to return HTTP 400 with "Unrecognized field" error.
@JsonIgnoreProperties(ignoreUnknown = true)
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
    // Maximum is 2^31-1 because the field is stored as Java int; larger values cause Jackson to throw
    // a deserialization error which results in HTTP 400 instead of a meaningful response.
    @Schema(description = "The absolute index of the first result to return", example = "0",
            minimum = "0", maximum = "2147483647")
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

    
    public String getQuery() {
        return query;
    }

    
    public void setQuery(String query) {
        this.query = query;
    }

    
    public List<String> getResultFields() {
        return resultFields;
    }

    
    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }

    
    public List<String> getSortFields() {
        return sortFields;
    }

    
    public void setSortFields(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    
    public String getSortOrder() {
        return sortOrder;
    }

    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    
    public String getJsonFormat() {
        return jsonFormat;
    }

    
    public void setJsonFormat(String jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    
    public int getCount() {
        return count;
    }

    
    public void setCount(int count) {
        this.count = count;
    }

    
    public int getOffset() {
        return offset;
    }

    
    public void setOffset(int offset) {
        this.offset = offset;
    }

    
    public boolean isRandomize() {
        return randomize;
    }

    
    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    
    public String getLanguage() {
        return language;
    }

    
    public void setLanguage(String language) {
        this.language = language;
    }

    
    public boolean isIncludeChildHits() {
        return includeChildHits;
    }

    
    public void setIncludeChildHits(boolean includeChildHits) {
        this.includeChildHits = includeChildHits;
    }

    
    public boolean isBoostTopLevelDocstructs() {
        return boostTopLevelDocstructs;
    }

    
    public void setBoostTopLevelDocstructs(boolean boostTopLevelDocstructs) {
        this.boostTopLevelDocstructs = boostTopLevelDocstructs;
    }

    
    public List<String> getFacetFields() {
        return facetFields;
    }

    
    public void setFacetFields(List<String> facetFields) {
        this.facetFields = facetFields;
    }
}
