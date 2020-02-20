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
package io.goobi.viewer.servlets.rest.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.servlets.rest.utils.SessionResource;

public class SessionResourceTest {

    private static final String TEST_SESSION_ID = "TEST-SESSION-ID";

    private SessionResource resource;

    @Before
    public void setUp() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));

        HttpServletRequest request = TestUtils.mockHttpRequest();
        resource = new SessionResource(request);
    }

    /**
     * @see SessionResource#getSessionInfo()
     * @verifies return session info correctly
     */
    @Test
    public void getSessionInfo_shouldReturnSessionInfoCorrectly() throws Exception {
        Map<String, String> sessionMd = new HashMap<>();
        sessionMd.put("foo", "bar");
        DataManager.getInstance().getSessionMap().put(TEST_SESSION_ID, sessionMd);

        String ret = resource.getSessionInfo();
        Assert.assertTrue(ret, resource.getSessionInfo().equals("foo: bar\n"));
    }
}
