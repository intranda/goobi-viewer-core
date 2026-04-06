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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_CMDI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_MEDIA;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_MEI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.JDOMException;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.MediaResourceBinding;
import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.MediaResourceHelper;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.record.views.FileType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
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
@jakarta.ws.rs.Path(RECORDS_FILES)
@ViewerRestServiceBinding
@CORSBinding
public class RecordFileResource {

    private static final Logger logger = LogManager.getLogger(RecordFileResource.class);
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    @Context
    private Configuration config;

    private final String pi;
    private final TextResourceBuilder builder = new TextResourceBuilder();

    /**
     * 
     * @param request the http request
     * @param response the http response
     * @param pi the requested indentifier
     */
    public RecordFileResource(@Context HttpServletRequest request, @Context HttpServletResponse response,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi) {
        // Reject PIs containing characters illegal in URI paths / Solr queries before any
        // Solr or file-system access occurs.  BadRequestException (HTTP 400) is an unchecked
        // WebApplicationException that Jersey maps to 400 before invoking the endpoint.
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
        this.servletRequest = request;
        this.servletResponse = response;
        this.pi = pi;
        /**
         * required to count download statistics in {@link RecordFileDownloadFilter}
         */
        servletRequest.setAttribute("pi", pi);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_ALTO)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get Alto fulltext for a single page")
    @ApiResponse(responseCode = "200", description = "ALTO XML for the requested page")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or filename")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "ALTO file not found")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getAlto(@Parameter(description = "Filename of the alto document",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, ServiceNotAllowedException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        StringPair ret = builder.getAltoDocument(pi, Path.of(filename).getFileName().toString());
        return ret.getOne();
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get plaintext for a single page")
    @ApiResponse(responseCode = "200", description = "Plaintext content for the requested page")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or filename")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "Text file not found")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getPlaintext(
            @Parameter(description = "Filename containing the text",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, ServiceNotAllowedException {
        logger.trace("getPlaintext: {}", filename);
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltext(pi, Path.of(filename).getFileName().toString());
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_TEI)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get fulltext for a single page in TEI format")
    @ApiResponse(responseCode = "200", description = "TEI XML for the requested page")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or filename")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "TEI file not found")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getTEI(
            @Parameter(description = "Filename containing the text",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, ContentLibException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltextAsTEI(pi, Path.of(filename).getFileName().toString());
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_MEI)
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags = { "records" }, summary = "Get MEI document for the record")
    @ApiResponse(responseCode = "200", description = "MEI document for the record")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "MEI file not found")
    public String getMEI() throws ContentLibException, DAOException, IOException, IndexUnreachableException, PresentationException {
        try {
            return DataFileTools.loadMei(pi, servletRequest);
        } catch (AccessDeniedException e) {
            throw new ServiceNotAllowedException("Access to MEI file for '" + pi + "' not allowed");
        } catch (RecordNotFoundException e) {
            throw new ContentNotFoundException("Record not found: " + pi);
        }
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_SOURCE)
    @Operation(tags = { "records" }, summary = "Get source files of record")
    @ApiResponse(responseCode = "200", description = "Source file content")
    @ApiResponse(responseCode = "400", description = "Invalid filename")
    @ApiResponse(responseCode = "403", description = "Access to this file is restricted")
    @ApiResponse(responseCode = "404", description = "Source file not found")
    @ApiResponse(responseCode = "500", description = "Content library error")
    public Response getSourceFile(
            @Parameter(description = "Source file name",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        if (!filename.equals(StringTools.stripJS(filename))) {
            throw new ServiceNotAllowedException("Script detected in input");
        }
        // DataFileTools.getDataFilePath calls FileTools.sanitizeFileName which throws
        // IllegalArgumentException for filenames with illegal characters (e.g. control chars).
        // Wrap it as IllegalRequestException so ContentExceptionMapper returns HTTP 400.
        Path path;
        try {
            path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder(), null, filename);
        } catch (IllegalArgumentException e) {
            throw new IllegalRequestException("Invalid file name: " + filename);
        }
        if (!Files.isRegularFile(path)) {
            throw new ContentNotFoundException("Source file " + filename + " not found");
        }

        boolean access = AccessConditionUtils.checkContentFileAccessPermission(pi, servletRequest).isGranted();
        if (!access) {
            throw new ServiceNotAllowedException("Access to source file " + filename + " not allowed");
        }

        String mimeType = "application/octet-stream";
        try {
            mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, filename, path);
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

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_MEDIA)
    @Operation(tags = { "records" }, summary = "Get media files of record")
    @ApiResponse(responseCode = "200", description = "Media file content")
    @ApiResponse(responseCode = "400", description = "Invalid filename")
    @ApiResponse(responseCode = "403", description = "Access to this file is restricted")
    @ApiResponse(responseCode = "404", description = "Media file not found")
    @ApiResponse(responseCode = "500", description = "IO or content library error")
    @CORSBinding
    @MediaResourceBinding
    @RecordFileDownloadBinding
    public Response getMediaFile(
            @Parameter(description = "Media file name",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        if (!filename.equals(StringTools.stripJS(filename))) {
            throw new ServiceNotAllowedException("Script detected in input");
        }

        // DataFileTools.getDataFilePath calls FileTools.sanitizeFileName which throws
        // IllegalArgumentException for filenames with illegal characters (e.g. control chars).
        // Wrap it as IllegalRequestException so ContentExceptionMapper returns HTTP 400.
        Path path;
        try {
            path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
        } catch (IllegalArgumentException e) {
            throw new IllegalRequestException("Invalid file name: " + filename);
        }
        if (!Files.isRegularFile(path)) {
            throw new ContentNotFoundException("Media file " + filename + " not found");
        }

        boolean access = checkMediaFileAccess(filename);
        if (!access) {
            throw new ServiceNotAllowedException("Access to source file " + filename + " not allowed");
        }

        String mimeType = "application/octet-stream";
        try {
            mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, filename, path);
        } catch (IOException e) {
            logger.error("Failed to probe file content type");
        }

        if (FileType.getContentTypeFor(filename).startsWith("model/")) {
            String baseFilename = FilenameUtils.getBaseName(filename);
            Path modelFolder = path.getParent().resolve(baseFilename);
            if (Files.exists(modelFolder)) {
                Path tempFolder = Path.of(DataManager.getInstance().getConfiguration().getTempFolder(), pi + "_3d_" + System.currentTimeMillis());
                try {
                    Files.createDirectories(tempFolder);
                    List<File> fileList = new ArrayList<>();
                    fileList.add(path.toFile());
                    FileTools.listFiles(modelFolder, p -> true).forEach(p -> {
                        fileList.add(p.toFile());
                    });
                    Path zipFile = tempFolder.resolve(FileTools.replaceExtension(Path.of(filename), "zip").toString());
                    FileTools.compressZipFile(fileList, zipFile.toFile(), 9);
                    mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, zipFile.getFileName().toString(), zipFile);
                    StreamingOutput so = out -> {
                        try (InputStream in = Files.newInputStream(zipFile)) {
                            IOUtils.copy(in, out);
                        } finally {
                            FileUtils.deleteQuietly(tempFolder.toFile());
                        }
                    };
                    return Response.ok(so, mimeType).build();
                } catch (IOException e) {
                    logger.error("Error creating zip archive for 3d file {}", path, e);
                    Response.serverError().entity("Error creating zip archive for 3d file " + path);
                }
            }
        }
        StreamingOutput so = out -> {
            try (InputStream in = Files.newInputStream(path)) {
                IOUtils.copy(in, out);
            }
        };
        return Response.ok(so, mimeType).build();

    }

    protected boolean checkMediaFileAccess(String filename) throws IndexUnreachableException, DAOException {

        try {
            MimeType mediaType = MimeType.of(Path.of(filename));
            if (mediaType.isImage()) {
                return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest.getSession(), pi, filename,
                        IPrivilegeHolder.PRIV_DOWNLOAD_IMAGES, NetTools.getIpAddress(servletRequest)).isGranted();
            }
        } catch (IOException e) {
            logger.warn("Unable to read mimetype of file {}", filename);
        }
        return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest.getSession(), pi, filename,
                IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES, NetTools.getIpAddress(servletRequest)).isGranted();
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_CMDI)
    @Operation(tags = { "records" }, summary = "Get cmdi for record file")
    @ApiResponse(responseCode = "200", description = "CMDI metadata for the requested file")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or filename")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "CMDI file not found")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getCMDI(
            @Parameter(description = "Image file name for cmdi",
                    schema = @Schema(pattern = "^[A-Za-z0-9_.-]+$")) @PathParam("filename") String filename,
            @Parameter(description = "Language for CMDI") @QueryParam("lang") final String lang)
            throws ContentLibException, PresentationException, IndexUnreachableException, IOException {
        checkFulltextAccessConditions(pi, filename);

        final Language language =
                DataManager.getInstance()
                        .getLanguageHelper()
                        .getLanguage(lang == null ? BeanUtils.getLocale().getLanguage() : StringTools.stripJS(lang));
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

    @GET
    @jakarta.ws.rs.Path(RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD)
    @Operation(tags = { "records" }, summary = "Download an external resource previously downloaded to the viewer server")
    @ApiResponse(responseCode = "200", description = "Downloaded external resource file")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "404", description = "Resource not found on server")
    @ApiResponse(responseCode = "500", description = "IO error reading resource")
    @RecordFileDownloadBinding
    public Response getDownloadedResource(
            @Parameter(description = "download resource task id") @PathParam("taskId") String taskId,
            @Parameter(description = "file path relative to the download directory") @PathParam("path") String path)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, IllegalRequestException {

        //TODO: check access conditions for some download action

        Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
        Path taskFolder = downloadFolder.resolve(taskId);
        Path resourceFile = taskFolder.resolve(Path.of(path)).normalize();
        if (!resourceFile.startsWith(taskFolder)) {
            throw new IllegalRequestException("May not download from path " + path);
        }
        String mimeType = "application/octet-stream";
        if (Files.isRegularFile(resourceFile)) {
            try {
                mimeType = new MediaResourceHelper(config).setContentHeaders(servletResponse, resourceFile.getFileName().toString(), resourceFile);
            } catch (IOException e) {
                logger.error("Failed to probe file content type");
            }
            StreamingOutput so = out -> {
                try (InputStream in = Files.newInputStream(resourceFile)) {
                    IOUtils.copy(in, out);
                }
            };
            return Response.ok(so, mimeType).build();
        }

        throw new ContentNotFoundException("No resource found at " + resourceFile);
    }

    /**
     * Throw an AccessDenied error if the request doesn't satisfy the access conditions
     * 
     * @param pi
     * @param filename
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
            throw new ServiceNotAllowedException("Access to fulltext file '" + pi + "/" + filename + "' not allowed");
        }
    }

    /**
     * Returns the first file on the given folder path that contains the requested language code in its name. ISO-3 files are preferred, with a
     * fallback to ISO-2.
     *
     * @param folder
     * @param language
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
