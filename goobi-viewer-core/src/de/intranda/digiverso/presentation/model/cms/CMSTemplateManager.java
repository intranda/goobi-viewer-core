/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

public final class CMSTemplateManager {

    private static final Logger logger = LoggerFactory.getLogger(CMSTemplateManager.class);

    private static final String TEMPLATE_BASE_PATH = "/cms/templates/";
    private static final String TEMPLATE_ICONS_PATH = "icons/";
    private static final String TEMPLATE_VIEWS_PATH = "views/";
    private static final String TEMPLATE_BASE_NAME = "template_base.xhtml";

    private static final boolean EXTERNAL_PATH = false;

    private static final Object lock = new Object();

    private static volatile CMSTemplateManager instance;

    private Map<String, CMSPageTemplate> templates;

    private String relativeTemplateBasePath;
    private String absoluteTemplateBasePath;

    public static CMSTemplateManager getInstance() {
        CMSTemplateManager ctm = instance;
        if (ctm == null) {
            synchronized (lock) {
                ctm = instance;
                if (ctm == null) {
                    try {
                        // TODO set web root path w/o calling JSF classes
                        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
                        String webContentRoot = servletContext.getRealPath("/");
                        ctm = new CMSTemplateManager(webContentRoot);
                        instance = ctm;
                    } catch (NullPointerException e) {
                        throw new IllegalStateException("Cannot access servlet context");
                    } catch (PresentationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return ctm;
    }

    public static CMSTemplateManager getInstance(String templateFolderPath) throws PresentationException {
        synchronized (lock) {
            instance = new CMSTemplateManager(templateFolderPath);
        }

        return instance;
    }

    private CMSTemplateManager(String templateFolderPath) throws PresentationException {
        if (templateFolderPath != null) {
            this.relativeTemplateBasePath = "/resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
            this.absoluteTemplateBasePath = templateFolderPath + this.relativeTemplateBasePath;

            File baseFolder = new File(this.absoluteTemplateBasePath);
            if (!baseFolder.exists()) {
                this.relativeTemplateBasePath = "/resources" + TEMPLATE_BASE_PATH;
                this.absoluteTemplateBasePath = templateFolderPath + this.relativeTemplateBasePath;
            }

            baseFolder = new File(this.absoluteTemplateBasePath);
            if (!baseFolder.exists()) {
                throw new PresentationException("Not cms template files found");
            }

        }
        updateTemplates();
    }

    // TODO fix for external themes
    private Map<String, CMSPageTemplate> loadTemplates() {
        Map<String, CMSPageTemplate> templates = new LinkedHashMap<>();
        File templateFolder = new File(this.absoluteTemplateBasePath);
        File[] files = templateFolder.listFiles(FileTools.filenameFilterXML);
        if (files == null) {
            logger.warn("No cms folder found in " + templateFolder.getAbsolutePath() + ". This theme is probably not configured to use cms");
            return templates;
        }
        Arrays.sort(files);
        // logger.trace(templateFolder.getAbsolutePath());
        for (File file : files) {
            // if (FilenameUtils.getExtension(file.getName()).equals("xml"))
            // {
            try {
                CMSPageTemplate template = CMSPageTemplate.loadFromXML(file);
                if (template != null) {
                    templates.put(template.getId(), template);
                } else {
                    throw new JDOMException("Cannot create cms template from file " + file.getAbsolutePath());
                }
            } catch (IOException | JDOMException e) {
                logger.warn("{} is not a template file", file.getAbsolutePath());
            }
            // }
        }
        return templates;
    }

    public void updateTemplates() {
        templates = loadTemplates();
    }

    public Collection<CMSPageTemplate> getTemplates() {
        return templates.values();
    }

    public CMSPageTemplate getTemplate(String id) {
        return templates.get(id);
    }

    private String getIconsPathRelative() {
        return relativeTemplateBasePath + TEMPLATE_ICONS_PATH;
    }

    private String getIconsPathJsfContext() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + getIconsPathRelative();
    }

    private String getViewsPathRelative() {
        return relativeTemplateBasePath + TEMPLATE_VIEWS_PATH;
    }

    private String getTemplateFileName(CMSPageTemplate template) {
        if (EXTERNAL_PATH) {
            return TEMPLATE_BASE_NAME;
        }
        return template.getHtmlFileName();
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateViewUrl(CMSPageTemplate template) {
        if (template != null) {
            StringBuilder sb = new StringBuilder(getViewsPathRelative());
            sb.append(getTemplateFileName(template));
            return sb.toString();
        }
        return "";
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateViewUrl(String templateId) {
        CMSPageTemplate template = getTemplate(templateId);
        return getTemplateViewUrl(template);
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateIconUrl(String templateId) {
        CMSPageTemplate template = getTemplate(templateId);
        if (template != null) {
            StringBuilder urlBuilder = new StringBuilder(getIconsPathJsfContext());
            urlBuilder.append(template.getIconFileName());
            return urlBuilder.toString();
        }
        return "";
    }

    public String getExternalTemplatePath(CMSPageTemplate template) {
        if (template != null) {
            StringBuilder pathBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getViewerHome());
            pathBuilder.append(DataManager.getInstance().getConfiguration().getCmsTemplateFolder());
            pathBuilder.append(TEMPLATE_VIEWS_PATH);
            pathBuilder.append(template.getHtmlFileName());
            return pathBuilder.toString();
        }
        return "";
    }

}
