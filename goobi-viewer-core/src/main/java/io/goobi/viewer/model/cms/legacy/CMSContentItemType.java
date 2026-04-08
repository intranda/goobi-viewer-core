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
package io.goobi.viewer.model.cms.legacy;

/**
 * Enumerates the content item types used by the legacy CMS template system, allowing lookup by
 * the type name string as found in CMS template XML files.
 *
 * <ul>
 *   <li>{@code TEXT} – plain short text</li>
 *   <li>{@code HTML} – rich HTML text</li>
 *   <li>{@code MEDIA} – a CMS media item (image/video/…)</li>
 *   <li>{@code SOLRQUERY} – record list driven by a Solr query</li>
 *   <li>{@code PAGELIST} – list of CMS pages</li>
 *   <li>{@code COLLECTION} – hierarchical collection browse</li>
 *   <li>{@code TILEGRID} – image tile grid</li>
 *   <li>{@code TOC} – table of contents</li>
 *   <li>{@code RSS} – RSS feed</li>
 *   <li>{@code SEARCH} – search interface</li>
 *   <li>{@code COMPONENT} – generic CMS component</li>
 *   <li>{@code TAGS} – tag cloud</li>
 *   <li>{@code METADATA} – metadata display</li>
 *   <li>{@code CAMPAIGNOVERVIEW} – crowdsourcing campaign overview</li>
 *   <li>{@code BOOKMARKLISTS} – bookmark list display</li>
 *   <li>{@code BROWSETERMS} – browse terms index</li>
 *   <li>{@code GEOMAP} – geographic map</li>
 *   <li>{@code SLIDER} – image/content slider</li>
 * </ul>
 */
public enum CMSContentItemType {
    TEXT,
    HTML,
    MEDIA,
    SOLRQUERY,
    PAGELIST,
    COLLECTION,
    TILEGRID,
    TOC,
    RSS,
    SEARCH,
    COMPONENT,
    TAGS,
    METADATA,
    CAMPAIGNOVERVIEW,
    BOOKMARKLISTS,
    BROWSETERMS,
    GEOMAP,
    SLIDER;

    /**
     * Evaluates the text from cms-template xml files to select the correct item type.
     *
     * @param name content item type name from CMS template XML
     * @return CMSContentItemType
     */
    public static CMSContentItemType getByName(String name) {
        if (name != null) {
            return CMSContentItemType.valueOf(name.toUpperCase());
        }
        return null;
    }
}
