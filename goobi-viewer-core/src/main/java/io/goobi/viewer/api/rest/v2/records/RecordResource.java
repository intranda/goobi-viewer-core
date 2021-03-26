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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_RECORD)
@ViewerRestServiceBinding
@CORSBinding
public class RecordResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;

    public RecordResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF manifest for record")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(
                    description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources from files") @QueryParam("mode") String mode)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException {
        return new Manifest3B
    }

}
