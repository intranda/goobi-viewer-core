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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexAugmenterException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.serialization.AnnotationIndexAugmenter;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.modules.interfaces.IndexAugmenter;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * Utility class for Solr indexer interactions (indexing, deleting, etc.).
 */
public final class IndexerTools {

    private static final Logger logger = LogManager.getLogger(IndexerTools.class);

    /** Constant <code>SUFFIX_FULLTEXT_CROWDSOURCING="_txtcrowd"</code> */
    public static final String SUFFIX_FULLTEXT_CROWDSOURCING = "_txtcrowd";
    /** Constant <code>SUFFIX_ALTO_CROWDSOURCING="_altocrowd"</code> */
    public static final String SUFFIX_ALTO_CROWDSOURCING = "_altocrowd";
    /** Constant <code>SUFFIX_USER_GENERATED_CONTENT="_ugc"</code> */
    public static final String SUFFIX_USER_GENERATED_CONTENT = "_ugc";
    /** Constant <code>SUFFIX_CMS="_cms"</code> */
    public static final String SUFFIX_CMS = "_cms";

    private IndexerTools() {
    }

    /**
     * Re-index in background thread to significantly decrease saving times.
     *
     * @param pi a {@link java.lang.String} object.
     */
    public static void triggerReIndexRecord(String pi) {
        triggerReIndexRecord(pi, DataManager.getInstance().getModules());
    }

    public static void triggerReIndexRecord(String pi, List<? extends IndexAugmenter> augmenters) {
        logger.debug("Re-indexing record {}", pi);
        new Thread(() -> {
            try {
                if (!reIndexRecord(pi, augmenters)) {
                    logger.error("Failed to re-index  record {}", pi);
                    Messages.error("reIndexRecordFailure");
                } else {
                    Messages.info("reIndexRecordSuccess");
                }
            } catch (DAOException | RecordNotFoundException e) {
                logger.error("Failed to reindex record {}: {}", pi, e.getMessage(), e);
                Messages.error("reIndexRecordFailure");
            }
        }).start();
    }

    /**
     * 
     * @param page
     * @param augmenters
     */
    public static void triggerReIndexCMSPage(CMSPage page, List<? extends IndexAugmenter> augmenters) {
        logger.debug("Re-indexing CMS page {}", page.getId());
        new Thread(() -> {
            if (!reIndexCMSPage(page, augmenters)) {
                logger.error("Failed to re-index CMS page {}", page);
                Messages.error("reIndexCmsPageFailure");
            } else {
                Messages.info("reIndexCmsPageSuccess");
            }
        }).start();
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
        return reIndexRecord(pi, getAllAugmenters(pi, null));
    }

    /**
     * Writes the record into the hotfolder for re-indexing. Modules can contribute data for re-indexing. Execution of method can take a while, so if
     * performance is of importance, use <code>triggerReIndexRecord</code> instead.
     *
     * @param pi a {@link java.lang.String} object.
     * @param augmenters
     * @return true if export for reindexing successful; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     * @should write overview page data
     */
    public static synchronized boolean reIndexRecord(String pi, Collection<? extends IndexAugmenter> augmenters)
            throws DAOException, RecordNotFoundException {
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }

        String dataRepository = null;
        String recordType = null;
        try {
            SolrDocument doc = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(SolrConstants.PI + ":" + pi,
                            Arrays.asList(SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT));
            if (doc == null) {
                throw new RecordNotFoundException("Record not found in index: " + pi);
            }
            dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
            recordType = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
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

        // If the same record is already being indexed, use an alternative naming scheme
        File fulltextDir =
                new File(DataManager.getInstance().getConfiguration().getHotfolder(), pi + SUFFIX_FULLTEXT_CROWDSOURCING);
        File altoDir =
                new File(DataManager.getInstance().getConfiguration().getHotfolder(), pi + SUFFIX_ALTO_CROWDSOURCING);
        File annotationsDir =
                new File(DataManager.getInstance().getConfiguration().getHotfolder(),
                        pi + AnnotationIndexAugmenter.SUFFIX_ANNOTATIONS);
        File cmsDir = new File(DataManager.getInstance().getConfiguration().getHotfolder(), pi + SUFFIX_CMS);

        String namingScheme = findNamingScheme(pi, "xml", fulltextDir, altoDir, annotationsDir, cmsDir);

        // Export related CMS page contents
        try {
            List<CMSPage> cmsPages = DataManager.getInstance().getDao().getCMSPagesForRecord(pi, null);
            if (!cmsPages.isEmpty()) {
                for (CMSPage page : cmsPages) {
                    page.exportTexts(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        // Module augmentations
        for (IndexAugmenter module : augmenters) {
            try {
                module.augmentReIndexRecord(pi, dataRepository, namingScheme);
            } catch (IndexAugmenterException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Finally, copy the record XML file to the hotfolder
        try {
            FileUtils.copyFile(recordXmlFile,
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme + ".xml"));
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
        return reIndexPage(pi, page, getAllAugmenters(pi, page));
    }

    /**
     * @param pi
     * @param page
     * @return Collection<? extends IndexAugmenter>
     * @throws DAOException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Collection<? extends IndexAugmenter> getAllAugmenters(String pi, Integer page) throws DAOException {
        List<IndexAugmenter> augmenters = new ArrayList<>();
        augmenters.addAll(DataManager.getInstance().getModules());

        //Don't index crowdsourcing campaign annotations. At least some (authority data) cause an exception when indexing
        //        List annos = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, page);
        //        IndexAugmenter annoAugmenter = new AnnotationIndexAugmenter(annos);
        //        augmenters.add(annoAugmenter);

        if (page != null) {
            // Only add comments if a page number is given, or else NPE
            List comments = DataManager.getInstance().getDao().getCommentsForPage(pi, page);
            IndexAugmenter commentAugmenter = new AnnotationIndexAugmenter(comments);
            augmenters.add(commentAugmenter);
        }

        return augmenters;
    }

    /**
     * <p>
     * reIndexPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @param augmenters
     * @return true if export for reindexing successful; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.io.IOException if any.
     */
    public static synchronized boolean reIndexPage(String pi, int page, Collection<? extends IndexAugmenter> augmenters)
            throws PresentationException, IndexUnreachableException, IOException {
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
                        Arrays.asList(SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.UGCTERMS));

        if (doc == null) {
            logger.error("No Solr document found for {}/{}", pi, page);
            return false;
        }
        StringBuilder sbNamingScheme = new StringBuilder(pi).append('#').append(page);

        // Module augmentations
        boolean writeTriggerFile = true;
        for (IndexAugmenter module : augmenters) {
            try {
                if (!module.augmentReIndexPage(pi, page, doc, dataRepository, sbNamingScheme.toString())) {
                    writeTriggerFile = false;
                }
            } catch (IndexAugmenterException e) {
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
            logger.info("Deletion trigger file created: {}", file.getFileName());
        } catch (FileAlreadyExistsException e) {
            logger.warn(e.getMessage());
        }
        return (Files.isRegularFile(file));
    }

    /**
     * 
     * @param page
     * @param augmenters
     * @return true if export for reindexing successful; false otherwise
     */
    public static synchronized boolean reIndexCMSPage(CMSPage page, Collection<? extends IndexAugmenter> augmenters) {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        Document doc = page.exportAsXml();
        String namingScheme = findNamingScheme("CMS" + page.getId(), "xml");

        // Finally, write the record XML file to the hotfolder
        try {
            XmlTools.writeXmlFile(doc,
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme + ".xml").getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * 
     * @param baseName Main file initial base name
     * @param extension Main file extension
     * @param otherFiles Optional additional files/folders to check for existence
     * @return Available file naming scheme
     * @should return original baseName if no files exist
     * @should return alternative naming scheme if initial name already exists
     */
    static String findNamingScheme(String baseName, String extension, File... otherFiles) {
        String namingScheme = baseName;
        boolean otherFileExists = false;
        if (otherFiles != null) {
            for (File f : otherFiles) {
                if (f.exists()) {
                    otherFileExists = true;
                    break;
                }
            }
        }

        File recordXmlFileInHotfolder = new File(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme + "." + extension);
        if (recordXmlFileInHotfolder.exists() || otherFileExists) {
            logger.info("'{}' is already being indexed, looking for an alternative naming scheme...", namingScheme);
            long iteration = System.currentTimeMillis();
            // Just checking for the presence of the record XML file at this
            // point, because this method is synchronized and no two
            // instances should be running at the same time.
            while ((new File(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme + "#" + iteration + ".xml")).exists()) {
                iteration = System.currentTimeMillis();
            }
            namingScheme += "#" + iteration;
            logger.info("Alternative naming scheme: {}", namingScheme);
        }

        return namingScheme;
    }
}
