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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ALTO_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CMDI_LANG;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_JSON;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_LAYER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_METADATA_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PLAINTEXT_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_TEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI_LANG;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TOC;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

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
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentation2ResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.NERBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.TocResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.json.JsonMetadataConfiguration;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author florian
 *
 */
@jakarta.ws.rs.Path(RECORDS_RECORD)
@ViewerRestServiceBinding
@CORSBinding
public class RecordResource {

    private static final Logger logger = LogManager.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;
    private final TextResourceBuilder builder = new TextResourceBuilder();

    private static Thread deleteRecordThread = null;

    public RecordResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Download ris as file")
    @AccessConditionBinding
    public String getRISAsFile()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {

        StructElement se = getStructElement(pi);
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        servletResponse.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }

    /**
     * <p>
     * getRISAsText.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_RIS_TEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get ris as text")
    public String getRISAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        StructElement se = getStructElement(pi);
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_TOC)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get table of contents of records")
    public String getTOCAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException, ViewerConfigurationException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return new TocResourceBuilder(servletRequest, servletResponse).getToc(pi);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record")
    public IAnnotationCollection getAnnotationsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations,"
                            + " otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new OpenAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false, servletRequest);
        }
        URI uri = URI.create(apiPath.build());
        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record")
    public IAnnotationCollection getCommentsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations,"
                            + " otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        if ("oa".equalsIgnoreCase(format)) {
            URI uri = URI.create(apiPath.query("format", "oa").build());
            return new AnnotationsResourceBuilder(urls, servletRequest).getOAnnotationListForRecordComments(pi, uri);
        }

        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecordComments(pi, uri);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "400", description = "If the page number is out of bounds")
    public AnnotationPage getCommentPageForRecord(@PathParam("page") Integer page)
            throws DAOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi).build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationPageForRecordComments(pi, uri, page);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_METADATA_SOURCE)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get record metadata source file")
    public StreamingOutput getSource() throws ContentNotFoundException, PresentationException, IndexUnreachableException {

        StructElement se = getStructElement(pi);

        String format = se.getSourceDocFormat();
        String dataRepository = se.getDataRepository();

        String filePath =
                DataFileTools.getSourceFilePath(pi + ".xml", dataRepository,
                        format != null ? format.toUpperCase() : SolrConstants.SOURCEDOCFORMAT_METS);
        Path path = Paths.get(filePath);

        if (Files.isRegularFile(path)) {
            return out -> {
                try (FileInputStream in = new FileInputStream(path.toFile())) {
                    FileTools.copyStream(out, in);
                } catch (IOException e) {
                    logger.trace(e.getMessage(), e);
                } finally {
                    out.flush();
                    out.close();
                }
            };
        }

        throw new ContentNotFoundException("No source file found for " + pi);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 manifest for record")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(
                    description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full"
                            + " IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple'"
                            + " ignores all resources from files") @QueryParam("mode") String mode)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException {
        IIIFPresentation2ResourceBuilder b = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        BuildMode buildMode = getBuildeMode(mode);
        if (BuildMode.IIIF == buildMode) {
            try {
                Optional<URI> forwardURI = b.getManifestBuilder().getExternalManifestURI(pi);
                if (forwardURI.isPresent()) {
                    servletResponse.sendRedirect(forwardURI.get().toString());
                    return null;
                }
            } catch (IOException e) {
                logger.error("Error forwarding manifest url", e);
            }
        }
        return b.getManifest(pi, Collections.emptyList(), buildMode);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_LAYER)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get a layer within a IIIF 2.1.1 manifest")
    @IIIFPresentationBinding
    public IPresentationModelElement getLayer(
            @Parameter(description = "Name of the manifest layer") @PathParam("name") String layerName,
            @Parameter(
                    description = "Build mode for manifes to select type of resources to include. Default is 'iiif' which returns the full"
                            + " IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple'"
                            + " ignores all resources from files") @QueryParam("mode") String mode)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, IllegalRequestException, IOException {
        IIIFPresentation2ResourceBuilder b = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        return b.getLayer(pi, layerName);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_NER_TAGS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get NER tags for a record")
    public DocumentReference getNERTags(
            @Parameter(description = "First page to get tags for") @QueryParam("start") Integer start,
            @Parameter(description = "Last page to get tags for") @QueryParam("end") Integer end,
            @Parameter(description = "Number of pages to combine into each group") @QueryParam("step") Integer stepSize,
            @Parameter(description = "Tag type to consider (person, coorporation, event or location)") @QueryParam("type") String type)
            throws PresentationException, IndexUnreachableException {
        NERBuilder b = new NERBuilder();
        return b.getNERTags(pi, type, start, end, stepSize == null ? 1 : stepSize, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get entire plaintext of record within a single text file")
    @CORSBinding
    @IIIFPresentationBinding
    public String getPlaintext() throws PresentationException, IndexUnreachableException, IOException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        TextResourceBuilder b = new TextResourceBuilder();
        return b.getFulltext(pi, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_PLAINTEXT_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = { "records" }, summary = "Get entire plaintext of record as a zip archive of text files per page")
    public StreamingOutput getPlaintextAsZip()
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        logger.trace("getPlaintextAsZip: {}", pi);
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
            String filename = pi + "_plaintext.zip";
            servletResponse.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");
        }

        return builder.getFulltextAsZip(pi, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_ALTO)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get entire alto document for record")
    public String getAlto() throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getAltoDocument(pi, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_ALTO_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = { "records" }, summary = "Get a zip archive of alto documents per page")
    public StreamingOutput getAltoAsZip() throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
            String filename = pi + "_alto.zip";
            servletResponse.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");
        }

        return builder.getAltoAsZip(pi, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_CMDI_LANG)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get CMDI record file in the requested language.",
            description = "If possible, directly read a CMDI file associated with the record")
    public String getCmdiLanguage(
            @Parameter(description = "perferred language for the TEI file, in ISO-639 format") @PathParam("lang") final String language)
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }

        return builder.getCmdiDocument(pi,
                language == null ? servletRequest.getLocale().getLanguage() : StringTools.stripPatternBreakingChars(language));
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_TEI_LANG)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get TEI record file in the requested language.",
            description = "If possible, directly read a TEI file associated with the record, otherwise convert all fulltexts to TEI documents")
    public String getTeiLanguage(
            @Parameter(description = "perferred language for the TEI file, in ISO-639 format") @PathParam("lang") final String language)
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }

        return builder.getTeiDocument(pi,
                language == null ? servletRequest.getLocale().getLanguage() : StringTools.stripPatternBreakingChars(language), servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_TEI)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get text of record in TEI format.",
            description = "If possible, directly read a TEI file associated with the record, otherwise convert all fulltexts to TEI documents")
    public String getTei() throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }

        return builder.getTeiDocument(pi, servletRequest.getLocale().getLanguage(), servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_TEI_ZIP)
    @Produces({ "application/zip" })
    @Operation(tags = { "records" }, summary = "Get text of record in TEI format as a zip file.",
            description = "If possible, directly read a TEI file associated with the record, otherwise convert all fulltexts to TEI documents")
    public StreamingOutput getTeiAsZip(
            @Parameter(description = "perferred language for the TEI file, in ISO-639 format") @QueryParam("lang") final String language)
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {
        checkFulltextAccessConditions(pi);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
            String filename = pi + "_tei.zip";
            servletResponse.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");
        }

        return builder.getTeiAsZip(pi, language == null ? servletRequest.getLocale().getLanguage() : StringTools.stripPatternBreakingChars(language),
                servletRequest);
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
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_SEARCH)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchResult searchInManifest(@PathParam("pi") String pi, @QueryParam("q") String query, @QueryParam("motivation") String motivation,
            @QueryParam("date") String date, @QueryParam("user") String user, @QueryParam("page") Integer page)
            throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation).setDate(date).setUser(user).setPage(page).build();
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
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_AUTOCOMPLETE)
    @Produces({ MediaType.APPLICATION_JSON })
    public AutoSuggestResult autoCompleteInManifest(@PathParam("pi") String pi, @QueryParam("q") String query,
            @QueryParam("motivation") String motivation, @QueryParam("date") String date, @QueryParam("user") String user,
            @QueryParam("page") Integer page) throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation)
                .setDate(date)
                .setUser(user)
                .setPage(page)
                .buildAutoSuggest();
    }

    /**
     * @param mode
     * @return {@link BuildMode}
     */
    public static BuildMode getBuildeMode(String mode) {
        if (StringUtils.isNotBlank(mode)) {
            switch (mode.toLowerCase()) {
                case "iiif-simple":
                case "iiif_simple":
                case "simple":
                    return BuildMode.IIIF_SIMPLE;
                case "iiif-thumbs":
                case "iiif_thumbs":
                case "thumbs":
                case "thumbnails":
                    return BuildMode.THUMBS;
                default:
                    return BuildMode.IIIF;
            }
        }
        return BuildMode.IIIF;
    }

    /**
     * <p>
     * deleteRecord.
     * </p>
     *
     * @param createTraceDocument
     * @return Short summary of files deleted
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @AuthorizationBinding
    @Operation(tags = { "records" }, summary = "Delete the record from the SOLR database",
            description = "Requires an authentication token. This operation may take a while, depending on the indexer queue. If the request"
                    + " aborts before deletion is complete, further deletion requests will be disallowed until the operation completes")
    public String deleteRecord(
            @Parameter(description = "set true to create a trace document of the delete action") @QueryParam("trace") Boolean createTraceDocument) {

        JSONObject ret = new JSONObject();

        if (deleteRecordThread == null || !deleteRecordThread.isAlive()) {
            deleteRecordThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (DataManager.getInstance().getSearchIndex().getHitCount(SolrConstants.PI_PARENT + ":" + pi) > 0) {
                            ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_FORBIDDEN);
                            ret.put(JsonTools.KEY_MESSAGE, ViewerResourceBundle.getTranslation("deleteRecord_failure_volumes_present", null));
                        }
                        if (IndexerTools.deleteRecord(pi, createTraceDocument != null && createTraceDocument,
                                Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()))) {
                            ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_OK);
                            ret.put(JsonTools.KEY_MESSAGE, ViewerResourceBundle.getTranslation("deleteRecord_success", null));
                        } else {
                            ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            ret.put(JsonTools.KEY_MESSAGE, ViewerResourceBundle.getTranslation("deleteRecord_failure", null));
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                        ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put(JsonTools.KEY_MESSAGE, e.getMessage());
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put(JsonTools.KEY_MESSAGE, e.getMessage());
                    } catch (PresentationException e) {
                        logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                        ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put(JsonTools.KEY_MESSAGE, e.getMessage());
                    }
                }
            });

            deleteRecordThread.start();
            try {
                deleteRecordThread.join();
            } catch (InterruptedException e) {
                deleteRecordThread.interrupt();
                logger.error(e.getMessage(), e);
            }
        } else {
            ret.put(JsonTools.KEY_STATUS, HttpServletResponse.SC_FORBIDDEN);
            ret.put(JsonTools.KEY_MESSAGE, "Record deletion currently in progress");
        }

        return ret.toString();
    }

    /**
     * @param pi Record identifier
     * @param template JSON configuration template name
     * @return {@link Response}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "json" }, summary = "List record metadata as JSON. Solr query and filed mapping are configured statically.")
    public Response getRecordMetadataAsJson(@PathParam("pi") String pi, @PathParam("template") String template)
            throws IndexUnreachableException, PresentationException {
        logger.trace("getRecordMetadataAsJson: {}/{}", pi, template);
        if (StringUtils.isEmpty(pi)) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "pi missing").build();
        }
        JsonMetadataConfiguration config = DataManager.getInstance().getConfiguration().getWebApiFields(template);
        if (config == null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "Template not found: " + template).build();
        }

        String query =
                "+(" + SolrConstants.PI + ":\"" + pi + "\")" + (StringUtils.isNotEmpty(config.getQuery()) ? ("+(" + config.getQuery() + ")") : "");
        logger.trace(query);
        SolrDocumentList docs =
                DataManager.getInstance()
                        .getSearchIndex()
                        .search(SearchHelper.buildFinalQuery(query, false, servletRequest, null));
        logger.trace("{} hits.", docs.size());
        if (docs.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        JSONObject jsonObj = JsonTools.createJsonObjectFromSolrDoc(docs.get(0), config.getFields());

        return Response.ok(jsonObj.toString(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * @param pi
     * @return {@link StructElement} constructed out of given pi
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private static StructElement getStructElement(String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("getStructElement: {}", pi); //NOSONAR Debug
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc("PI:" + pi, null);
        return new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
    }

    /**
     * Throw an AccessDenied error if the request doesn't satisfy the access conditions
     *
     * @param pi
     * @throws ServiceNotAllowedException
     */
    private void checkFulltextAccessConditions(String pi) throws ServiceNotAllowedException {
        // logger.trace("checkFulltextAccessConditions: {}", pi); //NOSONAR Debug
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, servletRequest)
                    .isGranted();
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(String.format("Cannot check fulltext access for pi %s: %s", pi, e.toString()));
        } catch (RecordNotFoundException e) {
            //
        }
        if (!access) {
            throw new ServiceNotAllowedException("Access to fulltext of " + pi + " not allowed");
        }
    }
}
