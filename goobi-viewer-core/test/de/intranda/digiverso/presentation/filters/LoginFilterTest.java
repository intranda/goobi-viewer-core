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
package de.intranda.digiverso.presentation.filters;

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
        Assert.assertTrue(LoginFilter.isRestrictedUri("/mysearches/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/mybookshelves/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/otherbookshelves/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/bookshelf/"));
        Assert.assertTrue(LoginFilter.isRestrictedUri("/editbookshelf/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for crowdsourcing uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForCrowdsourcingUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("boo.hoo/crowdMyAss/"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return false for crowdsourcing about page
     */
    @Test
    public void isRestrictedUri_shouldReturnFalseForCrowdsourcingAboutPage() throws Exception {
        Assert.assertFalse(LoginFilter.isRestrictedUri("boo.hoo/crowdsourcing/about.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for admin uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForAdminUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("boo.hoo/adminMyAss.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for user backend uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForUserBackendUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("boo.hoo/userBackendSlap.xhtml"));
    }

    /**
     * @see LoginFilter#isRestrictedUri(String)
     * @verifies return true for bookshelf uris
     */
    @Test
    public void isRestrictedUri_shouldReturnTrueForBookshelfUris() throws Exception {
        Assert.assertTrue(LoginFilter.isRestrictedUri("boo.hoo/bookshelfIsEmpty"));
    }
}