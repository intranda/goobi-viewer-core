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
package de.intranda.digiverso.presentation.model.annotation;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.model.security.user.User;

@Entity
@Index(name = "index_comments_pi_page", columnNames = { "pi", "page" })
@Table(name = "comments")
public class Comment implements Comparable<Comment> {

    private static final Logger logger = LoggerFactory.getLogger(Comment.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "page", nullable = false)
    private int page;

    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "text", nullable = false, columnDefinition = "LONGTEXT")
    private String text = "";

    /** Old text before the last update (used for notification emails). */
    @Transient
    private String oldText = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    private Date dateCreated = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated")
    private Date dateUpdated;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<Comment> children;

    public Comment() {
        // the emptiness inside
    }

    /**
     * 
     * @param pi
     * @param page
     * @param owner
     * @param text
     * @param parent
     * @should construct object correctly
     */
    public Comment(String pi, int page, User owner, String text, Comment parent) {
        this.pi = pi;
        this.page = page;
        this.owner = owner;
        this.text = text;
        this.parent = parent;
    }

    @Override
    public int compareTo(Comment o) {
        if (dateCreated != null) {
            return dateCreated.compareTo(o.getDateCreated());
        }

        return 1;
    }

    /**
     * Sends an email notification about a new or altered comment to the configured recipient addresses.
     *
     * @param comment
     * @param oldText
     * @param locale Language locale for the email text.
     * @return
     */
    public static boolean sendEmailNotifications(Comment comment, String oldText, Locale locale) {
        List<String> addresses = DataManager.getInstance().getConfiguration().getUserCommentsNotificationEmailAddresses();
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }

        String subject = null;
        String body = null;
        if (StringUtils.isEmpty(oldText)) {
            subject = Helper.getTranslation("commentNewNotificationEmailSubject", locale);
            subject = subject.replace("{0}", comment.getOwner().getDisplayName()).replace("{1}", comment.getPi()).replace("{2}",
                    String.valueOf(comment.getPage()));
            body = Helper.getTranslation("commentNewNotificationEmailBody", locale);
            body = body.replace("{0}", comment.getText());
        } else {
            subject = Helper.getTranslation("commentChangedNotificationEmailSubject", locale);
            subject = subject.replace("{0}", comment.getOwner().getDisplayName()).replace("{1}", comment.getPi()).replace("{2}",
                    String.valueOf(comment.getPage()));
            body = Helper.getTranslation("commentChangedNotificationEmailBody", locale);
            body = body.replace("{0}", oldText).replace("{1}", comment.getText());
        }

        try {
            Helper.postMail(addresses, subject, body);
            return true;
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Checks whether the user with the given ID is allowed to edit this comment (i.e. the annotation belongs to this (proper) user.
     *
     * @param userId The id of the querying user.
     * @return true if allowed; false otherwise
     * @should return true if use id equals owner id
     * @should return false if owner id is null
     * @should return false if user is null
     */
    public boolean mayEdit(User user) {
        return owner.getId() != null && user != null && owner.getId() == user.getId();
    }

    public String getDisplayDate(Date date) {
        return DateTools.formatterDEDateTime.print(date.getTime());
    }

    // Property accessors

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
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.oldText = this.text;
        this.text = text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    public String getDisplayText() {
        return Helper.stripJS(text);
    }

    /**
     * @return the oldText
     */
    public String getOldText() {
        return oldText;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the parent
     */
    public Comment getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Comment parent) {
        this.parent = parent;
    }

    /**
     * @return the children
     */
    public List<Comment> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<Comment> children) {
        this.children = children;
    }
}
