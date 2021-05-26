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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML utilities.
 */
public class XmlTools {

    private static final Logger logger = LoggerFactory.getLogger(XmlTools.class);

    static SAXBuilder getSAXBuilder() {
        SAXBuilder builder = new SAXBuilder();
        // Disable access to external entities
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        return builder;
    }

    /**
     * <p>
     * readXmlFile.
     * </p>
     *
     * @param filePath a {@link java.lang.String} object.
     * @should build document from string correctly
     * @should throw FileNotFoundException if file not found
     * @return a {@link org.jdom2.Document} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    public static Document readXmlFile(String filePath) throws FileNotFoundException, IOException, JDOMException {
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            return getSAXBuilder().build(fis);
        }
    }

    /**
     * Reads an XML document from the given URL and returns a JDOM2 document. Works with XML files within JARs.
     *
     * @param url a {@link java.net.URL} object.
     * @should build document from url correctly
     * @return a {@link org.jdom2.Document} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    public static Document readXmlFile(URL url) throws FileNotFoundException, IOException, JDOMException {
        try (InputStream is = url.openStream()) {
            return getSAXBuilder().build(is);
        }
    }

    /**
     * <p>
     * readXmlFile.
     * </p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @should build document from path correctly
     * @return a {@link org.jdom2.Document} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    public static Document readXmlFile(Path path) throws FileNotFoundException, IOException, JDOMException {
        try (InputStream is = Files.newInputStream(path)) {
            return getSAXBuilder().build(is);
        }
    }

    /**
     * <p>
     * writeXmlFile.
     * </p>
     *
     * @param doc a {@link org.jdom2.Document} object.
     * @param filePath a {@link java.lang.String} object.
     * @should write file correctly
     * @should throw FileNotFoundException if file is directory
     * @return a {@link java.io.File} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     */
    public static File writeXmlFile(Document doc, String filePath) throws FileNotFoundException, IOException {
        return FileTools.getFileFromString(getStringFromElement(doc, StringTools.DEFAULT_ENCODING), filePath, StringTools.DEFAULT_ENCODING, false);
    }

    /**
     * Create a JDOM document from an XML string.
     *
     * @param string a {@link java.lang.String} object.
     * @should build document correctly
     * @param encoding a {@link java.lang.String} object.
     * @return a {@link org.jdom2.Document} object.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     */
    public static Document getDocumentFromString(String string, String encoding) throws JDOMException, IOException {
        if (encoding == null) {
            encoding = StringTools.DEFAULT_ENCODING;
        }

        byte[] byteArray = null;
        try {
            byteArray = string.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
        }
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);

        return getSAXBuilder().build(baos);
    }

    /**
     * <p>
     * getStringFromElement.
     * </p>
     *
     * @param element a {@link java.lang.Object} object.
     * @param encoding a {@link java.lang.String} object.
     * @should return XML string correctly for documents
     * @should return XML string correctly for elements
     * @return a {@link java.lang.String} object.
     */
    public static String getStringFromElement(Object element, String encoding) {
        if (element == null) {
            throw new IllegalArgumentException("element may not be null");
        }
        if (encoding == null) {
            encoding = StringTools.DEFAULT_ENCODING;
        }
        Format format = Format.getRawFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        Format xmlFormat = outputter.getFormat();
        if (StringUtils.isNotEmpty(encoding)) {
            xmlFormat.setEncoding(encoding);
        }
        xmlFormat.setExpandEmptyElements(true);
        outputter.setFormat(xmlFormat);

        String docString = null;
        if (element instanceof Document) {
            docString = outputter.outputString((Document) element);
        } else if (element instanceof Element) {
            docString = outputter.outputString((Element) element);
        }

        return docString;
    }

    /**
     * Evaluates the given XPath expression to a list of elements.
     *
     * @param expr XPath expression to evaluate.
     * @param namespaces a {@link java.util.List} object.
     * @return {@link java.util.ArrayList} or null
     * @should return all values
     * @param element a {@link org.jdom2.Element} object.
     */
    public static List<Element> evaluateToElements(String expr, Element element, List<Namespace> namespaces) {
        List<Element> retList = new ArrayList<>();

        List<Object> list = evaluate(expr, element, Filters.element(), namespaces);
        if (list == null) {
            return null;
        }
        for (Object object : list) {
            if (object instanceof Element) {
                retList.add((Element) object);
            }
        }
        return retList;
    }

    /**
     * XPath evaluation with a given return type filter.
     *
     * @param expr XPath expression to evaluate.
     * @param parent If not null, the expression is evaluated relative to this element.
     * @param filter Return type filter.
     * @param namespaces a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<Object> evaluate(String expr, Object parent, Filter filter, List<Namespace> namespaces) {
        XPathBuilder<Object> builder = new XPathBuilder<>(expr.trim().replace("\n", ""), filter);

        if (namespaces != null && !namespaces.isEmpty()) {
            builder.setNamespaces(namespaces);
        }

        XPathExpression<Object> xpath = builder.compileWith(XPathFactory.instance());
        return xpath.evaluate(parent);

    }

    /**
     * <p>
     * determineFileFormat.
     * </p>
     *
     * @param xml a {@link java.lang.String} object.
     * @param encoding a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     */
    public static String determineFileFormat(String xml, String encoding) throws JDOMException, IOException {
        if (xml == null) {
            return null;
        }
        Document doc = getDocumentFromString(xml, encoding);
        return determineFileFormat(doc);
    }

    /**
     * Determines the format of the given XML file by checking for namespaces.
     *
     * @should detect mets files correctly
     * @should detect lido files correctly
     * @should detect abbyy files correctly
     * @should detect tei files correctly
     * @param doc a {@link org.jdom2.Document} object.
     * @return a {@link java.lang.String} object.
     */
    public static String determineFileFormat(Document doc) {
        if (doc == null || doc.getRootElement() == null) {
            return null;
        }

        if (doc.getRootElement().getNamespace("mets") != null) {
            return "METS";
        }
        if (doc.getRootElement().getNamespace("lido") != null) {
            return "LIDO";
        }
        if (doc.getRootElement().getNamespace().getURI().contains("abbyy")) {
            return "ABBYYXML";
        }
        if (doc.getRootElement().getName().equals("TEI") || doc.getRootElement().getName().equals("TEI.2")) {
            return "TEI";
        }

        return null;
    }

    /**
     * Transforms the given JDOM document via the given XSLT stylesheet.
     *
     * @param doc JDOM document to transform
     * @param stylesheetPath Absolute path to the XSLT stylesheet file
     * @param params Optional transformer parameters
     * @return Transformed document; null in case of errors
     * @should transform xml correctly
     */
    public static Document transformViaXSLT(Document doc, String stylesheetPath, Map<String, String> params) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (stylesheetPath == null) {
            throw new IllegalArgumentException("stylesheetPath may not be null");
        }

        // Set TransformerFactory to something other than org.apache.xalan.processor.TransformerFactoryImpl due to incompatibilities with XMLConstants values
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

        try {
            JDOMSource docFrom = new JDOMSource(doc);
            JDOMResult docTo = new JDOMResult();

            TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            // transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(stylesheetPath));
            if (params != null && !params.isEmpty()) {
                for (String param : params.keySet()) {
                    transformer.setParameter(param, params.get(param));
                }
            }
            transformer.transform(docFrom, docTo);
            return docTo.getDocument();
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
