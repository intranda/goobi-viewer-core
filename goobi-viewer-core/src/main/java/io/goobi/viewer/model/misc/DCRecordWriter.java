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
package io.goobi.viewer.model.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import io.goobi.viewer.controller.XmlTools;

/**
 * @author florian
 *
 *         Creates a xml document representing a simple Dublin Core record. Each instance of this class creates a single record which can be filled
 *         with metadata and eventually written to the file system
 *
 */
public class DCRecordWriter {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(DCRecordWriter.class);

    public static final Namespace NAMESPACE_DC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    private final Document doc;

    /**
     * Creates a new jdom document with an empty record element
     */
    public DCRecordWriter() {
        doc = new Document();
        Element rec = new Element("record");
        rec.addNamespaceDeclaration(NAMESPACE_DC);
        doc.setRootElement(rec);
    }

    /**
     * Add a metadata element with namespace "dc" to the record element
     *
     * @param name
     * @param value
     */
    public void addDCMetadata(String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            Element md = new Element(name, NAMESPACE_DC);
            md.setText(value);
            doc.getRootElement().addContent(md);
        }
    }

    /**
     * Reads the value of the given metadata from the jdom document
     *
     * @param name
     * @return Metadata value from the XML tree; null if none found
     */
    public String getMetadataValue(String name) {
        Element ele = doc.getRootElement().getChild(name, NAMESPACE_DC);
        if (ele != null) {
            return ele.getText();
        }

        return null;
    }

    /**
     * Get the base jdom2 document
     *
     * @return the jdom2 document representing the record
     */
    public Document getDocument() {
        return this.doc;
    }

    /**
     * Writes the created jdom document to the given path. If the path denotes a directory, a new file will be created within the directory with the
     * filename being the "identifier" metadata value if it exists. Otherwise the "title" metadata value or the current timestamp if title doesn't
     * exist either
     *
     * @param path The path to the file (created if it doesn't exist, overwritten if it does) or the directory which should contain the file
     * @throws IOException if the parent directory of the given path doesn't exist, or writing the file fails for some other reason
     */
    public void write(Path path) throws IOException {
        Path filePath = path;
        if (Files.isDirectory(path)) {
            if (StringUtils.isNotBlank(getMetadataValue("identifier"))) {
                filePath = path.resolve(getMetadataValue("identifier") + ".xml");
            } else if (StringUtils.isNotBlank(getMetadataValue("title"))) {
                filePath = path.resolve(getMetadataValue("title") + ".xml");
            } else {
                filePath = path.resolve(System.currentTimeMillis() + ".xml");
            }
        } else if (!Files.exists(path.getParent())) {
            throw new IOException("Parent directory of output destination " + path + " must exist to create file");
        }
        try (OutputStream out = Files.newOutputStream(filePath)) {
            XmlTools.getXMLOutputter().output(doc, out);
        }
    }

    public String getAsString() {
        StringWriter writer = new StringWriter();
        try {
            XmlTools.getXMLOutputter().output(doc, writer);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return writer.toString();
    }

}
