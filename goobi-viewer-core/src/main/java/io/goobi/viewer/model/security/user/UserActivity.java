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
package io.goobi.viewer.model.security.user;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.search.Search;

/**
 * @author florian
 *
 */
public class UserActivity {

    public static final int MAX_LABEL_LENGTH = 50;
    public static final String LABEL_TRUNCATE_SUFFIX = "...";

    public enum ActivityType {
        CROWDSOURCING_CONTENT("ugc", "note"),
        CROWDSOURCING_TRANSCRIPTION("transcription", "file"),
        CAMPAIGN_ANNOTATION("Annotation", "edit"),
        COMMENT("Comment", "message-circle"),
        SEARCH("label__user_search", "search"),
        BOOKMARK("bookmarkList", "bookmark");

        private final String icon;
        private final String label;

        private ActivityType(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final ActivityType type;
    private final String label;
    private final LocalDateTime date;
    private final boolean update;

    /**
     * 
     * @param type
     * @param label
     * @param date
     * @param update
     */
    public UserActivity(ActivityType type, String label, LocalDateTime date, boolean update) {
        this.type = type;
        this.label = label;
        this.date = date;
        this.update = update;
    }

    public static UserActivity getFromComment(Comment comment) {
        return new UserActivity(ActivityType.COMMENT, truncate(comment.getDisplayText()), comment.getDateCreated(), false);
    }

    public static UserActivity getFromCommentUpdate(Comment comment) {
        return new UserActivity(ActivityType.COMMENT, truncate(comment.getDisplayText()), comment.getDateModified(), true);
    }

    public static UserActivity getFromCampaignAnnotation(CrowdsourcingAnnotation anno) {
        return new UserActivity(ActivityType.CAMPAIGN_ANNOTATION, truncate(anno.getContentString()), anno.getDateCreated(), false);
    }

    public static UserActivity getFromCampaignAnnotationUpdate(CrowdsourcingAnnotation anno) {
        return new UserActivity(ActivityType.CAMPAIGN_ANNOTATION, truncate(anno.getContentString()), anno.getDateModified(), true);
    }

    public static UserActivity getFromBookmark(Bookmark bookmark) {
        String listName = truncate(bookmark.getBookmarkList().getName(), 22);
        String bookmarkName = truncate(bookmark.getName(), MAX_LABEL_LENGTH - listName.length() - 3);
        String label = bookmarkName + " (" + listName + ")";
        return new UserActivity(ActivityType.BOOKMARK, label, bookmark.getDateAdded(), false);
    }

    public static UserActivity getFromSearch(Search search) {
        return new UserActivity(ActivityType.SEARCH, truncate(search.getName()), search.getDateUpdated(), false);
    }

    /**
     * @return the type
     */
    public ActivityType getType() {
        return type;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the date
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * @return the update
     */
    public boolean isUpdate() {
        return update;
    }

    /**
     * @param text
     * @return text truncated to MAX_LABEL_LENGTH
     */
    private static String truncate(String text) {
        return truncate(text, MAX_LABEL_LENGTH);
    }

    private static String truncate(String text, int length) {
        if (StringUtils.isNotBlank(text) && text.length() > length) {
            return text.substring(0, length) + LABEL_TRUNCATE_SUFFIX;
        }
        return text;
    }

    /**
     * Equals if label and type are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            UserActivity other = (UserActivity) obj;
            return this.type.equals(other.type) && Strings.CS.equals(this.label, other.label);
        }
        return false;
    }

    /**
     * Build from label and type
     */
    @Override
    public int hashCode() {
        return (this.type.label + this.label).hashCode();
    }
}
