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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_METADATA_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_TEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TOC;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.IApiUrlManager;
import io.goobi.viewer.api.rest.IApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TocResourceBuilder;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.viewer.StructElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_RECORD)
@ViewerRestServiceBinding
public class RecordResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private IApiUrlManager urls;

    private final String pi;

    public RecordResource() {
        pi = null;
    }

    public RecordResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
    }

    @javax.ws.rs.Path(RECORDS_RIS_FILE)
    @CORSBinding
    @Operation(tags = { "records", "ris" }, summary = "Download ris as file")
    @Produces({ MediaType.TEXT_PLAIN })
    public StreamingOutput getRISAsFile()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {

        StructElement se = getStructElement(pi);
        return new RisResourceBuilder(servletRequest, servletResponse).writeRIS(se);
    }

    /**
     * <p>
     * getRISAsText.
     * </p>
     *
     * @param iddoc a long.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_RIS_TEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records", "ris" }, summary = "Get ris as text")
    public String getRISAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException {

        StructElement se = getStructElement(pi);
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_TOC)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get table of contents of records")
    public String getTOCAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException, ViewerConfigurationException {

        return new TocResourceBuilder(servletRequest, servletResponse).getToc(pi);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(tags = { "records", "annotations"}, summary = "List annotations for a record")
    public IResource getAnnotationsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls).getOAnnotationListForRecord(pi, uri);
        } else {
            URI uri = URI.create(apiPath.build());
            return new AnnotationsResourceBuilder(urls).getWebAnnotationCollectionForRecord(pi, uri);
        }

    }

    @GET
    @javax.ws.rs.Path(RECORDS_ANNOTATIONS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IResource getAnnotationPageForRecord()
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForRecord(pi, uri);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(tags = { "records", "annotations"}, summary = "List comments for a record")
    public IResource getCommentsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls).getOAnnotationListForRecordComments(pi, uri);
        } else {
            URI uri = URI.create(apiPath.build());
            return new AnnotationsResourceBuilder(urls).getWebAnnotationCollectionForRecord(pi, uri);
        }
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IResource getCommentPageForRecord()
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForRecordComments(pi, uri);
    }


    @GET
    @javax.ws.rs.Path(RECORDS_METADATA_SOURCE)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = {"records"}, summary = "Get record metadata source file")
    @CORSBinding
    public StreamingOutput getSource(
            @PathParam("pi") String pi)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, ContentNotFoundException,
            PresentationException, IndexUnreachableException {

        StructElement se = getStructElement(pi);

        String format = se.getSourceDocFormat();
        String dataRepository = se.getDataRepository();

        String filePath =
                DataFileTools.getSourceFilePath(pi + ".xml", dataRepository, format != null ? format.toUpperCase() : SolrConstants._METS);
        Path path = Paths.get(filePath);

        if (Files.isRegularFile(path)) {
            return (out) -> {
                try (FileInputStream in = new FileInputStream(path.toFile())) {
                    FileTools.copyStream(out, in);
                } catch (IOException e) {
                    logger.trace(e.getMessage(), e);
                } finally {
                    out.flush();
                    out.close();
                }
            };
        } else {
            throw new ContentNotFoundException("No source file found for " + pi);
        }
    }

    /**
     * @param pi
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StructElement getStructElement(String pi) throws PresentationException, IndexUnreachableException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc("PI:, + pi", null);
        StructElement struct = new StructElement((long) doc.getFieldValue(SolrConstants.IDDOC), doc);
        return struct;
    }
}
