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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_LIST;

import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentation2ResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 *
 * provides listings of records in reduced iiif form.
 *
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_LIST)
@ViewerRestServiceBinding
@CORSBinding
public class RecordsListResource {

    /**
     *
     */
    private static final int DEFAULT_MAX_ROWS = 100;
    private static final Logger logger = LogManager.getLogger(RecordsListResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "List records in an ordered collection page, use query parameter for filtering")
    public OrderedCollectionPage<IPresentationModelElement> listManifests(
            @Parameter(description = "filter query") @QueryParam("query") String query,
            @Parameter(description = "Index of the first result to return") @QueryParam("first") final Integer firstRow,
            @Parameter(description = "Number of results to return") @QueryParam("rows") final Integer rows,
            @Parameter(description = "filter for records from this date or later") @QueryParam("start") String start,
            @Parameter(description = "filter for records from this date or earlier") @QueryParam("end") String end,
            @Parameter(description = "filter for records of this subtheme") @QueryParam("subtheme") String subtheme,
            @Parameter(description = "sort string") @QueryParam("sort") String sort)
            throws IndexUnreachableException, DAOException, PresentationException, URISyntaxException, ViewerConfigurationException {

        String finalQuery = createQuery(query, start, end, subtheme);

        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);

        List<IPresentationModelElement> items =
                builder.getManifestsForQuery(finalQuery, sort, firstRow == null ? 0 : firstRow, rows == null ? DEFAULT_MAX_ROWS : rows);

        OrderedCollectionPage<IPresentationModelElement> page = new OrderedCollectionPage<>();
        page.setOrderedItems(items);

        return page;
    }

    /**
     * @param query
     * @param start
     * @param end
     * @param subtheme
     * @return Generated query
     */
    private String createQuery(String query, final String start, final String end, String subtheme) {
        String finalQuery = "";
        if (StringUtils.isNotBlank(query)) {
            finalQuery += "+(" + query + ")";
        }
        if (!StringUtils.isAllBlank(start, end)) {
            String s = StringUtils.isNotBlank(start) ? start : "*";
            String e = StringUtils.isNotBlank(end) ? end : "*";
            finalQuery += " +YEAR:[ " + s + " TO " + e + " ]";
        }
        if (StringUtils.isNotBlank(subtheme)) {
            String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
            finalQuery += " +" + discriminatorField + ":" + subtheme;
        }
        finalQuery += SearchHelper.getAllSuffixes(servletRequest, true, true);

        return finalQuery;
    }

}
