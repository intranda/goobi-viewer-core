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
package io.goobi.viewer.controller.mq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.util.PathConverter;

public class ActiveMQConfig {

    private static final Logger logger = LogManager.getLogger(ActiveMQConfig.class);

    private final Document config;
    private final Path configFilePath;

    public ActiveMQConfig(Document config) {
        this.config = config;
        this.configFilePath = null;
    }

    public ActiveMQConfig(Path filePath) throws IOException {
        this.configFilePath = filePath;
        if (Files.exists(filePath)) {
            try {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                builderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                builderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                this.config = builder.parse(filePath.toFile());
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new IOException("Cannot parse xml document from path " + filePath);
            }
        } else {
            throw new FileNotFoundException("Cannot find configuration file " + filePath);
        }

    }

    public ActiveMQConfig(String filename) throws IOException {
        this(getConfigResource(filename));

    }

    public static Path getConfigResource(String filename) {
        URI uri = PathConverter.toURI(ActiveMQConfig.class.getClassLoader().getResource(""));
        return PathConverter.getPath(uri).resolve(filename);
    }

    public String getConnectorURI() {

        String uriString = getValue("/beans/broker/transportConnectors/transportConnector[@name=\"openwire\"]/@uri", "tcp://0.0.0.0:61616");
        return UriBuilder.fromUri(uriString).replaceQuery("").build().toString();
    }

    public String getUsernameAdmin() {
        NodeList nodes = getNodes("/beans/broker/plugins/simpleAuthenticationPlugin/users/authenticationUser");
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Node attrGroups = node.getAttributes().getNamedItem("groups");
                if (attrGroups != null) {
                    String groups = attrGroups.getNodeValue();
                    if (groups != null && groups.contains("admins")) {
                        return node.getAttributes().getNamedItem("username").getNodeValue();
                    }
                }

            }
        }

        return "goobi";
    }

    public String getPasswordAdmin() {
        NodeList nodes = getNodes("/beans/broker/plugins/simpleAuthenticationPlugin/users/authenticationUser");
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Node attrGroups = node.getAttributes().getNamedItem("groups");
                if (attrGroups != null) {
                    String groups = attrGroups.getNodeValue();
                    if (groups != null && groups.contains("admins")) {
                        return node.getAttributes().getNamedItem("password").getNodeValue();
                    }
                }

            }
        }

        return "goobi";
    }

    public String getSchedulerDirectory() {
        return getValue("/beans/broker/@schedulerDirectory", "/temp/scheduler");
    }

    public Path getConfigFilePath() {
        return this.configFilePath;
    }

    private String getValue(String path, String defaultValue) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nodes = (NodeList) xPath.compile(path).evaluate(this.config, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes.item(0).getNodeValue();
            }
            logger.error("Cannot find node {}", path);
            return defaultValue;
        } catch (XPathExpressionException e) {
            logger.error("Cannot compile connector uri expression '{}': {}", path, e.toString());
            return defaultValue;
        }
    }

    private NodeList getNodes(String path) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return (NodeList) xPath.compile(path).evaluate(this.config, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            logger.error("Cannot compile connector uri expression '{}': {}", path, e.toString());
            return null;
        }
    }

}
