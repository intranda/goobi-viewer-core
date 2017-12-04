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
package de.intranda.digiverso.presentation.model.overviewpage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.Index;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.misc.Harvestable;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Data and methods for the overview page view.
 */
@Entity
@Table(name = "overview_pages")
public class OverviewPage implements Harvestable, Serializable {

    private static final long serialVersionUID = -8613810925005476807L;

    private static final Logger logger = LoggerFactory.getLogger(OverviewPage.class);

    private static final String DEFAULT_OVERVIEW_PAGE_FILE_NAME = "overviewpage.default.xml";
    protected static final String PAGE_BREAK_REGEX = "<span class=\"pagebreak\"></span>";

    private static int imageWidth = 400;
    private static int imageHeight = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "overview_page_id")
    private Long id;

    @Index(name = "index_overview_pages_pi")
    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "config_xml", columnDefinition = "LONGTEXT")
    private String configXml;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "publication_text", columnDefinition = "LONGTEXT")
    private String publicationText;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated", columnDefinition = "DATETIME default '1970-01-01 00:00:00'")
    private Date dateUpdated;

    @Transient
    private Document config;
    @Transient
    private String imageUrl;
    @Transient
    private List<Metadata> metadata = new ArrayList<>();
    @Transient
    private List<Metadata> allMetadata = new ArrayList<>();
    @Transient
    private StructElement structElement;
    @Transient
    private boolean metadataDirty = false;
    @Transient
    private boolean descriptionDirty = false;
    @Transient
    private boolean publicationTextDirty = false;
    @Transient
    private boolean editDescriptionMode = false;
    @Transient
    private boolean editPublicationMode = false;
    @Transient
    private String metadataFieldNameToAdd;
    @Transient
    private int publicationTextCurrentPageNumber = 1;
    @Transient
    private Locale locale;

    /**
     * Loads the overview page for the given StructElement. The object the client receives is a copy of the object returned from the database. Each
     * user session receives a separate copy of the page so that not everyone works on the same object.
     *
     * @param structElement
     * @param locale
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IllegalArgumentException
     * @throws DAOException
     * @should load overview page correctly
     */
    public static OverviewPage loadOverviewPage(StructElement structElement, Locale locale) throws IllegalArgumentException,
            IndexUnreachableException, PresentationException, DAOException {
        if (structElement == null) {
            throw new IllegalArgumentException("structElement may not be null");
        }
        if (OverviewPage.isAllowed(structElement.getPi())) {
            logger.trace("Overview page display is allowed for '{}'", structElement.getPi());
            OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(structElement.getPi(), null, null);
            if (overviewPage != null) {
                OverviewPage copy = new OverviewPage();
                copy.copyFields(overviewPage);
                copy.init(structElement, locale);
                return copy;
            }
        }

        return null;
    }

    /**
     * Checks whether overview pages are generally allowed for the record represented by the given identifier.
     *
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static boolean isAllowed(String pi) throws IndexUnreachableException, PresentationException {
        if (DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible()) {
            String overviewPageCondition = DataManager.getInstance().getConfiguration().getSidebarOverviewLinkCondition();
            if (StringUtils.isNotBlank(overviewPageCondition)) {
                StringBuilder sbQuery = new StringBuilder();
                sbQuery.append(SolrConstants.PI).append(':').append(pi).append(" AND (").append(overviewPageCondition).append(')');
                logger.trace("Evaluating condition: {}", sbQuery.toString());
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Clones the fields from the given object to this object.
     *
     * @param sourceOverviewPage
     * @should copy fields correctly
     * @should throw IllegalArgumentException if sourceOverviewPage is null
     */
    public void copyFields(OverviewPage sourceOverviewPage) {
        if (sourceOverviewPage == null) {
            throw new IllegalArgumentException("sourceOverviewPage may not be null");
        }
        id = sourceOverviewPage.getId();
        pi = sourceOverviewPage.getPi();
        configXml = sourceOverviewPage.getConfigXml();
        description = sourceOverviewPage.getDescription();
        publicationText = sourceOverviewPage.getPublicationText();
        dateUpdated = sourceOverviewPage.getDateUpdated();
    }

    /**
     * Loads transient data for this overview page.
     *
     * @param structElement
     * @param locale
     * @throws IllegalArgumentException
     * @throws IndexUnreachableException
     */
    public void init(StructElement structElement, Locale locale) throws IllegalArgumentException, IndexUnreachableException {
        if (structElement == null) {
            throw new IllegalArgumentException("structElement may not be null");
        }
        if (pi != null && !structElement.getPi().equals(pi)) {
            throw new IllegalArgumentException("structElement has a different PI (" + structElement.getPi() + ") than the overview page (" + pi
                    + ")");
        }
        this.structElement = structElement;
        this.pi = structElement.getPi();
        this.locale = locale;

        if (publicationText == null) {
            publicationText = "";
        }

        imageUrl = structElement.getImageUrl(imageWidth, imageHeight, 0, false, false);

        if (!loadConfig(configXml)) {
            config = new Document();
            config.setRootElement(new Element("overviewPage"));
        }
        parseConfig(config);
        // Add metadata values
        allMetadata = DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(structElement.getDocStructType());
        // Populate the available main metadata list with values to later determine which can be added
        fillMetadataValues(allMetadata, locale);
        // Populate the configured metadata with values
        fillMetadataValues(metadata, locale);

    }

    private boolean loadConfig(String configXml) {
        // Load default page config, if none provided by the document
        if (StringUtils.isNotEmpty(configXml)) {
            try (StringReader sr = new StringReader(configXml)) {
                config = new SAXBuilder().build(sr);
                return true;
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            // Load default page config, if none provided by the document
            File f = new File(DataManager.getInstance().getConfiguration().getConfigLocalPath(), DEFAULT_OVERVIEW_PAGE_FILE_NAME);
            if (f.isFile()) {
                logger.info("Using default overview page configuration.");
                try (FileInputStream fis = new FileInputStream(f)) {
                    config = new SAXBuilder().build(fis);
                    return true;
                } catch (IOException e) {
                    logger.error(e.getMessage());
                } catch (JDOMException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.warn("Default overview page configuration file not found: {}", f.getAbsolutePath());
            }
        }

        return false;
    }

    /**
     *
     * @param config
     * @should parse config document correctly
     */
    protected void parseConfig(Document config) {
        if (config != null && config.getRootElement() != null) {
            // Metadata
            metadata.clear();
            if (config.getRootElement().getChild("metadata") != null) {
                metadata = DataManager.getInstance().getConfiguration().getMetadataForTemplateFromJDOM(config.getRootElement().getChild("metadata"));
            }
            // Description (only load from XML if not yet in the database column)
            if (description == null) {
                Element eleDescription = config.getRootElement().getChild("description", null);
                if (eleDescription != null) {
                    if (eleDescription.getChild("div", null) != null) {
                        // Embedded page
                        XMLOutputter outputter = new XMLOutputter();
                        Format format = Format.getCompactFormat();
                        format.setExpandEmptyElements(true);
                        outputter.setFormat(format);
                        description = outputter.outputString(eleDescription.getChild("div", null));
                    } else {
                        // Regular string description
                        description = config.getRootElement().getChildText("description", null);
                    }
                }
                logger.trace("Description: {}", description);
            }
            // Publication text (only load from XML if not yet in the database column)
            if (publicationText == null) {
                Element elePublicationText = config.getRootElement().getChild("publicationText", null);
                if (elePublicationText != null) {
                    publicationText = config.getRootElement().getChildText("publicationText", null);
                }
                //                logger.trace("Publication text: {}", publicationText);
            }
        } else {
            logger.error("Invalid overview page XML document for '{}'.", pi);
        }
    }

    /**
     * 
     * @param metadata
     * @param locale
     * @throws IndexUnreachableException
     */
    private void fillMetadataValues(List<Metadata> metadata, Locale locale) throws IndexUnreachableException {
        for (Metadata md : metadata) {
            md.populate(structElement.getMetadataFields(), locale);
        }
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the pi
     */
    @Override
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the configXml
     */
    public String getConfigXml() {
        return configXml;
    }

    /**
     * @param configXml the configXml to set
     */
    public void setConfigXml(String configXml) {
        this.configXml = configXml;
    }

    /**
     * @return the publicationText
     */
    public String getPublicationText() {
        return publicationText;
    }

    /**
     * @param publicationText the publicationText to set
     */
    public void setPublicationText(String publicationText) {
        this.publicationText = publicationText;
        // fix page breaks being inside paragraphs (happens if a page break is inserted w/o a line break)
        this.publicationText = this.publicationText.replace(PAGE_BREAK_REGEX + "</p>", "</p>" + PAGE_BREAK_REGEX);
    }

    /**
     * Returns the text part of publicationText that belongs to the current page number.
     *
     * @return
     */
    public String getPublicationTextCurrentPage() {
        String[] publicationTextSplit = publicationText.split(PAGE_BREAK_REGEX);
        if (publicationTextCurrentPageNumber > publicationTextSplit.length) {
            publicationTextCurrentPageNumber = publicationTextSplit.length;
        }

        return publicationTextSplit[publicationTextCurrentPageNumber - 1];
    }

    /**
     * Returns the number of pages in <code>publicationText</code>. The number of pages is determined by the amount of line break char sequences in
     * the string.
     *
     * @return
     * @should return correct number
     */
    public int getPublicationTextNumPages() {
        if (publicationText != null) {
            String[] publicationTextSplit = publicationText.split(PAGE_BREAK_REGEX);
            return publicationTextSplit.length;
        }

        return 0;
    }

    /**
     * @return the publicationTextCurrentPageNumber
     */
    public int getPublicationTextCurrentPageNumber() {
        return publicationTextCurrentPageNumber;
    }

    /**
     * @param publicationTextCurrentPageNumber the publicationTextCurrentPageNumber to set
     */
    public void setPublicationTextCurrentPageNumber(int publicationTextCurrentPageNumber) {
        this.publicationTextCurrentPageNumber = publicationTextCurrentPageNumber;
        int maxPage = getPublicationTextNumPages();
        if (this.publicationTextCurrentPageNumber > maxPage) {
            this.publicationTextCurrentPageNumber = maxPage;
        }
    }

    public String publicationTextFirstPageAction() {
        logger.trace("publicationTextFirstPageAction");
        publicationTextCurrentPageNumber = 1;
        return "";
    }

    public String publicationTextPrevPageAction() {
        logger.trace("publicationTextPrevPageAction");
        if (publicationTextCurrentPageNumber > 1) {
            publicationTextCurrentPageNumber--;
        }
        return "";
    }

    public String publicationTextNextPageAction() {
        logger.trace("publicationTextNextPageAction");
        publicationTextCurrentPageNumber++;
        return "";
    }

    public String publicationTextLastPageAction() {
        logger.trace("publicationTextLastPageAction");
        publicationTextCurrentPageNumber = getPublicationTextNumPages();
        return "";
    }

    /**
     * @return the dateUpdated
     */
    @Override
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * Returns whether the sidebar overview linkis visible (= overview pages are enabled at all).
     *
     * @should return the value in sidenbarOverviewLinkVisible
     */
    public boolean isEnabled() {
        return DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible();
    }

    public boolean isDisplayImage() {
        return imageUrl != null;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isDisplayMetadata() {
        return !metadata.isEmpty();
    }

    public List<Metadata> getMetadata() {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null && adb.isRecordLoaded()) {
            return Metadata.filterMetadataByLanguage(metadata, adb.getSelectedRecordLanguage());
        }

        return metadata;
    }

    public boolean isDisplayDescription() {
        return StringUtils.isNotEmpty(description);
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the metadataDirty
     */
    public boolean isMetadataDirty() {
        return metadataDirty;
    }

    /**
     * @return the descriptionDirty
     */
    public boolean isDescriptionDirty() {
        return descriptionDirty;
    }

    /**
     *
     * @return
     * @should return names of configured main metadata elements
     * @should not return names already in use
     */
    public List<String> getAvailableMetadataFields() {
        List<String> ret = new ArrayList<>();

        for (Metadata md : allMetadata) {
            if (!metadata.contains(md) && !md.isBlank()) {
                ret.add(md.getLabel());
            }
        }

        return ret;
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     * @should add given field correctly
     * @should add given field's label to usedMetaadataNames
     */
    public String addMetadataFieldAction() throws IndexUnreachableException {
        if (StringUtils.isNotEmpty(metadataFieldNameToAdd)) {
            try {
                for (Metadata md : DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(structElement.getDocStructType())) {
                    if (metadataFieldNameToAdd.equals(md.getLabel())) {
                        // TODO clone?
                        metadata.add(md);
                        fillMetadataValues(metadata, locale);
                        metadataDirty = true;
                        return "";
                    }
                }
            } finally {
                metadataFieldNameToAdd = null;
            }
        }

        Messages.error("metadataNotFound");
        return "";
    }

    /**
     *
     * @param field
     * @return
     * @should remove given field correctly
     * @should remove the given field's label from usedMetadataNames
     */
    public String removeMetadataFieldAction(int index) {
        if (index < metadata.size()) {
            // Metadata md = metadata.get(index);
            metadata.remove(index);
            metadataDirty = true;
        }

        return "";
    }

    /**
     * @return the editDescriptionMode
     */
    public boolean isEditDescriptionMode() {
        return editDescriptionMode;
    }

    /**
     *
     * @return
     * @should toggle boolean correctly
     */
    public String toggleEditDescriptionModeAction() {
        editDescriptionMode = !editDescriptionMode;
        return "";
    }

    /**
     * @return the editPublicationMode
     */
    public boolean isEditPublicationMode() {
        //        logger.trace("editPublicationMode: {} ({})", editPublicationMode, this.toString());
        return editPublicationMode;
    }

    /**
     *
     * @return
     * @should toggle boolean correctly
     */
    public String toggleEditPublicationModeAction() {
        logger.trace("toggleEditPublicationModeAction: {}", !editPublicationMode);
        editPublicationMode = !editPublicationMode;
        return "";
    }

    public String resetEditModes() {
        logger.trace("resetEditModes");
        editDescriptionMode = false;
        editPublicationMode = false;

        return "";
    }

    /**
     * @return the metadataFieldNameToAdd
     */
    public String getMetadataFieldNameToAdd() {
        return metadataFieldNameToAdd;
    }

    /**
     * @param metadataFieldNameToAdd the metadataFieldNameToAdd to set
     */
    public void setMetadataFieldNameToAdd(String metadataFieldNameToAdd) {
        this.metadataFieldNameToAdd = metadataFieldNameToAdd;
    }

    /**
     *
     * @param configXml
     * @return true if sucessful; false otherwise
     * @throws DAOException
     */
    public boolean migrateToDB(String configXml, String pi) throws DAOException {
        if (configXml == null) {
            throw new IllegalArgumentException("configXml may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        this.pi = pi;
        this.configXml = configXml;
        loadConfig(configXml);
        parseConfig(config);

        return save();
    }

    /**
     * 
     * @param updatedBy
     * @return
     * @throws DAOException
     */
    public String saveAction(User updatedBy) throws DAOException {
        return saveAction(updatedBy, true);
    }

    /**
     * @param updatedBy User responsible for the update.
     * @param exportToGoobi
     * @return
     * @throws DAOException
     * @should update metadata list correctly
     * @should update description correctly
     * @should update publication text correctly
     * @should write to DB correctly
     * @should update timestamp
     * @should add history entry
     * @should reset edit modes
     */
    public String saveAction(User updatedBy, boolean exportToGoobi) throws DAOException {
        logger.debug("saveAction");

        if (config == null) {
            logger.error("Overview page XML document is null. Is '{}' missing?", DEFAULT_OVERVIEW_PAGE_FILE_NAME);
            Messages.error("errSave");
            return "";
        }

        // Update metadata list
        Element eleMetadataRoot = config.getRootElement().getChild("metadata", null);
        if (eleMetadataRoot == null) {
            eleMetadataRoot = new Element("metadata");
            config.getRootElement().addContent(eleMetadataRoot);
        }
        eleMetadataRoot.removeChildren("metadata", null);
        for (Metadata md : metadata) {
            Element eleMetadata = new Element("metadata");
            eleMetadata.setAttribute(new Attribute("label", md.getLabel()));
            eleMetadata.setAttribute(new Attribute("value", md.getMasterValue().equals("{0}") ? "" : md.getMasterValue()));
            if (md.isGroup()) {
                eleMetadata.setAttribute(new Attribute("group", "true"));
            }
            if (md.getNumber() != -1) {
                eleMetadata.setAttribute(new Attribute("number", String.valueOf(md.getNumber())));
            }
            if (md.getType() > 0) {
                eleMetadata.setAttribute(new Attribute("type", String.valueOf(md.getType())));
            }
            if (md.getParams() != null && !md.getParams().isEmpty()) {
                for (MetadataParameter param : md.getParams()) {
                    Element eleParam = new Element("param");
                    if (param.getType() != null) {
                        eleParam.setAttribute(new Attribute("type", param.getType().getKey()));
                    }
                    if (param.getSource() != null) {
                        eleParam.setAttribute(new Attribute("source", param.getSource()));
                    }
                    if (param.getKey() != null) {
                        eleParam.setAttribute(new Attribute("key", param.getKey()));
                    }
                    if (param.getDefaultValue() != null) {
                        eleParam.setAttribute(new Attribute("defaultValue", param.getDefaultValue()));
                    }
                    if (StringUtils.isNotEmpty(param.getPrefix())) {
                        eleParam.setAttribute(new Attribute("prefix", param.getPrefix().replace(" ", "_SPACE_")));
                    }
                    if (StringUtils.isNotEmpty(param.getSuffix())) {
                        eleParam.setAttribute(new Attribute("suffix", param.getSuffix().replace(" ", "_SPACE_")));
                    }
                    if (param.isAddUrl()) {
                        eleParam.setAttribute(new Attribute("url", "true"));
                    }
                    if (param.isDontUseTopstructValue()) {
                        eleParam.setAttribute(new Attribute("dontUseTopstructValue", "true"));
                    }
                    eleMetadata.addContent(eleParam);
                }
            }
            eleMetadataRoot.addContent(eleMetadata);
            logger.trace("Added metadata: " + eleMetadata.getAttributeValue("label"));
        }

        // Update description
        {
            Element eleDescription = config.getRootElement().getChild("description", null);
            if (eleDescription == null) {
                eleDescription = new Element("description");
                config.getRootElement().addContent(eleDescription);
            }
            if (eleDescription.getText() == null || !eleDescription.getText().equals(description)) {
                descriptionDirty = true;
                logger.trace("Description is dirty");
            }
            eleDescription.setText(description);
        }

        // Update publication text
        {
            Element elePublicationText = config.getRootElement().getChild("publicationText", null);
            if (elePublicationText == null) {
                elePublicationText = new Element("publicationText");
                config.getRootElement().addContent(elePublicationText);
            }
            if (elePublicationText.getText() == null || !elePublicationText.getText().equals(publicationText)) {
                publicationTextDirty = true;
                logger.trace("Publication text is dirty");
            }
            elePublicationText.setText(publicationText);
        }

        if (metadataDirty || descriptionDirty || publicationTextDirty) {
            if (save()) {
                // Write history
                OverviewPageUpdate update = new OverviewPageUpdate();
                update.setPi(pi);
                update.setDateUpdated(dateUpdated);
                update.setUpdatedBy(updatedBy);
                update.setMetadataChanged(metadataDirty);
                update.setDescriptionChanged(descriptionDirty);
                update.setPublicationTextChanged(publicationTextDirty);
                if (DataManager.getInstance().getDao().addOverviewPageUpdate(update)) {
                    logger.debug("Added overview page history entry.");
                } else {
                    logger.warn("Could not add overview page history entry.");
                }

                // Reset the dirty flags after saving
                metadataDirty = false;
                descriptionDirty = false;
                publicationTextDirty = false;
                Messages.info("savedSuccessfully");
            }

            // Re-index record
            Helper.triggerReIndexRecord(pi, structElement.getSourceDocFormat(), this);
        }

        resetEditModes();
        return "";
    }

    /**
     * Saves the changes to the overview page by loading the master copy of this page from the database, setting the new values, then writing it back.
     *
     * @return
     * @throws DAOException
     */
    private boolean save() throws DAOException {
        logger.trace("save");
        configXml = new XMLOutputter().outputString(config);
        dateUpdated = new Date();
        if (id != null) {
            OverviewPage masterCopy = DataManager.getInstance().getDao().getOverviewPage(id);
            if (masterCopy != null) {
                masterCopy.copyFields(this);
                return DataManager.getInstance().getDao().updateOverviewPage(masterCopy);
            }
        }

        return DataManager.getInstance().getDao().addOverviewPage(this);
    }

    /**
     * Deletes this overview page from the database.
     *
     * @return
     * @throws DAOException
     */
    public String deleteAction() throws DAOException {
        logger.debug("deleteAction: {}", this.getId());
        if (id != null) {
            if (DataManager.getInstance().getDao().deleteOverviewPage(this)) {
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                adb.setOverviewPage(null);
                logger.info("Overview page for {} deleted.", pi);
                Messages.info("viewOverviewDeletePageSuccess");
                return ""; // TODO redirect to different record view?
            }
        } else {
            logger.warn("Current overview page has no ID, so it probably is not persisted. Removing the current session object instead.");
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            adb.setOverviewPage(null);
        }

        Messages.error("viewOverviewDeletePagefailure");
        return "";
    }

    public List<OverviewPageUpdate> getHistory() throws DAOException {
        return DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord(pi);
    }

    /**
     * Creates an export XML string (config XML with additional PI and publicationText elements).
     *
     * @return
     * @should create export xml correctly
     */
    public String getExportFormat() {
        if (config == null) {
            if (!loadConfig(configXml)) {
                return "ERROR";
            }
        }
        Document export = config.clone();
        export.getRootElement().setNamespace(Helper.nsIntrandaViewerOverviewPage);
        export.getRootElement().addContent(new Element("pi", Helper.nsIntrandaViewerOverviewPage).setText(pi));
        if (export.getRootElement().getChild("publicationText", Helper.nsIntrandaViewerOverviewPage) == null) {
            export.getRootElement().addContent(new Element("publicationText", Helper.nsIntrandaViewerOverviewPage).setText(publicationText));
        }

        return new XMLOutputter().outputString(export);
    }

    /**
     * Writes description and publicationText values as files for re-indexing.
     * 
     * @param hotfolder
     * @param namingScheme
     * @throws IOException
     * @should write files correctly
     */
    public void exportTextData(String hotfolderPath, String namingScheme) throws IOException {
        if (StringUtils.isEmpty(hotfolderPath)) {
            throw new IllegalArgumentException("hotfolderPath may not be null or emptys");
        }
        if (StringUtils.isEmpty(namingScheme)) {
            throw new IllegalArgumentException("namingScheme may not be null or empty");
        }

        if (config == null) {
            // Freshly loaded overview page must have its description field populated from the config xml document
            loadConfig(this.configXml);
            parseConfig(config);
        }

        Path overviewPageDir = Paths.get(hotfolderPath, namingScheme + "_overview");
        if (!Files.isDirectory(overviewPageDir)) {
            Files.createDirectory(overviewPageDir);
        }
        logger.trace("Created overview page subdirectory: {}", overviewPageDir.toAbsolutePath().toString());
        if (StringUtils.isNotEmpty(description)) {
            File file = new File(overviewPageDir.toFile(), "description.xml");
            try {
                FileUtils.writeStringToFile(file, description, Helper.DEFAULT_ENCODING);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        if (StringUtils.isNotEmpty(publicationText)) {
            File file = new File(overviewPageDir.toFile(), "publicationtext.xml");
            try {
                FileUtils.writeStringToFile(file, publicationText, Helper.DEFAULT_ENCODING);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
