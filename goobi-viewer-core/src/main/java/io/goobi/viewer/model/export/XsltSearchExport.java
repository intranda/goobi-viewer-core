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
package io.goobi.viewer.model.export;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;

import io.goobi.viewer.controller.DataManager;

/**
 * Applies a configurable XSLT stylesheet to Solr search results serialised as XML,
 * producing output in formats such as RIS, Endnote XML, BibTeX, or any other
 * XSLT-derivable format.
 *
 * <p>Stylesheets are resolved from the viewer config directory under
 * {@code <configFolder>/xsl/} (e.g. {@code /opt/digiverso/viewer/config/xsl/}).
 */
public final class XsltSearchExport {

    private static final Logger logger = LogManager.getLogger(XsltSearchExport.class);

    private XsltSearchExport() {
        // static utility
    }

    /**
     * Transforms the given Solr documents through the named XSLT stylesheet.
     *
     * @param docs the Solr documents to transform
     * @param xsltFileName the name of the XSLT file (e.g. {@code "solr2endnote.xsl"})
     * @return the transformed string output
     * @throws TransformerException if the XSLT transformation fails
     * @throws ParserConfigurationException if the XML document builder cannot be created
     * @should transform documents using the given stylesheet
     * @should throw TransformerException for missing stylesheet
     */
    public static String transform(SolrDocumentList docs, String xsltFileName)
            throws TransformerException, ParserConfigurationException {
        if (docs == null) {
            throw new IllegalArgumentException("docs may not be null");
        }
        if (xsltFileName == null || xsltFileName.isBlank()) {
            throw new IllegalArgumentException("xsltFileName may not be null or blank");
        }

        Document xmlDoc = SolrDocXmlExport.toXmlDocument(docs);
        Source xsltSource = resolveStylesheet(xsltFileName);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(xsltSource);

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(xmlDoc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Resolves the XSLT stylesheet from the viewer config directory
     * ({@code <configFolder>/xsl/<xsltFileName>}).
     *
     * @param xsltFileName the XSLT file name (e.g. {@code "solr2endnote.xsl"})
     * @return a {@link Source} for the stylesheet
     * @throws TransformerException if the stylesheet cannot be found
     */
    static Source resolveStylesheet(String xsltFileName) throws TransformerException {
        String configFolder = DataManager.getInstance().getConfiguration().getConfigLocalPath();
        Path xslPath = Paths.get(configFolder, "xsl", xsltFileName);
        if (Files.isRegularFile(xslPath)) {
            logger.debug("Using XSLT stylesheet: {}", xslPath);
            return new StreamSource(xslPath.toFile());
        }

        throw new TransformerException("XSLT stylesheet not found: " + xslPath);
    }
}
