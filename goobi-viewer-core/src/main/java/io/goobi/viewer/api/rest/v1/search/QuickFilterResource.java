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
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST endpoint for quick filter facet values with i18n translation.
 */
@Path(ApiUrls.QUICKFILTERS)
@ViewerRestServiceBinding
public class QuickFilterResource {

    private static final Logger logger = LogManager.getLogger(QuickFilterResource.class);

    /**
     * Returns the facet values for a Solr field, grouped by the first letter of their (optionally translated) label.
     *
     * <p>Facet values are computed only over top-level work and anchor records, combined with the caller's access-condition
     * suffixes. Whether values are translated is configured per facet template and field in config_viewer.xml and defaults
     * to enabled; a field with no facet values returns an empty map rather than an error.
     *
     * @param field Solr field name to retrieve facet values for (e.g. MD_CREATOR, DOCSTRCT_TOP)
     * @param lang language tag for label translation (e.g. de, en). Defaults to current session locale
     * @param template facet template name whose field config to use for translation (defaults to _DEFAULT)
     * @return a {@link Response} with the facet values grouped by first-letter, or an error if 'field' is missing
     */
    @GET
    @Path(ApiUrls.QUICKFILTERS_FACETS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "search" }, summary = "Get translated facet values for a quick filter dropdown",
            description = "Facet values are computed only over top-level work and anchor records, filtered by the caller's"
                    + " access-condition suffixes, and grouped by the first letter of their label. Whether values are translated"
                    + " is configured per facet template and field (config_viewer.xml) and defaults to enabled; a field for which"
                    + " the index returns no facet values yields an empty map rather than an error.")
    @ApiResponse(responseCode = "200", description = "Map of first-letter groups to facet value entries with translated labels",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object", description = "Facet values grouped by group label")))
    @ApiResponse(responseCode = "400", description = "Missing or invalid 'field' parameter")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or internal error")
    public Response getFacetValues(
            @Parameter(description = "Solr field name to retrieve facet values for (e.g. MD_CREATOR, DOCSTRCT_TOP)")
            @QueryParam("field")
            String field,
            @Parameter(description = "Language tag for label translation (e.g. de, en). Defaults to current session locale.")
            @QueryParam("lang")
            String lang,
            @Parameter(description = "Facet template name whose field config to use for translation (defaults to _DEFAULT).")
            @QueryParam("template")
            String template) {
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

            String templateName = StringUtils.isNotBlank(template) ? template : StringConstants.DEFAULT_NAME;
            boolean translate = DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(templateName, field);
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
