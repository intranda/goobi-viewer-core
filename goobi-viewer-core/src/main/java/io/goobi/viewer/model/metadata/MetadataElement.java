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
package io.goobi.viewer.model.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * MetadataElement class.
 * </p>
 */
public class MetadataElement {

    /**
     * Wrapper class for the metadata type numerical value. Needed only for retrieving the proper message key for each type...
     */
    public class MetadataType implements Comparable<MetadataType> {

        private static final String KEY_ROOT = "metadataTab";

        private int type = 0;

        public MetadataType() {
            // the emptiness inside
        }

        public MetadataType(int type) {
            this.type = type;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + type;
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
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
            if (type != other.type) {
                return false;
            }
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
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

        @Deprecated
        public String getTabName() {
            return KEY_ROOT + type;
        }

        /**
         * 
         * @param viewIndex Metadata view index
         * @return Message key for this tab
         * @should return correct message key
         */
        public String getTabName(int viewIndex) {
            return KEY_ROOT + "_" + viewIndex + "_" + type;
        }

        public void setTabName(String tabName) {
        }

        /**
         * @return the type
         */
        public int getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(int type) {
            this.type = type;
        }

        private MetadataElement getOuterType() {
            return MetadataElement.this;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MetadataElement.class);

    private String label = null;
    private String title = null;
    private String docType = null;
    private String docStructType = null;
    private String mimeType = null;
    private String url = null;
    private List<Metadata> metadataList = new ArrayList<>();
    private List<Metadata> sidebarMetadataList = null;
    private List<MetadataType> metadataTypes;
    /** True if this ISWORK=true or ISANCHOR=true. */
    private final boolean topElement;
    private final boolean anchor;
    private final boolean filesOnly;
    private String selectedRecordLanguage;

    /**
     * <p>
     * Constructor for MetadataElement.
     * </p>
     *
     * @param se {@link io.goobi.viewer.model.viewer.StructElement}
     * @param index
     * @param sessionLocale a {@link java.util.Locale} object.
     * @param selectedRecordLanguage a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public MetadataElement(StructElement se, int index, Locale sessionLocale, String selectedRecordLanguage)
            throws PresentationException, IndexUnreachableException, DAOException {
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
        se.getPi(); // TODO why?
        anchor = se.isAnchor();
        filesOnly = "application".equalsIgnoreCase(getMimeType(se));
        this.selectedRecordLanguage = selectedRecordLanguage;

        PageType pageType = PageType.determinePageType(docStructType, getMimeType(se), se.isAnchor(), true, false);
        url = se.getUrl(pageType);

        for (Metadata metadata : DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(index, se.getDocStructType())) {
            try {
                if (!metadata.populate(se, sessionLocale)) {
                    continue;
                }
                if (metadata.hasParam(SolrConstants.URN) || metadata.hasParam(SolrConstants.IMAGEURN_OAI)) {
                    if (se.isWork() || se.isAnchor()) {
                        metadataList.add(metadata);
                    }
                } else {
                    metadataList.add(metadata);
                }
            } catch (Throwable e) {
                logger.error("Error populating " + metadata.getLabel(), e);
            }
        }

        // Populate sidebar metadata

        if (se.isGroup()) {
            docStructType = "_GROUPS";
        }
        List<Metadata> sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(docStructType);
        if (sidebarMetadataTempList.isEmpty()) {
            // Use default if no elements are defined for the current docstruct
            sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("_DEFAULT");
        }
        if (sidebarMetadataTempList.isEmpty()) {
            return;
        }
        // The component is only rendered if sidebarMetadataList != null
        sidebarMetadataList = new ArrayList<>(sidebarMetadataTempList.size());
        for (Metadata metadata : sidebarMetadataTempList) {
            if (!metadata.populate(se, sessionLocale)) {
                continue;
            }
            if (metadata.getLabel().equals(SolrConstants.URN) || metadata.getLabel().equals(SolrConstants.IMAGEURN_OAI)) {
                // TODO remove bean retrieval
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                if (adb != null && adb.getViewManager() != null && adb.getViewManager().getCurrentPage() != null
                        && adb.getViewManager().getCurrentPage().getUrn() != null && !adb.getViewManager().getCurrentPage().getUrn().equals("")) {
                    Metadata newMetadata =
                            new Metadata(String.valueOf(se.getLuceneId()), metadata.getLabel(), metadata.getMasterValue(),
                                    adb.getViewManager().getCurrentPage().getUrn());
                    sidebarMetadataList.add(newMetadata);
                }
            } else {
                sidebarMetadataList.add(metadata);
            }
        }
    }

    /**
     * Determines the mimetype from the structElement's metadata, or its first child if the structElement is an anchor
     *
     * @param se
     * @return
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

    /**
     * Returns a sorted list of all metadata types contained in metadataList.
     *
     * @return a {@link java.util.List} object.
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

    /**
     * Returns the first instance of a Metadata object whose label matches the given field name.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.metadata.Metadata} object.
     */
    public Metadata getMetadata(String name) {
        Metadata md = getMetadata(name, null);
        return md;
    }

    /**
     * Returns the first instance of a Metadata object whose label matches the given field name. If a language is given, a localized field name will
     * be used.
     *
     * @param name a {@link java.lang.String} object.
     * @param language Optional language
     * @should return correct language metadata field
     * @should fall back to non language field if language field not found
     * @return a {@link io.goobi.viewer.model.metadata.Metadata} object.
     */
    public Metadata getMetadata(String name, String language) {
        if (StringUtils.isEmpty(name) || metadataList.isEmpty()) {
            return null;
        }

        String fullFieldName = name;
        String fullFieldNameDe = name + SolrConstants._LANG_ + "DE";
        String fullFieldNameEn = name + SolrConstants._LANG_ + "EN";
        if (StringUtils.isNotEmpty(language)) {
            fullFieldName += SolrConstants._LANG_ + language.toUpperCase();
        }
        Metadata fallback = null;
        Metadata fallbackDe = null;
        Metadata fallbackEn = null;
        for (Metadata md : metadataList) {
            if (md.getLabel().equals(fullFieldName)) {
                // logger.trace("{}: {}", fullFieldName, md.getValues().size());
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
     * <p>
     * getMetadata.
     * </p>
     *
     * @param fields a {@link java.util.List} object.
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
     * <p>
     * Setter for the field <code>metadataList</code>.
     * </p>
     *
     * @param metadataList the metadataList to set
     */
    public void setMetadataList(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * <p>
     * Getter for the field <code>metadataList</code>.
     * </p>
     *
     * @return the oneMetadataList
     */
    public List<Metadata> getMetadataList() {
        return Metadata.filterMetadata(metadataList, selectedRecordLanguage, null);
    }

    /**
     * <p>
     * hasMetadata.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasMetadata() {
        if (metadataList != null) {
            return metadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    /**
     * <p>
     * hasSidebarMetadata.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasSidebarMetadata() {
        if (sidebarMetadataList != null) {
            return sidebarMetadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>sidebarMetadataList</code>.
     * </p>
     *
     * @return the sidebarMetadataList
     */
    public List<Metadata> getSidebarMetadataList() {
        return Metadata.filterMetadata(this.sidebarMetadataList, selectedRecordLanguage, null);
    }

    /**
     * <p>
     * Setter for the field <code>sidebarMetadataList</code>.
     * </p>
     *
     * @param sidebarMetadataList the sidebarMetadataList to set
     */
    public void setSidebarMetadataList(List<Metadata> sidebarMetadataList) {
        this.sidebarMetadataList = sidebarMetadataList;
    }

    /**
     * <p>
     * isHasSidebarMetadata.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasSidebarMetadata() {
        return sidebarMetadataList != null && !sidebarMetadataList.isEmpty();
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLabel() {
        if (StringUtils.isNotEmpty(label)) {
            return label;
        }
        return null;
    }

    /**
     * <p>
     * Setter for the field <code>label</code>.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     */
    public void setLabel(String string) {
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>
     * Getter for the field <code>url</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Setter for the field <code>url</code>.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
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
     * <p>
     * Getter for the field <code>docType</code>.
     * </p>
     *
     * @return the docType
     */
    public String getDocType() {
        return docType;
    }

    /**
     * <p>
     * Getter for the field <code>docStructType</code>.
     * </p>
     *
     * @return the docStructType
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * <p>
     * isAnchor.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * <p>
     * isFilesOnly.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isFilesOnly() {
        return filesOnly;
    }

    /**
     * <p>
     * getFirstMetadataValue.
     * </p>
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
     * <p>
     * getFirstMetadataValueIfExists.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<String> getFirstMetadataValueIfExists(String name) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getFirstMetadataValue.
     * </p>
     *
     * @param prefix a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @param suffix a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFirstMetadataValue(String prefix, String name, String suffix) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return prefix + value + suffix;
        }
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>selectedRecordLanguage</code>.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     */
    public void setSelectedRecordLanguage(String language) {
        this.selectedRecordLanguage = language;
    }
}
