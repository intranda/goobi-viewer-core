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
package io.goobi.viewer.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.Dataset;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Utility class for retrieving data folders, data files and source files.
 *
 */
public class DataFileTools {

    private static final Logger logger = LogManager.getLogger(DataFileTools.class);

    /**
     * Hidden constructor to avoid instantiating the class
     */
    private DataFileTools() {
    }

    /**
     * Retrieves the path to viewer home or repositories root, depending on the record. Used to generate a specific task client query parameter.
     *
     * @param pi Record identifier
     * @return The root folder path of the data repositories; viewer home if none are used
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getDataRepositoryPathForRecord(String pi) throws PresentationException, IndexUnreachableException {
        String dataRepositoryPath = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);
        return getDataRepositoryPath(dataRepositoryPath);
    }

    /**
     * Returns the absolute path to the data repository with the given name (including a slash at the end). Package private to discourage direct usage
     * by clients.
     *
     * @param dataRepositoryPath Data repository name or absolute path
     * @return
     * @should return correct path for empty data repository
     * @should return correct path for data repository name
     * @should return correct path for absolute data repository path
     */
    static String getDataRepositoryPath(String dataRepositoryPath) {
        if (StringUtils.isBlank(dataRepositoryPath)) {
            return DataManager.getInstance().getConfiguration().getViewerHome();
        }

        if (Paths.get(FileTools.adaptPathForWindows(dataRepositoryPath)).isAbsolute()) {
            return dataRepositoryPath + '/';
        }

        return DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + dataRepositoryPath + '/';
    }

    /**
     * Constructs the media folder path for the given pi, either directly in viewer-home or within a data repository
     *
     * @param pi The work PI. This is both the actual name of the folder and the identifier used to look up data repository in solr
     * @return A Path to the media folder for the given PI
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getMediaFolder(String pi) throws PresentationException, IndexUnreachableException {
        return getDataFolder(pi, DataManager.getInstance().getConfiguration().getMediaFolder());
    }

    /**
     * Returns a map of Paths for each data folder name passed as an argument.
     *
     * @param pi The record identifier. This is both the actual name of the folder and the identifier used to look up data repository in Solr
     * @return HashMap<dataFolderName,Path>
     * @should return all requested data folders
     * @param dataFolderNames a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Map<String, Path> getDataFolders(String pi, String... dataFolderNames) throws PresentationException, IndexUnreachableException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (dataFolderNames == null) {
            throw new IllegalArgumentException("dataFolderNames may not be null");
        }

        String dataRepositoryName = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);

        Map<String, Path> ret = new HashMap<>(dataFolderNames.length);
        for (String dataFolderName : dataFolderNames) {
            ret.put(dataFolderName, getDataFolder(pi, dataFolderName, dataRepositoryName));
        }

        return ret;
    }

    /**
     * Constructs the folder path for data of the given pi, either directly in viewer-home or within a data repository.
     *
     * @param pi The record identifier. This is both the actual name of the folder and the identifier used to look up data repository in Solr
     * @param dataFolderName the data folder within the repository; e.g 'media' or 'alto'
     * @return A Path to the data folder for the given PI
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getDataFolder(String pi, String dataFolderName) throws PresentationException, IndexUnreachableException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);
        return getDataFolder(pi, dataFolderName, dataRepository);
    }

    /**
     * Returns the data folder path for the given record identifier. To be used in clients that already possess the data repository name.
     *
     * @param pi a {@link java.lang.String} object.
     * @param dataFolderName a {@link java.lang.String} object.
     * @param dataRepositoryFolder Absolute path to the data repository folder or just the folder name
     * @should return correct folder if no data repository used
     * @should return correct folder if data repository used
     * @return a {@link java.nio.file.Path} object.
     */
    public static Path getDataFolder(String pi, String dataFolderName, String dataRepositoryFolder) {
        Path repository;
        // TODO Find a way to use absolute repo paths in unit tests
        if (StringUtils.isBlank(dataRepositoryFolder)) {
            repository = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        } else if (Paths.get(FileTools.adaptPathForWindows(dataRepositoryFolder)).isAbsolute()) {
            repository = Paths.get(dataRepositoryFolder);
        } else {
            repository = Paths.get(DataManager.getInstance().getConfiguration().getDataRepositoriesHome(), dataRepositoryFolder);
        }

        return repository.resolve(dataFolderName).resolve(pi);
    }

    /**
     * Returns the path to the data file (if file name given) or data folder for the given record identifier.
     *
     * @param pi Record identifier
     * @param dataFolderName Name of the data folder (e.g. 'alto') - first choice
     * @param altDataFolderName Name of the data folder - second choice
     * @param fileName Optional name of the content file
     * @return Path to the requested file or folder
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getDataFilePath(String pi, String dataFolderName, String altDataFolderName, String fileName)
            throws PresentationException, IndexUnreachableException {
        // Make sure fileName is a pure file name and not a path
        fileName = sanitizeFileName(fileName);

        java.nio.file.Path dataFolderPath = getDataFolder(pi, dataFolderName);
        if (StringUtils.isNotBlank(fileName)) {
            dataFolderPath = dataFolderPath.resolve(fileName);
        }

        // If selected path doesn't exist in the primary data folder, call again with alternative data folder
        if (StringUtils.isNotBlank(altDataFolderName) && !Files.exists(dataFolderPath)) {
            return getDataFilePath(pi, altDataFolderName, null, fileName);
        }

        return dataFolderPath;
    }

    /**
     * Removes any path elements from the given file name.
     *
     * @param fileName
     * @return Lowest level file name
     * @should remove everything but the file name from given path
     */
    static String sanitizeFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return fileName;
        }

        return Paths.get(fileName).getFileName().toString();
    }

    /**
     * <p>
     * getDataFilePath.
     * </p>
     *
     * @param pi Record identifier
     * @param relativeFilePath File path relative to data repositories root
     * @return File represented by the relative file path
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getDataFilePath(String pi, String relativeFilePath) throws PresentationException, IndexUnreachableException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        String dataRepositoryName = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);
        String dataRepositoryPath = getDataRepositoryPath(dataRepositoryName);

        return Paths.get(dataRepositoryPath, relativeFilePath);
    }

    /**
     * Returns the absolute path to the source (METS/LIDO) file with the given file name.
     *
     * @param fileName a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getSourceFilePath(String fileName, String format) throws PresentationException, IndexUnreachableException {
        String pi = FilenameUtils.getBaseName(fileName);
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);
        return getSourceFilePath(fileName, dataRepository, format);
    }

    /**
     * Returns the absolute path to the source (METS/LIDO/DENKXWEB/DUBLINCORE) file with the given file name.
     *
     * @param fileName a {@link java.lang.String} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @should construct METS file path correctly
     * @should construct METS_MARC file path correctly
     * @should construct LIDO file path correctly
     * @should construct DenkXweb file path correctly
     * @should throw IllegalArgumentException if fileName is null
     * @should throw IllegalArgumentException if format is unknown
     * @return a {@link java.lang.String} object.
     */
    public static String getSourceFilePath(String fileName, String dataRepository, String format) {
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName may not be null or empty");
        }
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("format may not be null or empty (file name: " + fileName + ")");
        }
        switch (format) {
            case SolrConstants.SOURCEDOCFORMAT_METS:
            case SolrConstants.SOURCEDOCFORMAT_METS_MARC:
            case SolrConstants.SOURCEDOCFORMAT_LIDO:
            case SolrConstants.SOURCEDOCFORMAT_DENKXWEB:
            case SolrConstants.SOURCEDOCFORMAT_WORLDVIEWS:
            case SolrConstants.SOURCEDOCFORMAT_DUBLINCORE:
                break;
            default:
                throw new IllegalArgumentException("format must be: METS | LIDO | DENKXWEB | DUBLINCORE | WORLDVIEWS");
        }

        StringBuilder sb = new StringBuilder(getDataRepositoryPath(dataRepository));
        switch (format) {
            case SolrConstants.SOURCEDOCFORMAT_METS:
            case SolrConstants.SOURCEDOCFORMAT_METS_MARC:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                break;
            case SolrConstants.SOURCEDOCFORMAT_LIDO:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                break;
            case SolrConstants.SOURCEDOCFORMAT_DENKXWEB:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedDenkxwebFolder());
                break;
            case SolrConstants.SOURCEDOCFORMAT_DUBLINCORE:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedDublinCoreFolder());
                break;
            case SolrConstants.SOURCEDOCFORMAT_WORLDVIEWS:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                break;
            default:
                break;
        }
        sb.append('/').append(fileName);

        return sb.toString();
    }

    /**
     * <p>
     * getTextFilePath.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param fileName a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @should return correct path
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getTextFilePath(String pi, String fileName, String format) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName may not be null or empty");
        }
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("format may not be null or empty");
        }

        String dataFolderName = null;
        switch (format) {
            case SolrConstants.FILENAME_ALTO:
                dataFolderName = DataManager.getInstance().getConfiguration().getAltoFolder();
                break;
            case SolrConstants.FILENAME_FULLTEXT:
                dataFolderName = DataManager.getInstance().getConfiguration().getFulltextFolder();
                break;
            case SolrConstants.FILENAME_TEI:
                dataFolderName = DataManager.getInstance().getConfiguration().getTeiFolder();
                break;
            default:
                break;
        }

        return getDataFilePath(pi, dataFolderName, null, fileName).toAbsolutePath().toString();
    }

    /**
     * <p>
     * getTextFilePath.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param relativeFilePath ALTO/text file path relative to the data folder
     * @return a {@link java.nio.file.Path} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getTextFilePath(String pi, String relativeFilePath) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(relativeFilePath)) {
            return null;
        }

        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);
        return Paths.get(getDataRepositoryPath(dataRepository), relativeFilePath);
    }

    /**
     * Loads plain full-text via the REST service. ALTO is is a fallback (and converted to plain text, with a plain text fallback.
     *
     * @param altoFilePath ALTO file path relative to the repository root (e.g. "alto/PPN123/00000001.xml")
     * @param fulltextFilePath plain full-text file path relative to the repository root (e.g. "fulltext/PPN123/00000001.xml")
     * @param mergeLineBreakWords a boolean.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @should load fulltext from alto correctly
     * @should load fulltext from plain text correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String loadFulltext(String altoFilePath, String fulltextFilePath, boolean mergeLineBreakWords, HttpServletRequest request)
            throws IOException, IndexUnreachableException {
        TextResourceBuilder builder = new TextResourceBuilder();
        if (fulltextFilePath != null) {
            // Plain full-text file
            try {
                String fulltext = builder.getFulltext(FileTools.getBottomFolderFromPathString(fulltextFilePath),
                        FileTools.getFilenameFromPathString(fulltextFilePath));
                if (fulltext != null) {
                    return fulltext;
                }
            } catch (ContentNotFoundException e) {
                loadFromApiURl(fulltextFilePath);
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            }

        }
        if (altoFilePath != null) {
            // ALTO file
            try {
                StringPair alto = loadAlto(altoFilePath);
                if (alto != null) {
                    return ALTOTools.getFulltext(alto.getOne(), alto.getTwo(), mergeLineBreakWords, request);
                }
            } catch (ContentNotFoundException e) {
                throw new FileNotFoundException(e.getMessage());
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            }
        }

        return null;
    }

    public static String loadFromApiURl(String fulltextFilePath) throws FileNotFoundException {
        //try loading from content api url (same source as image content)
        try {
            String filename = FileTools.getFilenameFromPathString(fulltextFilePath);
            String pi = FileTools.getBottomFolderFromPathString(fulltextFilePath);
            return DataManager.getInstance().getRestApiManager().getContentApiManager().map(urls -> {
                return urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_PLAINTEXT).params(pi, filename).build();
            })
                    .map(NetTools::callUrlGET)
                    .filter(array -> NetTools.isStatusOk(array[0]))
                    .map(array -> array[1])
                    .orElseThrow(() -> new ContentNotFoundException("Resource not found"));
        } catch (ContentNotFoundException e1) {
            return "";
        }
    }

    /**
     *
     * @param altoFilePath
     * @return StringPair(ALTO,charset)
     * @throws ContentNotFoundException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws FileNotFoundException
     * @should throw ContentNotFoundException
     */
    public static StringPair loadAlto(String altoFilePath)
            throws ContentNotFoundException, IndexUnreachableException, PresentationException, FileNotFoundException {
        if (altoFilePath == null) {
            return null;
        }

        String filename = FileTools.getFilenameFromPathString(altoFilePath);
        String pi = FileTools.getBottomFolderFromPathString(altoFilePath);
        // ALTO file
        try {
            TextResourceBuilder builder = new TextResourceBuilder();
            return builder.getAltoDocument(pi, filename);
        } catch (ContentNotFoundException e) {
            return new StringPair(DataManager.getInstance().getRestApiManager().getContentApiManager()
                    .map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_ALTO).params(pi, filename).build())
                    .map(NetTools::callUrlGET)
                    .filter(array -> NetTools.isStatusOk(array[0]))
                    .map(array -> array[1])
                    .orElseThrow(() -> new ContentNotFoundException("Resource not found")), null);
        }
    }

    /**
     * <p>
     * loadTei.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String loadTei(String pi, String language) throws FileNotFoundException, IOException {
        logger.trace("loadTei: {}/{}", pi, language);
        if (pi == null) {
            return null;
        }
        TextResourceBuilder builder = new TextResourceBuilder();
        try {
            return builder.getTeiDocument(pi, language);
        } catch (PresentationException | IndexUnreachableException | ContentLibException e) {
            logger.error(e.toString());
            return null;
        }
    }

    /**
     * creates a Dataset object, containing all relevant file paths
     * 
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.Dataset} object.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws RecordNotFoundException
     * @throws IOException
     */

    public static Dataset getDataset(String pi) throws PresentationException, IndexUnreachableException, RecordNotFoundException, IOException {

        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + pi, null);
        if (doc == null) {
            throw new RecordNotFoundException(pi);
        }

        Dataset work = new Dataset();
        work.setPi(pi);

        long iddoc = Long.parseLong((String) doc.getFieldValue(SolrConstants.IDDOC));
        StructElement se = new StructElement(iddoc, doc);

        String format = se.getSourceDocFormat();
        String dataRepository = se.getDataRepository();

        Path repository;
        if (StringUtils.isBlank(dataRepository)) {
            repository = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        } else if (Paths.get(FileTools.adaptPathForWindows(dataRepository)).isAbsolute()) {
            repository = Paths.get(dataRepository);
        } else {
            repository = Paths.get(DataManager.getInstance().getConfiguration().getDataRepositoriesHome(), dataRepository);
        }

        // path to metadata file
        work.setMetadataFilePath(Paths.get(DataFileTools.getSourceFilePath(pi + ".xml", dataRepository,
                format != null ? format.toUpperCase() : SolrConstants.SOURCEDOCFORMAT_METS)));

        // path to images
        work.setMediaFolderPath(repository.resolve(DataManager.getInstance().getConfiguration().getMediaFolder()).resolve(pi));

        // alto folder
        work.setAltoFolderPath(repository.resolve(DataManager.getInstance().getConfiguration().getAltoFolder()).resolve(pi));

        // pdf folder
        work.setPdfFolderPath(repository.resolve(DataManager.getInstance().getConfiguration().getPdfFolder()).resolve(pi));

        // collect files
        if (work.getMediaFolderPath() != null && Files.exists(work.getMediaFolderPath())) {
            try (Stream<Path> stream = Files.list(work.getMediaFolderPath())) {
                List<Path> media = stream.sorted().collect(Collectors.toList());
                work.setMediaFiles(media);
            }
        }
        if (work.getPdfFolderPath() != null && Files.exists(work.getPdfFolderPath())) {
            try (Stream<Path> stream = Files.list(work.getPdfFolderPath())) {
                List<Path> pdfs = stream.sorted().collect(Collectors.toList());
                work.setPdfFiles(pdfs);
            }
        }
        if (work.getAltoFolderPath() != null && Files.exists(work.getAltoFolderPath())) {
            try (Stream<Path> stream = Files.list(work.getAltoFolderPath())) {
                List<Path> alto = stream.sorted().collect(Collectors.toList());
                work.setAltoFiles(alto);
            }
        }

        return work;
    }

}
