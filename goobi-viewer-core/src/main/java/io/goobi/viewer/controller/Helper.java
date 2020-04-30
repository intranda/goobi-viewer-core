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
package io.goobi.viewer.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.Version;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.modules.IModule;

/**
 * Helper methods.
 */
public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    /** Constant <code>REGEX_QUOTATION_MARKS="\"[^()]*?\""</code> */
    public static final String REGEX_QUOTATION_MARKS = "\"[^()]*?\"";
    /** Constant <code>REGEX_PARENTHESES="\\([^()]*\\)"</code> */
    public static final String REGEX_PARENTHESES = "\\([^()]*\\)";
    /** Constant <code>REGEX_PARENTESES_DATES="\\([\\w|\\s|\\-|\\.|\\?]+\\)"</code> */
    public static final String REGEX_PARENTESES_DATES = "\\([\\w|\\s|\\-|\\.|\\?]+\\)";
    /** Constant <code>REGEX_BRACES="\\{(\\w+)\\}"</code> */
    public static final String REGEX_BRACES = "\\{(\\w+)\\}";
    /** Constant <code>REGEX_WORDS="[a-zäáàâöóòôüúùûëéèêßñ0123456789]+"</code> */
    public static final String REGEX_WORDS = "[a-zäáàâöóòôüúùûëéèêßñ0123456789]+";
    /** Constant <code>DEFAULT_ENCODING="UTF-8"</code> */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** Constant <code>SUFFIX_FULLTEXT_CROWDSOURCING="_txtcrowd"</code> */
    public static final String SUFFIX_FULLTEXT_CROWDSOURCING = "_txtcrowd";
    /** Constant <code>SUFFIX_ALTO_CROWDSOURCING="_altocrowd"</code> */
    public static final String SUFFIX_ALTO_CROWDSOURCING = "_altocrowd";
    /** Constant <code>SUFFIX_USER_GENERATED_CONTENT="_ugc"</code> */
    public static final String SUFFIX_USER_GENERATED_CONTENT = "_ugc";
    /** Constant <code>SUFFIX_ANNOTATIONS="_annotations"</code> */
    public static final String SUFFIX_ANNOTATIONS = "_annotations";
    /** Constant <code>SUFFIX_CMS="_cms"</code> */
    public static final String SUFFIX_CMS = "_cms";

    /** Constant <code>dfTwoDecimals</code> */
    public static DecimalFormat dfTwoDecimals = new DecimalFormat("0.00");
    /** Constant <code>dfTwoDigitInteger</code> */
    public static DecimalFormat dfTwoDigitInteger = new DecimalFormat("00");

    /** Constant <code>nsAlto</code> */
    public static Namespace nsAlto = Namespace.getNamespace("alto", "http://www.loc.gov/standards/alto/ns-v2#");
    // TODO final namespaces
    /** Constant <code>nsIntrandaViewerOverviewPage</code> */
    public static Namespace nsIntrandaViewerOverviewPage =
            Namespace.getNamespace("iv_overviewpage", "http://www.intranda.com/digiverso/intrandaviewer/overviewpage");
    /** Constant <code>nsIntrandaViewerCrowdsourcing</code> */
    public static Namespace nsIntrandaViewerCrowdsourcing =
            Namespace.getNamespace("iv_crowdsourcing", "http://www.intranda.com/digiverso/intrandaviewer/crowdsourcing");

    /**
     * Creates an MD5 hash of the given String.
     *
     * @param myString a {@link java.lang.String} object.
     * @return MD5 hash
     * @should hash string correctly
     */
    public static String generateMD5(String myString) {
        String answer = "";
        try {
            byte[] defaultBytes = myString.getBytes("UTF-8");
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();

            StringBuffer hexString = new StringBuffer();
            for (byte element : messageDigest) {
                String hex = Integer.toHexString(0xFF & element);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            answer = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        return answer;
    }

    /**
     * Re-index in background thread to significantly decrease saving times.
     *
     * @param pi a {@link java.lang.String} object.
     */
    public static void triggerReIndexRecord(String pi) {
        Thread backgroundThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (!Helper.reIndexRecord(pi)) {
                        logger.error("Failed to re-index  record {}", pi);
                        Messages.error("reIndexRecordFailure");
                    } else {
                        Messages.info("reIndexRecordSuccess");
                    }
                } catch (DAOException | RecordNotFoundException e) {
                    logger.error("Failed to reindex record " + pi + ": " + e.getMessage(), e);
                    Messages.error("reIndexRecordFailure");
                }
            }
        });

        logger.debug("Re-indexing record {}", pi);
        backgroundThread.start();
    }

    /**
     * Writes the record into the hotfolder for re-indexing. Modules can contribute data for re-indexing. Execution of method can take a while, so if
     * performance is of importance, use <code>triggerReIndexRecord</code> instead.
     *
     * @param pi a {@link java.lang.String} object.
     * @should write overview page data
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     */
    public static synchronized boolean reIndexRecord(String pi) throws DAOException, RecordNotFoundException {
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }

        String dataRepository = null;
        String recordType = null;
        try {
            SolrDocument doc = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(SolrConstants.PI + ":" + pi,
                            Arrays.asList(new String[] { SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT }));
            if (doc == null) {
                throw new RecordNotFoundException(pi);
            }
            dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
            recordType = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            return false;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }

        String filePath = getSourceFilePath(pi + ".xml", dataRepository, recordType);
        File recordXmlFile = new File(filePath);
        if (!recordXmlFile.isFile()) {
            logger.error("Cannot re-index '{}': record not found.", recordXmlFile.getAbsolutePath());
            return false;
        }
        logger.info("Preparing to re-index record: {}", recordXmlFile.getAbsolutePath());
        StringBuilder sbNamingScheme = new StringBuilder(pi);

        {
            // If the same record is already being indexed, use an alternative naming scheme
            File fulltextDir =
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_FULLTEXT_CROWDSOURCING);
            File altoDir =
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_ALTO_CROWDSOURCING);
            File annotationsDir =
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_ANNOTATIONS);
            File cmsDir = new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_CMS);

            File recordXmlFileInHotfolder = new File(DataManager.getInstance().getConfiguration().getHotfolder(), recordXmlFile.getName());
            if (recordXmlFileInHotfolder.exists() || fulltextDir.exists() || altoDir.exists() || annotationsDir.exists() || cmsDir.exists()) {
                logger.info("'{}' is already being indexed, looking for an alternative naming scheme...", sbNamingScheme.toString());
                int iteration = 0;
                // Just checking for the presence of the record XML file at this
                // point, because this method is synchronized and no two
                // instances should be running at the same time.
                while ((recordXmlFileInHotfolder =
                        new File(DataManager.getInstance().getConfiguration().getHotfolder(), pi + "#" + iteration + ".xml")).exists()) {
                    iteration++;
                }
                sbNamingScheme.append('#').append(iteration);
                logger.info("Alternative naming scheme: {}", sbNamingScheme.toString());
            }
        }

        // Export related CMS page contents
        try {
            List<CMSPage> cmsPages = DataManager.getInstance().getDao().getCMSPagesForRecord(pi, null);
            if (!cmsPages.isEmpty()) {
                for (CMSPage page : cmsPages) {
                    page.exportTexts(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        // Export annotations (only those that belong to a campaign for which the statistic for this record is marked as finished)
        List<CampaignRecordStatistic> statistics =
                DataManager.getInstance().getDao().getCampaignStatisticsForRecord(pi, CampaignRecordStatus.FINISHED);
        if (!statistics.isEmpty()) {
            for (CampaignRecordStatistic statistic : statistics) {
                Campaign campaign = statistic.getOwner();
                List<PersistentAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi);
                if (!annotations.isEmpty()) {
                    logger.debug("Found {} annotations for this record (campaign '{}').", annotations.size(), campaign.getTitle());
                    File annotationDir =
                            new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_ANNOTATIONS);
                    for (PersistentAnnotation annotation : annotations) {
                        try {
                            String json = annotation.getAsAnnotation().toString();
                            String jsonFileName = annotation.getTargetPI() + "_" + annotation.getId() + ".json";
                            FileUtils.writeStringToFile(new File(annotationDir, jsonFileName), json, Charset.forName(Helper.DEFAULT_ENCODING));
                        } catch (JsonParseException e) {
                            logger.error(e.getMessage(), e);
                        } catch (JsonMappingException e) {
                            logger.error(e.getMessage(), e);
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

        // Module augmentations
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                module.augmentReIndexRecord(pi, dataRepository, sbNamingScheme.toString());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Finally, copy the record XML file to the hotfolder
        try {
            FileUtils.copyFile(recordXmlFile,
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + ".xml"));
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * <p>
     * deleteRecord.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param createTraceDocument a boolean.
     * @should create delete file correctly
     * @should create purge file correctly
     * @param hotfolderPath a {@link java.nio.file.Path} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public static synchronized boolean deleteRecord(String pi, boolean createTraceDocument, Path hotfolderPath) throws IOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (hotfolderPath == null) {
            throw new IllegalArgumentException("hotfolderPath may not be null");
        }

        String fileName = pi + (createTraceDocument ? ".delete" : ".purge");
        Path file = Paths.get(hotfolderPath.toAbsolutePath().toString(), fileName);
        try {
            Files.createFile(file);
        } catch (FileAlreadyExistsException e) {
            logger.warn(e.getMessage());
        }
        return (Files.isRegularFile(file));
    }

    /**
     * <p>
     * reIndexPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.io.IOException if any.
     */
    public static synchronized boolean reIndexPage(String pi, int page)
            throws DAOException, PresentationException, IndexUnreachableException, IOException {
        logger.trace("reIndexPage: {}/{}", pi, page);
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("Illegal page number: " + page);
        }

        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepositoryName(pi);

        String query = new StringBuilder().append('+')
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(pi)
                .append(" +")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(page)
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE.name())
                .toString();
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(query,
                        Arrays.asList(new String[] { SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.UGCTERMS }));

        if (doc == null) {
            logger.error("No Solr document found for {}/{}", pi, page);
            return false;
        }
        StringBuilder sbNamingScheme = new StringBuilder(pi).append('#').append(page);

        // Module augmentations
        boolean writeTriggerFile = true;
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                if (!module.augmentReIndexPage(pi, page, doc, dataRepository, sbNamingScheme.toString())) {
                    writeTriggerFile = false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Create trigger file in hotfolder
        if (writeTriggerFile) {
            Path triggerFile = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + ".docupdate");
            Files.createFile(triggerFile);
        }

        return true;
    }


    /**
     * <p>
     * callUrlGET.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @return A String array with two elements. The first contains the HTTP status code, the second either the requested data (if status code is 200)
     *         or the error message.
     */
    public static String[] callUrlGET(String url) {
        logger.debug("callUrlGET: {}", url);
        String[] ret = new String[2];
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet); StringWriter writer = new StringWriter()) {
                ret[0] = String.valueOf(response.getStatusLine().getStatusCode());
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpServletResponse.SC_OK:
                        HttpEntity httpEntity = response.getEntity();
                        httpEntity.getContentLength();
                        IOUtils.copy(httpEntity.getContent(), writer, DEFAULT_ENCODING);
                        ret[1] = writer.toString();
                        break;
                    case 401:
                        logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                    default:
                        logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * Builds full-text document REST URL.
     *
     * @param filePath a {@link java.lang.String} object.
     * @return Full REST URL
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should build url correctly
     * @should escape spaces correctly
     */
    public static String buildFullTextUrl(String filePath) throws ViewerConfigurationException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath may not be null");
        }

        return new StringBuilder(DataManager.getInstance().getConfiguration().getContentRestApiUrl()).append("document/")
                .append(filePath.replace(" ", "%20"))
                .append('/')
                .toString();
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

        Path folder = repository.resolve(dataFolderName).resolve(pi);

        return folder;
    }

    /**
     * <p>
     * getDataFilePath.
     * </p>
     *
     * @param pi Record identifier
     * @param dataFolderName Name of the data folder (e.g. 'alto') - first choice
     * @param altDataFolderName Name of the data folder - second choice
     * @param fileName Name of the content file
     * @return Path to the requested file
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Path getDataFilePath(String pi, String dataFolderName, String altDataFolderName, String fileName)
            throws PresentationException, IndexUnreachableException {
        java.nio.file.Path dataFolderPath = getDataFolder(pi, dataFolderName);
        if (StringUtils.isNotBlank(fileName)) {
            dataFolderPath = dataFolderPath.resolve(fileName);
        }
        if (StringUtils.isNotBlank(altDataFolderName) && !Files.exists(dataFolderPath)) {
            return getDataFilePath(pi, altDataFolderName, null, fileName);
        }

        return dataFolderPath;
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
            throw new IllegalArgumentException("format may not be null or empty");
        }
        switch (format) {
            case SolrConstants._METS:
            case SolrConstants._LIDO:
            case SolrConstants._DENKXWEB:
            case SolrConstants._WORLDVIEWS:
            case SolrConstants._DUBLINCORE:
                break;
            default:
                throw new IllegalArgumentException("format must be: METS | LIDO | DENKXWEB | DUBLINCORE | WORLDVIEWS");
        }

        StringBuilder sb = new StringBuilder(getDataRepositoryPath(dataRepository));
        switch (format) {
            case SolrConstants._METS:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                break;
            case SolrConstants._LIDO:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                break;
            case SolrConstants._DENKXWEB:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedDenkxwebFolder());
                break;
            case SolrConstants._DUBLINCORE:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedDublinCoreFolder());
                break;
            case SolrConstants._WORLDVIEWS:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
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
        Path filePath = Paths.get(getDataRepositoryPath(dataRepository), relativeFilePath);

        return filePath;
    }

    /**
     * Returns the application version number.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getVersion() {
        return Version.VERSION + "-" + Version.BUILDDATE + "-" + Version.BUILDVERSION;
    }

    /**
     * Loads plain full-text via the REST service. ALTO is preferred (and converted to plain text, with a plain text fallback.
     *
     * @param dataRepository a {@link java.lang.String} object.
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
    public static String loadFulltext(String dataRepository, String altoFilePath, String fulltextFilePath, boolean mergeLineBreakWords,
            HttpServletRequest request)
            throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (altoFilePath != null) {
            // ALTO file
            String alto = loadFulltext(altoFilePath, request);
            if (alto != null) {
                return ALTOTools.getFullText(alto, mergeLineBreakWords, request);

            }
        }
        if (fulltextFilePath != null) {
            // Plain full-text file
            String fulltext = loadFulltext(fulltextFilePath, request);
            if (fulltext != null) {
                return fulltext;
            }
        }

        return null;
    }

    /**
     * Loads given text file path as a string, if the client has full-text access permission.
     *
     * @param filePath File path consisting of three party (datafolder/pi/filename); There must be two separators in the path!
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @should return file content correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String loadFulltext(String filePath, HttpServletRequest request)
            throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (filePath == null) {
            return null;
        }

        String url = Helper.buildFullTextUrl(filePath);
        try {
            return NetTools.getWebContentGET(url);
        } catch (HTTPException e) {
            //            logger.error("Could not retrieve file from {}", url);
            //            logger.error(e.getMessage());
            if (e.getCode() == 403) {
                logger.debug("Access denied for text file {}", filePath);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
        }

        return null;
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
    public static String loadTei(String pi, String language)
            throws AccessDeniedException, FileNotFoundException, IOException, ViewerConfigurationException {
        logger.trace("loadTei: {}/{}", pi, language);
        if (pi == null) {
            return null;
        }

        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getContentRestApiUrl()).append("tei/")
                .append(pi)
                .append('/')
                .append(language)
                .append('/')
                .toString();
        try {
            return NetTools.getWebContentGET(url);
        } catch (HTTPException e) {
            logger.error("Could not retrieve file from {}", url);
            logger.error(e.getMessage());
            if (e.getCode() == 403) {
                logger.debug("Access denied for TEI file {}/{}", pi, language);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
        }

        return null;
    }

    /**
     * <p>
     * parseBoolean.
     * </p>
     *
     * @param text a {@link java.lang.String} object.
     * @param defaultValue a boolean.
     * @return a boolean.
     */
    public static boolean parseBoolean(String text, boolean defaultValue) {
        if ("FALSE".equalsIgnoreCase(text)) {
            return false;
        } else if ("TRUE".equalsIgnoreCase(text)) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * <p>
     * parseBoolean.
     * </p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean parseBoolean(String text) {
        return parseBoolean(text, false);
    }

}
