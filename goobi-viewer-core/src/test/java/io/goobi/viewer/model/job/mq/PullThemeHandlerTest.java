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
package io.goobi.viewer.model.job.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.managedbeans.AdminDeveloperBean.VersionInfo;

class PullThemeHandlerTest {

    private static final String resultString = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<themepull>\n"
            + "  <branch>develop</branch>\n"
            + "  <revision>0f34462</revision>\n"
            + "  <message>pom.xml: enforce newer maven version</message>\n"
            + "</themepull>";

    @Test
    void testReadXml() throws JDOMException, IOException {
        Document doc = XmlTools.getDocumentFromString(resultString, "utf-8");
        assertEquals("develop", doc.getRootElement().getChildText("branch"));
        assertEquals("0f34462", doc.getRootElement().getChildText("revision"));
        assertEquals("pom.xml: enforce newer maven version", doc.getRootElement().getChildText("message"));
    }

    @Test
    void testReadInfo() throws JDOMException, IOException {
        VersionInfo info = PullThemeHandler.getVersionInfo(resultString, "2025-01-01");
        assertEquals("develop", info.getReleaseVersion());
        assertEquals("0f34462", info.getGitRevision());
        assertEquals("2025-01-01", info.getBuildDate());
    }

    @Test
    void testReadMessage() throws JDOMException, IOException {
        String message = PullThemeHandler.getMessage(resultString);
        assertEquals("pom.xml: enforce newer maven version", message);
    }

}
