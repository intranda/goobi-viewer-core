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
package io.goobi.viewer.model.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Represents a group of metadata fields belonging to a single structural element of a digitized record.
 */
public class MetadataElement implements Serializable {

    private static final long serialVersionUID = 222226787503688100L;

    /**
     * Wrapper class for the metadata type numerical value. Needed only for retrieving the proper message key for each type...
     */
    public class MetadataType implements Comparable<MetadataType>, Serializable {

        private static final long serialVersionUID = -2875502991726354737L;

        private static final String KEY_ROOT = "metadataTab";

        private int type;

        public MetadataType() {
            this.type = 0;
        }

        public MetadataType(int type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + type;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MetadataType other = (MetadataType) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                // TODO does this break anything?
                return false;
            }

            return type == other.type;
        }

        @Override
        public int compareTo(MetadataType o) {
            if (o.getType() > type) {
                return -1;
            }
            if (o.getType() < type) {
                return 1;
            }

            return 0;
        }

        /**
         *
         * @param viewIndex Metadata view index
         * @return Message key for this tab
         * @should return correct message key
         */
        public String getTabName(int viewIndex) {
            String key = KEY_ROOT + "_" + viewIndex + "_" + type;
            if (ViewerResourceBundle.getTranslation(key, null, true, false, false, false) != null) {
                return key;
            }

            return "";
        }

        public void setTabName(String tabName) {
            //
        }

        
        public int getType() {
            return type;
        }

        
        public void setType(int type) {
            this.type = type;
        }

        private MetadataElement getOuterType() {
            return MetadataElement.this;
        }
    }

    private static final Logger logger = LogManager.getLogger(MetadataElement.class);

    private String label = null;
    private String title = null;
    private String docType = null;
    private String docStructType = null;
    private String groupType = null;
    private String mimeType = null;
    private String url = null;
    private String pi = null;
    private List<Metadata> metadataList = new ArrayList<>();
    private List<Metadata> sidebarMetadataList = null;
    private List<MetadataType> metadataTypes;
    /** True if this ISWORK=true or ISANCHOR=true. */
    private boolean topElement;
    private boolean anchor;
    private boolean filesOnly = false;
    private boolean hasImages = true;
    /** Selected language version of the current record. This can be different from the current viewer locale. */
    private String selectedRecordLanguage;
    /**
     * The index of the element in the {@link #metadataList} before which the "fold" element occurs in the configuration. If no such element exists,
     * the index is {@link Integer#MAX_VALUE}. Otherwise, only metadata before the index should be displayed initially, and metadata starting with
     * this index should be shown only when the user expands the list
     */
    private int metadataFoldIndex = Integer.MAX_VALUE;

    /**
     *
     * @param se StructElement
     * @param metadataViewIndex Metadata view index
     * @param sessionLocale locale for translations/formatting
     * @return Constructed {@link MetadataElement}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public MetadataElement init(StructElement se, int metadataViewIndex, Locale sessionLocale)
            throws PresentationException, IndexUnreachableException {
        if (se == null) {
            logger.error("StructElement not defined!");
            throw new PresentationException("errMetaRead");
        }

        //create a label for this struct element for display in bibliographic data
        label = ViewerResourceBundle.getTranslation(se.getLabel(), null);
        if (label != null && label.equals("-")) {
            label = null;
        }
        title = se.getMetadataValue("MD_TITLE");
        docType = ViewerResourceBundle.getTranslation(se.getDocStructType(), null);
        docStructType = se.getDocStructType();
        topElement = se.isAnchor() || se.isWork();
        pi = se.getPi();
        anchor = se.isAnchor();
        filesOnly = "application".equalsIgnoreCase(getMimeType(se));
        hasImages = se.isHasImages();

        PageType pageType = PageType.determinePageType(docStructType, getMimeType(se), se.isAnchor(), hasImages, false);
        url = se.getUrl(pageType);
        List<MetadataListElement> metadataItems = DataManager.getInstance()
                .getConfiguration()
                .getMainMetadataListItemsForTemplate(metadataViewIndex, se.getDocStructType());
        for (MetadataListElement item : metadataItems) {
            if (item instanceof Metadata metadata) {
                try {
                    if (!metadata.populate(se, String.valueOf(se.getLuceneId()), metadata.getSortFields(), sessionLocale)) {
                        continue;
                    }
                    if (metadata.hasParam(SolrConstants.URN) || metadata.hasParam(SolrConstants.IMAGEURN_OAI)) {
                        if (se.isWork() || se.isAnchor()) {
                            metadataList.add(metadata);
                        }
                    } else {
                        metadataList.add(metadata);
                    }
                } catch (IndexUnreachableException | PresentationException e) {
                    logger.error("Error populating {}", metadata.getLabel(), e);
                }
            } else if (this.metadataFoldIndex == Integer.MAX_VALUE) { //only allow setting metadata fold index if it isn't already set
                this.metadataFoldIndex = metadataList.size();
            }
        }

        // Populate sidebar metadata

        if (se.isGroup()) {
            docStructType = "_GROUPS";
            groupType = se.getMetadataValue(SolrConstants.GROUPTYPE);
        }
        List<Metadata> sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(docStructType);
        if (sidebarMetadataTempList.isEmpty()) {
            // Use default if no elements are defined for the current docstruct
            sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(StringConstants.DEFAULT_NAME);
        }
        if (sidebarMetadataTempList.isEmpty()) {
            return this;
        }
        // The component is only rendered if sidebarMetadataList != null
        sidebarMetadataList = new ArrayList<>(sidebarMetadataTempList.size());
        for (Metadata metadata : sidebarMetadataTempList) {
            if (!metadata.populate(se, String.valueOf(se.getLuceneId()), metadata.getSortFields(), sessionLocale)) {
                continue;
            }
            if (metadata.getLabel().equals(SolrConstants.URN) || metadata.getLabel().equals(SolrConstants.IMAGEURN_OAI)) {
                // TODO remove bean retrieval
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                // Assign currentPage to a local variable to avoid TOCTOU race condition with concurrent AJAX requests
                PhysicalElement currentPage = (adb != null && adb.getViewManager() != null) ? adb.getViewManager().getCurrentPage() : null;
                if (currentPage != null && currentPage.getUrn() != null && !currentPage.getUrn().isEmpty()) {
                    Metadata newMetadata =
                            new Metadata(String.valueOf(se.getLuceneId()), metadata.getLabel(), metadata.getMasterValue(),
                                    currentPage.getUrn());
                    sidebarMetadataList.add(newMetadata);
                }
            } else {
                sidebarMetadataList.add(metadata);
            }
        }
        return this;
    }

    /**
     * Determines the mimetype from the structElement's metadata, or its first child if the structElement is an anchor.
     *
     * @param se {@link StructElement}
     * @return Mime type form metadata field
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private String getMimeType(StructElement se) throws PresentationException, IndexUnreachableException {
        if (mimeType == null) {
            if (se.isAnchor()) {
                mimeType = se.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
            } else {
                mimeType = se.getMetadataValue(SolrConstants.MIMETYPE);
            }
        }

        return mimeType;
    }

    public boolean isHasMetadataListFold() {
        return Integer.MAX_VALUE != this.metadataFoldIndex;
    }

    public int getMetadataFoldIndex() {
        return metadataFoldIndex;
    }

    /**
     *
     * @return true if all available metadata fields for this element are marked as hidden; false otherwise
     * @should return true if metadata list empty
     * @should return true if all metadata fields blank
     * @should return true if all metadata fields hidden
     * @should return false if non hidden metadata fields exist
     */
    public boolean isSkip() {
        for (Metadata md : metadataList) {
            if (!md.isBlank() && !md.isHideIfOnlyMetadataField()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a sorted list of all metadata types contained in metadataList.
     *
     * @return a list of distinct metadata types present in the metadata list
     */
    public List<MetadataType> getMetadataTypes() {
        if (metadataTypes == null) {
            metadataTypes = new ArrayList<>();
            for (Metadata md : getMetadataList()) {
                MetadataType mdt = new MetadataType(md.getType());
                if (!metadataTypes.contains(mdt)) {
                    metadataTypes.add(mdt);
                }
            }
            Collections.sort(metadataTypes);
        }

        return metadataTypes;
    }

    public boolean hasMetadataTypeLabels(int viewIndex) {
        return getMetadataTypes().stream().anyMatch(type -> StringUtils.isNotBlank(type.getTabName(viewIndex)));
    }

    /**
     * Returns the first instance of a Metadata object whose label matches the given field name.
     *
     * @param name Solr field name to look up
     * @return the first Metadata object whose label matches the given field name, or null if not found
     */
    public Metadata getMetadata(String name) {
        return getMetadata(name, null);
    }

    /**
     * Returns the first instance of a Metadata object whose label matches the given field name. If a language is given, a localized field name will
     * be used.
     *
     * @param name Solr field name to look up
     * @param language Optional language
     * @should return correct language metadata field
     * @should fall back to non language field if language field not found
     * @return the first Metadata object matching the field name and optional language, or null if not found
     */
    public Metadata getMetadata(String name, String language) {
        if (StringUtils.isEmpty(name) || metadataList.isEmpty()) {
            return null;
        }

        String fullFieldName = name;
        String fullFieldNameDe = name + SolrConstants.MIDFIX_LANG + "DE";
        String fullFieldNameEn = name + SolrConstants.MIDFIX_LANG + "EN";
        if (StringUtils.isNotEmpty(language)) {
            fullFieldName += SolrConstants.MIDFIX_LANG + language.toUpperCase();
        }
        Metadata fallback = null;
        Metadata fallbackDe = null;
        Metadata fallbackEn = null;
        for (Metadata md : metadataList) {
            if (md.getLabel().equals(fullFieldName)) {
                // logger.trace("{}: {}", fullFieldName, md.getValues().size()); //NOSONAR Debug
                return md;
            } else if (md.getLabel().equals(fullFieldNameDe)) {
                fallbackDe = md;
            } else if (md.getLabel().equals(fullFieldNameEn)) {
                fallbackEn = md;
            } else if (md.getLabel().equals(name)) {
                fallback = md;
            }
        }

        if (fallbackEn != null) {
            return fallbackEn;
        } else if (fallbackDe != null) {
            return fallbackDe;
        } else {
            return fallback;
        }
    }

    /**
     * getMetadata.
     *
     * @param fields Solr field names to retrieve metadata for
     * @return List of Metadata objects that match the given field names
     */
    public List<Metadata> getMetadata(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptyList();
        }

        List<Metadata> ret = new ArrayList<>(fields.size());
        for (String field : fields) {
            Metadata md = getMetadata(field);
            if (md != null) {
                ret.add(md);
            }
        }

        return ret;
    }

    /**
     * Setter for the field <code>metadataList</code>.
     *
     * @param metadataList the list of metadata fields to display in the main metadata section
     */
    public void setMetadataList(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * Getter for the field <code>metadataList</code>.
     *
     * @return the full list of {@link Metadata} objects for this element, filtered by the selected record language
     */
    public List<Metadata> getMetadataList() {
        return getMetadataList(false);
    }

    /**
     * Getter for the field <code>metadataList</code>.
     *
     * @param beforeFold if true, only list metadata before index #{@link #metadataFoldIndex}
     * @return the list of {@link Metadata} objects, optionally truncated at the fold index
     */
    public List<Metadata> getMetadataList(boolean beforeFold) {
        // Use a defensive copy of the subList to prevent ConcurrentModificationException when this
        // MetadataElement is held by a session-scoped bean (e.g., HighlightsBean) and accessed concurrently.
        List<Metadata> mdList = (beforeFold && isHasMetadataListFold())
                ? new ArrayList<>(metadataList.subList(0, this.metadataFoldIndex))
                : metadataList;
        return Metadata.filterMetadata(mdList, selectedRecordLanguage, null);
    }

    /**
     * Alias for {@link #getMetadataList(boolean) getMetadataList(true)}.
     *
     * @return the list of {@link Metadata} objects that appear before the fold index
     * @should get fold position
     */
    public List<Metadata> getMetadataListBeforeFold() {
        return getMetadataList(true);
    }

    public List<Metadata> getMetadataListAfterFold() {
        // Use a defensive copy of the subList to prevent ConcurrentModificationException (same reason as getMetadataList).
        List<Metadata> mdList = isHasMetadataListFold()
                ? new ArrayList<>(metadataList.subList(this.metadataFoldIndex, this.metadataList.size()))
                : metadataList;
        return Metadata.filterMetadata(mdList, selectedRecordLanguage, null);
    }

    /**
     * hasMetadata.
     *
     * @return true if this element has at least one non-blank metadata entry in the main metadata list, false otherwise
     */
    public boolean hasMetadata() {
        if (metadataList != null) {
            return metadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    /**
     * Checks whether all metadata fields for this element can be displayed in a single box (i.e. no table type grouped metadata are configured).
     *
     * @param type the metadata type to check
     * @return true if all metadata are not configured as single string; false otherwise
     * @should return false if at least one metadata with same type not single string
     * @should return true if all metadata of same type single string
     */
    public boolean isDisplayBoxed(int type) {
        for (Metadata md : getMetadataList()) {
            if (md.getType() != type) {
                continue;
            }
            if (!md.isSingleString()) {
                return false;
            }
        }

        return true;
    }

    /**
     * hasSidebarMetadata.
     *
     * @return true if this element has at least one non-blank metadata entry in the sidebar metadata list, false otherwise
     */
    public boolean hasSidebarMetadata() {
        if (sidebarMetadataList != null) {
            return sidebarMetadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    /**
     * Getter for the field <code>sidebarMetadataList</code>.
     *
     * @return the list of {@link Metadata} objects configured for display in the sidebar, filtered by the selected record language
     */
    public List<Metadata> getSidebarMetadataList() {
        return Metadata.filterMetadata(this.sidebarMetadataList, selectedRecordLanguage, null);
    }

    /**
     * Setter for the field <code>sidebarMetadataList</code>.
     *
     * @param sidebarMetadataList the list of metadata fields to display in the sidebar section
     */
    public void setSidebarMetadataList(List<Metadata> sidebarMetadataList) {
        this.sidebarMetadataList = sidebarMetadataList;
    }

    /**
     * isHasSidebarMetadata.
     *
     * @return true if the sidebar metadata list is non-null and not empty, false otherwise
     */
    public boolean isHasSidebarMetadata() {
        return sidebarMetadataList != null && !sidebarMetadataList.isEmpty();
    }

    /**
     * Returns the docstruct type or the group type if this is a record group.
     *
     * @return docstruct type if record; group type if group
     * @should return docstruct type if record
     * @should return group type if group
     */
    public String getDocStructTypeLabel() {
        if (StringUtils.isNotEmpty(getGroupType())) {
            return getGroupType();
        }

        return getDocStructType();
    }

    /**
     * Getter for the field <code>label</code>.
     *
     * @return the display label for this metadata element, or null if not set
     */
    public String getLabel() {
        if (StringUtils.isNotEmpty(label)) {
            return label;
        }
        return null;
    }

    /**
     * Setter for the field <code>label</code>.
     *
     * @param label display label for this metadata element
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Getter for the field <code>title</code>.
     *
     * @return the title of the record or structure element represented by this metadata element
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter for the field <code>url</code>.
     *
     * @return the URL associated with this metadata element
     */
    public String getUrl() {
        return url;
    }

    /**
     * Setter for the field <code>url</code>.
     *
     * @param url viewer URL for this element's record page
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns true if this MetadataElement represents a stand-alone record, volume or anchor element.
     *
     * @return topElement
     */
    public boolean isTopElement() {
        return topElement;
    }

    /**
     * Getter for the field <code>docType</code>.
     *
     * @return the document type string (e.g. "monograph", "periodical") of the record represented by this element
     */
    public String getDocType() {
        return docType;
    }

    /**
     * Getter for the field <code>docStructType</code>.
     *
     * @return the document structure type (e.g. "Chapter", "Article") of the record represented by this element
     */
    public String getDocStructType() {
        return docStructType;
    }

    
    void setDocStructType(String docStructType) {
        this.docStructType = docStructType;
    }

    
    public String getGroupType() {
        return groupType;
    }

    
    void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    
    public String getPi() {
        return pi;
    }

    /**
     * isAnchor.
     *
     * @return true if this metadata element belongs to an anchor (multi-volume) record, false otherwise
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * isFilesOnly.
     *
     * @return true if this metadata element represents a files-only record (no displayable image), false otherwise
     */
    public boolean isFilesOnly() {
        return filesOnly;
    }

    
    public boolean isHasImages() {
        return hasImages;
    }

    /**
     * getFirstMetadataValue.
     *
     * @param name The name of the metadata
     * @return the best available metadata value, or an empty string if no metadata was found
     */
    public String getFirstMetadataValue(String name) {
        Metadata md = getMetadata(name);
        if (md == null) {
            md = getMetadata(name, BeanUtils.getActiveDocumentBean().getSelectedRecordLanguage());
        }
        if (md != null) {
            if (StringUtils.isNotBlank(md.getMasterValue()) && !md.getMasterValue().equals("{0}") && !md.isGroup()) {
                return md.getMasterValue();
            } else if (!md.getValues().isEmpty()) {
                return md.getValues().get(0).getComboValueShort(0);
            }
        }
        return "";
    }

    /**
     * getFirstMetadataValueIfExists.
     *
     * @param name Solr field name to look up
     * @return an Optional containing the first non-blank metadata value for the field, or empty if not found
     */
    public Optional<String> getFirstMetadataValueIfExists(String name) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * getFirstMetadataValue.
     *
     * @param prefix string prepended to the metadata value
     * @param name Solr field name to look up
     * @param suffix string appended to the metadata value
     * @return the first metadata value for the given Solr field, wrapped with the given prefix and suffix
     */
    public String getFirstMetadataValue(String prefix, String name, String suffix) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return prefix + value + suffix;
        }
        return value;
    }

    /**
     * Setter for the field <code>selectedRecordLanguage</code>.
     *
     * @param language ISO language code for the selected record language
     * @return this
     */
    public MetadataElement setSelectedRecordLanguage(String language) {
        this.selectedRecordLanguage = language;
        return this;
    }

}
