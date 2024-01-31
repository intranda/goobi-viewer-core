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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.exceptions.IllegalArgumentException;

import de.intranda.api.annotation.wa.Motivation;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.notification.ChangeNotificator;
import io.goobi.viewer.model.annotation.notification.CommentMailNotificator;
import io.goobi.viewer.model.annotation.serialization.AnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.AnnotationLister;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Class to create comments from a text input for a given PI and page order and to save them using a given {@link AnnotationSaver}
 *
 * @author florian
 *
 */
public class CommentManager implements AnnotationLister<Comment> {

    private static final Logger logger = LogManager.getLogger(CommentManager.class);

    private final AnnotationSaver saver;
    private final AnnotationDeleter deleter;
    private final List<ChangeNotificator> notificators;
    private final AnnotationConverter converter = new AnnotationConverter();
    private final AnnotationLister<Comment> lister;

    /**
     *
     * @param saver
     * @param deleter
     * @param lister
     * @param notificators
     */
    public CommentManager(AnnotationSaver saver, AnnotationDeleter deleter, AnnotationLister<Comment> lister, ChangeNotificator... notificators) {
        this.saver = saver;
        this.deleter = deleter;
        this.lister = lister;
        this.notificators = Arrays.asList(notificators);
    }

    /**
     *
     * @param text
     * @param creator
     * @param pi
     * @param pageOrder
     * @param license
     * @param publicationStatus
     */
    public void createComment(String text, User creator, String pi, Integer pageOrder, String license, PublicationStatus publicationStatus) {
        String textCleaned = checkAndCleanScripts(text, creator, pi, pageOrder);
        Comment comment = new Comment(pi, pageOrder, creator, textCleaned, license, publicationStatus);
        comment.setPublicationStatus(publicationStatus);
        String viewerRootUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
        try {
            saver.save(comment);
            notificators.parallelStream().forEach(n -> {
                if (n.getClass().equals(CommentMailNotificator.class)) {
                    Thread fileChangedObserver = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                // Send notification mails to each user group that receives notifications
                                Set<UserGroup> groups = getNotificationUserGroupsForRecord(pi);
                                Set<String> usedAddresses = new HashSet<>();
                                for (UserGroup group : groups) {
                                    populateRecipientsForGroup(group, (CommentMailNotificator) n, usedAddresses);
                                    n.notifyCreation(comment, BeanUtils.getLocale(), viewerRootUrl);
                                }
                            } catch (DAOException | IndexUnreachableException | PresentationException e) {
                                logger.error(e.getMessage());
                            }
                        }
                    });
                    fileChangedObserver.start();
                } else {
                    n.notifyCreation(comment, BeanUtils.getLocale(), viewerRootUrl);
                }
            });
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    /**
     *
     * @param comment
     * @param text
     * @param editor
     * @param license
     * @param publicationStatus
     */
    public void editComment(Comment comment, String text, User editor, String license, PublicationStatus publicationStatus) {
        String textCleaned = checkAndCleanScripts(text, editor, comment.getTargetPI(), comment.getTargetPageOrder());
        Comment editedComment = new Comment(comment);
        editedComment.setText(textCleaned);
        editedComment.setPublicationStatus(publicationStatus);
        editedComment.setDateModified(LocalDateTime.now());
        String viewerRootUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
        try {
            saver.save(editedComment);
            notificators.parallelStream().forEach(n -> {
                if (n.getClass().equals(CommentMailNotificator.class)) {
                    Thread fileChangedObserver = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                // Send notification mails to each user group that receives notifications
                                Set<UserGroup> groups = getNotificationUserGroupsForRecord(editedComment.getTargetPI());
                                Set<String> usedAddresses = new HashSet<>();
                                for (UserGroup group : groups) {
                                    populateRecipientsForGroup(group, (CommentMailNotificator) n, usedAddresses);
                                    n.notifyEdit(comment, editedComment, BeanUtils.getLocale(), viewerRootUrl);
                                }
                            } catch (DAOException | IndexUnreachableException | PresentationException e) {
                                logger.error(e.getMessage());
                            }
                        }
                    });
                    fileChangedObserver.start();
                } else {
                    n.notifyEdit(comment, editedComment, BeanUtils.getLocale(), viewerRootUrl);
                }
            });
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    /**
     * Populates recipient and BCC lists for given from given user group owner and members.
     *
     * @param group
     * @param notificator
     * @param usedAddresses
     * @throws DAOException
     * @should not add addresses included in usedAddresses
     */
    static void populateRecipientsForGroup(UserGroup group, CommentMailNotificator notificator, Set<String> usedAddresses) throws DAOException {
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }
        if (notificator == null) {
            throw new IllegalArgumentException("notificator may not be null");
        }
        if (usedAddresses == null) {
            throw new IllegalArgumentException("usedAddresses may not be null");
        }

        notificator.setRecipients(Collections.singletonList(group.getOwner().getEmail()));
        usedAddresses.add(group.getOwner().getEmail());
        // logger.trace("Added owner for group '{}': '{}'", group.getName(), group.getOwner().getEmail()); //NOSONAR Debug
        Set<User> members = group.getMembers();
        List<String> bcc = new ArrayList<>(members.size());
        for (User member : members) {
            if (!usedAddresses.contains(member.getEmail())) {
                bcc.add(member.getEmail());
                usedAddresses.add(member.getEmail());
                // logger.trace("Added member for group '{}': '{}'", group.getName(), member.getEmail()); //NOSONAR Debug
            }
        }
        notificator.setBcc(bcc);
    }

    /**
     *
     * @param comment
     */
    public void deleteComment(Comment comment) {
        try {
            deleter.delete(comment);
            notificators.forEach(n -> n.notifyDeletion(comment, BeanUtils.getLocale()));
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAllAnnotations()
     */
    @Override
    public List<Comment> getAllAnnotations() {
        return lister.getAnnotations(0, Integer.MAX_VALUE, null, Arrays.asList(Motivation.COMMENTING), null, null, null, null, null, false);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getTotalAnnotationCount()
     */
    @Override
    public long getTotalAnnotationCount() {
        return getAllAnnotations().size();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotations(int, int, java.lang.String, java.util.List, 
     * java.util.List, java.util.List, java.lang.String, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<Comment> getAnnotations(int firstIndex, int items, String textQuery, List<String> motivations, List<Long> generators,
            List<Long> creators, String targetPi, Integer targetPage, String sortField, boolean sortDescending) {
        List<String> allMotivations = new ArrayList<>();
        allMotivations.add(Motivation.COMMENTING);
        if (motivations != null) {
            allMotivations.addAll(motivations);
        }
        return lister.getAnnotations(firstIndex, items, textQuery, allMotivations, generators, creators, targetPi, targetPage, sortField,
                sortDescending);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotationCount(java.lang.String, java.util.List, java.util.List,
     * java.util.List, java.lang.String, java.lang.Integer)
     */
    @Override
    public long getAnnotationCount(String textQuery, List<String> motivations, List<Long> generators, List<Long> creators, String targetPi,
            Integer targetPage) {
        List<String> allMotivations = new ArrayList<>();
        allMotivations.add(Motivation.COMMENTING);
        if (motivations != null) {
            allMotivations.addAll(motivations);
        }
        return lister.getAnnotationCount(textQuery, allMotivations, generators, creators, targetPi, targetPage);
    }

    /**
     *
     * @param text
     * @param editor
     * @param pi
     * @param page
     * @return text stripped of any JS
     */
    public static String checkAndCleanScripts(final String text, User editor, String pi, Integer page) {
        if (text != null) {
            String cleanText = StringTools.stripJS(text);
            if (cleanText.length() < text.length()) {
                logger.warn("User {} attempted to add a script block into a comment for {}, page {}, which was removed:\n{}",
                        Optional.ofNullable(editor).map(User::getId).orElse(null), pi, page, text);
            }
            return cleanText;
        }

        return text;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotation(java.lang.Long)
     */
    @Override
    public Optional<Comment> getAnnotation(Long id) {
        return lister.getAnnotation(id);
    }

    /**
     * Returns a list of email addresses that are configured (via comment views) to receive notifications for comments for the given record
     * identifier.
     *
     * @param pi
     * @return List of email addresses
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return user groups for matching comment views
     * @should skip comment groups where notifications disabled
     */
    static Set<UserGroup> getNotificationUserGroupsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        List<CommentGroup> commentGroups = DataManager.getInstance().getDao().getAllCommentGroups();
        if (commentGroups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UserGroup> ret = new HashSet<>();
        for (CommentGroup commentGroup : commentGroups) {
            if (commentGroup.getUserGroup() == null || !commentGroup.isSendEmailNotifications()) {
                continue;
            }
            if (StringUtils.isNotEmpty(commentGroup.getSolrQuery())) {
                if (queryCommentGroupIdentifiers(commentGroup)) {
                    if ((commentGroup.isCoreType() && StringUtils.isEmpty(commentGroup.getSolrQuery()))
                            || commentGroup.getIdentifiers().contains(pi)) {
                        ret.add(commentGroup.getUserGroup());
                    }
                }
            } else if (commentGroup.isCoreType()) {
                // "All comments" group without a filter query - add all users email addresses
                ret.add(commentGroup.getUserGroup());
            }
        }

        return ret;
    }

    /**
     * Returns {@link UserGroup}s linked to {@link CommentGroup}s (non-core only) whose Solr query matches the given <code>pi</code>.
     *
     * @param pi
     * @return Set<CommentGroup>
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static Set<CommentGroup> getRelevantCommentGroupsForRecord(String pi)
            throws DAOException, PresentationException, IndexUnreachableException {
        List<CommentGroup> commentGroups = DataManager.getInstance().getDao().getAllCommentGroups();
        if (commentGroups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<CommentGroup> ret = new HashSet<>();
        for (CommentGroup commentGroup : commentGroups) {
            if (commentGroup.isCoreType() || StringUtils.isEmpty(commentGroup.getSolrQuery())) {
                continue;
            }
            if (queryCommentGroupIdentifiers(commentGroup) && commentGroup.getIdentifiers().contains(pi)) {
                ret.add(commentGroup);
            }
        }

        return ret;
    }

    /**
     *
     *
     * @param commentGroup
     * @return true if commentGroup query has matching identifiers in the index; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static boolean queryCommentGroupIdentifiers(CommentGroup commentGroup) throws PresentationException, IndexUnreachableException {
        if (commentGroup == null) {
            return false;
        }
        if (StringUtils.isBlank(commentGroup.getSolrQuery())) {
            commentGroup.setIdentifiersQueried(true);
            return false;
        }

        String query = "+" + SolrConstants.ISWORK + ":true +(" + commentGroup.getSolrQuery() + ")";
        commentGroup.getIdentifiers().addAll(SearchHelper.getFacetValues(query, SolrConstants.PI, 1));
        commentGroup.setIdentifiersQueried(true);

        return !commentGroup.getIdentifiers().isEmpty();
    }

    /**
     * Checks whether the given user has access to any comment groups, whether via being admin or owner or member of any linked user group.
     *
     * @param user
     * @return true if user has access; false otherwise
     * @throws DAOException
     * @should return false if user null
     * @should return true if user admin
     * @should return true if user owner of user group linked to comment group
     * @should return true if user member of user group linked to comment group
     */
    public static boolean isUserHasAccessToCommentGroups(User user) throws DAOException {
        if (user == null) {
            return false;
        }
        if (user.isSuperuser()) {
            return true;
        }
        for (CommentGroup commentGroup : DataManager.getInstance().getDao().getAllCommentGroups()) {
            if (commentGroup.getUserGroup() != null) {
                if (user.equals(commentGroup.getUserGroup().getOwner()) || commentGroup.getUserGroup().getMembers().contains(user)) {
                    return true;
                }
            }
        }

        return false;
    }

}
