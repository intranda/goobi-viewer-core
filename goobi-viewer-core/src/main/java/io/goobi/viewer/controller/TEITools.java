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
package io.goobi.viewer.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.XSLTransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * TEITools class.
 * </p>
 */
public class TEITools {

    private final static Logger logger = LoggerFactory.getLogger(TEITools.class);

    /**
     * Returns the full-text part of the given TEI document string.
     *
     * @param tei Full TEI document as string
     * @return TEI full-text element
     * @should extract fulltext correctly
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     */
    public static String getTeiFulltext(String tei) throws JDOMException, IOException {
        if (tei == null) {
            return null;
        }

        Document doc = XmlTools.getDocumentFromString(tei, Helper.DEFAULT_ENCODING);
        if (doc == null) {
            return null;
        }
        if (doc.getRootElement() != null) {
            Element eleText = doc.getRootElement().getChild("text", null);
            if (eleText != null && eleText.getChild("body", null) != null) {
                String language = eleText.getAttributeValue("lang", Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace"));
                Element eleBody = eleText.getChild("body", null);
                Element eleNewRoot = new Element("tempRoot");
                for (Element ele : eleBody.getChildren()) {
                    eleNewRoot.addContent(ele.clone());
                }
                String ret = XmlTools.getStringFromElement(eleNewRoot, null).replace("<tempRoot>", "").replace("</tempRoot>", "").trim();
                ret = ret.replaceAll("<note>[\\s\\S]*?<\\/note>", "");
                return ret;
            }
        }

        return null;
    }

    /**
     * <p>
     * convertTeiToHtml.
     * </p>
     *
     * @param tei a {@link java.lang.String} object.
     * @return HTML conversion of the TEI
     * @should convert tei to html correctly
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    public static String convertTeiToHtml(String tei) throws IOException, JDOMException {
        if (tei == null) {
            return null;
        }

        Document teiDoc = XmlTools.getDocumentFromString(tei, Helper.DEFAULT_ENCODING);
        Document htmlDoc = XmlTools.transformViaXSLT(teiDoc,
                DataManager.getInstance().getConfiguration().getViewerHome() + "resources/TEI/html5/html5.xsl", null);
        if (htmlDoc != null) {
            return new XMLOutputter().outputString(htmlDoc);
        }

        return null;
    }

    /**
     * <p>
     * convertDocxToTei.
     * </p>
     *
     * @param docxFile a {@link java.nio.file.Path} object.
     * @should convert docx to tei correctly
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    @Deprecated
    public static String convertDocxToTei(Path docxFile) throws IOException {
        if (docxFile == null) {
            throw new IllegalArgumentException("docxFile may not be null");
        }

        //        try (InputStream in = new FileInputStream(docxFile.toFile()); XWPFDocument document = new XWPFDocument(in);
        //                OutputStream out = new ByteArrayOutputStream();) {
        //
        //                  XHTMLOptions options = XHTMLOptions.create().URIResolver(new FileURIResolver(new File("word/media")));
        //                        XHTMLConverter.getInstance().convert(document, out, options);
        //            
        //    
        //            return out.toString();
        //        }

        //      XHTMLOptions options = XHTMLOptions.create().URIResolver(new FileURIResolver(new File("word/media")));
        //            XHTMLConverter.getInstance().convert(document, out, options);

        // TODO Unzip docxFile
        try {
            String wordDirectory = "C:/digiverso/viewer/temp/docx/";
            Document docxDoc = XmlTools.readXmlFile(wordDirectory + "word/document.xml");

            Document teiDoc = XmlTools.transformViaXSLT(docxDoc,
                    DataManager.getInstance().getConfiguration().getViewerHome() + "resources/TEI/docx/from/docxtotei.xsl",
                    Collections.singletonMap("word-directory", wordDirectory));
            if (teiDoc != null) {
                return new XMLOutputter().outputString(teiDoc);
            }
        } catch (XSLTransformException e) {
            logger.error(e.getMessage(), e);
        } catch (JDOMException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
