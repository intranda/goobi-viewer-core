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

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.PageType;
import jakarta.mail.MessagingException;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Transient;

/**
 * Comment class.
 */
//@Entity
//@Index(name = "index_comments_pi_page", columnNames = { "pi", "page" })
//@Table(name = "comments")
public class CommentLegacy implements Comparable<CommentLegacy> {

    private static final Logger logger = LogManager.getLogger(CommentLegacy.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "page", nullable = true)
    private Integer page;

    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "text", nullable = false, columnDefinition = "LONGTEXT")
    private String text = "";

    /** Old text before the last update (used for notification emails). */
    @Transient
    private String oldText = null;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    //    @ManyToOne
    //    @JoinColumn(name = "parent_id")
    //    private Comment parent;

    //    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    //    @PrivateOwned
    //    @CascadeOnDelete
    //    private List<Comment> children;

    /**
     * Creates a new Comment instance.
     */
    public CommentLegacy() {
        // the emptiness inside
    }

    /**
     * Creates a new Comment instance.
     *
     * @param pi persistent identifier of the commented record
     * @param page page number the comment is attached to
     * @param owner user who created this comment
     * @param text comment body text
     * @param parent unused parent comment (legacy field)
     * @should construct object correctly
     */
    public CommentLegacy(String pi, int page, User owner, String text, CommentLegacy parent) {
        this.pi = pi;
        this.page = page;
        this.owner = owner;
        this.text = text;
        //        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(CommentLegacy o) {
        if (dateUpdated != null) {
            if (o.getDateUpdated() != null) {
                return dateUpdated.compareTo(o.getDateUpdated());
            }
            return dateUpdated.compareTo(o.getDateCreated());
        }
        if (dateCreated != null) {
            if (o.getDateUpdated() != null) {
                return dateCreated.compareTo(o.getDateUpdated());
            }
            return dateCreated.compareTo(o.getDateCreated());
        }

        return 1;
    }

    /**
     * Sends an email notification about a new or altered comment to the configured recipient addresses.
     *
     * @param comment comment that was created or modified
     * @param oldText previous comment text, or empty/null if this is a new comment
     * @param locale Language locale for the email text.
     * @return a boolean.
     */
    public static boolean sendEmailNotifications(CommentLegacy comment, String oldText, Locale locale) {
        List<String> addresses = new ArrayList<>(); // Static configured list of email addresses is no longer available
        String subject = null;
        String body = null;
        if (StringUtils.isEmpty(oldText)) {
            subject = ViewerResourceBundle.getTranslation("commentNewNotificationEmailSubject", locale);
            subject = subject.replace("{0}", comment.getOwner().getDisplayName())
                    .replace("{1}", comment.getPi())
                    .replace("{2}", String.valueOf(comment.getPage()));
            body = ViewerResourceBundle.getTranslation("commentNewNotificationEmailBody", locale);
            body = body.replace("{0}", comment.getText());
        } else {
            subject = ViewerResourceBundle.getTranslation("commentChangedNotificationEmailSubject", locale);
            subject = subject.replace("{0}", comment.getOwner().getDisplayName())
                    .replace("{1}", comment.getPi())
                    .replace("{2}", String.valueOf(comment.getPage()));
            body = ViewerResourceBundle.getTranslation("commentChangedNotificationEmailBody", locale);
            body = body.replace("{0}", oldText).replace("{1}", comment.getText());
        }

        try {
            NetTools.postMail(addresses, null, null, subject, body);
            return true;
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Checks whether the user with the given ID is allowed to edit this comment (i.e. the annotation belongs to this (proper) user.
     *
     * @param user user requesting edit access
     * @return true if allowed; false otherwise
     * @should return true if use id equals owner id
     * @should return false if owner id is null
     * @should return false if user is null
     */
    public boolean mayEdit(User user) {
        return owner.getId() != null && user != null && owner.getId().equals(user.getId());
    }

    /**
     * getDisplayDate.
     *
     * @param date date and time to format for display
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayDate(LocalDateTime date) {
        return DateTools.format(date, DateTools.FORMATTERDEDATETIME, false);
    }

    /**
     * Removes any script tags from the text value.
     *
     * @should remove scripts correctly
     */
    public void checkAndCleanScripts() {
        if (text != null) {
            String cleanText = StringTools.stripJS(text);
            if (cleanText.length() < text.length()) {
                logger.warn("User attempted to add a script block into a comment for {}, page {}, which was removed:\n{}", pi, page, text);
                text = cleanText;
            }
        }
    }

    // Property accessors

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>pi</code>.
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * Setter for the field <code>pi</code>.
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * Getter for the field <code>page</code>.
     *
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Setter for the field <code>page</code>.
     *
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * Getter for the field <code>owner</code>.
     *
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Setter for the field <code>owner</code>.
     *
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * Setter for the field <code>text</code>.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        this.oldText = this.text;
        this.text = text;
    }

    /**
     * Getter for the field <code>text</code>.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * getDisplayText.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayText() {
        return StringTools.stripJS(text);
    }

    /**
     * Getter for the field <code>oldText</code>.
     *
     * @return the oldText
     */
    public String getOldText() {
        return oldText;
    }

    /**
     * Getter for the field <code>dateCreated</code>.
     *
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * Setter for the field <code>dateCreated</code>.
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Getter for the field <code>dateUpdated</code>.
     *
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * Setter for the field <code>dateUpdated</code>.
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getLinkToRecord() {
        return PrettyUrlTools.getRecordURI(this.pi, Optional.ofNullable(this.page).map(Object::toString).orElse(""), null, PageType.viewObject);
    }
}
