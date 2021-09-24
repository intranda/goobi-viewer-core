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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.annotation.notification.CommentMailNotificator;
import io.goobi.viewer.model.annotation.notification.JsfMessagesNotificator;
import io.goobi.viewer.model.annotation.serialization.SolrAndSqlAnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.SolrAndSqlAnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationLister;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
@Named
@SessionScoped
public class CommentBean implements Serializable {

    /**
     * 
     */
    private static final String REQUIRES_COMMENT_RIGHTS = "REQUIRES_COMMENT_RIGHTS";
    private static final long serialVersionUID = -3653100353345867739L;
    
    private final CommentManager commentManager;
 
    @Inject
    private ActiveDocumentBean activeDocumentBean;
    @Inject
    private UserBean userBean;
    
    public CommentBean() throws IndexUnreachableException, DAOException {
        commentManager = new CommentManager(
                new SolrAndSqlAnnotationSaver(), 
                new SolrAndSqlAnnotationDeleter(),
                new SqlAnnotationLister(),
                new CommentMailNotificator(DataManager.getInstance().getConfiguration().getCommentsNotificationEmailAddresses()),
                new JsfMessagesNotificator());
    }
    
    public void createComment(String text, boolean restricted) throws IndexUnreachableException {
        this.commentManager.createComment(text, userBean.getUser(), activeDocumentBean.getViewManager().getPi(), activeDocumentBean.getViewManager().getCurrentImageOrder(), getLicense(restricted), getInitialPublicationStatus());
    }

    public void editComment(PersistentAnnotation original, String text, boolean restricted) throws IndexUnreachableException {
        this.commentManager.editComment(original, text, userBean.getUser(), getLicense(restricted), getInitialPublicationStatus());
    }
    
    public void deleteComment(PersistentAnnotation annotation) throws IndexUnreachableException {
        this.commentManager.deleteComment(annotation);
    }
    
    public List<PersistentAnnotation> getComments(int startIndex, int numItems, String filter, User user, String sortField, boolean descending) {
        return this.commentManager.getAnnotations(startIndex, numItems, filter, null, null, Collections.singletonList(user.getId()), null, null, sortField, descending);
    }
    
    public List<PersistentAnnotation> getCommentsForCurrentPage() throws IndexUnreachableException {
        return this.commentManager.getAnnotations(0, Integer.MAX_VALUE, null, null, null, null, activeDocumentBean.getViewManager().getPi(), activeDocumentBean.getViewManager().getCurrentImageOrder(), null, false)
                .stream()
                .filter(c -> PublicationStatus.PUBLISHED.equals(c.getPublicationStatus()) || Optional.ofNullable(c.getCreatorId()).map(id -> id.equals(getCurrentUserId())).orElse(false))
                //TODO: Check prvilege for viewing comment
                //.filter(c -> Optional.ofNullable(userBean).map(UserBean::getUser).map(u -> u.isHasAnnotationPrivilege(REQUIRES_COMMENT_RIGHTS)).orElse(false))
                .collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        return Optional.ofNullable(userBean).map(UserBean::getUser).map(User::getId).orElse(null);
    }
    
    public boolean isRestricted(PersistentAnnotation anno) {
        return REQUIRES_COMMENT_RIGHTS.equals(anno.getAccessCondition());
    }
    
    private String getLicense(boolean restricted) {
        return restricted ? getRestrictedLicense() : getPublicLicense();
    }
    
    /**
     * @return
     */
    private PublicationStatus getInitialPublicationStatus() {
        if(DataManager.getInstance().getConfiguration().reviewEnabledForComments()) {
            return PublicationStatus.REVIEW;
        } else {
            return PublicationStatus.PUBLISHED;
        }
    }

    /**
     * @return
     */
    private String getPublicLicense() {
        return SolrConstants.OPEN_ACCESS_VALUE;
    }

    /**
     * @return
     */
    private String getRestrictedLicense() {
        return REQUIRES_COMMENT_RIGHTS;
    }
    
}
