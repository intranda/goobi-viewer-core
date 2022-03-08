package io.goobi.viewer.model.annotation.comments;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.model.security.user.UserGroup;

/**
 * Filtered view on collections.
 */
@Entity
@Table(name = "comment_views")
public class CommentView {

    private static final Logger logger = LoggerFactory.getLogger(CommentView.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_view_id")
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
    boolean identifiersQueried = false;

    /**
     * Creates a {@link CommentView} instance representing unfiltered listing.
     * 
     * @return {@link CommentView}
     */
    public static CommentView createCommentViewAll() {
        CommentView commentViewAll = new CommentView();
        commentViewAll.setTitle("admin__comment_views_all_title");
        commentViewAll.setDescription("admin__comment_views_all_desc");
        commentViewAll.setCoreType(true);

        return commentViewAll;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     */
    public void setUserGroup(UserGroup userGroup) {
        logger.trace("setUserGroup: {}", userGroup);
        this.userGroup = userGroup;
    }

    /**
     * @return the sendEmailNotifications
     */
    public boolean isSendEmailNotifications() {
        return sendEmailNotifications;
    }

    /**
     * @param sendEmailNotifications the sendEmailNotifications to set
     */
    public void setSendEmailNotifications(boolean sendEmailNotifications) {
        logger.trace("setSendEmailNotifications: {}", sendEmailNotifications);
        this.sendEmailNotifications = sendEmailNotifications;
    }

    /**
     * @return the membersMayEditComments
     */
    public boolean isMembersMayEditComments() {
        return membersMayEditComments;
    }

    /**
     * @param membersMayEditComments the membersMayEditComments to set
     */
    public void setMembersMayEditComments(boolean membersMayEditComments) {
        logger.trace("setMembersMayEditComments: {}", membersMayEditComments);
        this.membersMayEditComments = membersMayEditComments;
    }

    /**
     * @return the membersMayDeleteComments
     */
    public boolean isMembersMayDeleteComments() {
        return membersMayDeleteComments;
    }

    /**
     * @param membersMayDeleteComments the membersMayDeleteComments to set
     */
    public void setMembersMayDeleteComments(boolean membersMayDeleteComments) {
        logger.trace("setMembersMayDeleteComments: {}", membersMayDeleteComments);
        this.membersMayDeleteComments = membersMayDeleteComments;
    }

    /**
     * @return the coreType
     */
    public boolean isCoreType() {
        return coreType;
    }

    /**
     * @param coreType the coreType to set
     */
    public void setCoreType(boolean coreType) {
        this.coreType = coreType;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the identifiers
     */
    public Set<String> getIdentifiers() {
        return identifiers;
    }

    /**
     * @return the identifiersQueried
     */
    public boolean isIdentifiersQueried() {
        return identifiersQueried;
    }

    /**
     * @param identifiersQueried the identifiersQueried to set
     */
    public void setIdentifiersQueried(boolean identifiersQueried) {
        this.identifiersQueried = identifiersQueried;
    }
}
