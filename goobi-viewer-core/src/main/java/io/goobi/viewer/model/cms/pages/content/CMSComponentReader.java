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
package io.goobi.viewer.model.cms.pages.content;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.model.jsf.JsfComponent;

public class CMSComponentReader {

    private static final Logger logger = LogManager.getLogger(CMSComponentReader.class);

    public CMSComponent read(Path templateFile) throws IOException, JDOMException {

        Document templateDoc = XmlTools.readXmlFile(templateFile);

        String jsfComponentLibrary =
                XmlTools.evaluateToFirstElement("jsfComponent/library", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);
        String jsfComponentName =
                XmlTools.evaluateToFirstElement("jsfComponent/name", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);
        String label = XmlTools.evaluateToFirstElement("label", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);
        String desc = XmlTools.evaluateToFirstElement("description", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);
        String icon = XmlTools.evaluateToFirstElement("icon", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);

        String scopeString = XmlTools.evaluateToFirstElement("scope", templateDoc.getRootElement(), null).map(Element::getText).orElse(null);
        CMSComponentScope scope = CMSComponentScope.PAGEVIEW;
        if (StringUtils.isNotBlank(scopeString)) {
            try {
                scope = CMSComponentScope.valueOf(scopeString.toUpperCase());
            } catch (IllegalStateException e) {
                logger.error("Unable to set scope for component template {}: {} is not a known scope", templateFile.getFileName(), scopeString);
            }
        }

        List<Element> attributeElements = XmlTools.evaluateToElements("attribute", templateDoc.getRootElement(), null);
        Map<String, CMSComponentAttribute> attributes = new HashMap<>(attributeElements.size());
        for (Element element : attributeElements) {
            CMSComponentAttribute attr = CMSComponentAttribute.loadFromXML(element);
            attributes.put(attr.getName(), attr);
        }

        List<CMSComponent.Property> properties = XmlTools.evaluateToFirstString("properties", templateDoc.getRootElement(), null)
                .map(s -> s.split("[;,\\s]+"))
                .map(array -> Arrays.stream(array).map(CMSComponent.Property::getProperty).filter(p -> p != null).toList())
                .orElse(Collections.emptyList());

        String filename = FilenameUtils.getBaseName(templateFile.getFileName().toString());
        CMSComponent component =
                new CMSComponent(new JsfComponent(jsfComponentLibrary, jsfComponentName), label, desc, icon, filename, scope, attributes,
                        properties,
                        null);

        List<Element> contentElements = XmlTools.evaluateToElements("content/item", templateDoc.getRootElement(), null);

        for (Element element : contentElements) {

            String className = XmlTools.evaluateToFirstElement("className", element, null).map(Element::getText).orElse(null);
            try {
                String elementJsfComponentLibrary =
                        XmlTools.evaluateToFirstElement("jsfComponent/library", element, null).map(Element::getText).orElse(null);
                String elementJsfComponentName =
                        XmlTools.evaluateToFirstElement("jsfComponent/name", element, null).map(Element::getText).orElse(null);
                String elementLabel = XmlTools.evaluateToFirstElement("label", element, null).map(Element::getText).orElse(null);
                String elementDesc = XmlTools.evaluateToFirstElement("description", element, null).map(Element::getText).orElse(null);
                String htmlGroup = XmlTools.evaluateToFirstElement("htmlGroup", element, null).map(Element::getText).orElse(null);
                String componentId = element.getAttributeValue("id");
                String requiredString = element.getAttributeValue("required", "false");
                boolean required = !requiredString.equalsIgnoreCase("false");

                CMSContent content = createContentFromClassName(className);
                CMSContentItem item = new CMSContentItem(componentId, content, elementLabel, elementDesc, htmlGroup,
                        new JsfComponent(elementJsfComponentLibrary, elementJsfComponentName), component, required);

                component.addContentItem(item);
            } catch (InstantiationException e) {
                logger.error("Error instantiating CMSContent from class '{}'", className);
            }
        }

        return component;

    }

    private static CMSContent createContentFromClassName(String className)
            throws InstantiationException {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor();
            Object object = ctor.newInstance();
            if (object instanceof CMSContent) {
                return (CMSContent) object;
            }
            throw new InstantiationException("Class '" + className + "' is not of type 'CMSContent'");
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException | NullPointerException e) {
            throw new InstantiationException(e.toString());
        }
    }

}
