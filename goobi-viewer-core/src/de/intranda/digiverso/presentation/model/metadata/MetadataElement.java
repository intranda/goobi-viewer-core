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
package de.intranda.digiverso.presentation.model.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
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
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

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

        public String getTabName() {
            return KEY_ROOT + type;
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
    private final String selectedRecordLanguage;

    /**
     * @param se {@link StructElement}
     * @param sessionLocale
     * @param selectedRecordLanguage
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public MetadataElement(StructElement se, Locale sessionLocale, String selectedRecordLanguage)
            throws PresentationException, IndexUnreachableException, DAOException {
        if (se == null) {
            logger.error("StructElement not defined!");
            throw new PresentationException("errMetaRead");
        }

        //create a label for this struct element for display in bibliographic data
        label = Helper.getTranslation(se.getLabel(), null);
        if (label != null && label.equals("-")) {
            label = null;
        }
        title = se.getMetadataValue("MD_TITLE");
        docType = Helper.getTranslation(se.getDocStructType(), null);
        docStructType = se.getDocStructType();
        topElement = se.isAnchor() || se.isWork();
        se.getPi(); // TODO why?
        anchor = se.isAnchor();
        filesOnly = "application".equalsIgnoreCase(getMimeType(se));
        this.selectedRecordLanguage = selectedRecordLanguage;

        PageType pageType = PageType.determinePageType(docStructType, getMimeType(se), se.isAnchor(), true, false, false);
        url = se.getUrl(pageType);

        for (Metadata metadata : DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(se.getDocStructType())) {
            if (metadata.populate(se.getMetadataFields(), sessionLocale)) {
                if (metadata.hasParam(SolrConstants.URN) || metadata.hasParam(SolrConstants.IMAGEURN_OAI)) {
                    if (se.isWork() || se.isAnchor()) {
                        metadataList.add(metadata);
                    }
                } else {
                    metadataList.add(metadata);
                }
            }
        }

        // Populate sidebar metadata
        List<Metadata> sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(se.getDocStructType());
        if (sidebarMetadataTempList.isEmpty()) {
            // Use default if no elements are defined for the current docstruct
            sidebarMetadataTempList = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("_DEFAULT");
        }
        if (!sidebarMetadataTempList.isEmpty()) {
            // The component is only rendered if sidebarMetadataList != null
            sidebarMetadataList = new ArrayList<>();
            for (Metadata metadata : sidebarMetadataTempList) {
                if (metadata.populate(se.getMetadataFields(), sessionLocale)) {
                    if (metadata.getLabel().equals(SolrConstants.URN) || metadata.getLabel().equals(SolrConstants.IMAGEURN_OAI)) {
                        // TODO remove bean retrieval
                        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                        if (adb != null && adb.getViewManager() != null && adb.getViewManager().getCurrentPage() != null
                                && adb.getViewManager().getCurrentPage().getUrn() != null
                                && !adb.getViewManager().getCurrentPage().getUrn().equals("")) {
                            Metadata newMetadata =
                                    new Metadata(metadata.getLabel(), metadata.getMasterValue(), adb.getViewManager().getCurrentPage().getUrn());
                            sidebarMetadataList.add(newMetadata);
                        }
                    } else {
                        sidebarMetadataList.add(metadata);
                    }
                }
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
     * @return
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
     * @param name
     * @return
     */
    public Metadata getMetadata(String name) {
        Metadata md = getMetadata(name, null);
        return md;
    }

    /**
     * Returns the first instance of a Metadata object whose label matches the given field name. If a langauge is given, a localized field name will
     * be used.
     * 
     * @param name
     * @param language Optional language
     * @return
     * @should return correct language metadata field
     * @should fall back to non language field if language field not found
     */
    public Metadata getMetadata(String name, String language) {
        if (StringUtils.isNotEmpty(name) && metadataList != null && !metadataList.isEmpty()) {
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

        return null;
    }

    /**
     * @param metadataList the metadataList to set
     */
    public void setMetadataList(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * @return the oneMetadataList
     */
    public List<Metadata> getMetadataList() {
        return Metadata.filterMetadataByLanguage(metadataList, selectedRecordLanguage);
    }

    public boolean hasMetadata() {
        if (metadataList != null) {
            return metadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    public boolean hasSidebarMetadata() {
        if (sidebarMetadataList != null) {
            return sidebarMetadataList.stream().anyMatch(md -> !md.isBlank());
        }
        return false;
    }

    /**
     * @return the sidebarMetadataList
     */
    public List<Metadata> getSidebarMetadataList() {
        return sidebarMetadataList;
    }

    /**
     * @param sidebarMetadataList the sidebarMetadataList to set
     */
    public void setSidebarMetadataList(List<Metadata> sidebarMetadataList) {
        this.sidebarMetadataList = sidebarMetadataList;
    }

    public boolean isHasSidebarMetadata() {
        return sidebarMetadataList != null && !sidebarMetadataList.isEmpty();
    }

    public String getLabel() {
        if (StringUtils.isNotEmpty(label)) {
            return label;
        }
        return null;
    }

    public void setLabel(String string) {
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

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
     * @return the docType
     */
    public String getDocType() {
        return docType;
    }

    /**
     * @return the docStructType
     */
    public String getDocStructType() {
        return docStructType;
    }

    public boolean isAnchor() {
        return anchor;
    }

    public boolean isFilesOnly() {
        return filesOnly;
    }

    /**
     * 
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

    public Optional<String> getFirstMetadataValueIfExists(String name) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    public String getFirstMetadataValue(String prefix, String name, String suffix) {
        String value = getFirstMetadataValue(name);
        if (StringUtils.isNotBlank(value)) {
            return prefix + value + suffix;
        }
        return value;
    }
}
