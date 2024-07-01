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
package io.goobi.viewer.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.goobi.viewer.model.xml.ReportErrorsErrorHandler;
import io.goobi.viewer.model.xml.XMLError;

/**
 * XML utilities.
 */
public final class XmlTools {

    private static final Logger logger = LogManager.getLogger(XmlTools.class);

    private XmlTools() {
    }

    public static SAXBuilder getSAXBuilder() {
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
    public static Document readXmlFile(String filePath) throws IOException, JDOMException {
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
    public static Document readXmlFile(URL url) throws IOException, JDOMException {
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
    public static Document readXmlFile(Path path) throws IOException, JDOMException {
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
     * @return a {@link java.io.File} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @should write file correctly
     * @should throw FileSystemException if file is directory
     */
    public static File writeXmlFile(Document doc, String filePath) throws IOException {
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
    public static Document getDocumentFromString(String string, final String encoding) throws JDOMException, IOException {
        byte[] byteArray = null;
        try {
            byteArray = string.getBytes(encoding == null ? StringTools.DEFAULT_ENCODING : encoding);
        } catch (UnsupportedEncodingException e) {
            logger.trace(e.getMessage());
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
    public static String getStringFromElement(Object element, final String encoding) {
        if (element == null) {
            throw new IllegalArgumentException("element may not be null");
        }

        Format format = Format.getRawFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        Format xmlFormat = outputter.getFormat();
        String useEncoding = encoding == null ? StringTools.DEFAULT_ENCODING : encoding;
        if (StringUtils.isNotEmpty(useEncoding)) {
            xmlFormat.setEncoding(useEncoding);
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
     * @param element a {@link org.jdom2.Element} object.
     * @param namespaces a {@link java.util.List} object.
     * @return {@link java.util.ArrayList} or null
     * @should return all values
     */
    public static List<Element> evaluateToElements(String expr, Element element, List<Namespace> namespaces) {
        List<Element> retList = new ArrayList<>();

        List<Object> list = evaluate(expr, element, Filters.element(), namespaces);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        for (Object object : list) {
            if (object instanceof Element) {
                retList.add((Element) object);
            }
        }

        return retList;
    }

    public static Optional<Element> evaluateToFirstElement(String expr, Element element, List<Namespace> namespaces) {
        return evaluateToElements(expr, element, namespaces).stream().findFirst();
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

    public static List<String> evaluateAttributeString(String expr, Object parent, List<Namespace> namespaces) {
        XPathBuilder<Attribute> builder = new XPathBuilder<>(expr.trim().replace("\n", ""), Filters.attribute());

        if (namespaces != null && !namespaces.isEmpty()) {
            builder.setNamespaces(namespaces);
        }

        XPathExpression<Attribute> xpath = builder.compileWith(XPathFactory.instance());
        return xpath.evaluate(parent)
                .stream()
                .map(Attribute::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<String> evaluateString(String expr, Object parent, List<Namespace> namespaces) {
        XPathBuilder<Element> builder = new XPathBuilder<>(expr.trim().replace("\n", ""), Filters.element());

        if (namespaces != null && !namespaces.isEmpty()) {
            builder.setNamespaces(namespaces);
        }

        XPathExpression<Element> xpath = builder.compileWith(XPathFactory.instance());
        return xpath.evaluate(parent)
                .stream()
                .map(Element::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Optional<String> evaluateToFirstAttributeString(String expr, Object parent, List<Namespace> namespaces) {
        return evaluateAttributeString(expr, parent, namespaces).stream().findFirst();
    }

    public static Optional<String> evaluateToFirstString(String expr, Object parent, List<Namespace> namespaces) {
        return evaluateString(expr, parent, namespaces).stream().findFirst();
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
     * 
     * @param xml
     * @return List<XMLError>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static List<XMLError> checkXMLWellformed(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        factory.setValidating(false);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        ReportErrorsErrorHandler eh = new ReportErrorsErrorHandler();
        builder.setErrorHandler(eh);

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try {
            builder.parse(bais);
        } catch (SAXParseException e) {
            //ignore this, because we collect the errors in the error handler and give them to the user.
        }

        return eh.getErrors();
    }

    /**
     * Create an XMLOutputter with default encoding and system linebreaks
     * 
     * @return a new XMLOutputter instance with standard format settings
     */
    public static XMLOutputter getXMLOutputter() {
        Format format = Format.getRawFormat();
        format.setEncoding(StringTools.DEFAULT_ENCODING);
        format.setLineSeparator(LineSeparator.SYSTEM);
        return new XMLOutputter(format);
    }
}
