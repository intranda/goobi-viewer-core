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
package io.goobi.viewer.model.metadata;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * @author florian
 * 
 * Creates a xml document representing a simple Dublin Core record.
 * Each instance of this class creates a single record which can be filled with metadata
 * and eventually written to the file system
 *
 */
public class DCRecordWriter {

    private static final Namespace namespaceDC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    private final Document doc;
    
    /**
     * Creates a new jdom document with an empty record element
     */
    public DCRecordWriter() {
        doc = new Document();
        Element record = new Element("record", namespaceDC);
        doc.setRootElement(record);
    }
    
    /**
     * Add a metadata element with namespace "dc" to the record element
     * 
     * @param name
     * @param value
     */
    public void addDCMetadata(String name, String value) {
        if(StringUtils.isNotBlank(value)) {            
            Element md = new Element(name, namespaceDC);
            md.setText(value);
            doc.getRootElement().addContent(md);
        }
    }
    
    public String getMetadataValue(String name) {
        Element ele = doc.getRootElement().getChild(name, namespaceDC);
        if(ele != null) {
            return ele.getText();
        } else {
            return null;
        }
    }
        
    /**
     * Get the base jdom2 document
     * 
     * @return the jdom2 document representing the record
     */
    public Document getDocument() {
        return this.doc;
    }
    
    public void write(Path path) throws IOException {
        Path filePath = path;
        if(Files.isDirectory(path)) {
            if(StringUtils.isNotBlank(getMetadataValue("identifier"))) {
                filePath = path.resolve(getMetadataValue("identifier") + ".xml");
            } else if(StringUtils.isNotBlank(getMetadataValue("title"))) {
                filePath = path.resolve(getMetadataValue("title") + ".xml");
            } else {
                filePath = path.resolve(System.currentTimeMillis() + ".xml");
            }
        } else if(!Files.exists(path.getParent())) {
            throw new IOException("Parent directory of output destination " + path + " must exist to create file");
        }
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        try(OutputStream out = Files.newOutputStream(filePath)) {
            xmlOutput.output(doc, out);
        }
    }
    
}
