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
