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

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.security.user.User;

/**
 * @author Florian Alpers
 *
 */
public class BookmarkResourceTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String PI_1 = "PPN514154005";
    private static final String PI_2 = "34115495_1940";
    private static final String PI_3 = "168714434_1805";
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
        Configuration configuration = new Configuration("src/test/resources/config_viewer.test.xml");
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

        BookmarkList bs = resource.getSessionBookmarkList();
        Assert.assertNotNull(bs);

        resource.addToSessionBookmarkList(PI_1);
        Assert.assertEquals(1, resource.countSessionBookmarks(), 0);
        resource.addToSessionBookmarkList(PI_2);
        Assert.assertEquals(2, resource.countSessionBookmarks(), 0);
        resource.addToSessionBookmarkList(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(3, resource.countSessionBookmarks(), 0);

        Assert.assertTrue(resource.isInSessionBookmarkList(PI_2));
        Assert.assertTrue(resource.isInSessionBookmarkList(PI_3, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInSessionBookmarkList(PI_2, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInSessionBookmarkList(PI_3));

        resource.deleteFromSessionBookmarkList(PI_2);
        Assert.assertEquals(2, resource.countSessionBookmarks(), 0);

        resource.deleteFromSessionBookmarkList(PI_3);
        Assert.assertEquals(2, resource.countSessionBookmarks(), 0);

        resource.deleteFromSessionBookmarkList(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(1, resource.countSessionBookmarks(), 0);

        resource.deleteSessionBookmarkList();
        Assert.assertEquals(0, resource.countSessionBookmarks(), 0);
    }

    @Test
    public void testUserBookshelves() throws DAOException, IOException, RestApiException {

        List<BookmarkList> bookmarkLists = resource.getAllUserBookmarkLists();
        Assert.assertNotNull(bookmarkLists);
        Assert.assertEquals(1, bookmarkLists.size());

        resource.addUserBookmarkList("List 1");
        resource.addUserBookmarkList("Test 2");
        resource.addUserBookmarkList();

        bookmarkLists = resource.getAllUserBookmarkLists();
        Assert.assertEquals(4, bookmarkLists.size());

        Long id1 = bookmarkLists.get(1).getId();
        Assert.assertNotNull(id1);
        Long id2 = bookmarkLists.get(2).getId();
        Assert.assertNotNull(id2);
        Long id3 = bookmarkLists.get(3).getId();
        Assert.assertNotNull(id3);

        BookmarkList bs1 = resource.getUserBookmarkListById(id1);
        Assert.assertNotNull(bs1);
        Assert.assertEquals("List 1", bs1.getName());
        BookmarkList bs2 = resource.getUserBookmarkListById(id2);
        Assert.assertNotNull(bs2);
        Assert.assertEquals("Test 2", bs2.getName());
        BookmarkList bs3 = resource.getUserBookmarkListById(id3);
        Assert.assertNotNull(bs3);
        Assert.assertEquals("List 2", bs3.getName());
        resource.setUserBookmarkListName(id3, "TEST");
        Assert.assertEquals("TEST", resource.getUserBookmarkListById(id3).getName());

        Assert.assertEquals(new SuccessMessage(true), resource.addBookmarkToUserBookmarkList(id1, PI_1));
        Assert.assertEquals(new SuccessMessage(true), resource.addBookmarkToUserBookmarkList(id2, PI_1));
        Assert.assertEquals(new SuccessMessage(true), resource.addBookmarkToUserBookmarkList(id2, PI_2));
        Assert.assertEquals(new SuccessMessage(false), resource.addBookmarkToUserBookmarkList(id2, PI_2));
        Assert.assertEquals(new SuccessMessage(true), resource.addBookmarkToUserBookmarkList(id2, PI_3, LOGID_1, PAGE_1));

        Assert.assertEquals(1, resource.countUserBookmarks(id1), 0);
        Assert.assertEquals(3, resource.countUserBookmarks(id2), 0);

        Assert.assertTrue(resource.getContainingUserBookmarkLists(PI_1).contains(bs1));
        Assert.assertTrue(resource.getContainingUserBookmarkLists(PI_1).contains(bs2));
        Assert.assertTrue(resource.getContainingUserBookmarkLists(PI_3, LOGID_1, PAGE_1).contains(bs2));

        resource.deleteBookmarkFromUserBookmarkList(id1, PI_1);
        Assert.assertEquals(0, resource.countUserBookmarks(id1), 0);
        resource.deleteBookmarkFromUserBookmarkList(id2, PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(2, resource.countUserBookmarks(id2), 0);

        resource.deleteUserBookmarkList(id1);
        resource.deleteUserBookmarkList(id3);
        resource.deleteUserBookmarkList(id2);
        Assert.assertEquals(1, resource.getAllUserBookmarkLists().size());

        List<BookmarkList> publicBookshelves = resource.getAllPublicBookmarkLists();
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
