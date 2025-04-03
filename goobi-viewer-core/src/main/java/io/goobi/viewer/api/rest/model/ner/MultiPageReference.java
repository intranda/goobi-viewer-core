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
package io.goobi.viewer.api.rest.model.ner;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * <p>
 * MultiPageReference class.
 * </p>
 */
@JsonPropertyOrder({ "firstPage, lastPage, tags" })
public class MultiPageReference implements TagGroup {

    private final Integer firstPage;
    private final Integer lastPage;
    private List<TagCount> tags;

    /**
     * <p>
     * Constructor for MultiPageReference.
     * </p>
     */
    public MultiPageReference() {
        super();
        this.firstPage = null;
        this.lastPage = null;
        this.tags = new ArrayList<>();
    }

    /**
     * <p>
     * Constructor for MultiPageReference.
     * </p>
     *
     * @param order a int.
     */
    public MultiPageReference(int order) {
        super();
        this.firstPage = order;
        this.lastPage = order;
        this.tags = new ArrayList<>();
    }

    /**
     * <p>
     * Constructor for MultiPageReference.
     * </p>
     *
     * @param first a {@link java.lang.Integer} object.
     * @param last a {@link java.lang.Integer} object.
     */
    public MultiPageReference(Integer first, Integer last) {
        super();
        this.firstPage = first;
        this.lastPage = last;
        this.tags = new ArrayList<>();
    }

    /**
     * <p>
     * Getter for the field <code>firstPage</code>.
     * </p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @JsonProperty("firstPage")
    public Integer getFirstPage() {
        return firstPage;
    }

    /**
     * <p>
     * Getter for the field <code>lastPage</code>.
     * </p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @JsonProperty("lastPage")
    public Integer getLastPage() {
        return lastPage;
    }

    /** {@inheritDoc} */
    @Override
    @JsonProperty("tags")
    public List<TagCount> getTags() {
        return tags;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(TagGroup o) {
        return this.getFirstPage().compareTo(o.getPageOrder());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (getFirstPage() != null) {
            return getFirstPage();
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(MultiPageReference.class)) {
            if (getFirstPage() != null && getLastPage() != null) {
                return getFirstPage().equals(((MultiPageReference) obj).getFirstPage())
                        && getLastPage().equals(((MultiPageReference) obj).getLastPage());
            } else if (getFirstPage() != null) {
                return getFirstPage().equals(((MultiPageReference) obj).getFirstPage()) && ((MultiPageReference) obj).getLastPage() == null;
            } else if (getLastPage() != null) {
                return getLastPage().equals(((MultiPageReference) obj).getLastPage()) && ((MultiPageReference) obj).getFirstPage() == null;
            } else {
                return ((MultiPageReference) obj).getFirstPage() == null && ((MultiPageReference) obj).getLastPage() == null;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public int getPages() {
        if (getLastPage() == null || getFirstPage() == null) {
            return 0;
        }
        return getLastPage() - getFirstPage() + 1;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.servlets.rest.ner.TagGroup#getPageOrder()
     */
    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public Integer getPageOrder() {
        return getFirstPage();
    }

    /** {@inheritDoc} */
    @Override
    public void addTags(List<TagCount> tags) {
        for (TagCount tagCount : tags) {
            addTag(tagCount);
        }

    }

    private void addTag(TagCount tagCount) {
        int index = tags.indexOf(tagCount);
        if (index > -1) {
            tags.get(index).addReferences(tagCount.getReferences());
        } else {
            tags.add(tagCount);
        }

    }

}
