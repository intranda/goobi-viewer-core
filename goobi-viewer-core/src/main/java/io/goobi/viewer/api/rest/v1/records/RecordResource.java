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

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentationResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.NERBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TocResourceBuilder;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.iiif.presentation.IIIFPresentationBinding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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
    private AbstractApiUrlManager urls;

    private final String pi;

    public RecordResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
    }

    @GET
    @javax.ws.rs.Path(RECORDS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records", "ris" }, summary = "Download ris as file")
    public String getRISAsFile()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {

        StructElement se = getStructElement(pi);
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        String ris = new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
        return ris;
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
    public IAnnotationCollection getAnnotationsForRecord(
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
    @ApiResponse(responseCode="400", description="If the page number is out of bounds")
    @CORSBinding
    public AnnotationPage getAnnotationPageForRecord(@PathParam("page") Integer page)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForRecord(pi, uri, page);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(tags = { "records", "annotations"}, summary = "List comments for a record")
    public IAnnotationCollection getCommentsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls).getOAnnotationListForRecordComments(pi, uri);
        } else {
            URI uri = URI.create(apiPath.build());
            return new AnnotationsResourceBuilder(urls).getWebAnnotationCollectionForRecordComments(pi, uri);
        }
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode="400", description="If the page number is out of bounds")
    @CORSBinding
    public AnnotationPage getCommentPageForRecord(@PathParam("page") Integer page)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi).build());
        return new AnnotationsResourceBuilder(urls).getWebAnnotationPageForRecordComments(pi, uri, page);
    }


    @GET
    @javax.ws.rs.Path(RECORDS_METADATA_SOURCE)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = {"records"}, summary = "Get record metadata source file")
    @CORSBinding
    public StreamingOutput getSource()
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
    
    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = {"records", "iiif"}, summary = "Get IIIF manifest for record")
    @CORSBinding
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources from files")@QueryParam("mode") String mode) throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        BuildMode buildMode = getBuildeMode(mode);
        return builder.getManifest(pi, buildMode);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_LAYER)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = {"records", "iiif"}, summary = "Get a layer within a IIIF manifest")
    @CORSBinding
    @IIIFPresentationBinding
    public IPresentationModelElement getLayer(
            @Parameter(description = "Name of the manifest layer") @PathParam("name") String layerName,
            @Parameter(description = "Build mode for manifes to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources from files")@QueryParam("mode") String mode) throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException, IllegalRequestException, IOException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        BuildMode buildMode = getBuildeMode(mode);
        return builder.getLayer(pi, layerName);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_NER_TAGS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = {"records"}, summary = "Get NER tags for a record")
    @CORSBinding
    @IIIFPresentationBinding
    public DocumentReference getNERTags(
            @Parameter(description = "First page to get tags for")@QueryParam("start") Integer start,
            @Parameter(description = "Last page to get tags for")@QueryParam("end") Integer end,
            @Parameter(description = "Number of pages to combine into each group")@QueryParam("step") Integer stepSize,
            @Parameter(description = "Tag type to consider (person, coorporation, event or location)")@QueryParam("type") String type
            ) throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        NERBuilder builder = new NERBuilder();
        return builder.getNERTags(pi, type, start, end, stepSize == null ? 1 : stepSize);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = {"records", "fulltext"}, summary = "Get entire plaintext of record")
    @CORSBinding
    @IIIFPresentationBinding
    public String getPlaintext() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, ServiceNotAllowedException, IOException, DAOException {

        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getFulltext(pi);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_PLAINTEXT_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = {"records", "fulltext"}, summary = "Get entire plaintext of record")
    @CORSBinding
    @IIIFPresentationBinding
    public StreamingOutput getPlaintextAsZip() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IOException, DAOException, ContentLibException {
        
        String filename = pi + "_plaintext.zip";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        
        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getFulltextAsZip(pi);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_ALTO)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = {"records", "fulltext"}, summary = "Get entire alto document for record")
    @CORSBinding
    @IIIFPresentationBinding
    public String getAlto() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IOException, DAOException, ContentLibException, JDOMException {

        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getAltoDocument(pi);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_ALTO_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = {"records", "fulltext"}, summary = "Get entire plaintext of record")
    @CORSBinding
    @IIIFPresentationBinding
    public StreamingOutput getAltoAsZip() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IOException, DAOException, ContentLibException {
        
        String filename = pi + "_alto.zip";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        
        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getAltoAsZip(pi);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_TEI)
    @Produces({MediaType.TEXT_XML})
    @Operation(tags = {"records", "fulltext"}, summary = "Get text of record in TEI format.", description ="If possible, directly read a TEI file associated with the record, otherwise convert all fulltexts to TEI documents")
    @CORSBinding
    @IIIFPresentationBinding
    public String getTei(
            @Parameter(description="perferred language for the TEI file, in ISO-639 format")@QueryParam("lang") String language) throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IOException, DAOException, ContentLibException {
        
        String filename = pi + "_tei.zip";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        
        if(language == null) {
            language = servletRequest.getLocale().getLanguage();
        }
        
        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getTeiDocument(pi, language);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_TEI_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = {"records", "fulltext"}, summary = "Get text of record in TEI format as a zip file.", description ="If possible, directly read a TEI file associated with the record, otherwise convert all fulltexts to TEI documents")
    @CORSBinding
    @IIIFPresentationBinding
    public StreamingOutput getTeiAsZip(
            @Parameter(description="perferred language for the TEI file, in ISO-639 format")@QueryParam("lang") String language) throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IOException, DAOException, ContentLibException {
        
        String filename = pi + "_tei.zip";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        
        if(language == null) {
            language = servletRequest.getLocale().getLanguage();
        }
        
        TextResourceBuilder builder = new TextResourceBuilder(servletRequest, servletResponse);
        return builder.getTeiAsZip(pi, language);
    }
    
    /**
     * Endpoint for IIIF Search API service in a manifest. Depending on the given motivation parameters, fulltext (motivation=painting), user comments
     * (motivation=commenting) and general (crowdsourcing-) annotations (motivation=describing) may be searched.
     *
     * @param pi The pi of the manifest to search
     * @param query The search query; a list of space separated terms. The search is for all complete words which match any of the query terms. Terms
     *            may contain the wildcard charachter '*' to represent an arbitrary number of characters within the word
     * @param motivation a space separated list of motivations of annotations to search for. Search for the following motivations is implemented:
     *            <ul>
     *            <li>painting: fulltext resources</li>
     *            <li>non-painting: all supported resources except fulltext</li>
     *            <li>commenting: user comments</li>
     *            <li>describing: Crowdsourced or other general annotations</li>
     *            </ul>
     * @param date not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param user not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param page the page number for paged result sets. if this is empty, page=1 is assumed
     * @return a {@link de.intranda.api.iiif.search.SearchResult} containing all annotations matching the query in the 'resources' property
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST_SEARCH)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchResult searchInManifest(@PathParam("pi") String pi, @QueryParam("q") String query, @QueryParam("motivation") String motivation,
            @QueryParam("date") String date, @QueryParam("user") String user, @QueryParam("page") Integer page)
            throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi).setMotivation(motivation).setDate(date).setUser(user).setPage(page).build();
    }

    /**
     * <p>
     * autoCompleteInManifest.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.lang.String} object.
     * @param date a {@link java.lang.String} object.
     * @param user a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a {@link de.intranda.api.iiif.search.AutoSuggestResult} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST_AUTOSUGGEST)
    @Produces({ MediaType.APPLICATION_JSON })
    public AutoSuggestResult autoCompleteInManifest(@PathParam("pi") String pi, @QueryParam("q") String query,
            @QueryParam("motivation") String motivation, @QueryParam("date") String date, @QueryParam("user") String user,
            @QueryParam("page") Integer page) throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi).setMotivation(motivation)
                .setDate(date)
                .setUser(user)
                .setPage(page)
                .buildAutoSuggest();
    }


    /**
     * @param mode
     * @return
     */
    public BuildMode getBuildeMode(String mode) {
        BuildMode buildMode = BuildMode.IIIF;
        if(StringUtils.isNotBlank(mode)) {             
            try {                
                buildMode = BuildMode.valueOf(mode);
            } catch(IllegalArgumentException e) {
                logger.warn("Illegal query parameter value for 'mode': " + mode + ". Ignore parameter");
            }
        }
        return buildMode;
    }

    /**
     * @param pi
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StructElement getStructElement(String pi) throws PresentationException, IndexUnreachableException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc("PI:" + pi, null);
        StructElement struct = new StructElement(Long.valueOf((String)doc.getFieldValue(SolrConstants.IDDOC)), doc);
        return struct;
    }
}
