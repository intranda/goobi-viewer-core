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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * Associates a display label with a target URL for use in navigation and breadcrumb components.
 */
public class LabeledLink implements Serializable {

    private static final long serialVersionUID = -2546718627110716169L;

    public static final LabeledLink EMPTY = new LabeledLink();

    protected IMetadataValue name;
    protected String url;
    protected int weight;

    /**
     * Internal constructor for empty value.
     */
    private LabeledLink() {

    }

    /**
     * Creates a new LabeledLink instance.
     *
     * @param name display label for the link.
     * @param url target URL of the link.
     * @param weight sort weight for ordering.
     */
    public LabeledLink(String name, String url, int weight) {
        this.name = new SimpleMetadataValue(name);
        this.url = url;
        this.weight = weight;
    }

    /**
     * Creates a new LabeledLink instance.
     *
     * @param name multilingual display label for the link.
     * @param url target URL of the link.
     * @param weight sort weight for ordering.
     */
    public LabeledLink(IMetadataValue name, String url, int weight) {
        this.name = name;
        this.url = url;
        this.weight = weight;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    /** {@inheritDoc} */
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
        LabeledLink other = (LabeledLink) obj;
        if (getName().isEmpty()) {
            if (!other.getName().isEmpty()) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        //        if (url == null) {
        //            if (other.url != null)
        //                return false;
        //        } else if (!url.equals(other.url))
        //            return false;
        return true;
    }

    /**
     * Getter for the field <code>name</code>.
     *

     */
    public String getName() {
        return name.getValue(BeanUtils.getLocale()).orElse("");
    }

    /**
     * Setter for the field <code>name</code>.
     *

     */
    public void setName(String name) {
        this.name = new SimpleMetadataValue(name);
    }

    /**
     * Setter for the field <code>name</code>.
     *
     * @param name multilingual display label to assign.
     */
    public void setName(IMetadataValue name) {
        this.name = name;
    }

    /**
     * Getter for the field <code>url</code>.
     *

     */
    public String getUrl() {
        return url;
    }

    /**
     * Setter for the field <code>url</code>.
     *

     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Getter for the field <code>weight</code>.
     *

     */
    public int getWeight() {
        return weight;
    }

    /**
     * Setter for the field <code>weight</code>.
     *

     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * isLink.
     *
     * @return a boolean.
     */
    public boolean isLink() {
        return StringUtils.isNotBlank(getUrl());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name + " : " + url + " : " + weight;
    }
}
