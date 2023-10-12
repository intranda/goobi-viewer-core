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
package io.goobi.viewer.model.cms.pages;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Startup;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.legacy.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;

/**
 * <p>
 * CMSTemplateManager class.
 * </p>
 */
@Singleton
@Startup
@Named("cmsTemplateManager")
public class CMSTemplateManager implements Serializable {

    private static final long serialVersionUID = 5783005781012709309L;

    private static final Logger logger = LogManager.getLogger(CMSTemplateManager.class);

    private static final String TEMPLATE_BASE_PATH = "/cms/templates/";
    private static final String TEMPLATE_ICONS_PATH = "icons/";
    private static final String TEMPLATE_VIEWS_PATH = "views/";

    private Map<String, CMSComponent> legacyTemplateComponents;

    private transient Optional<String> coreTemplateFolderUrl = Optional.empty();
    private transient Optional<String> themeTemplateFolderUrl = Optional.empty();
    private transient Optional<Path> coreFolderPath = Optional.empty();
    private transient Optional<Path> themeFolderPath = Optional.empty();
    private transient Optional<Path> passedFileSystemPath = Optional.empty();

    private CMSPageContentManager contentManager = null;

    @Inject
    private transient ServletContext servletContext;

    private String filesystemPath;
    private String themeRootPath;

    public CMSTemplateManager() {
        filesystemPath = null;
        themeRootPath = DataManager.getInstance().getConfiguration().getThemeRootPath();
    }

    public CMSTemplateManager(String filesystemPath, String themeRootPath) throws PresentationException {
        this.filesystemPath = filesystemPath;
        this.themeRootPath = themeRootPath;
        this.init();
    }

    /**
     *
     * @param filesystemPath
     * @param themeRootPath If the theme contents are in an external folder, its root path must be provided here
     * @throws PresentationException
     */
    @PostConstruct
    public void init() throws PresentationException {
        String webContentRoot = "";
        if (filesystemPath == null) {
            if (servletContext == null) {
                if (FacesContext.getCurrentInstance() == null) {
                    throw new PresentationException("No faces context found");
                }
                servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            }
            webContentRoot = servletContext.getContextPath();
        }

        //check if the themeFolderPath contains any xml files
        try {
            boolean absolutetemplateFolderUrl = false;
            String templateFolderUrl = "resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
            String templateFolder = templateFolderUrl;
            if (StringUtils.isNotEmpty(themeRootPath)) {
                if (!themeRootPath.endsWith("/")) {
                    themeRootPath += '/';
                }
                templateFolder = themeRootPath + DataManager.getInstance().getConfiguration().getTheme() + TEMPLATE_BASE_PATH;
                absolutetemplateFolderUrl = true;
            }
            Optional<URL> themeFolderUrl = getThemeFolderUrl(filesystemPath, servletContext, templateFolder, absolutetemplateFolderUrl);
            themeFolderPath = themeFolderUrl.map(CMSTemplateManager::toURI);
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
            coreFolderPath = coreFolderUrl.map(CMSTemplateManager::toURI);
            boolean templatesFound = false;
            if (coreFolderPath.isPresent()) {
                logger.trace("coreFolderPath: {}", coreFolderPath.get());
                try (Stream<java.nio.file.Path> templateFiles = Files.list(coreFolderPath.get())) {
                    templatesFound = templateFiles.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".xml"))
                            .peek(file -> logger.trace("Found core cms template file {}", file))
                            .findAny()
                            .isPresent();
                }
            } else {
                logger.warn("coreFolderPath not found at {}, {} servletContent null ? {}", filesystemPath, templateFolderUrl, servletContext == null);
            }
            if (templatesFound) {
                this.coreTemplateFolderUrl = Optional.of(webContentRoot + "/" + templateFolderUrl);
            }
        } catch (IOException | URISyntaxException e) {
            logger.error(e.toString(), e);
        }

        logger.info("Creating CMSPageContentManager from paths {} and {}", coreFolderPath.orElse(null), themeFolderPath.orElse(null));
        //Add the fileSystem passed as argument to the contentManager so unit tests can define their own template path
        this.passedFileSystemPath = Optional.ofNullable(filesystemPath).map(Paths::get);
        this.reloadContentManager();
        this.updateTemplates(coreFolderPath.map(p -> p.resolve("legacy")), themeFolderPath.map(p -> p.resolve("legacy")));
    }

    public Optional<CMSPageTemplate> loadLegacyTemplate(String filename) {
        Optional<Path> corePath = coreFolderPath.map(p -> p.resolve("legacy")).map(p -> p.resolve(filename));
        if (corePath.isPresent()) {
            return corePath.map(p -> CMSPageTemplate.loadFromXML(p));
        } else {
            Optional<Path> themePath = themeFolderPath.map(p -> p.resolve("legacy")).map(p -> p.resolve(filename));
            if (themePath.isPresent()) {
                return themePath.map(p -> CMSPageTemplate.loadFromXML(p));
            }
        }
        return Optional.empty();
    }

    public CMSPageContentManager getContentManager() {
        return contentManager;
    }

    public void reloadContentManager() {
        try {
            this.contentManager =
                    new CMSPageContentManager(passedFileSystemPath.orElse(null), coreFolderPath.orElse(null), themeFolderPath.orElse(null));
        } catch (IOException e) {
            logger.error("Error creating CMSPageContentManager from paths {} and {}", coreFolderPath.orElse(null), themeFolderPath.orElse(null), e);
        }
    }

    /**
     * <p>
     * toURI.
     * </p>
     *
     * @param url a {@link java.net.URL} object.
     * @return a {@link java.nio.file.Path} object.
     */
    public static Path toURI(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>coreTemplateFolderUrl</code>.
     * </p>
     *
     * @param filesystemPath a {@link java.lang.String} object.
     * @param servletContext a {@link javax.servlet.ServletContext} object.
     * @param templateFolderUrl a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     * @throws java.net.MalformedURLException if any.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public static Optional<URL> getCoreTemplateFolderUrl(String filesystemPath, ServletContext servletContext, String templateFolderUrl)
            throws MalformedURLException, URISyntaxException {
        Optional<URL> fileUrl = Optional.empty();
        if (servletContext != null) {
            String basePath = servletContext.getRealPath(templateFolderUrl);
            logger.trace("basePath: {}", basePath);
            Path path = Paths.get(basePath);
            if (Files.exists(path)) {
                fileUrl = Optional.of(path.toFile().toURI().toURL());
            } else {
                logger.warn("Template folder path not found: {}", path.toAbsolutePath());
            }
            //                    fileUrl = servletContext.getResource(this.templateFolderUrl);
        } else if (filesystemPath != null) {
            String templateFolderPath = filesystemPath + templateFolderUrl;
            Path path = PathConverter.getPath(PathConverter.toURI(templateFolderPath));
            if (Files.exists(path)) {
                fileUrl = Optional.of(new URL(filesystemPath + templateFolderUrl));
            } else {
                logger.warn("Template folder path not found: {}", path.toAbsolutePath());
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
            logger.debug("Looking for external theme template folder in {}", path.toAbsolutePath());
            if (Files.isDirectory(path)) {
                coreFolderUrl = Optional.of(path.toUri().toURL());
            }
        } else if (servletContext != null) {
            coreFolderUrl = Optional.ofNullable(servletContext.getResource(templateFolderUrl));
        } else {
            Path path = PathConverter.getPath(PathConverter.toURI(filesystemPath + templateFolderUrl));
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
                        .map(CMSPageTemplate::loadFromXML)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.warn("Failed to read template files from {}. Cause: {}", path, e.toString());
        }

        if (templateList == null) {
            logger.warn("No cms folder found in {}. This theme is probably not configured to use cms", path);
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

    /**
     * <p>
     * updateTemplates.
     * </p>
     *
     * @param corePath a {@link java.util.Optional} object.
     * @param themePath a {@link java.util.Optional} object.
     */
    public synchronized void updateTemplates(Optional<Path> corePath, Optional<Path> themePath) {
        legacyTemplateComponents = new HashMap<>();
        try {
            //load theme templates
            if (themePath.isPresent()) {
                logger.trace("Loading THEME CMS templates from {}", themePath.get().toAbsolutePath());
            }
            themePath.map(CMSTemplateManager::loadTemplates)
                    .ifPresent(map -> map.entrySet()
                            .stream()
                            .peek(entry -> entry.getValue().setThemeTemplate(true))
                            .forEach(entry -> legacyTemplateComponents.putIfAbsent(entry.getKey(), entry.getValue().createCMSComponent())));
            int size = legacyTemplateComponents.size();
            logger.debug("Loaded {} THEME CMS templates", size);

            //load core templates
            if (corePath.isPresent()) {
                logger.trace("Loading CORE CMS templates from {}", corePath.get().toAbsolutePath());
            }
            corePath.map(CMSTemplateManager::loadTemplates)
                    .ifPresent(map -> map.entrySet()
                            .stream()
                            .forEach(entry -> legacyTemplateComponents.putIfAbsent(entry.getKey(), entry.getValue().createCMSComponent())));
            logger.debug("Loaded {} CORE CMS templates", legacyTemplateComponents.size() - size);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update cms templates: {}", e.toString(), e);
        }
    }

    /**
     * <p>
     * Getter for the field <code>templates</code>.
     * </p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<CMSComponent> getLegacyComponents() {
        return legacyTemplateComponents.values();
    }

    public Map<String, CMSComponent> getLegacyComponentMap() {
        return legacyTemplateComponents;
    }

    /**
     * <p>
     * getTemplate.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSPageTemplate} object.
     */
    public CMSComponent getLegacyComponent(String templateId) {
        return legacyTemplateComponents.get(templateId);
    }

    /**
     * <p>
     * Getter for the field <code>themeFolderPath</code>.
     * </p>
     *
     * @return the themeFolderPath
     */
    public Optional<Path> getThemeFolderPath() {
        return themeFolderPath;
    }

    /**
     * <p>
     * Getter for the field <code>coreFolderPath</code>.
     * </p>
     *
     * @return the coreFolderPath
     */
    public Optional<Path> getCoreFolderPath() {
        return coreFolderPath;
    }

    /**
     * <p>
     * getCoreViewFolderPath.
     * </p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<Path> getCoreViewFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    /**
     * <p>
     * getThemeViewFolderPath.
     * </p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<Path> getThemeViewFolderPath() {
        return getThemeFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    /**
     * <p>
     * getCoreIconFolderPath.
     * </p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<Path> getCoreIconFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_ICONS_PATH));
    }

    /**
     * <p>
     * getThemeIconFolderPath.
     * </p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<Path> getThemeIconFolderPath() {
        return getThemeFolderPath().map(path -> path.resolve(TEMPLATE_ICONS_PATH));
    }

    public Optional<CMSComponent> getComponent(String templateFilename) {
        if (templateFilename == null) {
            return Optional.empty();
        }

        Optional<CMSComponent> component = this.getContentManager().getComponent(templateFilename);
        if (component.isEmpty()) {
            return this.legacyTemplateComponents.values().stream().filter(t -> templateFilename.equals(t.getTemplateFilename())).findAny();
        }

        return component;
    }

}
