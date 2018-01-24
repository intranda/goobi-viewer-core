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
package de.intranda.digiverso.presentation.model.cms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.managedbeans.CmsBean;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;
import de.intranda.digiverso.presentation.model.cms.CMSPageLanguageVersion.CMSPageStatus;

/**
 * Page templates are read from XML configuration files and are not stored in the DB.
 */
public class CMSPageTemplate {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(CMSPageTemplate.class);

    private String id;

    private String name;

    private String version;

    private String description;

    private String htmlFileName;

    private String templateFileName;

    private String iconFileName;

    private boolean displaySortingField = false;
    
    private boolean appliesToExpandedUrl = false;

    private List<CMSContentItemTemplate> contentItems = new ArrayList<>();
    
    private boolean themeTemplate = false;

    /**
     * Loads a page template from the given template file and returns the template object.
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JDOMException
     * @throws IllegalArgumentException if file is null
     * @should load template correctly
     * @should throw IllegalArgumentException if file is null
     */
    public static CMSPageTemplate loadFromXML(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }
        Document doc;
        try {
            doc = FileTools.readXmlFile(file);
        } catch (IOException | JDOMException e1) {
           logger.error(e1.toString(), e1);
           return null;
        }
        if (doc != null) {
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
                    CMSContentItemType type = CMSContentItemType.getByName(eleContentItem.getAttributeValue("type"));
                    CMSContentItemTemplate item = new CMSContentItemTemplate(type);
                    item.setItemId(eleContentItem.getAttributeValue("id"));
                    item.setItemLabel(eleContentItem.getAttributeValue("label"));
                    item.setMandatory(Boolean.valueOf(eleContentItem.getAttributeValue("mandatory")));
                    item.setMode(ContentItemMode.get(eleContentItem.getAttributeValue("mode")));
                    if (eleContentItem.getAttribute("order") != null) {
                        try {
                            int order = Integer.parseInt(eleContentItem.getAttributeValue("order"));
                            item.setOrder(order);
                        } catch (NumberFormatException e) {
                            logger.error("Error parsing order attribute of cms template {}. Value is {}", file.getFileName(), eleContentItem
                                    .getAttributeValue("order"));
                        }
                    }
                    template.getContentItems().add(item);
                }
                Collections.sort(template.getContentItems());
                Element options = root.getChild("options");
                if (options != null) {
                    template.setDisplaySortingField(Boolean.parseBoolean(options.getChildText("useSorterField")));
                    template.setAppliesToExpandedUrl(Boolean.parseBoolean(options.getChildText("appliesToExpandedUrl")));
                }
                template.validate();
                return template;
            } catch (NullPointerException e) {
                logger.error("Could not parse CMS template file '{}', check document structure.", file.getFileName());
            }
        }

        return null;
    }

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
                CMSContentItem item = contentItems.get(i);
                if (StringUtils.isEmpty(item.getItemId())) {
                    logger.warn("Item {} has no id.", i);
                }
                if (item.getType() == null) {
                    logger.warn("Item {} has no type.", i);
                }
            }
        }
    }

    /**
     * Returns a page instance based on this template's configuration.
     *
     * @param locales List of available locales.
     * @return New <code>CMSPage</code> instance of this template configuration.
     * @should create page instance correctly
     */
    public CMSPage createNewPage(List<Locale> locales) {
        CMSPage page = new CMSPage();
        page.setTemplateId(id);
        page.setPublished(false);
        for (Locale locale : locales) {
            CMSPageLanguageVersion langVersion = createNewLanguageVersion(page, locale.getLanguage());
            page.getLanguageVersions().add(langVersion);
            if(locale.equals(CmsBean.getDefaultLocaleStatic())) {
            	langVersion.setStatus(CMSPageStatus.FINISHED);
            }
        }
        //add global language for language independent items
        CMSPageLanguageVersion globalLanguageVersion = new CMSPageLanguageVersion();
        globalLanguageVersion.setOwnerPage(page);
        globalLanguageVersion.setLanguage(CMSPage.GLOBAL_LANGUAGE);
        globalLanguageVersion.setStatus(CMSPageStatus.WIP);
        page.getLanguageVersions().add(globalLanguageVersion);

        for (CMSContentItem item : contentItems) {
            if (item.getType() == CMSContentItemType.HTML || item.getType() == CMSContentItemType.TEXT) {
                for (CMSPageLanguageVersion langVersion : page.getLanguageVersions()) {
                    if (langVersion.getLanguage() != CMSPage.GLOBAL_LANGUAGE) {
                        CMSContentItem actualItem = item.clone();
                        actualItem.setOwnerPageLanguageVersion(langVersion);
                        langVersion.getContentItems().add(actualItem);
                    }
                }
            } else {
                CMSContentItem actualItem = item.clone();
                actualItem.setOwnerPageLanguageVersion(globalLanguageVersion);
                globalLanguageVersion.getContentItems().add(actualItem);
            }
        }

        return page;
    }

    public CMSPageLanguageVersion createNewLanguageVersion(CMSPage page, String language) {
        CMSPageLanguageVersion langVersion = new CMSPageLanguageVersion();
        langVersion.setOwnerPage(page);
        langVersion.setLanguage(language);
        langVersion.setStatus(CMSPageStatus.WIP);
        return langVersion;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the htmlFileName
     */
    public String getHtmlFileName() {
        //	return "template_base.xhtml";
        return htmlFileName;
    }

    /**
     * @param htmlFileName the htmlFileName to set
     */
    public void setHtmlFileName(String htmlFileName) {
        this.htmlFileName = htmlFileName;
    }

    /**
     * @return the templateFileName
     */
    public String getTemplateFileName() {
        return templateFileName;
    }

    /**
     * @param templateFileName the templateFileName to set
     */
    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    /**
     * @return the iconFileName
     */
    public String getIconFileName() {
        return iconFileName;
    }

    /**
     * @param iconFileName the iconFileName to set
     */
    public void setIconFileName(String iconFileName) {
        this.iconFileName = iconFileName;
    }

    /**
     * @return the contentItems
     */
    public List<CMSContentItemTemplate> getContentItems() {
        return contentItems;
    }

    public CMSContentItem getContentItem(String itemId) {
        for (CMSContentItem item : contentItems) {
            if(item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * @param contentItems the contentItems to set
     */
    public void setContentItems(List<CMSContentItemTemplate> contentItems) {
        this.contentItems = contentItems;
    }

    public boolean isDisplaySortingField() {
        return displaySortingField;
    }

    public void setDisplaySortingField(boolean displaySortingField) {
        this.displaySortingField = displaySortingField;
    }
    
    /**
     * @return the themeTemplate
     */
    public boolean isThemeTemplate() {
        return themeTemplate;
    }
    
    /**
     * @param themeTemplate the themeTemplate to set
     */
    public void setThemeTemplate(boolean themeTemplate) {
        this.themeTemplate = themeTemplate;
    }

    /**
     * @param appliesToExpandedUrl the appliesToExpandedUrl to set
     */
    public void setAppliesToExpandedUrl(boolean appliesToExpandedUrl) {
        this.appliesToExpandedUrl = appliesToExpandedUrl;
    }
    
    /**
     * @return the appliesToExpandedUrl
     */
    public boolean isAppliesToExpandedUrl() {
        return appliesToExpandedUrl;
    }


}
