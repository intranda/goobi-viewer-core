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
@Schema(name="SolrRequestParameters", description="SOLR query and additional parameters", requiredProperties= {"query"})
public class RecordsRequestParameters {

    @Schema(description = "Raw SOLR query", example="ISWORK:true")
    public String query;
    @Schema(description = "A string list of SOLR field names which should be included in the response, allows wildcards", example="[\"PI*\",\"IDDOC\",\"DOCTYPE\",\"DOCSTRCT\",\"LABEL\"]")
    public List<String> resultFields = new ArrayList<>();
    @Schema(description = "A string list of SOLR fields used for sorting", example="[\"SORTNUM_YEAR\",\"LABEL\"]")
    public List<String> sortFields = new ArrayList<>();
    @Schema(description = "If this has the value 'desc', the results will be sorted by the given sortFields in descending order, otherwise ascending", example="asc")
    public String sortOrder = "asc";
    @Schema(description = "If this has the value 'datecentric', the results will bedelivered in a date centric format", example="recordcentric")
    public String jsonFormat = "";
    @Schema(description = "The maximum number of results to return", example="10")
    public int count = 0;
    @Schema(description = "The absolute index of the first result to return", example="0")
    public int offset = 0;
    @Schema(description = "Set to 'true' to randomize all results. If used in conjuction with sortFields, randomization only applies to results with identical values in the sortFields", example="false")
    public boolean randomize = false;
    @Schema(description = "If this field is set, all SOLR field names and values will be translated into this language, if possible", example="en")
    public String language = "";
    @Schema(description = "Set to 'true' to include all child documents (sections, pages) that match the query. Child documents are appended in the 'children' property", example="false")
    public boolean includeChildHits = false;

}
