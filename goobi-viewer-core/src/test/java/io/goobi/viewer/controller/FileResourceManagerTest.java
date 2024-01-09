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
package io.goobi.viewer.controller;

import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author florian
 *
 */
public class FileResourceManagerTest {

    FileResourceManager manager;

    private static final String THEME_PATH = "/opt/digiverso/themes/theme/reference/WebContent/";
    private static final String CORE_PATH = "/var/lib/tomcat/webapps/viewer/resources/";

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getRealPath(Mockito.matches("^resources\\/themes.*"))).thenAnswer(invocation -> {
            return THEME_PATH + invocation.getArgument(0);
        });
        Mockito.when(servletContext.getRealPath(Mockito.matches("^resources(?!/themes).*"))).thenAnswer(invocation -> {
            return CORE_PATH + invocation.getArgument(0);
        });
        manager = new FileResourceManager(servletContext, "reference");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link io.goobi.viewer.controller.FileResourceManager#getCoreResourcePath(java.lang.String)}.
     */
    @Test
    void testGetCoreResourcePath() {
        assertEquals(CORE_PATH + "resources/images/icon.png", manager.getCoreResourcePath("images/icon.png").toString());
    }

    /**
     * Test method for {@link io.goobi.viewer.controller.FileResourceManager#getThemeResourcePath(java.lang.String)}.
     */
    @Test
    void testGetThemeResourcePath() {
        assertEquals(THEME_PATH + "resources/themes/reference/images/icon.png", manager.getThemeResourcePath("images/icon.png").toString());
    }

    /**
     * Test method for {@link io.goobi.viewer.controller.FileResourceManager#getCoreResourceURI(java.lang.String)}.
     */
    @Test
    void testGetCoreResourceURI() {
        assertEquals("/resources/images/icon.png", manager.getCoreResourceURI("images/icon.png").toString());
    }

    /**
     * Test method for {@link io.goobi.viewer.controller.FileResourceManager#getThemeResourceURI(java.lang.String)}.
     */
    @Test
    void testGetThemeResourceURI() {
        assertEquals("/resources/themes/reference/images/icon.png", manager.getThemeResourceURI("images/icon.png").toString());
    }

}
