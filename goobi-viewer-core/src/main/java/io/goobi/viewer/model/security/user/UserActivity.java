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
package io.goobi.viewer.model.security.user;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class UserActivity {

    public static final int MAX_LABEL_LENGTH = 100;
    public static final String LABEL_TRUNCATE_SUFFIX = "...";
    
    public enum ActivityType {
        crowdsourcingContent("ugc", "fa fa-sticky-note-o"),
        crowdsourcingTranscription("transcription", "fa fa-file-text"),
        campaignAnnotation("Annotation", "fa fa-sticky-note"),
        comment("Comment", "fa fa-comment"),
        search("label__user_search", "fa fa-search"),
        bookmark("bookmarkList", "fa fa-bookmark");
        
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
    
    public UserActivity(ActivityType type, String label, LocalDateTime date, boolean update) {
        this.type = type;
        this.label = label;
        this.date = date;
        this.update = update;
    }
    
    public static UserActivity getFromComment(Comment comment) {
        return new UserActivity(ActivityType.comment, truncate(comment.getDisplayText()), comment.getDateCreated(), false);
    }
    
    public static UserActivity getFromCommentUpdate(Comment comment) {
        return new UserActivity(ActivityType.comment, truncate(comment.getDisplayText()), comment.getDateUpdated(), true);
    }
    
    public static UserActivity getFromCampaignAnnotation(PersistentAnnotation anno) {
        return new UserActivity(ActivityType.campaignAnnotation, truncate(anno.getContentString()), anno.getDateCreated(), false);

    }
 
    /**
     * @param displayText
     * @return
     */
    private static String truncate(String text) {
        if(StringUtils.isNotBlank(text) && text.length() > MAX_LABEL_LENGTH) {
            return text.substring(0, MAX_LABEL_LENGTH) + LABEL_TRUNCATE_SUFFIX;
        } else {
            return text;
        }
    }
   
    
    
}
