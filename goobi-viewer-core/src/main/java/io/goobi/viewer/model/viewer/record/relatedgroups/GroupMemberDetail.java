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
package io.goobi.viewer.model.viewer.record.relatedgroups;

import java.io.Serializable;

/**
 * Display data for a single related-group record (group membership, anchor sibling).
 *
 * Immutable value object, JSF-EL friendly.
 */
public class GroupMemberDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String pi;
    private final String title;
    private final String subtitle;
    private final String yearPublish;
    private final String thumbnailUrl;

    public GroupMemberDetail(String pi, String title, String subtitle, String yearPublish, String thumbnailUrl) {
        this.pi = pi;
        this.title = title;
        this.subtitle = subtitle;
        this.yearPublish = yearPublish;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPi() {
        return pi;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getYearPublish() {
        return yearPublish;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
