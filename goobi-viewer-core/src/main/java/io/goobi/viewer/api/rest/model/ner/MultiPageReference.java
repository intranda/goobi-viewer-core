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
 * REST API model representing a contiguous range of pages (defined by a first and last page order number) that share a set of NER tag counts.
 * Implements {@link TagGroup} to allow uniform handling alongside single-page references.
 */
@JsonPropertyOrder({ "firstPage, lastPage, tags" })
public class MultiPageReference implements TagGroup {

    private final Integer firstPage;
    private final Integer lastPage;
    private List<TagCount> tags;

    /**
     * Creates a new MultiPageReference instance.
     */
    public MultiPageReference() {
        super();
        this.firstPage = null;
        this.lastPage = null;
        this.tags = new ArrayList<>();
    }

    /**
     * Creates a new MultiPageReference instance.
     *
     * @param order page order number used for both first and last page
     */
    public MultiPageReference(int order) {
        super();
        this.firstPage = order;
        this.lastPage = order;
        this.tags = new ArrayList<>();
    }

    /**
     * Creates a new MultiPageReference instance.
     *
     * @param first order number of the first page in the range
     * @param last order number of the last page in the range
     */
    public MultiPageReference(Integer first, Integer last) {
        super();
        this.firstPage = first;
        this.lastPage = last;
        this.tags = new ArrayList<>();
    }

    /**
     * Getter for the field <code>firstPage</code>.
     *
     * @return the page order number of the first page in this range
     */
    @JsonProperty("firstPage")
    public Integer getFirstPage() {
        return firstPage;
    }

    /**
     * Getter for the field <code>lastPage</code>.
     *
     * @return the page order number of the last page in this range
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (getFirstPage() != null) {
            return getFirstPage();
        }

        return 0;
    }

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
