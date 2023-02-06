package io.goobi.viewer.controller.mq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        this(Paths.get(ActiveMQConfig.class.getClassLoader().getResource("").getFile(), filename));

    }

    public String getConnectorURI() {

        String uriString = getValue("/beans/broker/transportConnectors/transportConnector[@name=\"openwire\"]/@uri", "tcp://0.0.0.0:61616");
        return UriBuilder.fromUri(uriString).replaceQuery("").build().toString();
    }

    public String getUsernameAdmin() {
        NodeList nodes = getNodes("/beans/broker/plugins/simpleAuthenticationPlugin/users/authenticationUser");
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
        return "goobi";
    }
    
    public String getPasswordAdmin() {
        NodeList nodes = getNodes("/beans/broker/plugins/simpleAuthenticationPlugin/users/authenticationUser");
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
        return "goobi";
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
            } else {
                logger.error("Cannot find node {}", path);
                return defaultValue;
            }
        } catch (XPathExpressionException e) {
            logger.error("Cannot compile connector uri expression '{}': {}", path, e.toString());
            return defaultValue;
        }
    }

    private NodeList getNodes(String path) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nodeList = (NodeList) xPath.compile(path).evaluate(this.config, XPathConstants.NODESET);
            return nodeList;
        } catch (XPathExpressionException e) {
            logger.error("Cannot compile connector uri expression '{}': {}", path, e.toString());
            return null;
        }
    }

}
