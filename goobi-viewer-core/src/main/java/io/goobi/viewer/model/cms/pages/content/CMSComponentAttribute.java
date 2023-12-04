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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import io.goobi.viewer.controller.XmlTools;

public class CMSComponentAttribute implements Serializable {

    private static final long serialVersionUID = -1054428745597079708L;

    private final String name;
    private final String label;
    private final String type;
    private final boolean display;
    private final boolean booleanValue;
    private final List<Option> options;
    private final String value;

    public CMSComponentAttribute(CMSComponentAttribute orig, String value) {
        this(orig.name, orig.label, orig.type, orig.display, orig.booleanValue, orig.options, value);
    }

    public CMSComponentAttribute(String name, String label, String type, boolean display, boolean booleanValue, List<Option> options, String value) {
        super();
        this.name = name;
        this.label = label;
        this.type = type;
        this.options = options;
        this.value = value;
        this.display = display;
        this.booleanValue = booleanValue;
    }

    public CMSComponentAttribute(CMSComponentAttribute orig) {
        this.name = orig.name;
        this.label = orig.label;
        this.type = orig.type;
        this.display = orig.display;
        this.options = new ArrayList<>(orig.options);
        this.value = orig.value;
        this.booleanValue = orig.booleanValue;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the options
     */
    public List<Option> getOptions() {
        return options;
    }

    public boolean getBooleanValue() {
        return Boolean.parseBoolean(this.value);
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }
    
    public static CMSComponentAttribute loadFromXML(Element element) {
        String attrName = element.getAttributeValue("name");
        String attrLabel = element.getAttributeValue("label");
        String attrType = element.getAttributeValue("type");
        boolean bool = Optional.ofNullable(element.getAttributeValue("boolean")).map(Boolean::parseBoolean).orElse(false);
        boolean display = Optional.ofNullable(element.getAttributeValue("display")).map(Boolean::parseBoolean).orElse(true);
        List<Option> attrOptions = XmlTools.evaluateToElements("value", element, null).stream().map(CMSComponentAttribute::createOption).collect(Collectors.toList());
        String value = XmlTools.evaluateToFirstElement("value[@default='true']", element, null).map(Element::getText).orElse("");
        if (!display && StringUtils.isBlank(value)) {
            value = attrOptions.iterator().next().getValue();
        }
        return new CMSComponentAttribute(attrName, attrLabel, attrType, display, bool, attrOptions, value);
    }
    
    private static Option createOption(Element element) {
        String label = element.getAttributeValue("label");
        String value = element.getText();
        if (StringUtils.isBlank(label)) {
            label = value;
        }
        return new Option(value, label);
    }


}
