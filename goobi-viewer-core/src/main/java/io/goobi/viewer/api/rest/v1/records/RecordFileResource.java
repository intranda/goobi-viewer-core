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
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.StringPair;
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

    private static final Logger logger = LogManager.getLogger(RecordFileResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final String pi;
    private final TextResourceBuilder builder = new TextResourceBuilder();

    /**
     * 
     * @param pi
     */
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
            ServiceNotAllowedException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        StringPair ret = builder.getAltoDocument(pi, filename);
        return ret.getOne();
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_PLAINTEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get plaintext for a single page")
    public String getPlaintext(
            @Parameter(description = "Filename containing the text") @PathParam("filename") String filename)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, ServiceNotAllowedException {
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
            throws PresentationException, IndexUnreachableException, ContentLibException {
        checkFulltextAccessConditions(pi, filename);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
        return builder.getFulltextAsTEI(pi, filename);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_SOURCE)
    @Operation(tags = { "records" }, summary = "Get source files of record")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getSourceFile(
            @Parameter(description = "Source file name") @PathParam("filename") String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        if (!filename.equals(StringTools.stripJS(filename))) {
            throw new ServiceNotAllowedException("Script detected in input");
        }
        Path path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder(), null, filename);
        if (!Files.isRegularFile(path)) {
            throw new ContentNotFoundException("Source file " + filename + " not found");
        }

        boolean access = AccessConditionUtils.checkContentFileAccessPermission(pi, servletRequest).isGranted();
        if (!access) {
            throw new ServiceNotAllowedException("Access to source file " + filename + " not allowed");
        }

        try {
            String contentType = Files.probeContentType(path);
            logger.trace("content type: {}", contentType);
            if (StringUtils.isNotBlank(contentType)) {
                servletResponse.setContentType(contentType);
            }
            servletResponse.setHeader("Content-Disposition", new StringBuilder("attachment;filename=").append(filename).toString());
            servletResponse.setHeader("Content-Length", String.valueOf(Files.size(path)));
        } catch (IOException e) {
            logger.error("Failed to probe file content type");
        }

        return out -> {
            try (InputStream in = Files.newInputStream(path)) {
                IOUtils.copy(in, out);
            }
        };
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_MEDIA)
    @Operation(tags = { "records" }, summary = "Get media files of record")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getMediaFile(
            @Parameter(description = "Media file name") @PathParam("filename") String filename)
            throws ContentLibException, PresentationException, IndexUnreachableException, DAOException {
        if (!filename.equals(StringTools.stripJS(filename))) {
            throw new ServiceNotAllowedException("Script detected in input");
        }
        Path path = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
        if (!Files.isRegularFile(path)) {
            throw new ContentNotFoundException("Media file " + filename + " not found");
        }

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, filename,
                IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES).isGranted();
        if (!access) {
            throw new ServiceNotAllowedException("Access to source file " + filename + " not allowed");
        }

        try {
            String contentType = Files.probeContentType(path);
            logger.trace("content type: {}", contentType);
            if (StringUtils.isNotBlank(contentType)) {
                servletResponse.setContentType(contentType);
            }
            servletResponse.setHeader("Content-Disposition", new StringBuilder("attachment;filename=").append(filename).toString());
            servletResponse.setHeader("Content-Length", String.valueOf(Files.size(path)));
        } catch (IOException e) {
            logger.error("Failed to probe file content type");
        }

        return out -> {
            try (InputStream in = Files.newInputStream(path)) {
                IOUtils.copy(in, out);
            }
        };
    }

    @GET
    @javax.ws.rs.Path(RECORDS_FILES_CMDI)
    @Operation(tags = { "records" }, summary = "Get cmdi for record file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getCMDI(
            @Parameter(description = "Image file name for cmdi") @PathParam("filename") String filename,
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
    @javax.ws.rs.Path(RECORDS_FILES_EXTERNAL_RESOURCE_DOWNLOAD)
    @Operation(tags = { "records" }, summary = "Get cmdi for record file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getDownloadedResource(
            @Parameter(description = "download resource task id") @PathParam("taskId") String taskId,
            @Parameter(description = "file path relative to the download directory") @PathParam("path") String path)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException {

        //TODO: check access conditions for some download action

        Path downloadFolder = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getDownloadFolder("resource"));
        Path taskFolder = downloadFolder.resolve(taskId);
        Path resourceFile = taskFolder.resolve(Path.of(path));
        if (Files.isRegularFile(resourceFile)) {
            try {
                servletResponse.setHeader("Content-Disposition",
                        new StringBuilder("attachment;filename=").append(resourceFile.getFileName()).toString());
                servletResponse.setHeader("Content-Length", String.valueOf(Files.size(resourceFile)));
                String contentType = Files.probeContentType(resourceFile);
                logger.trace("content type: {}", contentType);
                if (StringUtils.isNotBlank(contentType)) {
                    servletResponse.setContentType(contentType);
                }
            } catch (IOException e) {
                logger.error("Failed to probe file content type");
            }
            return out -> {
                try (InputStream in = Files.newInputStream(resourceFile)) {
                    IOUtils.copy(in, out);
                }
            };
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
            access = AccessConditionUtils.checkAccess(servletRequest, "text", pi, filename, false).isGranted();
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
