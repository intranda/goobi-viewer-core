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

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class XmlTools {

    /**
     * Evaluates the given XPath expression to a list of elements.
     * 
     * @param expr XPath expression to evaluate.
     * @param parent If not null, the expression is evaluated relative to this element.
     * @param namespaces
     * @return {@link ArrayList} or null
     * @should return all values
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
     * @param namespaces
     * @return
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
}
