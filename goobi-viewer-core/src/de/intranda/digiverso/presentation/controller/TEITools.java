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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

public class TEITools {

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

        Document doc = FileTools.getDocumentFromString(tei, Helper.DEFAULT_ENCODING);
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
                return FileTools.getStringFromElement(eleNewRoot, null).replace("<tempRoot>", "").replace("</tempRoot>", "").trim();
            }
        }

        return null;
    }
}
