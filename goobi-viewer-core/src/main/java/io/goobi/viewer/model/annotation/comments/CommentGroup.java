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
package io.goobi.viewer.model.annotation.comments;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.model.security.user.UserGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Filtered view on collections.
 */
@Entity
@Table(name = "comment_groups")
public class CommentGroup implements Serializable {

    private static final long serialVersionUID = -6284942965581963665L;

    private static final Logger logger = LogManager.getLogger(CommentGroup.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_group_id")
    private Long id;

    @Column(name = "title", nullable = true)
    private String title;

    @Column(name = "solr_query", nullable = true)
    private String solrQuery;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @Column(name = "send_email_notifications")
    private boolean sendEmailNotifications = false;

    @Column(name = "members_may_edit_comments")
    private boolean membersMayEditComments = false;

    @Column(name = "members_may_delete_comments")
    private boolean membersMayDeleteComments = false;

    @Column(name = "core_type")
    private boolean coreType = false;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Transient
    private final Set<String> identifiers = new HashSet<>();

    @Transient
    private boolean identifiersQueried = false;

    /**
     * Creates a {@link CommentGroup} instance representing unfiltered listing.
     *
     * @return {@link CommentGroup}
     */
    public static CommentGroup createCommentGroupAll() {
        CommentGroup commentGroupAll = new CommentGroup();
        commentGroupAll.setTitle("admin__comment_groups_all_comments_title");
        commentGroupAll.setDescription("admin__comment_groups_all_comments_desc");
        commentGroupAll.setSendEmailNotifications(true);
        commentGroupAll.setCoreType(true);

        return commentGroupAll;
    }

    
    public Long getId() {
        return id;
    }

    
    public void setId(Long id) {
        this.id = id;
    }

    
    public String getTitle() {
        return title;
    }

    
    public void setTitle(String title) {
        this.title = title;
    }

    
    public String getSolrQuery() {
        return solrQuery;
    }

    
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    
    public String getDescription() {
        return description;
    }

    
    public void setDescription(String description) {
        this.description = description;
    }

    
    public UserGroup getUserGroup() {
        return userGroup;
    }

    
    public void setUserGroup(UserGroup userGroup) {
        logger.trace("setUserGroup: {}", userGroup);
        this.userGroup = userGroup;
    }

    
    public boolean isSendEmailNotifications() {
        return sendEmailNotifications;
    }

    
    public void setSendEmailNotifications(boolean sendEmailNotifications) {
        logger.trace("setSendEmailNotifications: {}", sendEmailNotifications);
        this.sendEmailNotifications = sendEmailNotifications;
    }

    
    public boolean isMembersMayEditComments() {
        return membersMayEditComments;
    }

    
    public void setMembersMayEditComments(boolean membersMayEditComments) {
        logger.trace("setMembersMayEditComments: {}", membersMayEditComments);
        this.membersMayEditComments = membersMayEditComments;
    }

    
    public boolean isMembersMayDeleteComments() {
        return membersMayDeleteComments;
    }

    
    public void setMembersMayDeleteComments(boolean membersMayDeleteComments) {
        logger.trace("setMembersMayDeleteComments: {}", membersMayDeleteComments);
        this.membersMayDeleteComments = membersMayDeleteComments;
    }

    
    public boolean isCoreType() {
        return coreType;
    }

    
    public void setCoreType(boolean coreType) {
        this.coreType = coreType;
    }

    
    public boolean isEnabled() {
        return enabled;
    }

    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    
    public Set<String> getIdentifiers() {
        return identifiers;
    }

    
    public boolean isIdentifiersQueried() {
        return identifiersQueried;
    }

    
    public void setIdentifiersQueried(boolean identifiersQueried) {
        this.identifiersQueried = identifiersQueried;
    }
}
