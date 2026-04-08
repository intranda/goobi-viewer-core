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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REST API model representing a digitized document identified by its persistent identifier (PI), together with the NER tag groups
 * found across its pages.
 * Page ranges are kept in sorted order and are accessible both as a whole list and by individual page order number.
 */
@JsonInclude(Include.NON_NULL)
public class DocumentReference {

    private final String pi;
    private List<TagGroup> pageRanges;

    /**
     * Creates a new DocumentReference instance.
     */
    public DocumentReference() {
        super();
        this.pi = null;
        pageRanges = new ArrayList<>();
    }

    /**
     * Creates a new DocumentReference instance.
     *
     * @param piTopStruct persistent identifier of the top-level structure
     */
    public DocumentReference(String piTopStruct) {
        super();
        this.pi = piTopStruct;
        pageRanges = new ArrayList<>();
    }

    /**
     * Getter for the field <code>pi</code>.
     *
     * @return the persistent identifier (PI) of the top-level structure element
     */
    public String getPi() {
        return pi;
    }

    /**
     * Setter for the field <code>pageRanges</code>.
     *
     * @param ranges sorted list of tag groups to set
     */
    public void setPageRanges(List<TagGroup> ranges) {
        this.pageRanges = ranges;
        Collections.sort(this.pageRanges);
    }

    /**
     * addPageRange.
     *
     * @param range tag group representing a page range to add
     */
    public void addPageRange(TagGroup range) {
        this.pageRanges.add(range);
        Collections.sort(this.pageRanges);
    }

    /**
     * addPageRanges.
     *
     * @param ranges collection of tag groups to add
     */
    public void addPageRanges(Collection<TagGroup> ranges) {
        this.pageRanges.addAll(ranges);
        Collections.sort(this.pageRanges);
    }

    /**
     * Getter for the field <code>pageRanges</code>.
     *
     * @return a list of tag groups, one per page range in this document reference
     */
    @JsonProperty("pages")
    public List<TagGroup> getPageRanges() {
        return pageRanges;
    }

    /**
     * getPageRange.
     *
     * @param startPage page order number to look up in ranges
     * @return the TagGroup whose range contains the given start page, or null if none found
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

    /**
     * getRangeSize.
     *
     * @return a int.
     */
    public int getRangeSize() {
        if (pageRanges != null && !pageRanges.isEmpty()) {
            return pageRanges.get(0).getPages();
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (pi != null) {
            return pi.hashCode();
        }

        return 0;
    }

    /** {@inheritDoc} */
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
