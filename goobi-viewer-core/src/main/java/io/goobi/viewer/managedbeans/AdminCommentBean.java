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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.security.user.User;

/**
 * <p>AdminCommentBean class.</p>
 */
@Named
@SessionScoped
public class AdminCommentBean implements Serializable {

    private static final long serialVersionUID = -640422863609139392L;

    private static final Logger logger = LogManager.getLogger(AdminCommentBean.class);

    @Inject
    private UserBean userBean;

    private TableDataProvider<Comment> lazyModelComments;

    private CommentGroup commentGroupAll;
    private CommentGroup currentCommentGroup;
    private Comment currentComment = null;

    /**
     * <p>init.</p>
     *
     * @should sort lazyModelComments by dateCreated desc by default
     */
    @PostConstruct
    public void init() {
        try {
            commentGroupAll = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
        } catch (DAOException e) {
            logger.error(e.getMessage());
        }

        lazyModelComments = new TableDataProvider<>(new TableDataSource<Comment>() {

            @Override
            public List<Comment> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder, Map<String, String> filters) {
                try {
                    String useSortField = sortField;
                    SortOrder useSortOrder = sortOrder;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "dateCreated";
                        useSortOrder = SortOrder.DESCENDING;
                    }
                    if (currentCommentGroup == null) {
                        return Collections.emptyList();
                    }
                    if (currentCommentGroup.isCoreType() && userBean != null && userBean.isAdmin()) {
                        return DataManager.getInstance()
                                .getDao()
                                .getComments(first, pageSize, useSortField, useSortOrder.asBoolean(), filters, null);
                    }
                    if (!currentCommentGroup.isIdentifiersQueried()) {
                        CommentManager.queryCommentGroupIdentifiers(currentCommentGroup);
                    }
                    if (currentCommentGroup.getIdentifiers().isEmpty()) {
                        return Collections.emptyList();
                    }
                    return DataManager.getInstance()
                            .getDao()
                            .getComments(first, pageSize, useSortField, useSortOrder.asBoolean(), filters, currentCommentGroup.getIdentifiers());
                } catch (DAOException | IndexUnreachableException | PresentationException e) {
                    logger.error(e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (currentCommentGroup == null) {
                    return 0;
                }

                try {
                    if (currentCommentGroup.isCoreType() && userBean != null && userBean.isAdmin()) {
                        return DataManager.getInstance().getDao().getCommentCount(filters, null, null);
                    }

                    if (!currentCommentGroup.isIdentifiersQueried()) {
                        CommentManager.queryCommentGroupIdentifiers(currentCommentGroup);
                    }
                    if (currentCommentGroup.getIdentifiers().isEmpty()) {
                        return 0;
                    }
                    return DataManager.getInstance().getDao().getCommentCount(filters, null, currentCommentGroup.getIdentifiers());
                } catch (DAOException | IndexUnreachableException | PresentationException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
                //
            }

        });
        lazyModelComments.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
        lazyModelComments.getFilter("body_targetPI");
    }

    /**
     * <p>isUserCommentsEnabled.</p>
     *
     * @return true if comments enabled; false otherwise
     */
    public boolean isUserCommentsEnabled() {
        if (commentGroupAll != null) {
            return commentGroupAll.isEnabled();
        }

        return false;
    }

    /**
     * <p>Setter for the field <code>userBean</code>.</p>
     *
     * @param userBean a {@link io.goobi.viewer.managedbeans.UserBean} object
     */
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * <p>Getter for the field <code>userBean</code>.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.UserBean} object
     */
    public UserBean getUserBean() {
        return userBean;
    }

    /**
     * <p>setUserCommentsEnabled.</p>
     *
     * @param userCommentsEnabled a boolean
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setUserCommentsEnabled(boolean userCommentsEnabled) throws DAOException {
        if (commentGroupAll != null && commentGroupAll.isEnabled() != userCommentsEnabled) {
            commentGroupAll.setEnabled(userCommentsEnabled);
            DataManager.getInstance().getDao().updateCommentGroup(commentGroupAll);
        }
    }

    /**
     * <p>getAllCommentGroups.</p>
     *
     * @return All comment groups in the database
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public List<CommentGroup> getAllCommentGroups() throws DAOException {
        return DataManager.getInstance().getDao().getAllCommentGroups();
    }

    /**
     * <p>getCommentGroupsForUser.</p>
     *
     * @param user Current user
     * @return Filtered list of available {@link io.goobi.viewer.model.annotation.comments.CommentGroup}s to the given user
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public List<CommentGroup> getCommentGroupsForUser(User user) throws DAOException {
        if (user == null) {
            return Collections.emptyList();
        }

        // Unfiltered list for admins
        if (user.isSuperuser()) {
            return DataManager.getInstance().getDao().getAllCommentGroups();
        }

        // Regular users
        List<CommentGroup> ret = new ArrayList<>();
        for (CommentGroup commentGroup : DataManager.getInstance().getDao().getAllCommentGroups()) {
            if (!commentGroup.isCoreType() && commentGroup.getUserGroup() != null
                    && commentGroup.getUserGroup().getMembersAndOwner().contains(user)) {
                ret.add(commentGroup);
            }
        }

        return ret;
    }

    /**
     * <p>resetCurrentCommentGroupAction.</p>
     */
    public void resetCurrentCommentGroupAction() {
        currentCommentGroup = null;
    }

    /**
     * <p>newCurrentCommentGroupAction.</p>
     */
    public void newCurrentCommentGroupAction() {
        logger.trace("newCurrentCommentGroupAction");
        currentCommentGroup = new CommentGroup();
    }

    /**
     * <p>saveCurentCommentGroupAction.</p>
     *
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public String saveCurentCommentGroupAction() throws DAOException {
        return saveCommentGroupAction(currentCommentGroup);
    }

    /**
     * <p>
     * saveCommentGroupAction.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCommentGroupAction(CommentGroup commentGroup) throws DAOException {
        logger.trace("saveCommentGroupAction");
        if (commentGroup.getId() != null) {
            if (DataManager.getInstance().getDao().updateCommentGroup(commentGroup)) {
                Messages.info("updatedSuccessfully");
                currentCommentGroup = null;
                return "pretty:adminUserCommentGroups";
            }
            Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
        } else {
            if (DataManager.getInstance().getDao().addCommentGroup(commentGroup)) {
                Messages.info("addedSuccessfully");
                currentCommentGroup = null;
                return "pretty:adminUserCommentGroups";
            }
            Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
        }
        return "";
    }

    /**
     * <p>
     * deleteCommentGroupAction.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCommentGroupAction(CommentGroup commentGroup) throws DAOException {
        if (DataManager.getInstance().getDao().deleteCommentGroup(commentGroup)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }

        return "";
    }

    // Comments

    /**
     * <p>resetCurrentCommentAction.</p>
     */
    public void resetCurrentCommentAction() {
        currentComment = null;
    }

    /**
     * <p>
     * saveCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveCommentAction(Comment comment) throws DAOException {
        logger.trace("saveCommentAction");
        if (comment.getId() != null) {
            // Set updated timestamp
            comment.setDateModified(LocalDateTime.now());
            logger.trace(comment.getContentString());
            if (DataManager.getInstance().getDao().updateComment(comment)) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            }
        } else {
            if (DataManager.getInstance().getDao().addComment(comment)) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            }
        }
        resetCurrentCommentAction();
    }

    /**
     * <p>
     * deleteCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCommentAction(Comment comment) throws DAOException {
        if (DataManager.getInstance().getDao().deleteComment(comment)) {
            Messages.info("commentDeleteSuccess");
        } else {
            Messages.error("commentDeleteFailure");
        }

        return "";
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelComments</code>.
     * </p>
     *
     * @return the lazyModelComments
     */
    public TableDataProvider<Comment> getLazyModelComments() {
        return lazyModelComments;
    }

    /**
     * <p>
     * getPageComments.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Comment> getPageComments() {
        return lazyModelComments.getPaginatorList();
    }

    /**
     * <p>Getter for the field <code>currentCommentGroup</code>.</p>
     *
     * @return the currentCommentGroup
     */
    public CommentGroup getCurrentCommentGroup() {
        return currentCommentGroup;
    }

    /**
     * <p>Setter for the field <code>currentCommentGroup</code>.</p>
     *
     * @param currentCommentGroup the currentCommentGroup to set
     */
    public void setCurrentCommentGroup(CommentGroup currentCommentGroup) {
        this.currentCommentGroup = currentCommentGroup;
        init();
    }

    /**
     * Returns the ID of <code>currentCommentGroup</code>.
     *
     * @return currentCommentGroup.id
     */
    public Long getCurrentCommentGroupId() {
        if (currentCommentGroup != null && currentCommentGroup.getId() != null) {
            return currentCommentGroup.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentCommentGroup</code> by loading it from the DB via the given ID.
     *
     * @param id a {@link java.lang.Long} object
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setCurrentCommentGroupId(Long id) throws DAOException {
        logger.trace("setCurrentCommentGroupId: {}", id);
        try {
            Long longId = id;
            if (ObjectUtils.notEqual(getCurrentCommentGroupId(), longId)) {
                if (id != null) {
                    setCurrentCommentGroup(DataManager.getInstance().getDao().getCommentGroup(longId));
                } else {
                    setCurrentCommentGroup(null);
                }
            }
        } catch (NumberFormatException e) {
            setCurrentCommentGroup(null);
        }
    }

    /**
     * <p>
     * Getter for the field <code>selectedComment</code>.
     * </p>
     *
     * @return the selectedComment
     */
    public Comment getSelectedComment() {
        return currentComment;
    }

    /**
     * <p>
     * Setter for the field <code>selectedComment</code>.
     * </p>
     *
     * @param selectedComment the selectedComment to set
     */
    public void setSelectedComment(Comment selectedComment) {
        this.currentComment = selectedComment;
    }

}
