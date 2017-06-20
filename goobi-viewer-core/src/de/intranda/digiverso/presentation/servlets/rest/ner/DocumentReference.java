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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
@XmlType(propOrder = { "pi", "pages" })
@JsonInclude(Include.NON_NULL)
public class DocumentReference {

    private final String pi;
    private List<TagGroup> pageRanges;

    public DocumentReference() {
        super();
        this.pi = null;
        pageRanges = new ArrayList<>();
    }

    public DocumentReference(String piTopStruct) {
        super();
        this.pi = piTopStruct;
        pageRanges = new ArrayList<>();
    }

    @XmlElement
    public String getPi() {
        return pi;
    }

    public void setPageRanges(List<TagGroup> ranges) {
        this.pageRanges = ranges;
        Collections.sort(this.pageRanges);
    }

    public void addPageRange(TagGroup range) {
        this.pageRanges.add(range);
        Collections.sort(this.pageRanges);
    }

    public void addPageRanges(Collection<TagGroup> ranges) {
        this.pageRanges.addAll(ranges);
        Collections.sort(this.pageRanges);
    }

    @JsonProperty("pages")
    @XmlElementWrapper(name = "pages")
    @XmlElement(name = "pages")
    public List<TagGroup> getPageRanges() {
        return pageRanges;
    }

    /**
     * 
     * @param order 1-based page number
     * @return
     */
    public TagGroup getPageRange(int startPage) {
        try {
            ListIterator<TagGroup> forward = pageRanges.listIterator(startPage / getRangeSize());
            ListIterator<TagGroup> backward = pageRanges.listIterator(startPage / getRangeSize());
            while (forward.hasNext() || backward.hasPrevious()) {
                if (forward.hasNext()) {
                    TagGroup next = forward.next();
                    if (next.getPageOrder().equals(startPage)) {
                        return next;
                    }
                }
                if (backward.hasPrevious()) {
                    TagGroup previous = backward.previous();
                    if (previous.getPageOrder().equals(startPage)) {
                        return previous;
                    }
                }
            }
        } catch (ArithmeticException e) {
        }

        return null;
    }

    public int getRangeSize() {
        if (pageRanges != null && !pageRanges.isEmpty()) {
            return pageRanges.get(0).getPages();
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (pi != null) {
            return pi.hashCode();
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(DocumentReference.class)) {
            if (pi != null) {
                return pi.equals(((DocumentReference) obj).getPi());
            }
            return ((DocumentReference) obj).getPi() == null;
        }

        return false;
    }

}
