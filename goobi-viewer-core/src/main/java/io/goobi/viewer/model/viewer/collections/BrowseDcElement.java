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
package io.goobi.viewer.model.viewer.collections;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Collection tree element.
 */
public class BrowseDcElement implements Comparable<BrowseDcElement>, Serializable {

    private static final long serialVersionUID = -3308596220913009726L;

    /** Collection name */
    private final String name;
    private final String field;
    private final String splittingChar;
    private long number;
    private String singleRecordUrl = null;
    private String sortField = "-";
    private boolean showSubElements = false;
    private boolean hasSubelements = false;
    private boolean showDescription = false;
    private int displayNumberOfVolumesLevel;
    private BrowseElementInfo info;
    /**
     * A list of metadata values of a specified SORL field contained in any volumes within the collection. Used to group collections into groups with
     * matching elements in "facetValues"
     */
    private List<String> facetValues = new ArrayList<>();

    /**
     * <p>
     * Constructor for BrowseDcElement.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param number a long.
     * @param field a {@link java.lang.String} object.
     * @param sortField a {@link java.lang.String} object.
     * @param splittingChar
     * @param displayNumberOfVolumesLevel
     */
    public BrowseDcElement(String name, long number, String field, String sortField, String splittingChar, int displayNumberOfVolumesLevel) {
        this.name = name != null ? name.intern() : name;
        this.field = field;
        this.number = number;
        this.sortField = sortField;
        if (StringUtils.isEmpty(this.sortField)) {
            this.sortField = "-";
        } else {
            this.sortField = this.sortField.intern();
        }
        this.displayNumberOfVolumesLevel = displayNumberOfVolumesLevel;
        this.info = new SimpleBrowseElementInfo(name);
        this.splittingChar = splittingChar;
    }

    /**
     * <p>
     * Constructor for BrowseDcElement.
     * </p>
     *
     * @param blueprint a {@link io.goobi.viewer.model.viewer.collections.BrowseDcElement} object.
     */
    public BrowseDcElement(BrowseDcElement blueprint) {
        this.name = blueprint.name;
        this.field = blueprint.field;
        this.splittingChar = blueprint.splittingChar;
        this.number = blueprint.number;
        this.facetValues = blueprint.facetValues;
        this.sortField = blueprint.sortField;
        this.hasSubelements = blueprint.hasSubelements;
        this.showDescription = blueprint.showDescription;
        this.info = blueprint.info;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(BrowseDcElement o) {
        return this.getName().compareTo(o.getName());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        //        BrowseDcElement in = (BrowseDcElement) obj;
        //        return name.equals(in.getName());
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BrowseDcElement other = (BrowseDcElement) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equalsIgnoreCase(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>. If the <code>CMSCollection</code> translation is desired, use <code>getLabel()</code> instead.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getField() {
        return field;
    }

    /**
     * <p>
     * getLabel.
     * </p>
     *
     * @return <code>CMSCollection</code> translation, if ava ilable; name otherwise
     */
    public String getLabel() {
        if (getInfo() != null) {
            return getInfo().getName();
        }
        return name;
    }

    /**
     * Returns the message key for the collection description.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        if (getInfo() != null) {
            return getInfo().getDescription();
        }
        return new StringBuilder(name).append("_DESCRIPTION").toString();
    }

    /**
     * Returns the message key for the collection description for the given language.
     *
     * @param language Requested language (ISO 639-1)
     * @return {@link String}
     */
    public String getDescription(String language) {
        if (getInfo() != null) {
            return getInfo().getDescription(language);
        }
        return new StringBuilder(name).append("_DESCRIPTION").toString();
    }

    /**
     * Returns the message key for the collection representative image url
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRepresentant() {
        if (getInfo() != null && getInfo().getIconURI() != null) {
            return getInfo().getIconURI().toString();
        }
        return new StringBuilder(name).append("_REPRESENTANT").toString();
    }

    /**
     * <p>
     * getLuceneName.
     * </p>
     *
     * @return the name
     */
    public String getLuceneName() {
        return getName();
    }

    /**
     * <p>
     * addToNumber.
     * </p>
     *
     * @param inNumber a long.
     */
    public void addToNumber(long inNumber) {
        number += inNumber;
    }

    /**
     * <p>
     * Setter for the field <code>hasSubelements</code>.
     * </p>
     *
     * @param hasSubelements a boolean.
     */
    public void setHasSubelements(boolean hasSubelements) {
        this.hasSubelements = hasSubelements;
    }

    /**
     * <p>
     * isHasSubelements.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasSubelements() {
        return hasSubelements;
    }

    /**
     * <p>
     * isDisplayNumberOfVolumes.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayNumberOfVolumes() {
        return getLevel() >= displayNumberOfVolumesLevel;
    }

    /**
     * <p>
     * getNumberOfVolumes.
     * </p>
     *
     * @return number of elements
     */
    public long getNumberOfVolumes() {
        return number;
    }

    /**
     * @return the singleRecordUrl
     */
    public String getSingleRecordUrl() {
        return singleRecordUrl;
    }

    /**
     * @param singleRecordUrl the singleRecordUrl to set
     */
    public void setSingleRecordUrl(String singleRecordUrl) {
        this.singleRecordUrl = singleRecordUrl;
    }

    /**
     * <p>
     * getLevel.
     * </p>
     *
     * @return a int.
     */
    public int getLevel() {
        if (StringUtils.isNotEmpty(splittingChar)) {
            return name.split("\\" + splittingChar).length - 1;
        }
        return 0;
    }

    /**
     * <p>
     * getParentName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getParentName() {
        if (getLevel() > 0) {
            String parentTitle = name;
            return parentTitle.substring(0, parentTitle.lastIndexOf(splittingChar));
        }
        return "";
    }

    /**
     * <p>
     * Getter for the field <code>sortField</code>.
     * </p>
     *
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * <p>
     * Setter for the field <code>sortField</code>.
     * </p>
     *
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * <p>
     * isShowSubElements.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isShowSubElements() {
        return showSubElements;
    }

    /**
     * <p>
     * Setter for the field <code>showSubElements</code>.
     * </p>
     *
     * @param showSubElements a boolean.
     */
    public void setShowSubElements(boolean showSubElements) {
        this.showSubElements = showSubElements;
    }

    /**
     * <p>
     * isShowDescription.
     * </p>
     *
     * @return the showDescription
     */
    public boolean isShowDescription() {
        return showDescription;
    }

    /**
     * <p>
     * Setter for the field <code>showDescription</code>.
     * </p>
     *
     * @param showDescription the showDescription to set
     */
    public void setShowDescription(boolean showDescription) {
        this.showDescription = showDescription;
    }

    /**
     * Returns the RSS feed URL for this collection using the JSF context.
     *
     * @return RSS feed URL for this collection
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRssUrl() throws ViewerConfigurationException {
        return buildRssUrl();
    }

    /**
     *
     * @return +({field}:{name} {field}:{name}.*) +(ISWORK:* ISANCHOR:*)
     */
    public String getSolrFilterQuery() {
        return String.format("+(%s:\"%s\" %s:\"%s.*\") +(ISWORK:* ISANCHOR:*)", field, name, field, name);
    }

    /**
     *
     * @return RSS feed URL for this collection
     */
    private String buildRssUrl() {
        String query = new StringBuilder()
                .append("(")
                .append(field)
                .append(':')
                .append(name)
                .append(SolrConstants.SOLR_QUERY_OR)
                .append(field)
                .append(':')
                .append(name)
                .append(".*) AND (ISWORK:true OR ISANCHOR:true)")
                .toString();

        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);

        if (urls == null) {

            try {
                return new StringBuilder().append(DataManager.getInstance().getConfiguration().getRestApiUrl())
                        .append("rss/search/")
                        .append(URLEncoder.encode(query, SearchBean.URL_ENCODING))
                        .append("/-/-/-/")
                        .toString();
            } catch (UnsupportedEncodingException e) {
                return new StringBuilder().append(DataManager.getInstance().getConfiguration().getRestApiUrl())
                        .append("rss/search/")
                        .append(query)
                        .append("/-/-/-/")
                        .toString();
            }
        }

        return urls.path(ApiUrls.RECORDS_RSS)
                .query("query", query)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>info</code>.
     * </p>
     *
     * @param info the info to set
     */
    public void setInfo(BrowseElementInfo info) {
        this.info = info;
    }

    /**
     * <p>
     * Getter for the field <code>info</code>.
     * </p>
     *
     * @return the info
     */
    public BrowseElementInfo getInfo() {
        return info;
    }

    public static class TranslationComparator implements Comparator<BrowseDcElement>, Serializable {

        private static final long serialVersionUID = -2594688666989841956L;

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(BrowseDcElement o1, BrowseDcElement o2) {
            return ViewerResourceBundle.getTranslation(o1.getName(), null).compareTo(ViewerResourceBundle.getTranslation(o2.getName(), null));
        }
    }

    /**
     * <p>
     * hasCMSDescription.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasCMSDescription() {
        return !(this.info instanceof SimpleBrowseElementInfo) && StringUtils.isNotBlank(info.getDescription());
    }

    /**
     * <p>
     * hasIcon.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasIcon() {
        return getInfo() != null && getInfo().getIconURI() != null;
    }

    /**
     * <p>
     * getIcon.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIcon() {
        if (getInfo() != null) {
            return getInfo().getIconURI();
        }

        return null;
    }

    /**
     * @return the facetValues
     */
    public List<String> getFacetValues() {
        return facetValues;
    }

    /**
     * @param facetValues the facetValues to set
     */
    public void setFacetValues(Collection<String> facetValues) {
        this.facetValues = new ArrayList<>(facetValues);
    }

}
