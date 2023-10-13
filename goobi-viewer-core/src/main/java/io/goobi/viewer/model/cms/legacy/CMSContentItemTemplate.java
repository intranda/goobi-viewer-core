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
package io.goobi.viewer.model.cms.legacy;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.CMSContentItem;
import io.goobi.viewer.model.cms.pages.content.ContentItemMode;
import io.goobi.viewer.model.cms.pages.content.types.CMSBrowseContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSDocumentContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSGeomapContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSGlossaryContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSImageListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediaContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediumTextContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMetadataContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSPageListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSRSSContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSRecordListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSearchContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSliderContent;
import io.goobi.viewer.model.jsf.JsfComponent;

/**
 * A contentItem to be used in a CMSPage template. Stores a value for content item mode
 *
 * @author Florian Alpers
 */
public class CMSContentItemTemplate implements Comparable<CMSContentItemTemplate> {

    private String mediaFilter = "";
    private ContentItemMode mode = ContentItemMode.simple;
    private String inlineHelp = null;
    private String itemLabel = "";
    private int order = 0;
    private boolean mandatory = false;
    private boolean preview = false;
    private boolean ignoreCollectionHierarchy = false;
    private final CMSContentItemType type;
    private String itemId;

    /**
     * For SOLRQUERY items: If true, show number of hits and sort order options on page and hide them in cms backend
     */
    boolean hitListOptions = false;
    boolean randomizeItems = false;

    /**
     * <p>
     * Constructor for CMSContentItemTemplate.
     * </p>
     *
     * @param type a CMSContentItemType object.
     */
    public CMSContentItemTemplate(CMSContentItemType type) {
        this.type = type;
    }

    public CMSContentItemType getType() {
        return type;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * <p>
     * Setter for the field <code>mode</code>.
     * </p>
     *
     * @param mode the mode to set
     */
    public void setMode(ContentItemMode mode) {
        this.mode = mode;
    }

    /** {@inheritDoc} */

    public ContentItemMode getMode() {
        return mode;
    }

    /** {@inheritDoc} */

    public boolean isHasInlineHelp() {
        return StringUtils.isNotBlank(inlineHelp);
    }

    /**
     * {@inheritDoc}
     *
     * Message key to display when clicking the inline help button
     */

    public String getInlineHelp() {
        return this.inlineHelp;
    }

    /**
     * <p>
     * Setter for the field <code>inlineHelp</code>.
     * </p>
     *
     * @param inlineHelp the inlineHelp to set
     */
    public void setInlineHelp(String inlineHelp) {
        this.inlineHelp = inlineHelp;
    }

    /** {@inheritDoc} */

    public String getMediaFilter() {
        return mediaFilter;
    }

    /**
     * <p>
     * Setter for the field <code>mediaFilter</code>.
     * </p>
     *
     * @param mediaFilter the mediaFilter to set
     */
    public void setMediaFilter(String mediaFilter) {
        this.mediaFilter = mediaFilter == null ? "" : mediaFilter;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSContentItem#isPreview()
     */
    /** {@inheritDoc} */

    public boolean isPreview() {
        return this.preview;
    }

    /**
     * <p>
     * Setter for the field <code>preview</code>.
     * </p>
     *
     * @param preview the preview to set
     */
    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    /**
     * @param mandatory the mandatory to set
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSContentItem#isMandatory()
     */

    public boolean isMandatory() {
        return this.mandatory;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSContentItem#setItemLabel(java.lang.String)
     */
    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSContentItem#getItemLabel()
     */

    public String getItemLabel() {
        return this.itemLabel;
    }

    /**
     * @return the ignoreCollectionHierarchy
     */
    public boolean isIgnoreCollectionHierarchy() {
        return ignoreCollectionHierarchy;
    }

    /**
     * @param ignoreCollectionHierarchy the ignoreCollectionHierarchy to set
     */
    public void setIgnoreCollectionHierarchy(boolean ignoreCollectionHierarchy) {
        this.ignoreCollectionHierarchy = ignoreCollectionHierarchy;
    }

    /**
     * @return the hitListOptions
     */
    public boolean isHitListOptions() {
        return hitListOptions;
    }

    /**
     * @param hitListOptions the hitListOptions to set
     */
    public void setHitListOptions(boolean hitListOptions) {
        this.hitListOptions = hitListOptions;
    }

    /**
     * @param radomizeItems the radomizeItems to set
     */
    public void setRandomizeItems(boolean radomizeItems) {
        this.randomizeItems = radomizeItems;
    }

    /**
     *
     */
    public boolean isRandomizeItems() {
        return this.randomizeItems;
    }

    @Override
    public int compareTo(CMSContentItemTemplate o) {
        return Integer.compare(this.order, o.getOrder());
    }

    public CMSContentItem createCMSContentItem(CMSComponent component) {

        CMSContent content = createCMSContent(this.type, this.mediaFilter);
        if (content != null) {
            JsfComponent jsf = new JsfComponent("cms/components/backend/content", content.getBackendComponentName());
            return new CMSContentItem(this.itemId, content, this.itemLabel, this.inlineHelp, null, jsf, component, this.isMandatory());
        }
        return null;
    }

    /**
     * 
     * @param type
     * @return
     */
    private static CMSContent createCMSContent(CMSContentItemType type, String mediaFilter) {
        switch (type.name()) {
            case "TEXT":
                return new CMSShortTextContent();
            case "HTML":
                return new CMSMediumTextContent();
            case "SOLRQUERY":
                return new CMSRecordListContent();
            case "PAGELIST":
                return new CMSPageListContent();
            case "COLLECTION":
                return new CMSCollectionContent();
            case "TILEGRID":
                return new CMSImageListContent();
            case "RSS":
                return new CMSRSSContent();
            case "SEARCH":
                return new CMSSearchContent();
            case "GLOSSARY":
                return new CMSGlossaryContent();
            case "MEDIA":
                if (StringUtils.isNotBlank(mediaFilter) && mediaFilter.toLowerCase().contains(".pdf")) {
                    return new CMSDocumentContent();
                } else {
                    return new CMSMediaContent();
                }
            case "METADATA":
                return new CMSMetadataContent();
            case "GEOMAP":
                return new CMSGeomapContent();
            case "SLIDER":
                return new CMSSliderContent();
            case "BROWSETERMS":
                return new CMSBrowseContent();
            default:
                return null;
        }
    }
}
