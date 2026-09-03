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
package io.goobi.viewer.api.rest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Record-wide index of ALTO layout tags: which tag LABEL occurs on which pages. Built from the Solr page documents only
 * (no ALTO access), so it is cheap enough to be loaded once per record; the per-page occurrences are delivered
 * separately by the page-range endpoint.
 */
@JsonPropertyOrder({ "pi", "pageCount", "tags" })
public class LayoutTagIndex {

    /** One layout tag with its resolved display name and the ascending list of page orders it occurs on. */
    @JsonPropertyOrder({ "label", "name", "styleClass", "pages" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tag {

        private final String label;
        private final String name;
        private final String styleClass;
        private final List<Integer> pages;

        /**
         * @param label raw layout-tag LABEL (stable key for the page-range endpoint)
         * @param name resolved display name
         * @param styleClass configured CSS class of an annotation tag, or null for tags without one
         * @param pages ascending, duplicate-free page orders carrying the tag
         */
        public Tag(String label, String name, String styleClass, List<Integer> pages) {
            this.label = label;
            this.name = name;
            this.styleClass = styleClass;
            this.pages = pages != null ? pages : new ArrayList<>();
        }

        public String getLabel() {
            return label;
        }

        public String getName() {
            return name;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public List<Integer> getPages() {
            return pages;
        }
    }

    private final String pi;
    private final int pageCount;
    private final List<Tag> tags;

    /**
     * @param pi record identifier (topstruct PI)
     * @param pageCount total number of pages of the record
     * @param tags tags sorted by descending page count
     */
    public LayoutTagIndex(String pi, int pageCount, List<Tag> tags) {
        this.pi = pi;
        this.pageCount = pageCount;
        this.tags = tags != null ? tags : Collections.emptyList();
    }

    public String getPi() {
        return pi;
    }

    public int getPageCount() {
        return pageCount;
    }

    public List<Tag> getTags() {
        return tags;
    }
}
