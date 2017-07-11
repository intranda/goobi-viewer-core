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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

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

    //    private String relativeTemplateBasePath;
    //    private String absoluteTemplateBasePath;

    private String templateFolderUrl = null;
    private Path templateFolderPath = null;

    public static CMSTemplateManager getInstance() {
        //                instance = null;
        CMSTemplateManager ctm = instance;
        if (ctm == null) {
            synchronized (lock) {
                ctm = instance;
                if (ctm == null) {
                    try {

                        ctm = new CMSTemplateManager(null);
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

    private CMSTemplateManager(String filesystemPath) throws PresentationException {
        ServletContext servletContext = null;
        String webContentRoot = "";
        if(filesystemPath == null && FacesContext.getCurrentInstance() != null) {         
            servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            webContentRoot = servletContext.getContextPath();
        }
        List<Path> templateFiles = new ArrayList<>();
        //        if (filesystemPath == null) {

        try {

            this.templateFolderUrl = "resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
            URL fileUrl;
            if(servletContext != null) {                
                fileUrl = servletContext.getResource(this.templateFolderUrl);
            } else {
                fileUrl = new URL(filesystemPath + this.templateFolderUrl);
            }
            if (fileUrl != null) {
                try {
                    Map<String, String> env = new HashMap<>();
                    env.put("create", "true");
                    FileSystem zipfs = FileSystems.newFileSystem(fileUrl.toURI(), env);
                } catch (FileSystemAlreadyExistsException | IllegalArgumentException e) {
                    //no comment...
                }
                if (Files.exists(Paths.get(fileUrl.toURI()))) {
                    this.templateFolderPath = Paths.get(fileUrl.toURI());
                    templateFiles = Files.list(templateFolderPath).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).peek(
                            (file) -> logger.trace("Found cms template file " + file)).collect(Collectors.toList());
                }
            }
        } catch (URISyntaxException | IOException e) {
            logger.error(e.toString(), e);
        }

        if (templateFiles.isEmpty()) {
            try {
                this.templateFolderUrl = "resources/" + TEMPLATE_BASE_PATH;
                URL fileUrl;
                if(servletContext != null) {                
                    fileUrl = servletContext.getResource(this.templateFolderUrl);
                } else {
                    fileUrl = new URL(filesystemPath + this.templateFolderUrl);
                }
                if (fileUrl != null) {
                    try {
                        Map<String, String> env = new HashMap<>();
                        env.put("create", "true");
                        FileSystem zipfs = FileSystems.newFileSystem(fileUrl.toURI(), env);
                    } catch (FileSystemAlreadyExistsException | IllegalArgumentException e) {
                        //no comment...
                    }
                    if (Files.exists(Paths.get(fileUrl.toURI()))) {
                        this.templateFolderPath = Paths.get(fileUrl.toURI());
                        templateFiles = Files.list(templateFolderPath).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml"))
                                .peek((file) -> logger.trace("Found cms template file " + file)).collect(Collectors.toList());
                    }
                }
            } catch (URISyntaxException | IOException e) {
                logger.error(e.toString(), e);
            }
        }

        //        } else {
        //            this.templateFolderPath = Paths.get(filesystemPath, TEMPLATE_BASE_PATH);
        //            try {
        //                this.templateFolderUrl = templateFolderPath.toUri().toURL().toString();
        //                if (Files.exists(templateFolderPath)) {
        //                    templateFiles = Files.list(templateFolderPath).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).peek(
        //                            (file) -> logger.trace("Found cms template file " + file)).collect(Collectors.toList());
        //                }
        //            } catch (IOException e) {
        //                logger.error(e.toString(), e);
        //            }
        //        }

        this.templateFolderUrl = webContentRoot + "/" + this.templateFolderUrl;

        //            this.relativeTemplateBasePath = "/resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
        //            this.absoluteTemplateBasePath = templateFolderPath + this.relativeTemplateBasePath;
        //
        //            File baseFolder = new File(this.absoluteTemplateBasePath);
        //            if (!baseFolder.exists()) {
        //                this.relativeTemplateBasePath = "/resources" + TEMPLATE_BASE_PATH;
        //                this.absoluteTemplateBasePath = templateFolderPath + this.relativeTemplateBasePath;
        //            }
        //
        //            baseFolder = new File(this.absoluteTemplateBasePath);
        //            if (!baseFolder.exists()) {
        //                throw new PresentationException("Not cms template files found");
        //            }

        //        }
        updateTemplates();
    }

    // TODO fix for external themes
    private Map<String, CMSPageTemplate> loadTemplates() throws IOException {
        Map<String, CMSPageTemplate> templates = new LinkedHashMap<>();

        List<CMSPageTemplate> templateList = Files.list(this.templateFolderPath).filter(
                file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).sorted().map(
                        templatePath -> CMSPageTemplate.loadFromXML(templatePath)).filter(template -> template != null).collect(Collectors.toList());

        if (templateList == null) {
            logger.warn("No cms folder found in " + this.templateFolderPath + ". This theme is probably not configured to use cms");
            return templates;
        }
        // logger.trace(templateFolder.getAbsolutePath());
        for (CMSPageTemplate template : templateList) {
            templates.put(template.getId(), template);
        }
        return templates;
    }

    public void updateTemplates() {
        try {
            templates = loadTemplates();
        } catch (IOException e) {
            logger.error("Failed to load templates", e);
        }
    }

    public Collection<CMSPageTemplate> getTemplates() {
        return templates.values();
    }

    public CMSPageTemplate getTemplate(String id) {
        return templates.get(id);
    }

    private String getIconFolderUrl() {
        return getTemplateFolderUrl() + TEMPLATE_ICONS_PATH;
    }

    /**
     * @return
     */
    private String getTemplateFolderUrl() {
        return this.templateFolderUrl;
    }

    private String getViewFolderUrl() {
        return getTemplateFolderUrl() + TEMPLATE_VIEWS_PATH;
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateViewUrl(CMSPageTemplate template) {
        if (template != null) {
            return getViewFolderUrl() + "/" + template.getHtmlFileName();
            //            Path iconPath = Paths.get(getViewFolderUrl().toString(), template.getHtmlFileName());
            //            return iconPath.toUri().toString();
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
            Path iconPath = Paths.get(getIconFolderUrl(), template.getIconFileName());
            return getIconFolderUrl() + template.getIconFileName();
            //            StringBuilder urlBuilder = new StringBuilder(getIconsPathJsfContext());
            //            urlBuilder.append(template.getIconFileName());
            //            return urlBuilder.toString();
        }
        return "";
    }

}
