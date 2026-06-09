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
package io.goobi.viewer.api.rest.v1.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST endpoint for quick filter facet values with i18n translation.
 */
@Path(ApiUrls.QUICKFILTERS)
@ViewerRestServiceBinding
public class QuickFilterResource {

    private static final Logger logger = LogManager.getLogger(QuickFilterResource.class);

    @GET
    @Path(ApiUrls.QUICKFILTERS_FACETS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "search" }, summary = "Get translated facet values for a quick filter dropdown")
    @ApiResponse(responseCode = "200", description = "Map of first-letter groups to facet value entries with translated labels")
    @ApiResponse(responseCode = "400", description = "Missing or invalid 'field' parameter")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or internal error")
    public Response getFacetValues(
            @Parameter(description = "Solr field name to retrieve facet values for (e.g. MD_CREATOR, DOCSTRCT_TOP)")
            @QueryParam("field")
            String field,
            @Parameter(description = "Language tag for label translation (e.g. de, en). Defaults to current session locale.")
            @QueryParam("lang")
            String lang) {
        if (StringUtils.isBlank(field)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Missing 'field' parameter\"}").build();
        }

        try {
            Locale locale = StringUtils.isNotBlank(lang) ? Locale.forLanguageTag(lang) : BeanUtils.getLocale();

            String facetField = SearchHelper.facetifyField(field);
            String query = "+(ISWORK:true OR ISANCHOR:true)" + SearchHelper.getAllSuffixes();
            QueryResponse resp = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(query, null, Collections.singletonList(facetField), 1, null, null, false);

            FacetField solrFacetField = resp != null ? resp.getFacetField(facetField) : null;
            if (solrFacetField == null) {
                return Response.ok(Collections.emptyMap()).build();
            }

            boolean translate = DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(field);
            Map<String, List<FacetValueEntry>> grouped = new TreeMap<>();

            for (Count count : solrFacetField.getValues()) {
                String value = count.getName();
                if (StringUtils.isBlank(value) || value.startsWith("\\u0001")) {
                    continue;
                }

                String label;
                if (translate) {
                    String translated = ViewerResourceBundle.getTranslation(value, locale);
                    label = translated != null ? translated : value;
                } else {
                    label = value;
                }

                String firstChar = label.substring(0, 1).toUpperCase();
                grouped.computeIfAbsent(firstChar, k -> new ArrayList<>())
                        .add(new FacetValueEntry(value, label, count.getCount()));
            }

            return Response.ok(grouped).build();
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error loading quick filter facet values for field '{}': {}", field, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * DTO for a single facet value entry in the quick filter dropdown.
     */
    @SuppressWarnings("unused")
    public static class FacetValueEntry {
        private final String value;
        private final String label;
        private final long count;

        public FacetValueEntry(String value, String label, long count) {
            this.value = value;
            this.label = label;
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
        }
    }
}
