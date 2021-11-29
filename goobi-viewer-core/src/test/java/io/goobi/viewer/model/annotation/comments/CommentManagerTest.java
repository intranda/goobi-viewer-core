package io.goobi.viewer.model.annotation.comments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
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

public class CommentManagerTest extends AbstractDatabaseEnabledTest {

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
    
    @Before
    public void setup() throws Exception {
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
    public void testCreate() throws DAOException {
        Comment comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        assertNull(comment);
        
        this.manager.createComment(COMMENT_TEXT, this.user, PI, page, OPEN_ACCESS, PUBLISHED);
        comment = dao.getCommentsForPage(PI, page).stream().findFirst().orElse(null);
        
        assertNotNull(comment);
        assertEquals(COMMENT_TEXT, comment.getContentString());
        assertEquals(OPEN_ACCESS, comment.getAccessCondition());
        assertEquals(user, comment.getCreator());
        assertEquals(PUBLISHED, comment.getPublicationStatus());
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        Mockito.verify(notificator, Mockito.times(1)).notifyCreation(Mockito.any(), Mockito.any());
    }
    
    @Test
    public void testModify() throws DAOException {
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
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        assertEquals(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), comment.getDateModified().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1000l);
        assertTrue(comment.getDateCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < comment.getDateModified().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        Mockito.verify(notificator, Mockito.times(1)).notifyEdit(Mockito.any(), Mockito.any(), Mockito.any());
    }
    
    @Test
    public void testDelete() throws DAOException {
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

}
