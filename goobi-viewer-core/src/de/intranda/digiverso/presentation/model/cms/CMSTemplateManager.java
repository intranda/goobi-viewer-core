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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

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
                        ctm = new CMSTemplateManager(null, DataManager.getInstance().getConfiguration().getThemeRootPath());
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

    public static CMSTemplateManager getInstance(String templateFolderPath, String themeRootPath) throws PresentationException {
        synchronized (lock) {
            instance = new CMSTemplateManager(templateFolderPath, themeRootPath);
        }

        return instance;
    }

    /**
     * 
     * @param filesystemPath
     * @param themeRootPath If the theme contents are in an external folder, its root path must be provided here
     * @throws PresentationException
     */
    private CMSTemplateManager(String filesystemPath, String themeRootPath) throws PresentationException {
        ServletContext servletContext = null;
        String webContentRoot = "";
        if (filesystemPath == null && FacesContext.getCurrentInstance() != null) {
            servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            webContentRoot = servletContext.getContextPath();
        } else if (filesystemPath == null) {
            throw new PresentationException("No faces context found");
        }

        //check if the themeFolderPath contains any xml files
        try {
            boolean absolutetemplateFolderUrl = false;
            String templateFolderUrl = "resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
            if (StringUtils.isNotEmpty(themeRootPath)) {
                if (!themeRootPath.endsWith("/")) {
                    themeRootPath += '/';
                }
                templateFolderUrl = themeRootPath + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
                absolutetemplateFolderUrl = true;
            }
            Optional<URL> themeFolderUrl = getThemeFolderUrl(filesystemPath, servletContext, templateFolderUrl, absolutetemplateFolderUrl);
            themeFolderPath = themeFolderUrl.map(url -> toURI(url));
            boolean templatesFound = false;
            if (themeFolderPath.isPresent()) {
                try (Stream<java.nio.file.Path> templateFiles = Files.list(themeFolderPath.get())) {
                    templatesFound = templateFiles.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml")).findAny().isPresent();
                }
            }
            if (templatesFound) {
                this.themeTemplateFolderUrl = Optional.of(webContentRoot + "/" + templateFolderUrl);
            }
        } catch (URISyntaxException | IOException e) {
            logger.error(e.toString(), e);
        } catch (FileSystemNotFoundException | ProviderNotFoundException e) {
            logger.debug("Unable to scan theme-jar for cms-template files. Probably an older tomcat");
        }

        //check if the coreFolderPath contains any xml files
        try {
            String templateFolderUrl = "resources" + TEMPLATE_BASE_PATH;
            Optional<URL> coreFolderUrl = getCoreTemplateFolderUrl(filesystemPath, servletContext, templateFolderUrl);
            coreFolderPath = coreFolderUrl.map(path -> toURI(path));
            boolean templatesFound = false;
            if (coreFolderPath.isPresent()) {
                logger.trace("coreFolderPath: {}", coreFolderPath.get());
                try (Stream<java.nio.file.Path> templateFiles = Files.list(coreFolderPath.get())) {
                    templatesFound = templateFiles.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml"))
                            .peek(file -> logger.trace("Found core cms template file " + file))
                            .findAny()
                            .isPresent();
                }
            } else {
                logger.warn("coreFolderPath not found at {}, {} servletContent null ? {}", filesystemPath, templateFolderUrl, servletContext == null);
            }
            if (templatesFound) {
                this.coreTemplateFolderUrl = Optional.of(webContentRoot + "/" + templateFolderUrl);
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }

        updateTemplates(coreFolderPath, themeFolderPath);
    }

    /**
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public static Path toURI(URL url) {
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
    public static Optional<URL> getCoreTemplateFolderUrl(String filesystemPath, ServletContext servletContext, String templateFolderUrl)
            throws MalformedURLException, UnsupportedEncodingException {
        Optional<URL> fileUrl = Optional.empty();
        if (servletContext != null) {
            String basePath = servletContext.getRealPath("/");
            logger.trace("basePath: {}", basePath);
            Path path = Paths.get(basePath, templateFolderUrl);
            if (Files.exists(path)) {
                fileUrl = Optional.of(path.toFile().toURI().toURL());
            } else {
                logger.warn("Template folder path not found: {}", path.toAbsolutePath().toString());
            }
            //                    fileUrl = servletContext.getResource(this.templateFolderUrl);
        } else if (filesystemPath != null) {
            Path path = Paths.get(URLDecoder.decode(new URL(filesystemPath + templateFolderUrl).getPath(), "utf-8"));
            if (Files.exists(path)) {
                fileUrl = Optional.of(new URL(filesystemPath + templateFolderUrl));
            } else {
                logger.warn("Template folder path not found: {}", path.toAbsolutePath().toString());
            }
        }
        return fileUrl;
    }

    /**
     * Returns an url pointing to the cms template folder of the viewer theme.
     * 
     * @param filesystemPath
     * @param servletContext
     * @param templateFolderUrl
     * @param absolutetemplateFolderUrl
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private static Optional<URL> getThemeFolderUrl(String filesystemPath, ServletContext servletContext, String templateFolderUrl,
            boolean absolutetemplateFolderUrl) throws IOException, URISyntaxException {
        Optional<URL> coreFolderUrl = Optional.empty();
        if (absolutetemplateFolderUrl) {
            Path path = Paths.get(templateFolderUrl);
            logger.debug("Looking for external theme template folder in {}", path.toAbsolutePath().toString());
            if (Files.isDirectory(path)) {
                coreFolderUrl = Optional.of(path.toUri().toURL());
            }
        } else if (servletContext != null) {
            coreFolderUrl = Optional.ofNullable(servletContext.getResource(templateFolderUrl));
        } else {
            Path path = Paths.get(filesystemPath + templateFolderUrl);
            if (Files.isDirectory(path)) {
                coreFolderUrl = Optional.of(path.toUri().toURL());
            }
        }
        // create new file system if necessary
        if (coreFolderUrl.isPresent()) {
            try {
                FileSystems.newFileSystem(coreFolderUrl.get().toURI(), Collections.singletonMap("create", "true"));
            } catch (FileSystemAlreadyExistsException | IllegalArgumentException e) {
                //no comment...
            }
        }
        return coreFolderUrl;
    }

    private static Map<String, CMSPageTemplate> loadTemplates(Path path) throws IllegalArgumentException {
        Map<String, CMSPageTemplate> templates = new LinkedHashMap<>();
        List<CMSPageTemplate> templateList = null;
        try {
            try (Stream<java.nio.file.Path> templateFiles = Files.list(path)) {
                templateList = templateFiles.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml"))
                        .sorted()
                        .map(templatePath -> CMSPageTemplate.loadFromXML(templatePath))
                        .filter(template -> template != null)
                        .collect(Collectors.toList());
            }
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

    public void updateTemplates(Optional<Path> corePath, Optional<Path> themePath) {
        templates = new HashMap<>();
        //        logger.trace("themePath: {}", themePath.orElse(Paths.get("none")).toAbsolutePath().toString());
        try {
            //load theme templates
            if (themePath.isPresent()) {
                logger.trace("Loading THEME CMS templates from {}", themePath.get().toAbsolutePath().toString());
            }
            themePath.map(path -> loadTemplates(path))
                    .ifPresent(map -> map.entrySet().stream().peek(entry -> entry.getValue().setThemeTemplate(true)).forEach(
                            entry -> templates.putIfAbsent(entry.getKey(), entry.getValue())));
            int size = templates.size();
            logger.debug("Loaded {} THEME CMS templates", size);

            //load core templates
            if (corePath.isPresent()) {
                logger.trace("Loading CORE CMS templates from {}", corePath.get().toAbsolutePath().toString());
            }
            corePath.map(path -> loadTemplates(path))
                    .ifPresent(map -> map.entrySet().stream().forEach(entry -> templates.putIfAbsent(entry.getKey(), entry.getValue())));
            logger.debug("Loaded {} CORE CMS templates", templates.size() - size);
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
            Optional<String> folderUrl = template.isThemeTemplate() ? getThemeViewFolderUrl() : getCoreViewFolderUrl();
            Optional<String> viewUrl = folderUrl.map(url -> url + "/" + template.getHtmlFileName());
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
            Optional<String> folderUrl = template.isThemeTemplate() ? getThemeIconFolderUrl() : getCoreIconFolderUrl();
            Optional<String> viewUrl = folderUrl.map(url -> url + "/" + template.getIconFileName());
            return viewUrl.orElse("");
        }
        return "";
    }

}
