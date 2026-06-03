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
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Serialises a {@link SolrDocumentList} into Solr-style XML that can be
 * consumed directly or fed into an XSLT transformation pipeline.
 *
 * <p>The output follows the well-known Solr response XML schema:
 * <pre>{@code
 * <result name="response" numFound="N" start="0">
 *   <doc>
 *     <str name="field">value</str>
 *     <arr name="multiField"><str>v1</str><str>v2</str></arr>
 *   </doc>
 * </result>
 * }</pre>
 */
public final class SolrDocXmlExport {

    private SolrDocXmlExport() {
        // static utility
    }

    /**
     * Converts the given Solr document list to an XML string.
     *
     * @param docs the Solr documents to serialise; must not be null
     * @return XML string in Solr response format
     * @throws ParserConfigurationException if the XML document builder cannot be created
     * @throws TransformerException if the DOM-to-string transformation fails
     * @should serialise single-valued fields correctly
     * @should serialise multi-valued fields correctly
     * @should return empty result element for empty list
     */
    public static String toXmlString(SolrDocumentList docs) throws ParserConfigurationException, TransformerException {
        Document xmlDoc = toXmlDocument(docs);
        return domToString(xmlDoc);
    }

    /**
     * Converts the given Solr document list to a DOM {@link Document}.
     *
     * @param docs the Solr documents to serialise; must not be null
     * @return DOM document in Solr response format
     * @throws ParserConfigurationException if the XML document builder cannot be created
     */
    public static Document toXmlDocument(SolrDocumentList docs) throws ParserConfigurationException {
        if (docs == null) {
            throw new IllegalArgumentException("docs may not be null");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.newDocument();

        Element resultEl = xmlDoc.createElement("result");
        resultEl.setAttribute("name", "response");
        resultEl.setAttribute("numFound", String.valueOf(docs.getNumFound()));
        resultEl.setAttribute("start", String.valueOf(docs.getStart()));
        xmlDoc.appendChild(resultEl);

        for (SolrDocument solrDoc : docs) {
            Element docEl = xmlDoc.createElement("doc");
            resultEl.appendChild(docEl);

            for (Map.Entry<String, Object> entry : solrDoc.entrySet()) {
                appendField(xmlDoc, docEl, entry.getKey(), entry.getValue());
            }
        }

        return xmlDoc;
    }

    /**
     * Serialises a DOM document to a string with XML declaration and indentation.
     *
     * @param doc the DOM document to serialise
     * @return the XML string
     * @throws TransformerException if the transformation fails
     */
    static String domToString(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        // Prevent XXE: disable external DTD and stylesheet access (java:S2755)
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Appends a Solr field to the given DOM element. Multi-valued fields are wrapped
     * in an {@code <arr>} element; single-valued fields become {@code <str>} elements.
     *
     * @param xmlDoc the owning DOM document
     * @param docEl the parent {@code <doc>} element
     * @param fieldName the Solr field name
     * @param value the field value (single object or Collection)
     */
    @SuppressWarnings("unchecked")
    private static void appendField(Document xmlDoc, Element docEl, String fieldName, Object value) {
        if (value instanceof Collection) {
            Element arrEl = xmlDoc.createElement("arr");
            arrEl.setAttribute("name", fieldName);
            for (Object item : (Collection<Object>) value) {
                Element strEl = xmlDoc.createElement("str");
                strEl.setTextContent(String.valueOf(item));
                arrEl.appendChild(strEl);
            }
            docEl.appendChild(arrEl);
        } else {
            Element strEl = xmlDoc.createElement("str");
            strEl.setAttribute("name", fieldName);
            strEl.setTextContent(String.valueOf(value));
            docEl.appendChild(strEl);
        }
    }
}
