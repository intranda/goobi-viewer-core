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
 * <p>
 * Comment class.
 * </p>
 */
//@Entity
//@Index(name = "index_comments_pi_page", columnNames = { "pi", "page" })
//@Table(name = "comments")
public class CommentLegacy implements Comparable<CommentLegacy> {

    private static final Logger logger = LogManager.getLogger(CommentLegacy.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
     * <p>
     * Constructor for Comment.
     * </p>
     */
    public CommentLegacy() {
        // the emptiness inside
    }

    /**
     * <p>
     * Constructor for Comment.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @param text a {@link java.lang.String} object.
     * @param parent a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
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
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @param oldText a {@link java.lang.String} object.
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
     * @return true if allowed; false otherwise
     * @should return true if use id equals owner id
     * @should return false if owner id is null
     * @should return false if user is null
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public boolean mayEdit(User user) {
        return owner.getId() != null && user != null && owner.getId().equals(user.getId());
    }

    /**
     * <p>
     * getDisplayDate.
     * </p>
     *
     * @param date a {@link java.time.LocalDateTime} object.
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
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Getter for the field <code>page</code>.
     * </p>
     *
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * <p>
     * Setter for the field <code>page</code>.
     * </p>
     *
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * Setter for the field <code>text</code>.
     * </p>
     *
     * @param text the text to set
     */
    public void setText(String text) {
        this.oldText = this.text;
        this.text = text;
    }

    /**
     * <p>
     * Getter for the field <code>text</code>.
     * </p>
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * <p>
     * getDisplayText.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayText() {
        return StringTools.stripJS(text);
    }

    /**
     * <p>
     * Getter for the field <code>oldText</code>.
     * </p>
     *
     * @return the oldText
     */
    public String getOldText() {
        return oldText;
    }

    /**
     * <p>
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
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
