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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Collection tree element.
 */
public class BrowseDcElement implements Comparable<BrowseDcElement>, Serializable {

    private static final long serialVersionUID = -3308596220913009726L;

    public static String split = DataManager.getInstance().getConfiguration().getSplittingCharacter();

    private String name;
    private long number;
    private String sortField = "-";
    private boolean showSubElements = false;
    private boolean hasSubelements = false;
    private boolean showDescription = false;
    private int displayNumberOfVolumesLevel;
    private BrowseElementInfo info;

    public BrowseDcElement(String name, long number, String field, String sortField) throws PresentationException {
        this.name = name != null ? name.intern() : name;
        this.number = number;
        this.sortField = sortField;
        if (StringUtils.isEmpty(this.sortField)) {
            this.sortField = "-";
        } else {
            this.sortField = this.sortField.intern();
        }
        this.displayNumberOfVolumesLevel = DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(field);
        this.info = new SimpleBrowseElementInfo(name);
    }
    
    /**
     * 
     */
    public BrowseDcElement(BrowseDcElement blueprint) {
        this.name = blueprint.name;
        this.number = blueprint.number;
        this.sortField = blueprint.sortField;
        this.hasSubelements = blueprint.hasSubelements;
        this.showDescription = blueprint.showDescription;
        this.info = blueprint.info;
    }

    @Override
    public int compareTo(BrowseDcElement o) {
        return this.getName().compareTo(o.getName());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
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
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    public String getLabel() {
        if(getInfo() != null) {
            return getInfo().getName();
        } else {
            return name;
        }
    }

    /**
     * Returns the message key for the collection description.
     *
     * @return
     */
    public String getDescription() {
        if(getInfo() != null) {
            return getInfo().getDescription();
        } else {            
            return new StringBuilder(name).append("_DESCRIPTION").toString();
        }
    }

    /**
     * Returns the message key for the collection representative image url
     *
     * @return
     */
    public String getRepresentant() {
        if(getInfo() != null && getInfo().getIconURI() != null) {
            return getInfo().getIconURI().toString();
        } else {            
            return new StringBuilder(name).append("_REPRESENTANT").toString();
        }
    }

    /**
     * @return the name
     */
    public String getLuceneName() {
        return getName();
    }

    public void addToNumber(long inNumber) {
        number += inNumber;
    }

    public void setHasSubelements(boolean hasSubelements) {
        this.hasSubelements = hasSubelements;
    }

    public boolean isHasSubelements() {
        return hasSubelements;
    }

    public boolean isDisplayNumberOfVolumes() {
        return getLevel() >= displayNumberOfVolumesLevel;
    }

    /**
     *
     * @return number of elements
     */
    public long getNumberOfVolumes() {
        return number;
    }

    public int getLevel() {
        if (StringUtils.isNotEmpty(split)) {
            return name.split("\\" + split).length - 1;
        }
        return 0;
    }

    public String getParentName() {
        if (getLevel() > 0) {
            String parentTitle = name;
            return parentTitle.substring(0, parentTitle.lastIndexOf(split));
        }
        return "";
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public boolean isShowSubElements() {
        return showSubElements;
    }

    public void setShowSubElements(boolean showSubElements) {
        this.showSubElements = showSubElements;
    }

    /**
     * @return the showDescription
     */
    public boolean isShowDescription() {
        return showDescription;
    }

    /**
     * @param showDescription the showDescription to set
     */
    public void setShowDescription(boolean showDescription) {
        this.showDescription = showDescription;
    }

    /**
     * Returns the RSS feed URL for this collection.
     *
     * @return
     */
    public String getRssUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(NavigationHelper.URL_RSS).append("?q=(DC:").append(name)
                .append(" OR DC:").append(name).append(".*) AND (ISWORK:true OR ISANCHOR:true)");

        return sb.toString();
    }
    
    public String getRssUrl(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(ServletUtils.getServletPathWithHostAsUrlFromRequest(request)).append('/').append(NavigationHelper.URL_RSS).append("?q=(DC:").append(name)
                .append(" OR DC:").append(name).append(".*) AND (ISWORK:true OR ISANCHOR:true)");

        return sb.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(BrowseElementInfo info) {
        this.info = info;
    }

    /**
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
            return Helper.getTranslation(o1.getName(), null).compareTo(Helper.getTranslation(o2.getName(), null));
        }

    }

    
    public boolean hasCMSDescription() {
    	return !(this.info instanceof SimpleBrowseElementInfo);
    }
    
    public boolean hasIcon() {
        return getInfo() != null && getInfo().getIconURI() != null;
    }
    
    public URI getIcon() {
        if(getInfo() != null) {
            return getInfo().getIconURI();
        } else {
            return null;
        }
    }
}
