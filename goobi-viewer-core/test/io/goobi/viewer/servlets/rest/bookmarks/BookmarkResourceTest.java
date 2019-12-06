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
package io.goobi.viewer.servlets.rest.bookmarks;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.SuccessMessage;
import io.goobi.viewer.servlets.rest.bookmarks.BookmarkResource;

/**
 * @author Florian Alpers
 *
 */
public class BookmarkResourceTest extends AbstractDatabaseEnabledTest {

    private static final String PI_1 = "pi1";
    private static final String PI_2 = "pi2";
    private static final String PI_3 = "pi3";
    private static final String LOGID_1 = "LOG_0004";
    private static final String PAGE_1 = "7";

    private BookmarkResource resource;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Configuration configuration = new Configuration("resources/test/config_viewer.test.xml");
        DataManager.getInstance().injectConfiguration(configuration);

        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        UserBean userBean = new UserBean();
        userBean.setUser(user);

        HttpServletRequest request = TestUtils.mockHttpRequest();

        resource = new BookmarkResource(userBean, request);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSessionBookshelf() throws DAOException, IOException, RestApiException {

        BookmarkList bs = resource.getSessionBookshelf();
        Assert.assertNotNull(bs);

        resource.addToSessionBookshelf(PI_1);
        Assert.assertEquals(1, resource.countSessionBookshelfItems(), 0);
        resource.addToSessionBookshelf(PI_2);
        Assert.assertEquals(2, resource.countSessionBookshelfItems(), 0);
        resource.addToSessionBookshelf(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(3, resource.countSessionBookshelfItems(), 0);

        Assert.assertTrue(resource.isInSessionBookshelf(PI_2));
        Assert.assertTrue(resource.isInSessionBookshelf(PI_3, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInSessionBookshelf(PI_2, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInSessionBookshelf(PI_3));

        resource.deleteFromSessionBookshelf(PI_2);
        Assert.assertEquals(2, resource.countSessionBookshelfItems(), 0);

        resource.deleteFromSessionBookshelf(PI_3);
        Assert.assertEquals(2, resource.countSessionBookshelfItems(), 0);

        resource.deleteFromSessionBookshelf(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(1, resource.countSessionBookshelfItems(), 0);

        resource.deleteSessionBookshelf();
        Assert.assertEquals(0, resource.countSessionBookshelfItems(), 0);
    }

    @Test
    public void testUserBookshelves() throws DAOException, IOException, RestApiException {

        List<BookmarkList> bookmarkLists = resource.getAllUserBookmarkLists();
        Assert.assertNotNull(bookmarkLists);
        Assert.assertEquals(1, bookmarkLists.size());

        resource.addUserBookshelf("List 1");
        resource.addUserBookshelf("Test 2");
        resource.addUserBookshelf();

        bookmarkLists = resource.getAllUserBookmarkLists();
        Assert.assertEquals(4, bookmarkLists.size());

        Long id1 = bookmarkLists.get(1).getId();
        Assert.assertNotNull(id1);
        Long id2 = bookmarkLists.get(2).getId();
        Assert.assertNotNull(id2);
        Long id3 = bookmarkLists.get(3).getId();
        Assert.assertNotNull(id3);

        BookmarkList bs1 = resource.getUserBookshelfById(id1);
        Assert.assertNotNull(bs1);
        Assert.assertEquals("List 1", bs1.getName());
        BookmarkList bs2 = resource.getUserBookshelfById(id2);
        Assert.assertNotNull(bs2);
        Assert.assertEquals("Test 2", bs2.getName());
        BookmarkList bs3 = resource.getUserBookshelfById(id3);
        Assert.assertNotNull(bs3);
        Assert.assertEquals("List 2", bs3.getName());
        resource.setUserBookmarkListName(id3, "TEST");
        Assert.assertEquals("TEST", resource.getUserBookshelfById(id3).getName());

        Assert.assertEquals(new SuccessMessage(true), resource.addItemToUserBookshelf(id1, PI_1));
        Assert.assertEquals(new SuccessMessage(true), resource.addItemToUserBookshelf(id2, PI_1));
        Assert.assertEquals(new SuccessMessage(true), resource.addItemToUserBookshelf(id2, PI_2));
        Assert.assertEquals(new SuccessMessage(false), resource.addItemToUserBookshelf(id2, PI_2));
        Assert.assertEquals(new SuccessMessage(true), resource.addItemToUserBookmarkList(id2, PI_3, LOGID_1, PAGE_1));

        Assert.assertEquals(1, resource.countUserBookshelfItems(id1), 0);
        Assert.assertEquals(3, resource.countUserBookshelfItems(id2), 0);

        Assert.assertTrue(resource.getContainingUserBookshelves(PI_1).contains(bs1));
        Assert.assertTrue(resource.getContainingUserBookshelves(PI_1).contains(bs2));
        Assert.assertTrue(resource.getContainingUserBookshelves(PI_3, LOGID_1, PAGE_1).contains(bs2));

        resource.deleteFromUserBookshelf(id1, PI_1);
        Assert.assertEquals(0, resource.countUserBookshelfItems(id1), 0);
        resource.deleteFromUserBookshelf(id2, PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(2, resource.countUserBookshelfItems(id2), 0);

        resource.deleteUserBookshelf(id1);
        resource.deleteUserBookshelf(id3);
        resource.deleteUserBookshelf(id2);
        Assert.assertEquals(1, resource.getAllUserBookmarkLists().size());

        List<BookmarkList> publicBookshelves = resource.getAllPublicBookshelfs();
        List<BookmarkList> sharedBookshelves = resource.getAllSharedBookmarkLists();
        Assert.assertTrue(publicBookshelves.stream().allMatch(bs -> bs.isIsPublic()));
        //        Assert.assertTrue(sharedBookshelves.stream()
        //                .flatMap(bs -> bs.getGroupShares().stream())
        //                .flatMap(group -> {
        //                    try {
        //                        return group.getMembers().stream();
        //                    } catch (DAOException e) {
        //                        throw new IllegalStateException();
        //                    }
        //                })
        //                .anyMatch(user -> user.equals(bs1.getOwner())));

    }

}
