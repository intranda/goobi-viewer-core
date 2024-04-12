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
package io.goobi.viewer.model.search;

import java.util.List;

/**
 * Class to represent children of {@link SearchHit} which don't represent a complete record and thus need less data. Currently unused
 */
public class SearchChildHit {

    private static final String SEARCH_HIT_TYPE_PREFIX = "searchHitType_";
    private static final String SEARCH_HIT_TYPE_ICON_CLASS = "searchHitIconClass_";

    private final HitType type;
    private final String label;
    private final String url;
    private final List<SearchChildHit> children;

    public SearchChildHit(HitType type, List<SearchChildHit> children, BrowseElement browseElement) {
        this.type = type;
        this.url = browseElement.getUrl();
        this.label = getDisplayText(browseElement);
        this.children = children;
    }

    public String getUrl() {
        return this.url;
    }

    public String getLabel() {
        return this.label;
    }

    public String getIconClass() {
        return getIconClassForType(this.type);
    }

    public String getTypeLabelKey() {
        return type != null ? SEARCH_HIT_TYPE_PREFIX + type.name() : "";
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public HitType getType() {
        return type;
    }

    private static String getIconClassForType(HitType type) {
        if (type != null) {
            switch (type) {
                case PAGE:
                    return "fa fa-file-text";
                case PERSON:
                    return "fa fa-user";
                case CORPORATION:
                    return "fa fa-university";
                case ADDRESS:
                    return "fa fa-envelope";
                case COMMENT:
                    return "fa fa-comment-o";
                case CMS:
                    return "fa fa-file-text-o";
                case EVENT:
                    return "fa fa-calendar";
                case ACCESSDENIED:
                    return "fa fa-lock";
                default:
                    return "fa fa-file-text";
            }
        }

        return "";
    }

    private String getDisplayText(BrowseElement browseElement) {
        if (browseElement != null && this.type != null) {
            switch (this.type) {
                case CMS:
                case ACCESSDENIED:
                case PAGE:
                    return browseElement.getFulltextForHtml();
                default:
                    return browseElement.getLabelShort();

            }
        }

        return "";
    }
}
