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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.language.Language;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * Resource for delivering content documents such as ALTO and plain full-text.
 */
@Path("/content")
@ViewerRestServiceBinding
public class ContentResource {

    private static final Logger logger = LoggerFactory.getLogger(ContentResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public ContentResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected ContentResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * API method for retrieving any type of content by its relative path within its data repository.
     * 
     * @param pi Record identifier
     * @param dataRepository Absolute path of the data repository
     * @param filePath File path relative to the data repository
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/document/{dataRepository}/{contentFolder}/{pi}/{fileName}")
    @Produces({ MediaType.TEXT_XML })
    public String getContentDocument(@PathParam("dataRepository") String dataRepository, @PathParam("contentFolder") String contentFolder,
            @PathParam("pi") String pi, @PathParam("fileName") String fileName)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
        if ("-".equals(dataRepository)) {
            dataRepository = null;
        }
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), contentFolder, pi, fileName);
        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), Helper.DEFAULT_ENCODING);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }

    /**
     * @param pi
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/alto/{pi}/{fileName}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getAltoDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        String filePath = DataManager.getInstance().getConfiguration().getAltoFolder() + '/' + pi + '/' + fileName;

        logger.trace(filePath);
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        if (file != null && Files.isRegularFile(file)) {
            try {
                Document doc = FileTools.readXmlFile(file);
                return new XMLOutputter().outputString(doc);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException("Resource not found");

    }

    /**
     * @param pi Record identifier
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/fulltext/{pi}/{fileName}")
    @Produces({ MediaType.TEXT_HTML })
    public String getFulltextDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        String filePath = DataManager.getInstance().getConfiguration().getFulltextFolder() + '/' + pi + '/' + fileName;

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if (!access) {
            throw new ContentNotFoundException("No permission found");
        }

        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        ;
        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), Helper.DEFAULT_ENCODING);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            file = getAltoFile(pi, fileName.replace(".txt", ".xml"), dataRepository);
            if (file != null && Files.isRegularFile(file)) {
                try {
                    AltoDocument alto = AltoDocument.getDocumentFromFile(file.toFile());
                    return alto.getContent();
                } catch (IOException | JDOMException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }

    /**
     * @param pi
     * @param lang
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException * @throws DAOException
     * @throws ContentNotFoundException
     * @throws IOException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/tei/{pi}/{lang}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getTeiDocument(@PathParam("pi") String pi, @PathParam("lang") String langCode)
            throws PresentationException, IndexUnreachableException, DAOException, ContentNotFoundException, IOException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getTeiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(teiPath)) {
            // This will return the file with the requested language or alternatively the first file in the TEI folder
            try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                filePath =
                        teiFiles.filter(path -> path.getFileName().toString().endsWith("_tei_" + language.getIsoCode() + ".xml")).findFirst().orElse(
                                null);
                // Try old naming scheme without the format name in the file name
                if (filePath == null) {
                    filePath =
                            teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(
                                    null);
                }
            }
        }

        if (filePath != null) {
            boolean access =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, servletRequest);
            if (!access) {
                throw new ContentNotFoundException("No permission found");
            }

            if (Files.isRegularFile(filePath)) {
                try {
                    Document doc = FileTools.readXmlFile(filePath);
                    return new XMLOutputter().outputString(doc);
                } catch (FileNotFoundException e) {
                    logger.debug(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                } catch (JDOMException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }

    /**
     * @param pi
     * @param lang
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException * @throws DAOException
     * @throws ContentNotFoundException
     * @throws IOException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/cmdi/{pi}/{lang}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getCmdiDocument(@PathParam("pi") String pi, @PathParam("lang") String langCode)
            throws PresentationException, IndexUnreachableException, DAOException, ContentNotFoundException, IOException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path cmdiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getCmdiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(cmdiPath)) {
            // This will return the file with the requested language or alternatively the first file in the CMDI folder
            try (Stream<java.nio.file.Path> cmdiFiles = Files.list(cmdiPath)) {
                filePath = cmdiFiles.filter(path -> path.getFileName().toString().endsWith("_cmdi_" + language.getIsoCode() + ".xml"))
                        .findFirst()
                        .orElse(null);
                // Try old naming scheme without the format name in the file name
                if (filePath == null) {
                    filePath =
                            cmdiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(
                                    null);
                }
            }
        }
        if (filePath != null) {
            boolean access =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, servletRequest);
            if (!access) {
                throw new ContentNotFoundException("No permission found");
            }

            if (Files.isRegularFile(filePath)) {
                try {
                    Document doc = FileTools.readXmlFile(filePath);
                    return new XMLOutputter().outputString(doc);
                } catch (FileNotFoundException e) {
                    logger.debug(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                } catch (JDOMException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        throw new ContentNotFoundException("Resource not found");
    }

    public static URI getAltoURI(String pi, String filename) throws URISyntaxException {
        URI uri = new URI(DataManager.getInstance().getConfiguration().getRestApiUrl());
        uri = uri.resolve("content/alto/" + pi + "/" + filename);
        return uri;
    }

    public static URI getFulltextURI(String pi, String filename) throws URISyntaxException {
        URI uri = new URI(DataManager.getInstance().getConfiguration().getRestApiUrl());
        uri = uri.resolve("content/fulltext/" + pi + "/" + filename);
        return uri;
    }

    public static URI getTEIURI(String pi, String locale) {
        URI uri = null;
        try {
            uri = new URI(DataManager.getInstance().getConfiguration().getRestApiUrl());
            uri = uri.resolve("content/tei/" + pi + "/" + locale);
        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
        }
        return uri;
    }

    public static URI getCMDIURI(String pi, String locale) {
        URI uri = null;
        try {
            uri = new URI(DataManager.getInstance().getConfiguration().getRestApiUrl());
            uri = uri.resolve("content/cmdi/" + pi + "/" + locale);
        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
        }
        return uri;
    }

    public static String getDataRepository(String pi) throws PresentationException, IndexUnreachableException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        return dataRepository;
    }

    public static java.nio.file.Path getAltoFile(String pi, String fileName, String dataRepository)
            throws PresentationException, IndexUnreachableException {
        String filePath = DataManager.getInstance().getConfiguration().getAltoFolder() + '/' + pi + '/' + fileName;
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        return file;
    }

    public static java.nio.file.Path getFulltextFile(String pi, String fileName, String dataRepository)
            throws PresentationException, IndexUnreachableException {
        String filePath = DataManager.getInstance().getConfiguration().getFulltextFolder() + '/' + pi + '/' + fileName;
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        return file;
    }

    public static java.nio.file.Path getTEIFile(String pi, String langCode, String dataRepository)
            throws PresentationException, IndexUnreachableException, IOException {
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getTeiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(teiPath)) {
            // This will return the file with the requested language or alternatively the first file in the TEI folder
            try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                filePath = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(
                        null);
            }
        }
        return filePath;
    }

    public static List<java.nio.file.Path> getTEIFiles(String pi, String dataRepository) {
        java.nio.file.Path teiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getTeiFolder(), pi);
        List<java.nio.file.Path> filePaths = new ArrayList<>();
        if (Files.exists(teiPath)) {
            // This will return the file with the requested language or alternatively the first file in the TEI folder
            try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                filePaths = teiFiles.filter(path -> path.getFileName().toString().matches(".*_[a-z]{1,3}\\.xml")).collect(Collectors.toList());
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }
        return filePaths;
    }

    public static java.nio.file.Path getCMDIFile(String pi, String langCode, String dataRepository)
            throws PresentationException, IndexUnreachableException, IOException {
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path cmdiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getCmdiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(cmdiPath)) {
            // This will return the file with the requested language or alternatively the first file in the CMDI folder
            try (Stream<java.nio.file.Path> cmdiFiles = Files.list(cmdiPath)) {
                filePath = cmdiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(
                        null);
            }
        }
        return filePath;
    }

    public static List<java.nio.file.Path> getCMDIFiles(String pi, String dataRepository) {
        java.nio.file.Path teiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getCmdiFolder(), pi);
        List<java.nio.file.Path> filePaths = new ArrayList<>();
        if (Files.exists(teiPath)) {
            // This will return the file with the requested language or alternatively the first file in the TEI folder
            try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                filePaths = teiFiles.filter(path -> path.getFileName().toString().matches(".*_[a-z]{1,3}\\.xml")).collect(Collectors.toList());
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }
        return filePaths;
    }

    /**
     * @param string
     * @return
     */
    public static Optional<String> getLanguage(String filename) {
        String regex = "([a-z]{1,3})\\.[a-z]+";
        Matcher matcher = Pattern.compile(regex).matcher(filename);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }
}
