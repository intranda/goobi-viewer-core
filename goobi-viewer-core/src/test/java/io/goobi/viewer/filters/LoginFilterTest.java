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
package io.goobi.viewer.filters;

import org.junit.Assert;
import org.junit.Test;

public class LoginFilterTest {

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for certain pretty uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForCertainPrettyUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("/myactivity/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/user/annotations/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/user/searches/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/user/bookmarks/show"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/viewer/user/bookmarks/show"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for crowdsourcing uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForCrowdsourcingUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("/crowdMyAss/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for crowdsourcing about page
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForCrowdsourcingAboutPage() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/crowdsourcing/about.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for admin uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForAdminUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("/adminMyAss.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for user backend uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForUserBackendUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("/userBackendSlap.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for user bookmarks uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForUserBookmarksUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("/viewer/user/bookmarks/etc"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for bookmarks list uri
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForBookmarksListUri() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/bookmarks"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for bookmarks session uris
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForBookmarksSessionUris() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/bookmarks/session/foo"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for bookmarks share key uris
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForBookmarksShareKeyUris() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/bookmarks/key/somesharekey/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for bookmarks send list uris
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForBookmarksSendListUris() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/bookmarks/send/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for user account activation uris
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForUserAccountActivationUris() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/user/activate/foo@bar.com/abcde/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for user password reset uris
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForUserPasswordResetUris() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("/user/resetpw/foo@bar.com/abcde/"));
    }
}
