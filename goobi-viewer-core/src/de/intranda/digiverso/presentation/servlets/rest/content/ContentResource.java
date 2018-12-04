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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.tei.TEIBuilder;
import de.intranda.digiverso.ocr.tei.convert.HtmlToTEIConvert;
import de.intranda.digiverso.ocr.tei.header.Identifier;
import de.intranda.digiverso.ocr.tei.header.Person;
import de.intranda.digiverso.ocr.tei.header.TEIHeaderBuilder;
import de.intranda.digiverso.ocr.tei.header.Title;
import de.intranda.digiverso.ocr.xml.DocumentReader;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.language.Language;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;

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
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/document/{dataRepository}/{contentFolder}/{pi}/{fileName}")
    @Produces({ MediaType.TEXT_XML })
    public String getContentDocument(@PathParam("dataRepository") String dataRepository, @PathParam("contentFolder") String contentFolder,
            @PathParam("pi") String pi, @PathParam("fileName") String fileName) throws PresentationException, IndexUnreachableException, DAOException,
            MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {
        setResponseHeader("");
        checkAccess(pi, fileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        if ("-".equals(dataRepository)) {
            dataRepository = null;
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
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/alto/{pi}")
    @Produces({ "application/zip" })
    public StreamingOutput getAltoDocument(@PathParam("pi") String pi)
            throws PresentationException, ContentLibException, IndexUnreachableException, DAOException, MalformedURLException {

        setResponseHeader(pi + ".zip");
        checkAccess(pi, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        java.nio.file.Path altoPath = getPath(pi, DataManager.getInstance().getConfiguration().getAltoFolder(), null, null);
        java.nio.file.Path altoPathCrowd = getPath(pi, DataManager.getInstance().getConfiguration().getAltoFolder() + "_crowd", null, null);

        try {
            List<File> altoFilePaths =
                    getFiles(altoPathCrowd, altoPath, "(?i).*\\.(alto|xml)").stream().map(java.nio.file.Path::toFile).collect(Collectors.toList());

            File tempFile = new File(DataManager.getInstance().getConfiguration().getTempFolder(), pi + "_alto.zip");
            if (!tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs()) {
                throw new ContentLibException("Not allowed to create temp file directory " + tempFile.getParentFile());
            }

            FileTools.compressZipFile(altoFilePaths, tempFile, 9);
            return (out) -> {
                try (FileInputStream in = new FileInputStream(tempFile)) {
                    FileTools.copyStream(out, in);
                    //                  IOUtils.copyLarge(in, out);   
                } finally {
                    out.flush();
                    out.close();
                }
            };

        } catch (IOException e) {
            throw new ContentNotFoundException("Resource not found or not accessible", e);
        }
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
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/alto/{pi}/{fileName}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getAltoDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName) throws PresentationException,
            IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {

        setResponseHeader("");
        checkAccess(pi, fileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        java.nio.file.Path file = getPath(pi, DataManager.getInstance().getConfiguration().getAltoFolder() + "_crowd",
                DataManager.getInstance().getConfiguration().getAltoFolder(), fileName);

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
     * @param pi
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws IOException
     * @throws ContentNotFoundException
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/fulltext/{pi}")
    @Produces({ "application/zip" })
    public StreamingOutput getFulltextDocument(@PathParam("pi") String pi)
            throws PresentationException, ContentLibException, IndexUnreachableException, DAOException, IOException {

        setResponseHeader(pi + ".zip");
        checkAccess(pi, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        Map<java.nio.file.Path, String> fileMap = getFulltext(pi);

        try {

            File tempFile = new File(DataManager.getInstance().getConfiguration().getTempFolder(), pi + "_text.zip");
            if (!tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs()) {
                throw new ContentLibException("Not allowed to create temp file directory " + tempFile.getParentFile());
            }

            FileTools.compressZipFile(fileMap, tempFile, 9);
            return (out) -> {
                try (FileInputStream in = new FileInputStream(tempFile)) {
                    FileTools.copyStream(out, in);
                    //                  IOUtils.copyLarge(in, out);   
                } finally {
                    out.flush();
                    out.close();
                }
            };

        } catch (IOException e) {
            throw new ContentNotFoundException("Resource not found or not accessible", e);
        }

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
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/fulltext/{pi}/{fileName}")
    @Produces({ MediaType.TEXT_HTML })
    public String getFulltextDocument(@PathParam("pi") String pi, @PathParam("fileName") String fileName) throws PresentationException,
            IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {

        setResponseHeader("");
        checkAccess(pi, fileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        return getFulltext(pi, fileName);
    }

    /**
     * @param pi
     * @param lang
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException * @throws DAOException
     * @throws JDOMException
     * @throws ContentNotFoundException
     * @throws IOException
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/tei/{pi}/{filename}/{lang}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getFulltextAsTEI(@PathParam("pi") String pi, @PathParam("filename") String filename, @PathParam("lang") String language)
            throws PresentationException, ContentLibException, IndexUnreachableException, DAOException, MalformedURLException, JDOMException {

        setResponseHeader("");
        checkAccess(pi, filename, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (solrDoc != null) {

            String text = getFulltext(pi, filename);
            HtmlToTEIConvert textConverter = new HtmlToTEIConvert();
            text = textConverter.convert(text);

            TEIHeaderBuilder header = createTEIHeader(solrDoc);

            TEIBuilder builder = new TEIBuilder();
            Document xmlDoc = builder.build(header, text);
            return DocumentReader.getAsString(xmlDoc, Format.getPrettyFormat());

        } else {
            throw new ContentNotFoundException("No document found with pi " + pi);
        }

    }

    /**
     * @param pi
     * @param lang
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException * @throws DAOException
     * @throws ContentNotFoundException
     * @throws IOException
     * @throws ServiceNotAllowedException
     * @throws JDOMException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/tei/{pi}/{lang}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getTeiDocument(@PathParam("pi") String pi, @PathParam("lang") String langCode) throws PresentationException,
            IndexUnreachableException, DAOException, ContentNotFoundException, IOException, ServiceNotAllowedException, JDOMException {

        setResponseHeader("");
        checkAccess(pi, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);

        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath = getPath(pi, DataManager.getInstance().getConfiguration().getTeiFolder(), null, null);
        java.nio.file.Path filePath = getDocumentLanguageVersion(teiPath, language);

        if (filePath != null && Files.isRegularFile(filePath)) {

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
        } else {

            SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            if (solrDoc != null) {

                Map<java.nio.file.Path, String> fulltexts = getFulltext(pi);

                if (!fulltexts.isEmpty()) {

                    TEIBuilder builder = new TEIBuilder();
                    TEIHeaderBuilder header = createTEIHeader(solrDoc);
                    HtmlToTEIConvert textConverter = new HtmlToTEIConvert();

                    List<String> pages = fulltexts.values()
                            .stream()
                            .filter(text -> StringUtils.isNotBlank(text))
                            .map(textConverter::convert)
                            .collect(Collectors.toList());

                    Document xmlDoc = builder.build(header, pages);
                    return DocumentReader.getAsString(xmlDoc, Format.getPrettyFormat());
                }

            } else {
                throw new ContentNotFoundException("No document found with pi " + pi);
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
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @GET
    @Path("/cmdi/{pi}/{lang}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getCmdiDocument(@PathParam("pi") String pi, @PathParam("lang") String langCode)
            throws PresentationException, IndexUnreachableException, DAOException, ContentNotFoundException, IOException, ServiceNotAllowedException {
        
        setResponseHeader("");
        checkAccess(pi, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path cmdiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getCmdiFolder(), pi);
        java.nio.file.Path filePath = getDocumentLanguageVersion(cmdiPath, language);
        if (filePath != null) {

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
     * @param fileName
     * @param privilegeHolder
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ServiceNotAllowedException
     */
    public void checkAccess(String pi, String fileName, String privilegeHolder)
            throws IndexUnreachableException, DAOException, ServiceNotAllowedException {
        boolean access =
                AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, fileName, privilegeHolder);
        if (!access) {
            throw new ServiceNotAllowedException("No permission found");
        }
    }

    /**
     * @param pi
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ServiceNotAllowedException
     */
    public void checkAccess(String pi, String privilegeHolder) throws IndexUnreachableException, DAOException, ServiceNotAllowedException {
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, privilegeHolder, servletRequest);
        if (!access) {
            throw new ServiceNotAllowedException("No permission found");
        }
    }

    /**
     * @param pi
     */
    public void setResponseHeader(String filename) {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            if (StringUtils.isNotBlank(filename)) {
                servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            }
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
    }

    /**
     * @param pi
     * @param fileName
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     */
    public String getFulltext(String pi, String fileName) throws PresentationException, IndexUnreachableException, ContentNotFoundException {
        java.nio.file.Path file = getPath(pi, DataManager.getInstance().getConfiguration().getFulltextFolder() + "_crowd",
                DataManager.getInstance().getConfiguration().getFulltextFolder(), fileName);
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
            file = getPath(pi, DataManager.getInstance().getConfiguration().getAltoFolder() + "_crowd",
                    DataManager.getInstance().getConfiguration().getAltoFolder(), fileName.replaceAll("(i?)\\.txt", ".xml"));
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
     * @return
     * @throws IOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Map<java.nio.file.Path, String> getFulltext(String pi) throws IOException, PresentationException, IndexUnreachableException {
        Map<java.nio.file.Path, String> fileMap;
        List<java.nio.file.Path> fulltextFiles = getFiles(pi, DataManager.getInstance().getConfiguration().getFulltextFolder() + "_crowd",
                DataManager.getInstance().getConfiguration().getFulltextFolder(), "(i?).*\\.txt");

        if (!fulltextFiles.isEmpty()) {
            fileMap = fulltextFiles.stream().collect(Collectors.toMap(p -> p, p -> {
                try {
                    return FileTools.getStringFromFile(p.toFile(), Helper.DEFAULT_ENCODING);
                } catch (IOException e) {
                    logger.error("Error reading file " + p, e);
                    return "";
                }
            }));
        } else {
            List<java.nio.file.Path> altoFiles = getFiles(pi, DataManager.getInstance().getConfiguration().getAltoFolder() + "_crowd",
                    DataManager.getInstance().getConfiguration().getAltoFolder(), "(i?).*\\.(alto|xml)");
            fileMap = altoFiles.stream().collect(Collectors.toMap(p -> Paths.get(p.toString().replaceAll("(i?)\\.(alto|xml)", ".txt")), p -> {
                try {
                    return AltoDocument.getDocumentFromFile(p.toFile()).getContent();
                } catch (IOException | JDOMException e) {
                    logger.error("Error reading file " + p, e);
                    return "";
                }
            }));
        }
        return fileMap;
    }

    public List<java.nio.file.Path> getFiles(String pi, String foldername, String altFoldername, String filter)
            throws IOException, PresentationException, IndexUnreachableException {

        java.nio.file.Path folder1 = getPath(pi, foldername, null, null);
        java.nio.file.Path folder2 = getPath(pi, altFoldername, null, null);

        return getFiles(folder1, folder2, filter);
    }

    /**
     * @param filePath
     * @return
     * @throws IOException
     */
    public List<java.nio.file.Path> getFiles(java.nio.file.Path folder, java.nio.file.Path altFolder, String filter) throws IOException {

        List<java.nio.file.Path> files = new ArrayList<>();

        if (folder != null && Files.isDirectory(folder)) {
            files = Files.list(folder)
                    .filter(p -> p.getFileName().toString().toLowerCase().matches(StringUtils.isBlank(filter) ? ".*" : filter))
                    .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        if (altFolder != null && Files.isDirectory(altFolder)) {
            List<java.nio.file.Path> altFiles = Files.list(altFolder)
                    .filter(p -> p.getFileName().toString().toLowerCase().matches(StringUtils.isBlank(filter) ? ".*" : filter))
                    .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                    .collect(Collectors.toList());

            files = new ArrayList<>(Stream.of(files, altFiles)
                    .flatMap(List::stream)
                    .collect(Collectors.toMap(java.nio.file.Path::getFileName, path -> path,
                            (java.nio.file.Path path1, java.nio.file.Path path2) -> path1 == null ? path2 : path1))
                    .values());
        }
        return files;
    }

    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public java.nio.file.Path getPath(String pi, String foldername, String altFoldername, String filename)
            throws PresentationException, IndexUnreachableException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        java.nio.file.Path filePath = Paths.get(Helper.getRepositoryPath(dataRepository), foldername, pi);
        if (StringUtils.isNotBlank(filename)) {
            filePath = filePath.resolve(filename);
        }
        if (StringUtils.isNotBlank(altFoldername) && !Files.exists(filePath)) {
            return getPath(pi, altFoldername, null, filename);
        }

        return filePath;
    }

    /**
     * @param solrDoc
     * @return
     */
    public TEIHeaderBuilder createTEIHeader(SolrDocument solrDoc) {
        TEIHeaderBuilder header = new TEIHeaderBuilder();

        Optional.ofNullable(solrDoc.getFieldValue(SolrConstants.LABEL))
                .map(Object::toString)
                .map(Title::new)
                .ifPresent(title -> header.setTitle(title));

        List<String> authors = Optional.ofNullable(solrDoc.getFieldValues("MD_AUTHOR")).orElse(Collections.emptyList()).stream().map(Object::toString).collect(Collectors.toList());
        for (String name : authors) {
            if (name.contains(",")) {
                String[] parts = name.split(",");
                header.addAuthor(new Person(parts[1], parts[0]));
            } else {
                header.addAuthor(new Person(name));
            }
        }

        Optional.ofNullable(solrDoc.getFieldValue(SolrConstants.PI))
                .map(Object::toString)
                .map(Identifier::new)
                .ifPresent(id -> header.addIdentifier(id));
        return header;
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
    static java.nio.file.Path getDocumentLanguageVersion(java.nio.file.Path folder, Language language) throws IOException {
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

    public static URI getAltoURI(String pi, String filename) throws URISyntaxException, ViewerConfigurationException {
        URI uri = new URI(DataManager.getInstance().getConfiguration().getRestApiUrl());
        uri = uri.resolve("content/alto/" + pi + "/" + filename);
        return uri;
    }

    public static URI getFulltextURI(String pi, String filename) throws URISyntaxException, ViewerConfigurationException {
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
        } catch (ViewerConfigurationException e) {
            logger.error(e.toString());
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
        } catch (ViewerConfigurationException e) {
            logger.error(e.toString());
        }
        return uri;
    }

    public static String getDataRepository(String pi) throws PresentationException, IndexUnreachableException {
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        return dataRepository;
    }

    public static java.nio.file.Path getAltoFile(String pi, String fileName, String dataRepository) {
        String filePath = DataManager.getInstance().getConfiguration().getAltoFolder() + '/' + pi + '/' + fileName;
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        return file;
    }

    public static java.nio.file.Path getFulltextFile(String pi, String fileName, String dataRepository) {
        String filePath = DataManager.getInstance().getConfiguration().getFulltextFolder() + '/' + pi + '/' + fileName;
        java.nio.file.Path file = Paths.get(Helper.getRepositoryPath(dataRepository), filePath);
        return file;
    }

    public static java.nio.file.Path getTEIFile(String pi, String langCode, String dataRepository) throws IOException {
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getTeiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(teiPath)) {
            // This will return the file with the requested language or alternatively the first file in the TEI folder
            try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                filePath = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml"))
                        .findFirst()
                        .orElse(null);
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

    public static java.nio.file.Path getCMDIFile(String pi, String langCode, String dataRepository) throws IOException {
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path cmdiPath =
                Paths.get(Helper.getRepositoryPath(dataRepository), DataManager.getInstance().getConfiguration().getCmdiFolder(), pi);
        java.nio.file.Path filePath = null;
        if (Files.exists(cmdiPath)) {
            // This will return the file with the requested language or alternatively the first file in the CMDI folder
            try (Stream<java.nio.file.Path> cmdiFiles = Files.list(cmdiPath)) {
                filePath = cmdiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml"))
                        .findFirst()
                        .orElse(null);
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
        }
        return Optional.empty();
    }

}
