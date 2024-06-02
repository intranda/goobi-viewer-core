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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.notification.ChangeNotificator;
import io.goobi.viewer.model.annotation.serialization.AnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.AnnotationLister;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlCommentLister;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 *
 * @author florian
 *
 */
class CommentManagerTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final PublicationStatus PUBLISHED = PublicationStatus.PUBLISHED;
    private static final String OPEN_ACCESS = "OPEN_ACCESS";
    private static final String COMMENT_TEXT = "My Test Comment 1";
    private static final String COMMENT_TEXT_EDIT = "My Test Comment 2";
    private static final String PI = "PI_TEST_1";
    private static final Integer page = 10;

    private CommentManager manager;
    private User user;
    private IDAO dao;
    private ChangeNotificator notificator;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dao = DataManager.getInstance().getDao();
        AnnotationSaver saver = new SqlAnnotationSaver(dao);
        AnnotationDeleter deleter = new SqlAnnotationDeleter(dao);
        AnnotationLister<Comment> lister = new SqlCommentLister(dao);
        notificator = Mockito.mock(ChangeNotificator.class);
        this.manager = new CommentManager(saver, deleter, lister, notificator);
        this.user = dao.getUser(1l);
    }

    @Test
    void testCreate() throws DAOException {
        Comment comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNull(comment);

        this.manager.createComment(COMMENT_TEXT, this.user, PI, page, OPEN_ACCESS, PUBLISHED);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);

        assertNotNull(comment);
        assertEquals(COMMENT_TEXT, comment.getContentString());
        assertEquals(OPEN_ACCESS, comment.getAccessCondition());
        assertEquals(user, comment.getCreator());
        assertEquals(PUBLISHED, comment.getPublicationStatus());
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        Mockito.verify(notificator, Mockito.times(1)).notifyCreation(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testModify() throws DAOException {
        Comment comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNull(comment);

        this.manager.createComment(COMMENT_TEXT, this.user, PI, page, OPEN_ACCESS, PUBLISHED);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNotNull(comment);

        this.manager.editComment(comment, COMMENT_TEXT_EDIT, user, OPEN_ACCESS, PUBLISHED);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);

        assertNotNull(comment);
        assertEquals(COMMENT_TEXT_EDIT, comment.getContentString());
        assertEquals(OPEN_ACCESS, comment.getAccessCondition());
        assertEquals(user, comment.getCreator());
        assertEquals(PUBLISHED, comment.getPublicationStatus());
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                comment.getDateModified().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        assertTrue(comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < comment.getDateModified()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
        Mockito.verify(notificator, Mockito.times(1)).notifyEdit(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testDelete() throws DAOException {
        Comment comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNull(comment);

        this.manager.createComment(COMMENT_TEXT, this.user, PI, page, OPEN_ACCESS, PUBLISHED);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNotNull(comment);

        this.manager.deleteComment(comment);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNull(comment);

        Mockito.verify(notificator, Mockito.times(1)).notifyDeletion(Mockito.any(), Mockito.any());

    }

    /**
     * @see CommentManager#getNotificationUserGroupsForRecord(String)
     * @verifies return user groups for matching comment views
     */
    @Test
    void getNotificationUserGroupsForRecord_shouldReturnUserGroupsForMatchingCommentViews() throws Exception {
        Set<UserGroup> result = CommentManager.getNotificationUserGroupsForRecord("02008011811811");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }

    /**
     * @see CommentManager#isUserHasAccessToCommentGroups(User)
     * @verifies return false if user null
     */
    @Test
    void isUserHasAccessToCommentGroups_shouldReturnFalseIfUserNull() throws Exception {
        Assertions.assertFalse(CommentManager.isUserHasAccessToCommentGroups(null));
    }

    /**
     * @see CommentManager#isUserHasAccessToCommentGroups(User)
     * @verifies return true if user admin
     */
    @Test
    void isUserHasAccessToCommentGroups_shouldReturnTrueIfUserAdmin() throws Exception {
        User admin = new User();
        admin.setSuperuser(true);
        Assertions.assertTrue(CommentManager.isUserHasAccessToCommentGroups(admin));
    }

    /**
     * @see CommentManager#isUserHasAccessToCommentGroups(User)
     * @verifies return true if user owner of user group linked to comment group
     */
    @Test
    void isUserHasAccessToCommentGroups_shouldReturnTrueIfUserOwnerOfUserGroupLinkedToCommentGroup() throws Exception {
        User owner = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(owner);
        Assertions.assertTrue(CommentManager.isUserHasAccessToCommentGroups(owner));
    }

    /**
     * @see CommentManager#isUserHasAccessToCommentGroups(User)
     * @verifies return true if user member of user group linked to comment group
     */
    @Test
    void isUserHasAccessToCommentGroups_shouldReturnTrueIfUserMemberOfUserGroupLinkedToCommentGroup() throws Exception {
        User member = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(member);
        Assertions.assertTrue(CommentManager.isUserHasAccessToCommentGroups(member));
    }
}
