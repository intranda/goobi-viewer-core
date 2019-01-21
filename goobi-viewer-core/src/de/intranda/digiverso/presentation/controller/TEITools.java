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
package de.intranda.digiverso.presentation.controller;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.transform.XSLTransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TEITools {

    private final static Logger logger = LoggerFactory.getLogger(TEITools.class);

    /**
     * Returns the full-text part of the given TEI document string.
     * 
     * @param tei Full TEI document as string
     * @return TEI full-text element
     * @throws JDOMException
     * @throws IOException
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
                return XmlTools.getStringFromElement(eleNewRoot, null).replace("<tempRoot>", "").replace("</tempRoot>", "").trim();
            }
        }

        return null;
    }

    /**
     * 
     * @param docxFile
     * @return
     * @throws IOException
     * @should convert docx to tei correctly
     */
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
            JDOMSource docFrom = new JDOMSource(docxDoc);
            JDOMResult docTo = new JDOMResult();

            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(
                            new StreamSource(DataManager.getInstance().getConfiguration().getViewerHome() + "resources/TEI/docx/from/docxtotei.xsl"));
            transformer.setParameter("word-directory", wordDirectory);
            transformer.transform(docFrom, docTo);
            return new XMLOutputter().outputString(docTo.getDocument());
        } catch (XSLTransformException e) {
            logger.error(e.getMessage(), e);
        } catch (JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (TransformerConfigurationException e) {
            logger.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            logger.error(e.getMessage(), e);
        } catch (TransformerException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
