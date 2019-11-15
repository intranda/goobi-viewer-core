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
package io.goobi.viewer.servlets.rest.iiif.image;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * Adds additional parameters to iiif contentServer requests as requestContext properties Parameters must be named "param:" followed by the name of
 * the corresponding contentServer request parameter
 */
@Provider
@ContentServerBinding
public class ImageParameterFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageParameterFilter.class);

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String uri = request.getUriInfo().getPath();
        if (uri.contains("image/") || uri.contains("pdf/mets/")) {

            String requestPath;
            if (uri.contains("image/")) {
                requestPath = uri.substring(uri.indexOf("image/") + 6);
            } else {
                requestPath = uri.substring(uri.indexOf("pdf/mets/") + 9);
            }

            // logger.trace("Filtering request {}", requestPath);
            StrTokenizer tokenizer = new StrTokenizer(requestPath, "/");
            List<String> pathSegments = tokenizer.getTokenList();
            String pi = pathSegments.get(0).replaceAll("\\..+", "");
            try {
                if (StringUtils.isNotBlank(pi) && !"-".equals(pi)) {
                    addRepositoryPathIfRequired(request, pi);
                }
            } catch (PresentationException e) {
                String mediaType = MediaType.TEXT_XML;
                if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
                    mediaType = MediaType.APPLICATION_JSON;
                }
                Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR)
                        .type(mediaType)
                        .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                        .build();
                request.abortWith(errorResponse);
            }
        }

    }

    /**
     * @param request
     * @throws PresentationException
     */
    public static void addRepositoryPathIfRequired(ContainerRequestContext request, String pi) throws PresentationException {
        String dataRepository = null;
        try {
            dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            throw e;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            throw new PresentationException(e.getMessage());
        }
        if (StringUtils.isNotEmpty(dataRepository)) {
            String repositoriesHome;
            if (Paths.get(dataRepository).isAbsolute() || URI.create(dataRepository).isAbsolute()) {
                repositoriesHome = "";
            } else {
                repositoriesHome = Paths.get(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).toUri().toString();
            }
            dataRepository = repositoriesHome + dataRepository;

            addRepositoryParameter("param:imageSource", Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getMediaFolder()).toAbsolutePath().toString(), request);
            addRepositoryParameter("param:pdfSource", Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getPdfFolder()).toAbsolutePath().toString(), request);
            addRepositoryParameter("param:altoSource", Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getAltoFolder()).toAbsolutePath().toString(), request);
            addRepositoryParameter("param:metsSource", Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getIndexedMetsFolder()).toAbsolutePath().toString(), request);

        }

    }

    /**
     * @param request
     * @param dataFolderPath
     * @param requestParameter
     */
    private static void addRepositoryParameter(String requestParameter, String dataFolderPath, ContainerRequestContext request) {
        URI imageRepositoryPath;
        imageRepositoryPath = Paths.get(dataFolderPath).toUri();
        request.setProperty(requestParameter, imageRepositoryPath.toString());
    }

}
