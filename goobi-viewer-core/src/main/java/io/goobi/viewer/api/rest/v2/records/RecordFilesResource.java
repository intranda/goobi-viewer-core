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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_CMDI;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_TEI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.MediaResourceHelper;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.StringPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(RECORDS_FILES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordFilesResource {

    private static final Logger logger = LogManager.getLogger(RecordFilesResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private Configuration config;
    @Inject
    private ApiUrls urls;

    private final String pi;
    private final TextResourceBuilder builder = new TextResourceBuilder();

    public RecordFilesResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
    }

    /**
     * Returns the ALTO document for a single page.
     *
     * <p>The file is looked up first in the crowdsourced ALTO folder and, if not found there, in the record's regular ALTO folder, so
     * corrections made through crowdsourcing take precedence over the originally indexed file. Access requires the fulltext view
     * permission for the requested page.
     *
     * @param filename filename of the alto document
     * @return the ALTO document as an XML string
     * @throws ServiceNotAllowedException if the fulltext view permission is not granted
     * @throws ContentNotFoundException if no ALTO file exists for the given filename
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_ALTO)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get Alto fulltext for a single page",
            description = "The file is looked up first in the crowdsourced ALTO folder and, if not found there, in the record's regular ALTO"
                    + " folder, so crowdsourcing corrections take precedence over the originally indexed file. Access requires the fulltext"
                    + " view permission for the requested page.")
    @ApiResponse(responseCode = "200", description = "ALTO document of the page", useReturnTypeSchema = true)
    // Access-denied and not-found responses are returned as application/json even though the success content type is text/xml
    @ApiResponse(responseCode = "403", description = "Access to this fulltext file is restricted")
    @ApiResponse(responseCode = "404", description = "ALTO file not found for the given record and filename")
    public String getAlto(
            @Parameter(description = "Filename of the alto document") @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException,
            ServiceNotAllowedException {
        String cleanedFilename = Path.of(filename).getFileName().toString();
        checkFulltextAccessConditions(pi, cleanedFilename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        StringPair ret = builder.getAltoDocument(pi, cleanedFilename);
        return ret.getOne();
    }

    /**
     * Returns the plain text content of a single page.
     *
     * <p>The text is looked up first in the crowdsourced plaintext folder and then in the regular plaintext folder; if neither contains a
     * matching file, the page's ALTO file is converted to plain text on the fly instead. Access requires the fulltext view permission for
     * the requested page.
     *
     * @param filename filename containing the text
     * @return the plain text content of the page
     * @throws ServiceNotAllowedException if the fulltext view permission is not granted
     * @throws ContentNotFoundException if no plaintext or ALTO file exists for the filename
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get plaintext for a single page",
            description = "The text is looked up first in the crowdsourced plaintext folder and then in the regular plaintext folder; if"
                    + " neither contains a matching file, the page's ALTO file is converted to plain text on the fly instead. Access"
                    + " requires the fulltext view permission for the requested page.")
    @ApiResponse(responseCode = "200", description = "Plain text of the page", useReturnTypeSchema = true)
    // Access-denied and not-found responses are returned as application/json even though the success content type is text/plain
    @ApiResponse(responseCode = "403", description = "Access to this fulltext file is restricted")
    @ApiResponse(responseCode = "404", description = "Plaintext file not found for the given record and filename")
    public String getPlaintext(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, ServiceNotAllowedException {
        String cleanedFilename = Path.of(filename).getFileName().toString();
        checkFulltextAccessConditions(pi, cleanedFilename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltext(pi, cleanedFilename);
    }

    /**
     * Returns the fulltext of a single page converted to TEI format.
     *
     * <p>The plain text of the page (falling back to a converted ALTO file, as for the plaintext endpoint) is wrapped in a generated TEI
     * header built from the record's indexed metadata. Access requires the fulltext view permission for the requested page.
     *
     * @param filename filename containing the text
     * @return the page fulltext as a TEI XML document
     * @throws ServiceNotAllowedException if the fulltext view permission is not granted
     * @throws ContentNotFoundException if no record is found for the given identifier, or if no plaintext or ALTO file exists for the
     *             filename
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_TEI)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get fulltext for a single page in TEI format",
            description = "The plain text of the page (falling back to a converted ALTO file, as for the plaintext endpoint) is wrapped in a"
                    + " generated TEI header built from the record's indexed metadata. Access requires the fulltext view permission for the"
                    + " requested page.")
    @ApiResponse(responseCode = "200", description = "TEI document of the page", useReturnTypeSchema = true)
    // Access-denied and not-found responses are returned as application/json even though the success content type is text/xml
    @ApiResponse(responseCode = "403", description = "Access to this fulltext file is restricted")
    @ApiResponse(responseCode = "404", description = "TEI file not found for the given record and filename")
    public String getTEI(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, ContentLibException {
        String cleanedFilename = Path.of(filename).getFileName().toString();
        checkFulltextAccessConditions(pi, cleanedFilename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltextAsTEI(pi, cleanedFilename);
    }

    /**
     * Returns a source metadata file (e.g. METS/LIDO) of the record.
     *
     * <p>The requested filename is sanitized to strip any path components and restricted to letters, digits, spaces and the characters
     * {@code . , - _ ! ( ) '} before being resolved beneath the record's configured source folder; any other name is rejected. Rejection
     * happens as an {@code IllegalArgumentException} that is neither caught here nor mapped, so the caller sees an undeclared 500 rather
     * than the 400 the v1 endpoint returns. The content type is determined from the file name; if it cannot be determined, the response
     * falls back to the declared octet stream type. Access requires the download-original-content privilege for the record.
     *
     * @param filename source file name
     * @return the source file content as a binary stream
     * @throws ContentNotFoundException if no such file exists
     * @throws ServiceNotAllowedException if the download-original-content privilege is not granted
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_SOURCE)
    @Operation(tags = { "records" }, summary = "Get source files of record",
            description = "The requested filename is sanitized to strip any path components and restricted to letters, digits, spaces and"
                    + " the characters . , - _ ! ( ) ' before being resolved beneath the record's configured source folder; any other name is"
                    + " rejected, though unlike the v1 endpoint an invalid name is not reported as a 400. The content type is determined from"
                    + " the file name; if it cannot be determined, the response falls back to the declared octet stream type. Access requires"
                    + " the download-original-content privilege for the record.")
    @ApiResponse(responseCode = "200", description = "Source file of the record",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")))
    // Error responses (404, 403) are returned as application/json even though success is application/octet-stream
    @ApiResponse(responseCode = "403", description = "Access to this source file is restricted")
    @ApiResponse(responseCode = "404", description = "Source file not found")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getSourceFile(
            @Parameter(description = "Source file name") @PathParam("filename") final String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        String sanitizedFileName = FileTools.sanitizeFileName(filename); // Make sure filename doesn't inject a path traversal
        Path path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder(), null, sanitizedFileName);
        if (!Files.isRegularFile(path)) { //NOSONAR File name is sanitized at this point
            throw new ContentNotFoundException("Source file " + sanitizedFileName + " not found");
        }

        boolean access = AccessConditionUtils.checkContentFileAccessPermission(pi, servletRequest).isGranted();
        if (!access) {
            throw new ServiceNotAllowedException("Access to source file " + sanitizedFileName + " not allowed");
        }

        String mimeType = "appplication/octet-stream";
        try {
            mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, sanitizedFileName, path);
        } catch (IOException e) {
            logger.error("Failed to probe file content type");
        }

        StreamingOutput so = out -> {
            try (InputStream in = Files.newInputStream(path)) {
                IOUtils.copy(in, out);
            }
        };
        return Response.ok(so, mimeType).build();
    }

    /**
     * Returns the CMDI metadata document for a record file.
     *
     * <p>The requested language defaults to the current user's locale if not given. The record's CMDI folder is searched for a file whose
     * name matches "{@code _<isoCode>.xml}" for that language's ISO-3 code, falling back to the ISO-2 code if no ISO-3 match exists.
     * Access requires the fulltext view permission for the given filename.
     *
     * @param filename image file name for cmdi
     * @param lang language for CMDI
     * @return the CMDI document as an XML string
     * @throws ServiceNotAllowedException if the fulltext view permission is not granted
     * @throws ContentNotFoundException if no matching CMDI file exists
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_CMDI)
    @Operation(tags = { "records" }, summary = "Get cmdi for record file",
            description = "The requested language defaults to the current user's locale if not given. The record's CMDI folder is searched"
                    + " for a file whose name matches `_<isoCode>.xml` for that language's ISO-3 code, falling back to the ISO-2 code if no"
                    + " ISO-3 match exists. Access requires the fulltext view permission for the given filename.")
    @ApiResponse(responseCode = "200", description = "CMDI document of the record file",
            content = @Content(mediaType = MediaType.TEXT_XML, schema = @Schema(type = "string")))
    // Access-denied and not-found responses are returned as application/json
    @ApiResponse(responseCode = "403", description = "Access to this file is restricted")
    @ApiResponse(responseCode = "404", description = "CMDI file not found for the given record and filename")
    public String getCMDI(
            @Parameter(description = "Image file name for cmdi") @PathParam("filename") String filename,
            @Parameter(description = "Language for CMDI") @QueryParam("lang") final String lang)
            throws ContentLibException, PresentationException, IndexUnreachableException, IOException {
        String cleanedFilename = Path.of(filename).getFileName().toString();
        checkFulltextAccessConditions(pi, cleanedFilename);

        final Language language =
                DataManager.getInstance().getLanguageHelper().getLanguage(lang == null ? BeanUtils.getLocale().getLanguage() : lang);
        Path cmdiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getCmdiFolder());
        Path filePath = getDocumentLanguageVersion(cmdiPath, language);
        if (filePath != null && Files.isRegularFile(filePath)) {
            try {
                Document doc = XmlTools.readXmlFile(filePath);
                return XmlTools.getXMLOutputter().outputString(doc);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException | JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
    }

    /**
     * Throw an AccessDenied error if the request doesn't satisfy the access conditions.
     * 
     * @param pi persistent identifier of the record
     * @param filename name of the fulltext file to check access for
     * @throws ServiceNotAllowedException
     */
    private void checkFulltextAccessConditions(String pi, String filename) throws ServiceNotAllowedException {
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccess(servletRequest.getSession(), "text", pi, filename, NetTools.getIpAddress(servletRequest), false)
                    .isGranted();
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(String.format("Cannot check fulltext access for pi %s and file %s: %s", pi, filename, e.toString()));
        }
        if (!access) {
            throw new ServiceNotAllowedException("Access to fulltext file " + pi + "/" + filename + " not allowed");
        }
    }

    /**
     * Returns the first file on the given folder path that contains the requested language code in its name. ISO-3 files are preferred, with a
     * fallback to ISO-2.
     *
     * @param folder directory containing the language-versioned document files
     * @param language requested language for the document version
     * @return Path of the requested file; null if not found
     * @throws IOException
     */
    static Path getDocumentLanguageVersion(Path folder, Language language) throws IOException {
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }
        if (folder == null || !Files.isDirectory(folder)) {
            return null;
        }

        java.nio.file.Path ret;
        // This will return the file with the requested language or alternatively the first file in the TEI folder
        try (Stream<java.nio.file.Path> teiFiles = Files.list(folder)) {
            ret = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(null);
        }
        // Fallback to ISO-2
        if (ret == null) {
            try (Stream<java.nio.file.Path> teiFiles = Files.list(folder)) {
                ret = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCodeOld() + ".xml"))
                        .findFirst()
                        .orElse(null);
            }
        }

        return ret;
    }

}
