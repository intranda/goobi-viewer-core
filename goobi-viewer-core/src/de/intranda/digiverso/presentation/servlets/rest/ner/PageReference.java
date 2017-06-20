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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class PageReference implements TagGroup {

    private final Integer pageOrder;
    private List<TagCount> tags;

    public PageReference() {
        super();
        this.pageOrder = null;
        this.tags = new ArrayList<>();
    }

    public PageReference(int pageOrder) {
        super();
        this.pageOrder = pageOrder;
        this.tags = new ArrayList<>();
    }

    @Override
    @XmlElement
    public Integer getPageOrder() {
        return pageOrder;
    }

    @Override
    @JsonProperty("tags")
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    public List<TagCount> getTags() {
        return tags;
    }

    /**
     * @param nerTags
     */
    public void setTags(List<TagCount> nerTags) {
        this.tags = nerTags;

    }

    @Override
    public int compareTo(TagGroup o) {
        return this.getPageOrder().compareTo(o.getPageOrder());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (getPageOrder() != null) {
            return getPageOrder();
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(PageReference.class)) {
            if (getPageOrder() != null) {
                return getPageOrder().equals(((PageReference) obj).getPageOrder());
            }
            return ((PageReference) obj).getPageOrder() == null;
        }

        return false;
    }

    @JsonIgnore
    @Override
    public int getPages() {
        return 1;
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
