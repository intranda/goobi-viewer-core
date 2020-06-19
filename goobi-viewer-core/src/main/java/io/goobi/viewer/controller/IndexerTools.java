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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.modules.IModule;

/**
 * Utility class for Solr indexer interactions (indexing, deleting, etc.).
 */
public class IndexerTools {

    private static final Logger logger = LoggerFactory.getLogger(IndexerTools.class);

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
                    if (!reIndexRecord(pi)) {
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

        String filePath = DataFileTools.getSourceFilePath(pi + ".xml", dataRepository, recordType);
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
            AbstractApiUrlManager urls = new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl());
            AnnotationsResourceBuilder annoBuilder = new AnnotationsResourceBuilder(urls);
            for (CampaignRecordStatistic statistic : statistics) {
                Campaign campaign = statistic.getOwner();
                List<PersistentAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi);
                if (!annotations.isEmpty()) {
                    logger.debug("Found {} annotations for this record (campaign '{}').", annotations.size(), campaign.getTitle());
                    File annotationDir =
                            new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_ANNOTATIONS);
                    for (PersistentAnnotation annotation : annotations) {
                        try {
                            String json = annoBuilder.getAsWebAnnotation(annotation).toString();
                            String jsonFileName = annotation.getTargetPI() + "_" + annotation.getId() + ".json";
                            FileUtils.writeStringToFile(new File(annotationDir, jsonFileName), json, Charset.forName(StringTools.DEFAULT_ENCODING));
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
}
