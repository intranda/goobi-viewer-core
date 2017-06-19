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
package de.intranda.digiverso.presentation.servlets.rest.ner;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@XmlRootElement
@XmlType(propOrder = { "firstPage, lastPage, tags" })
@JsonPropertyOrder({ "firstPage, lastPage, tags" })
public class MultiPageReference implements TagGroup {

    private final Integer firstPage;
    private final Integer lastPage;
    private List<TagCount> tags;

    public MultiPageReference() {
        super();
        this.firstPage = null;
        this.lastPage = null;
        this.tags = new ArrayList<>();
    }

    public MultiPageReference(int order) {
        super();
        this.firstPage = order;
        this.lastPage = order;
        this.tags = new ArrayList<>();
    }

    public MultiPageReference(Integer first, Integer last) {
        super();
        this.firstPage = first;
        this.lastPage = last;
        this.tags = new ArrayList<>();
    }

    @JsonProperty("firstPage")
    @XmlElement
    public Integer getFirstPage() {
        return firstPage;
    }

    @JsonProperty("lastPage")
    @XmlElement
    public Integer getLastPage() {
        return lastPage;
    }

    @Override
    @JsonProperty("tags")
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    public List<TagCount> getTags() {
        return tags;
    }

    @Override
    public int compareTo(TagGroup o) {
        return this.getFirstPage().compareTo(o.getPageOrder());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
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
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(MultiPageReference.class)) {
            if (getFirstPage() != null && getLastPage() != null) {
                return getFirstPage().equals(((MultiPageReference) obj).getFirstPage()) && getLastPage().equals(((MultiPageReference) obj)
                        .getLastPage());
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

    @Override
    @JsonIgnore
    public int getPages() {
        if (getLastPage() == null || getFirstPage() == null) {
            return 0;
        }
        return getLastPage() - getFirstPage() + 1;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.servlets.rest.ner.TagGroup#getPageOrder()
     */
    @JsonIgnore
    @Override
    public Integer getPageOrder() {
        return getFirstPage();
    }

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
