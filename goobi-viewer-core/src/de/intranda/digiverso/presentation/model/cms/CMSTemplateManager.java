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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ddf.EscherColorRef.SysIndexSource;
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

    //    private String templateFolderUrl = null;
    //    private Path templateFolderPath = null;

    private Optional<String> coreTemplateFolderUrl = Optional.empty();
    private Optional<String> themeTemplateFolderUrl = Optional.empty();
    private Optional<Path> coreFolderPath = Optional.empty();
    private Optional<Path> themeFolderPath = Optional.empty();

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
        if (filesystemPath == null && FacesContext.getCurrentInstance() != null) {
            servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            webContentRoot = servletContext.getContextPath();
        }

        try {
            String themeFolder = "resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
            Optional<URL> themeFolderUrl = getThemeFolderUrl(filesystemPath, servletContext, themeFolder);
            themeFolderPath = themeFolderUrl.map(url -> toURI(url));

            //check if the coreFolderPath contains any xml files
            boolean templatesFound = false;
            if (themeFolderPath.isPresent()) {
                templatesFound = Files.list(themeFolderPath.get()).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).peek(
                        file -> logger.trace("Found cms theme template file " + file)).findAny().isPresent();
            }
            if (templatesFound) {
                this.themeTemplateFolderUrl = Optional.of(webContentRoot + "/" + themeFolder);
            }
        } catch (URISyntaxException | IOException e) {
            logger.error(e.toString(), e);
        } catch (FileSystemNotFoundException | ProviderNotFoundException e) {
            logger.debug("Unable to scan theme-jar for cms-template files. Probably an older tomcat");
        }

        try {
            String templateFolderUrl = "resources/" + TEMPLATE_BASE_PATH;
            Optional<URL> coreFolderUrl = getTemplateFolderUrl(filesystemPath, servletContext, templateFolderUrl);
            coreFolderPath = coreFolderUrl.map(path -> toURI(path));
            System.out.println("Core folder template path = " + coreFolderPath);
            //check if the themeFolderPath contains any xml files
            boolean templatesFound = false;
            if (coreFolderPath.isPresent()) {
                templatesFound = Files.list(coreFolderPath.get()).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).peek(
                        file -> logger.trace("Found core cms template file " + file)).findAny().isPresent();
            }
            if (templatesFound) {
                this.coreTemplateFolderUrl = Optional.of(webContentRoot + "/" + templateFolderUrl);
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }

        updateTemplates(themeFolderPath, coreFolderPath);
    }

    /**
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public Path toURI(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @param filesystemPath
     * @param servletContext
     * @param templateFolderUrl
     * @return
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException 
     */
    public Optional<URL> getTemplateFolderUrl(String filesystemPath, ServletContext servletContext, String templateFolderUrl)
            throws MalformedURLException, UnsupportedEncodingException {
        Optional<URL> fileUrl = Optional.empty();
        Path path = Paths.get(URLDecoder.decode(new URL(filesystemPath + templateFolderUrl).getPath(), "utf-8"));
        System.out.println("path = " + path);
        if (servletContext != null) {
            String basePath = servletContext.getRealPath("/");
            if (Files.exists(Paths.get(basePath, templateFolderUrl))) {
                fileUrl = Optional.of(Paths.get(basePath, templateFolderUrl).toFile().toURI().toURL());
            }
            //                    fileUrl = servletContext.getResource(this.templateFolderUrl);
        } else if (Files.exists(path)) {
            fileUrl = Optional.of(new URL(filesystemPath + templateFolderUrl));
        }
        return fileUrl;
    }

    /**
     * Returns an url pointing to the cms template folder of the viewer theme.
     * 
     * @param filesystemPath
     * @param servletContext
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public Optional<URL> getThemeFolderUrl(String filesystemPath, ServletContext servletContext, String coreFolder) throws IOException,
            URISyntaxException {
        Optional<URL> coreFolderUrl = Optional.empty();
        if (servletContext != null) {
            coreFolderUrl = Optional.ofNullable(servletContext.getResource(coreFolder));
        } else {
            Path path = Paths.get(filesystemPath + coreFolder);
            if (Files.isDirectory(path)) {
                coreFolderUrl = Optional.of(path.toUri().toURL());
            }
        }
        //create new file system if neccessary
        if (coreFolderUrl.isPresent()) {
            try {
                FileSystems.newFileSystem(coreFolderUrl.get().toURI(), Collections.singletonMap("create", "true"));
            } catch (FileSystemAlreadyExistsException | IllegalArgumentException e) {
                //no comment...
            }
        }
        return coreFolderUrl;
    }

    private Map<String, CMSPageTemplate> loadTemplates(Path path) throws IllegalArgumentException {
        Map<String, CMSPageTemplate> templates = new LinkedHashMap<>();
        System.out.println("Loading templates from " + path);
        List<CMSPageTemplate> templateList = null;
        ;
        try {
            templateList = Files.list(path).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).sorted().peek(
                    templatePath -> System.out.println("Loading template from " + templatePath)).map(templatePath -> CMSPageTemplate.loadFromXML(
                            templatePath)).peek(template -> System.out.println("Loaded template " + template.getId())).filter(
                                    template -> template != null).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading files from " + path, e);
        }

        if (templateList == null) {
            logger.warn("No cms folder found in " + path + ". This theme is probably not configured to use cms");
            return templates;
        }
        // logger.trace(templateFolder.getAbsolutePath());
        for (CMSPageTemplate template : templateList) {
            if (templates.get(template.getId()) != null) {
                throw new IllegalArgumentException("Found two templates with id " + template.getId());
            }
            templates.put(template.getId(), template);
        }
        return templates;
    }

    public void updateTemplates(Optional<Path>... templatePaths) {
        templates = new HashMap<>();
        try {
            for (Optional<Path> oPath : templatePaths) {
                oPath.map(path -> loadTemplates(path)).ifPresent(map -> {
                    map.entrySet().stream().forEach(entry -> templates.putIfAbsent(entry.getKey(), entry.getValue()));
                });
            }
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update cms templates: " + e.toString(), e);
        }
    }

    public Collection<CMSPageTemplate> getTemplates() {
        return templates.values();
    }

    public CMSPageTemplate getTemplate(String id) {
        return templates.get(id);
    }

    private Optional<String> getCoreIconFolderUrl() {
        return getCoreTemplateFolderUrl().map(url -> url + TEMPLATE_ICONS_PATH);
    }

    private Optional<String> getThemeIconFolderUrl() {
        return getThemeTemplateFolderUrl().map(url -> url + TEMPLATE_ICONS_PATH);
    }

    /**
     * @return the url path to the core viewer cms template url if it exists and contains files
     */
    private Optional<String> getCoreTemplateFolderUrl() {
        return this.coreTemplateFolderUrl;
    }

    private Optional<String> getThemeTemplateFolderUrl() {
        return this.themeTemplateFolderUrl;
    }

    private Optional<String> getCoreViewFolderUrl() {
        return getCoreTemplateFolderUrl().map(url -> url + TEMPLATE_VIEWS_PATH);
    }

    private Optional<String> getThemeViewFolderUrl() {
        return getThemeTemplateFolderUrl().map(url -> url + TEMPLATE_VIEWS_PATH);
    }

    /**
     * @return the themeFolderPath
     */
    public Optional<Path> getThemeFolderPath() {
        return themeFolderPath;
    }

    /**
     * @return the coreFolderPath
     */
    public Optional<Path> getCoreFolderPath() {
        return coreFolderPath;
    }

    public Optional<Path> getCoreViewFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    public Optional<Path> getThemeViewFolderPath() {
        return getThemeFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    public Optional<Path> getCoreIconFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_ICONS_PATH));
    }

    public Optional<Path> getThemeIconFolderPath() {
        return getThemeFolderPath().map(path -> path.resolve(TEMPLATE_ICONS_PATH));
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateViewUrl(CMSPageTemplate template) {
        if (template != null) {
            Optional<String> viewUrl = getThemeViewFolderUrl().filter(url -> getThemeViewFolderPath().isPresent() && Files.exists(
                    getThemeViewFolderPath().get().resolve(template.getHtmlFileName()))).map(url -> url + "/" + template.getHtmlFileName());
            if (!viewUrl.isPresent()) {
                viewUrl = getCoreViewFolderUrl().filter(url -> getCoreViewFolderPath().isPresent() && Files.exists(getCoreViewFolderPath().get()
                        .resolve(template.getHtmlFileName()))).map(url -> url + "/" + template.getHtmlFileName());
            }
            return viewUrl.orElse("");
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
        if (StringUtils.isNotBlank(templateId)) {
            CMSPageTemplate template = getTemplate(templateId);
            return getTemplateIconUrl(template);
        } else {
            return "";
        }
    }

    /**
     * @param templateId
     * @return
     */
    public String getTemplateIconUrl(CMSPageTemplate template) {
        if (template != null) {
            Optional<String> viewUrl = getThemeIconFolderUrl().filter(url -> getThemeIconFolderPath().isPresent() && Files.exists(
                    getThemeIconFolderPath().get().resolve(template.getIconFileName()))).map(url -> url + "/" + template.getIconFileName());
            if (!viewUrl.isPresent()) {
                viewUrl = getCoreIconFolderUrl().filter(url -> getCoreIconFolderPath().isPresent() && Files.exists(getCoreIconFolderPath().get()
                        .resolve(template.getIconFileName()))).map(url -> url + "/" + template.getIconFileName());
            }
            return viewUrl.orElse("");
        }
        return "";
    }

}
