package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentView;

@Named
@SessionScoped
public class AdminCommentBean implements Serializable {

    private static final long serialVersionUID = -640422863609139392L;

    private static final Logger logger = LoggerFactory.getLogger(AdminCommentBean.class);

    private TableDataProvider<Comment> lazyModelComments;

    private CommentView currentCommentView;
    private Comment currentComment = null;

    /**
     * @should sort lazyModelComments by dateCreated desc by default
     */
    @PostConstruct
    public void init() {
        {
            lazyModelComments = new TableDataProvider<>(new TableDataSource<Comment>() {

                @Override
                public List<Comment> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isEmpty(sortField)) {
                            sortField = "dateCreated";
                            sortOrder = SortOrder.DESCENDING;
                        }
                        return DataManager.getInstance().getDao().getComments(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                    } catch (DAOException e) {
                        logger.error(e.getMessage());
                    }
                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    try {
                        return DataManager.getInstance().getDao().getCommentCount(filters, null);
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    }
                }

                @Override
                public void resetTotalNumberOfRecords() {
                }

            });
            lazyModelComments.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
            lazyModelComments.setFilters("body_targetPI");
        }
    }

    /**
     * 
     * @return
     * @throws DAOException
     */
    public List<CommentView> getAllCommentViews() throws DAOException {
        return DataManager.getInstance().getDao().getAllCommentViews();
    }

    /**
     * 
     */
    public void newCurrentCommentViewAction() {
        currentCommentView = new CommentView();
    }

    /**
     * <p>
     * saveCommentViewAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.CommentView} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCommentViewAction(CommentView commentView) throws DAOException {
        logger.trace("saveCommentViewAction");
        if (commentView.getId() != null) {
            if (DataManager.getInstance().getDao().updateCommentView(commentView)) {
                Messages.info("updatedSuccessfully");
                currentCommentView = null;
                return "pretty:adminUserCommentViews";
            }
            Messages.info("errSave");
        } else {
            if (DataManager.getInstance().getDao().addCommentView(commentView)) {
                Messages.info("addedSuccessfully");
                currentCommentView = null;
                return "pretty:adminUserCommentViews";
            }
            Messages.info("errSave");
        }
        return "";
    }

    // Comments

    /**
     * 
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
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addComment(comment)) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
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
     * @return the currentCommentView
     */
    public CommentView getCurrentCommentView() {
        return currentCommentView;
    }

    /**
     * @param currentCommentView the currentCommentView to set
     */
    public void setCurrentCommentView(CommentView currentCommentView) {
        this.currentCommentView = currentCommentView;
    }

    /**
     * Returns the ID of <code>currentCommentView</code>.
     * @return currentCommentView.id
     */
    public Long getCurrentCommentViewId() {
        if (currentCommentView != null) {
            return currentCommentView.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentCommentView</code> by loading it from the DB via the given ID.
     * 
     * @param id
     * @throws DAOException
     */
    public void setCurrentCommentViewId(Long id) throws DAOException {
        if (id != null) {
            this.currentCommentView = DataManager.getInstance().getDao().getCommentView(id);
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
