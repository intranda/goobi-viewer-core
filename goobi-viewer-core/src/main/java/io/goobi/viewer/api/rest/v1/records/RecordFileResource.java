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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_PDF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PDF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_FILES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordFileResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private AbstractApiUrlManager urls;

    private final String pi;
    private final TextResourceBuilder builder = new TextResourceBuilder();

    public RecordFileResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_ALTO)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get Alto fulltext for a single page")
    public String getAlto(
            @Parameter(description = "Filename of the alto document") @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException,
            ServiceNotAllowedException, DAOException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getAltoDocument(pi, filename);
    }



    @GET
    @javax.ws.rs.Path(RECORDS_FILES_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get plaintext for a single page")
    public String getPlaintext(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, DAOException, ServiceNotAllowedException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltext(pi, filename);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_TEI)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get fulltext for a single page in TEI format")
    public String getTEI(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltextAsTEI(pi, filename);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_PDF)
    @Produces({ "application/pdf" })
    @Operation(tags = { "records" }, summary = "Non-canonical URL to PDF file")
    public Response getPDF(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws ContentLibException {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_PDF).params(pi, filename).build();
        try {
            Response resp = Response.seeOther(PathConverter.toURI(url)).build();
            return resp;
        } catch (URISyntaxException e) {
            throw new ContentLibException("Cannot create redirect url to " + url);
        }
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_SOURCE)
    @Operation(tags = { "records" }, summary = "Get source files of record")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getSourceFile(
            @Parameter(description = "Source file name") @PathParam("filename") String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        Path path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder(), null, filename);
        if (Files.isRegularFile(path)) {
            boolean access = !AccessConditionUtils.checkContentFileAccessPermission(pi, servletRequest, Collections.singletonList(path))
                    .containsValue(Boolean.FALSE);
            if (access) {
                try {
                    String contentType = Files.probeContentType(path);
                    logger.trace("content type: {}", contentType);
                    if (StringUtils.isNotBlank(contentType)) {
                        servletResponse.setContentType(contentType);
                    }
                    servletResponse.setHeader("Content-Disposition", new StringBuilder("attachment;filename=").append(filename).toString());
                    servletResponse.setHeader("Content-Length", String.valueOf(Files.size(path)));
                } catch(IOException e) {
                    logger.error("Failed to probe file content type");
                }
                
                return (out) -> {
                    try(InputStream in = Files.newInputStream(path)) {
                        IOUtils.copy(in, out);
                    }
                };
            } else {
                throw new ServiceNotAllowedException("Access to source file " + filename + " not allowed");
            }
        } else {
            throw new ContentNotFoundException("Source file " + filename + " not found");
        }
    }
    
    /**
     * Throw an AccessDenied error if the request doesn't satisfy the access conditions
     * @throws ServiceNotAllowedException 
     */
    private void checkFulltextAccessConditions(String pi, String filename) throws ServiceNotAllowedException {
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccess(servletRequest, "text", pi, filename, false);
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(String.format("Cannot check fulltext access for pi %s and file %s: %s", pi, filename, e.toString()));
        }
        if(!access) {            
            throw new ServiceNotAllowedException("Access to fulltext file " + pi + "/" + filename + " not allowed");
        }
    }

}
