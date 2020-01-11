package io.goobi.viewer;
/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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


import org.junit.BeforeClass;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

/**
 * JUnit test classes that extend this class will have test-specific logging and config configurations.
 */
public abstract class AbstractTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("log4j.configurationFile", "resources/test/log4j2.test.xml");
        
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }
}