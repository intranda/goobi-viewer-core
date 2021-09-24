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
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.annotation.notification.CommentMailNotificator;
import io.goobi.viewer.model.annotation.notification.JsfMessagesNotificator;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationLister;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationSaver;
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
                new SqlAnnotationSaver(), 
                new SqlAnnotationDeleter(),
                new SqlAnnotationLister(),
                new CommentMailNotificator(DataManager.getInstance().getConfiguration().getCommentsNotificationEmailAddresses()),
                new JsfMessagesNotificator());
    }
    
    public void createComment(String text, boolean restricted) throws IndexUnreachableException {
        this.commentManager.createComment(text, userBean.getUser(), activeDocumentBean.getViewManager().getPi(), activeDocumentBean.getViewManager().getCurrentImageOrder(), restricted ? getRestrictedLicense() : getPublicLicense());
    }
    
    public void editComment(PersistentAnnotation original, String text, boolean restricted) throws IndexUnreachableException {
        this.commentManager.editComment(original, text, userBean.getUser(), restricted ? getRestrictedLicense() : getPublicLicense());
    }
    
    public void deleteComment(PersistentAnnotation annotation) throws IndexUnreachableException {
        this.commentManager.deleteComment(annotation);
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
