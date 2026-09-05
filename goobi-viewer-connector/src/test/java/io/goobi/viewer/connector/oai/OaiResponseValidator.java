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
package io.goobi.viewer.connector.oai;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Assertions;
import org.xml.sax.SAXException;

/**
 * Validates OAI-PMH responses against the protocol schema.
 *
 * <p>Only the protocol envelope is checked. The schema declares the children of {@code metadata}, {@code about} and
 * {@code description} with {@code processContents="strict"}, which would pull in the schema of every metadata format;
 * those children are therefore replaced by a placeholder before validation. Whether a metadata payload is valid is the
 * responsibility of the system that produced it, not of this interface.
 */
public final class OaiResponseValidator {

    /** Local copy of https://www.openarchives.org/OAI/2.0/OAI-PMH.xsd (the schema referenced by every response). */
    private static final File SCHEMA_FILE = new File("src/test/resources/oai/OAI-PMH.xsd");

    private static final Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    /** Namespace of the placeholder that replaces metadata payloads; anything but the OAI namespace satisfies {@code ##other}. */
    private static final Namespace PLACEHOLDER_NS = Namespace.getNamespace("p", "urn:goobi:oai-test:payload");

    private OaiResponseValidator() {
    }

    /**
     * Asserts that the given response is a schema-valid OAI-PMH response.
     *
     * @param response response document as returned by the servlet
     */
    public static void assertValid(Document response) {
        String xml = new XMLOutputter(Format.getPrettyFormat()).outputString(stripPayloads(response));
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            restrictExternalAccess(factory);
            Schema schema = factory.newSchema(SCHEMA_FILE);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        } catch (SAXException e) {
            Assertions.fail("Response does not validate against OAI-PMH.xsd: " + e.getMessage() + "\n" + xml);
        } catch (IOException e) {
            Assertions.fail("Could not read the OAI-PMH schema: " + e.getMessage());
        }
    }

    /**
     * Parses the given XML and asserts that it is a schema-valid OAI-PMH response.
     *
     * @param xml serialized response
     * @return the parsed response, for further assertions
     */
    public static Document assertValid(String xml) {
        Document doc;
        try {
            doc = new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (JDOMException | IOException e) {
            Assertions.fail("Response is not well-formed XML: " + e.getMessage() + "\n" + xml);
            return null;
        }
        assertValid(doc);
        return doc;
    }

    /**
     * Forbids the schema factory from resolving anything outside the local schema file. The properties are optional:
     * not every parser on the classpath knows them, and the schema used here has no imports to resolve anyway.
     *
     * @param factory factory to restrict
     */
    private static void restrictExternalAccess(SchemaFactory factory) {
        for (String property : List.of(XMLConstants.ACCESS_EXTERNAL_DTD, XMLConstants.ACCESS_EXTERNAL_SCHEMA)) {
            try {
                factory.setProperty(property, "");
            } catch (SAXException e) {
                // Parser does not support the property; the schema is a local file without imports, so nothing is resolved
            }
        }
    }

    /**
     * Returns a copy of the response in which every metadata payload is replaced by a placeholder element.
     *
     * @param response response document
     * @return copy with payloads replaced
     */
    private static Document stripPayloads(Document response) {
        Document copy = response.clone();
        replacePayloads(copy.getRootElement());
        return copy;
    }

    private static void replacePayloads(Element element) {
        for (Element child : element.getChildren()) {
            if (OAI_NS.equals(child.getNamespace()) && List.of("metadata", "about", "description").contains(child.getName())) {
                child.removeContent();
                child.addContent(new Element("payload", PLACEHOLDER_NS));
            } else {
                replacePayloads(child);
            }
        }
    }
}
