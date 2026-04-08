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
package io.goobi.viewer.model.cms.legacy;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSComponentAttribute;
import io.goobi.viewer.model.cms.pages.content.CMSComponentScope;
import io.goobi.viewer.model.cms.pages.content.CMSContentItem;
import io.goobi.viewer.model.cms.pages.content.ContentItemMode;
import io.goobi.viewer.model.jsf.JsfComponent;

/**
 * Page templates are read from XML configuration files and are not stored in the DB.
 */
public class CMSPageTemplate implements Serializable {

    private static final String ICONS_PATH = "cms/templates/icons/";

    private static final String JSF_COMPONENT_PATH = "cms/templates/views";

    private static final long serialVersionUID = -4254711480254674992L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(CMSPageTemplate.class);

    private String id;

    private String name;

    private String version;

    private String description;

    private String htmlFileName;

    private String templateFileName;

    private String iconFileName;

    private boolean displaySortingField = false;

    private boolean appliesToExpandedUrl = true;

    private boolean mayHaveTopBarSlider = false;

    private List<CMSContentItemTemplate> contentItems = new ArrayList<>();

    private final Map<String, CMSComponentAttribute> attributes = new HashMap<>();

    private boolean themeTemplate = false;

    /**
     * Loads a page template from the given template file and returns the template object.
     *
     * @param file path to the XML template file to load
     * @return the loaded CMSPageTemplate, or null if the file could not be read or parsed
     * @throws java.lang.IllegalArgumentException if file is null
     * @should load template correctly
     * @should throw IllegalArgumentException if file is null
     */
    public static CMSPageTemplate loadFromXML(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }
        Document doc;
        try {
            doc = XmlTools.readXmlFile(file);
        } catch (IOException | JDOMException e1) {
            logger.error(e1.toString(), e1);
            return null;
        }
        if (doc == null) {
            return null;
        }

        Element root = doc.getRootElement();
        try {
            CMSPageTemplate template = new CMSPageTemplate();
            template.setTemplateFileName(file.getFileName().toString());
            template.setId(root.getAttributeValue("id"));
            template.setVersion(root.getAttributeValue("version"));
            template.setName(root.getChildText("name"));
            template.setDescription(root.getChildText("description"));
            template.setIconFileName(root.getChildText("icon"));
            template.setHtmlFileName(root.getChildText("html"));
            for (Element eleContentItem : root.getChild("content").getChildren("item")) {

                String itemId = eleContentItem.getAttributeValue("id");
                if ("preview01".equals(itemId)) {
                    continue; //preview texts are directly in cmsPage. They should not be loaded as content item
                }

                CMSContentItemType type = CMSContentItemType.getByName(eleContentItem.getAttributeValue("type"));
                CMSContentItemTemplate item = new CMSContentItemTemplate(type);
                item.setItemId(itemId);
                item.setItemLabel(eleContentItem.getAttributeValue("label"));
                item.setMandatory(Boolean.valueOf(eleContentItem.getAttributeValue("mandatory")));
                item.setMode(ContentItemMode.get(eleContentItem.getAttributeValue("mode")));
                item.setMediaFilter(eleContentItem.getAttributeValue("filter"));
                item.setInlineHelp(eleContentItem.getAttributeValue("inlinehelp"));
                item.setPreview(Boolean.parseBoolean(eleContentItem.getAttributeValue("preview")));
                item.setIgnoreCollectionHierarchy(Boolean.parseBoolean(eleContentItem.getAttributeValue("ignoreHierarchy")));
                item.setHitListOptions(Boolean.parseBoolean(eleContentItem.getAttributeValue("hitListOptions")));
                item.setRandomizeItems(Boolean.parseBoolean(eleContentItem.getAttributeValue("random")));

                if (eleContentItem.getAttribute("order") != null) {
                    try {
                        int order = Integer.parseInt(eleContentItem.getAttributeValue("order"));
                        item.setOrder(order);
                    } catch (NumberFormatException e) {
                        logger.error("Error parsing order attribute of cms template {}. Value is {}", file.getFileName(),
                                eleContentItem.getAttributeValue("order"));
                    }
                }
                template.getContentItems().add(item);
            }
            Collections.sort(template.getContentItems());
            Element options = root.getChild("options");
            if (options != null) {
                template.setDisplaySortingField(parseBoolean(options.getChildText("useSorterField")));
                template.setAppliesToExpandedUrl(parseBoolean(options.getChildText("appliesToExpandedUrl"), true));
                template.setMayHaveTopBarSlider(parseBoolean(options.getChildText("topBarSlider"), false));
            }
            Element attributes = root.getChild("attributes");
            if (attributes != null) {
                List<Element> attributeElements = attributes.getChildren("attribute");
                for (Element element : attributeElements) {
                    CMSComponentAttribute attr = CMSComponentAttribute.loadFromXML(element);
                    template.getAttributes().put(attr.getName(), attr);
                }
            }
            template.validate();
            return template;
        } catch (NullPointerException e) {
            logger.error("Could not parse CMS template file '{}', check document structure.", file.getFileName());
        }

        return null;
    }

    /**
     * validate.
     */
    public void validate() {
        if (StringUtils.isEmpty(id)) {
            logger.error("Template '{}' has no id.", templateFileName);
        }
        if (StringUtils.isEmpty(htmlFileName)) {
            logger.error("Template '{}' has no associated HTML file.", templateFileName);
        }
        if (StringUtils.isEmpty(version)) {
            logger.warn("Template '{}' has no version.", templateFileName);
        }
        if (StringUtils.isEmpty(name)) {
            logger.warn("Template '{}' has no name.", templateFileName);
        }
        if (StringUtils.isEmpty(description)) {
            logger.warn("Template '{}' has no description.", templateFileName);
        }
        if (StringUtils.isEmpty(iconFileName)) {
            logger.warn("Template '{}' has no associated icon file.", templateFileName);
        }
        if (contentItems.isEmpty()) {
            logger.warn("Template '{}' has no content items.", templateFileName);
        } else {
            for (int i = 0; i < contentItems.size(); ++i) {
                CMSContentItemTemplate item = contentItems.get(i);
                if (StringUtils.isEmpty(item.getItemId())) {
                    logger.warn("Item {} has no id.", i);
                }
                if (item.getType() == null) {
                    logger.warn("Item {} has no type.", i);
                }
            }
        }
    }

    public CMSComponent createCMSComponent() {
        String jsfLibraryPath = JSF_COMPONENT_PATH;
        Path componentPath = Paths.get(this.htmlFileName);
        boolean allItemsArePreview = contentItems.stream().allMatch(CMSContentItemTemplate::isPreview);
        CMSComponentScope scope = allItemsArePreview ? CMSComponentScope.PREVIEW : CMSComponentScope.PAGEVIEW;
        if (componentPath.getNameCount() > 1) {
            jsfLibraryPath = componentPath.getParent().toString();
        }
        JsfComponent jsfComponent = new JsfComponent(jsfLibraryPath, componentPath.getFileName().toString());
        CMSComponent component = new CMSComponent(jsfComponent, this.name, this.description, Collections.emptyList(),
                this.templateFileName, scope, this.attributes, null, null);
        for (CMSContentItemTemplate itemTemplate : contentItems) {
            CMSContentItem item = itemTemplate.createCMSContentItem(component);
            if (item != null) {
                component.addContentItem(item);
            }
        }

        return component;
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the unique identifier of this CMS page template
     */
    public String getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the template identifier to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>name</code>.
     *
     * @return the display name of this CMS page template
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the field <code>name</code>.
     *
     * @param name the display name of this page template
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the field <code>version</code>.
     *
     * @return the version string of this CMS page template
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter for the field <code>version</code>.
     *
     * @param version the version string of this page template
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Getter for the field <code>description</code>.
     *
     * @return the human-readable description of this CMS page template
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for the field <code>description</code>.
     *
     * @param description the human-readable description of this page template
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for the field <code>htmlFileName</code>.
     *
     * @return the name of the HTML file used to render pages of this template
     */
    public String getHtmlFileName() {
        //	return "template_base.xhtml";
        return htmlFileName;
    }

    /**
     * Setter for the field <code>htmlFileName</code>.
     *
     * @param htmlFileName the name of the HTML file used to render pages of this template
     */
    public void setHtmlFileName(String htmlFileName) {
        this.htmlFileName = htmlFileName;
    }

    /**
     * Getter for the field <code>templateFileName</code>.
     *
     * @return the file name of the template descriptor (e.g. XML file)
     */
    public String getTemplateFileName() {
        return templateFileName;
    }

    /**
     * Setter for the field <code>templateFileName</code>.
     *
     * @param templateFileName the file name of the template descriptor (e.g. XML file)
     */
    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    /**
     * Getter for the field <code>iconFileName</code>.
     *
     * @return the file name of the icon image representing this template
     */
    public String getIconFileName() {
        return iconFileName;
    }

    /**
     * Setter for the field <code>iconFileName</code>.
     *
     * @param iconFileName the file name of the icon image representing this template
     */
    public void setIconFileName(String iconFileName) {
        this.iconFileName = iconFileName;
    }

    /**
     * Getter for the field <code>contentItems</code>.
     *
     * @return the list of content item templates defined for this page template
     */
    public List<CMSContentItemTemplate> getContentItems() {
        return contentItems;
    }

    /**
     * getContentItem.
     *
     * @param itemId identifier of the content item to retrieve
     * @return the content item template with the given ID, or null if not found
     */
    public CMSContentItemTemplate getContentItem(String itemId) {
        for (CMSContentItemTemplate item : contentItems) {
            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }
        //return custom template for special items
        switch (itemId) {
            case CMSPage.TOPBAR_SLIDER_ID:
                return new CMSContentItemTemplate(CMSContentItemType.SLIDER);
            default:
                break;
        }
        return null;
    }

    /**
     * Setter for the field <code>contentItems</code>.
     *
     * @param contentItems the list of content item templates to assign to this page template
     */
    public void setContentItems(List<CMSContentItemTemplate> contentItems) {
        this.contentItems = contentItems;
    }

    /**
     * isDisplaySortingField.
     *
     * @return true if the sorting field should be displayed in the template UI, false otherwise
     */
    public boolean isDisplaySortingField() {
        return displaySortingField;
    }

    /**
     * Setter for the field <code>displaySortingField</code>.
     *
     * @param displaySortingField true to show the sorting field in the template UI
     */
    public void setDisplaySortingField(boolean displaySortingField) {
        this.displaySortingField = displaySortingField;
    }

    /**
     * isThemeTemplate.
     *
     * @return true if this template originates from the theme, false otherwise
     */
    public boolean isThemeTemplate() {
        return themeTemplate;
    }

    /**
     * Setter for the field <code>themeTemplate</code>.
     *
     * @param themeTemplate true if this template originates from the theme
     */
    public void setThemeTemplate(boolean themeTemplate) {
        this.themeTemplate = themeTemplate;
    }

    /**
     * Setter for the field <code>appliesToExpandedUrl</code>.
     *
     * @param appliesToExpandedUrl true if this template applies to expanded record URLs
     */
    public void setAppliesToExpandedUrl(boolean appliesToExpandedUrl) {
        this.appliesToExpandedUrl = appliesToExpandedUrl;
    }

    /**
     * isAppliesToExpandedUrl.
     *
     * @return true if this template applies to expanded record URLs, false otherwise
     */
    public boolean isAppliesToExpandedUrl() {
        return appliesToExpandedUrl;
    }

    
    public void setMayHaveTopBarSlider(boolean mayHaveTopBarSlider) {
        this.mayHaveTopBarSlider = mayHaveTopBarSlider;
    }

    
    public boolean isMayHaveTopBarSlider() {
        return mayHaveTopBarSlider;
    }

    /**
     * parseBoolean.
     *
     * @param text string value to parse, expected "true" or "false" (case-insensitive)
     * @param defaultValue value to return when text is neither "true" nor "false"
     * @return true if text equals "true" (case-insensitive), false if it equals "false", or defaultValue if it matches neither
     */
    public static boolean parseBoolean(String text, boolean defaultValue) {
        if ("FALSE".equalsIgnoreCase(text)) {
            return false;
        } else if ("TRUE".equalsIgnoreCase(text)) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * parseBoolean.
     *
     * @param text string value to parse, defaults to false if unrecognized
     * @return true if text equals "true" (case-insensitive), false otherwise
     */
    public static boolean parseBoolean(String text) {
        return parseBoolean(text, false);
    }

    public Map<String, CMSComponentAttribute> getAttributes() {
        return attributes;
    }

}
