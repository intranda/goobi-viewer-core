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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

public class LabeledLink implements Serializable {

    private static final long serialVersionUID = -2546718627110716169L;

    protected IMetadataValue name;
    protected String url;
    protected int weight;

    /**
     * 
     * @param name
     * @param url
     * @param weight
     */
    public LabeledLink(String name, String url, int weight) {
        this.name = new SimpleMetadataValue(name);
        this.url = url;
        this.weight = weight;
    }

    /**
     * 
     * @param name
     * @param url
     * @param weight
     */
    public LabeledLink(IMetadataValue name, String url, int weight) {
        this.name = name;
        this.url = url;
        this.weight = weight;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LabeledLink other = (LabeledLink) obj;
        if (getName().isEmpty()) {
            if (!other.getName().isEmpty())
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
//        if (url == null) {
//            if (other.url != null)
//                return false;
//        } else if (!url.equals(other.url))
//            return false;
        return true;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name.getValue(BeanUtils.getLocale()).orElse("");
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = new SimpleMetadataValue(name);
    }

    public void setName(IMetadataValue name) {
        this.name = name;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isLink() {
        return StringUtils.isNotBlank(getUrl());
    }

    @Override
    public String toString() {
        return name + " : " + url + " : " + weight;
    }
}
