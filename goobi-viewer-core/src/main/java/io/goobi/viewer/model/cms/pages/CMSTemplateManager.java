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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
import io.goobi.viewer.modules.IModule;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;

/**
 * Loads and provides access to available CMS page templates from the filesystem.
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
            return corePath.map(CMSPageTemplate::loadFromXML);
        }
        Optional<Path> themePath = themeFolderPath.map(p -> p.resolve("legacy")).map(p -> p.resolve(filename));
        if (themePath.isPresent()) {
            return themePath.map(p -> CMSPageTemplate.loadFromXML(p));
        }
        return Optional.empty();
    }

    public CMSPageContentManager getContentManager() {
        return contentManager;
    }

    public void reloadContentManager() {
        try {
            // Module-aware reload (#15809): forward all module-contributed component folders alongside core+theme.
            // Module folders may resolve to JAR-internal paths; collectModuleComponentFolders takes care of mounting
            // the JarFileSystem for such URLs.
            List<Path> moduleFolders = collectModuleComponentFolders();
            List<Path> all = new ArrayList<>();
            passedFileSystemPath.ifPresent(all::add);
            coreFolderPath.ifPresent(all::add);
            themeFolderPath.ifPresent(all::add);
            all.addAll(moduleFolders);
            this.contentManager = new CMSPageContentManager(all.toArray(Path[]::new));
        } catch (IOException e) {
            logger.error("Error creating CMSPageContentManager", e);
        }
    }

    /**
     * Collects CMS-component folders contributed by all loaded modules. Each module returns a {@link URL} that may
     * point either at a real filesystem directory ({@code file:}) or at a JAR-internal directory
     * ({@code jar:file:.../module.jar!/path}). The latter requires splitting the URI into the bare JAR location
     * (used to mount the {@link FileSystems#newFileSystem ZIP filesystem}) and the inner path (resolved via
     * {@link FileSystem#getPath}). {@link Paths#get(URI)} alone will not work for the {@code jar:} case.
     *
     * @return list of folder paths, never null, possibly empty
     * @should ignore unloaded modules
     * @should resolve file urls to filesystem paths
     * @should resolve jar urls to jarfilesystem paths
     */
    private List<Path> collectModuleComponentFolders() {
        List<Path> out = new ArrayList<>();
        for (IModule module : DataManager.getInstance().getModules()) {
            if (!module.isLoaded()) {
                continue;
            }
            Optional<URL> urlOpt = module.getCmsComponentFolderUrl();
            if (urlOpt.isEmpty()) {
                continue;
            }
            try {
                Path path = resolveModuleFolderUrl(module.getId(), urlOpt.get());
                if (path != null && Files.isDirectory(path)) {
                    out.add(path);
                } else if (path != null) {
                    logger.warn("Module {} cmsComponentFolderUrl is not a directory: {}", module.getId(), path);
                }
            } catch (URISyntaxException | IOException e) {
                logger.error("Cannot resolve cms component folder for module {}: {}", module.getId(), e.getMessage());
            }
        }
        return out;
    }

    /**
     * Splits a possibly-jar URL into a {@link Path} usable by {@link Files#list(Path)}. The scheme decides:
     * <ul>
     *   <li>{@code file:} -&gt; {@link Paths#get(URI)} directly.</li>
     *   <li>{@code jar:} -&gt; split into outer JAR URI (everything before {@code !/}) and inner path; mount the JAR's
     *       ZIP filesystem if not yet mounted; return {@link FileSystem#getPath} on the inner path.</li>
     * </ul>
     *
     * @param moduleId for diagnostics only
     * @param url URL to convert
     * @return Path, or null if the scheme is unsupported
     * @throws URISyntaxException if the URL cannot be converted to a URI
     * @throws IOException if mounting the JAR filesystem fails
     */
    private Path resolveModuleFolderUrl(String moduleId, URL url) throws URISyntaxException, IOException {
        URI uri = url.toURI();
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            return Paths.get(uri);
        }
        if ("jar".equals(scheme)) {
            String spec = uri.getRawSchemeSpecificPart();
            int sep = spec.indexOf("!/");
            if (sep < 0) {
                logger.warn("Module {} jar URL has no '!/' separator: {}", moduleId, uri);
                return null;
            }
            URI jarUri = URI.create("jar:" + spec.substring(0, sep));
            String innerPath = spec.substring(sep + 1); // includes leading '/'
            FileSystem fs;
            try {
                fs = FileSystems.newFileSystem(jarUri, Collections.singletonMap("create", "true")); //NOSONAR S2095: FS kept open for returned Path
            } catch (FileSystemAlreadyExistsException ignore) {
                fs = FileSystems.getFileSystem(jarUri);
            }
            return fs.getPath(innerPath);
        }
        logger.warn("Module {} contributed cmsComponentFolderUrl with unsupported scheme '{}': {}", moduleId, scheme, uri);
        return null;
    }

    /**
     * toURI.
     *
     * @param url URL to convert to a filesystem path
     * @return the filesystem path corresponding to the given URL, or null if conversion fails
     */
    public static Path toURI(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Getter for the field <code>coreTemplateFolderUrl</code>.
     *
     * @param filesystemPath base filesystem path used when servlet context is absent
     * @param servletContext servlet context for resolving web resource paths
     * @param templateFolderUrl relative URL path of the template folder
     * @return an Optional containing the URL to the core CMS template folder, or empty if not found
     * @throws java.net.MalformedURLException if any.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public static Optional<URL> getCoreTemplateFolderUrl(String filesystemPath, ServletContext servletContext, String templateFolderUrl)
            throws MalformedURLException, URISyntaxException {
        Optional<URL> fileUrl = Optional.empty();
        if (servletContext != null) {
            String basePath = servletContext.getRealPath(templateFolderUrl);
            if (basePath == null) {
                throw new IllegalStateException("CMS template folder '" + templateFolderUrl + "' could not be found in viewer webapp folder");
            }
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
                fileUrl = Optional.of(new URI(filesystemPath + templateFolderUrl).toURL());
            } else {
                logger.warn("Template folder path not found: {}", path.toAbsolutePath());
            }
        }
        return fileUrl;
    }

    /**
     * Returns an url pointing to the cms template folder of the viewer theme.
     *
     * @param filesystemPath base filesystem path for template lookup
     * @param servletContext servlet context for resource lookup; may be null
     * @param templateFolderUrl relative or absolute URL of the template folder
     * @param absolutetemplateFolderUrl true if templateFolderUrl is an absolute path
     * @return Optional<URL>
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
                        .toList();
            }
        } catch (IOException e) {
            logger.warn("Failed to read template files from {}. Cause: {}", path, e.toString());
        }

        if (templateList == null) {
            logger.warn("No cms folder found in {}. This theme is probably not configured to use cms", path);
            return templates;
        }
        // logger.trace(templateFolder.getAbsolutePath()); //NOSONAR Debug
        for (CMSPageTemplate template : templateList) {
            if (templates.get(template.getId()) != null) {
                throw new IllegalArgumentException("Found two templates with id " + template.getId());
            }
            templates.put(template.getId(), template);
        }
        return templates;
    }

    /**
     * updateTemplates.
     *
     * @param corePath optional path to the core legacy template folder
     * @param themePath optional path to the theme legacy template folder
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
     * Getter for the field <code>templates</code>.
     *
     * @return the collection of all loaded legacy CMS template components
     */
    public Collection<CMSComponent> getLegacyComponents() {
        return legacyTemplateComponents.values();
    }

    public Map<String, CMSComponent> getLegacyComponentMap() {
        return legacyTemplateComponents;
    }

    /**
     * getTemplate.
     *
     * @param templateId unique identifier of the legacy template
     * @return the legacy CMS component matching the given template ID, or null if not found
     */
    public CMSComponent getLegacyComponent(String templateId) {
        return legacyTemplateComponents.get(templateId);
    }

    /**
     * Getter for the field <code>themeFolderPath</code>.
     *
     * @return an Optional containing the path to the theme's CMS template folder, or empty if no theme folder is configured
     */
    public Optional<Path> getThemeFolderPath() {
        return themeFolderPath;
    }

    /**
     * Getter for the field <code>coreFolderPath</code>.
     *
     * @return an Optional containing the path to the core CMS template folder, or empty if no core folder is configured
     */
    public Optional<Path> getCoreFolderPath() {
        return coreFolderPath;
    }

    /**
     * getCoreViewFolderPath.
     *
     * @return an Optional containing the path to the core CMS template views folder, or empty if no core folder is configured
     */
    public Optional<Path> getCoreViewFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    /**
     * getThemeViewFolderPath.
     *
     * @return an Optional containing the path to the theme CMS template views folder, or empty if no theme folder is configured
     */
    public Optional<Path> getThemeViewFolderPath() {
        return getThemeFolderPath().map(path -> path.resolve(TEMPLATE_VIEWS_PATH));
    }

    /**
     * getCoreIconFolderPath.
     *
     * @return an Optional containing the path to the core CMS template icons folder, or empty if no core folder is configured
     */
    public Optional<Path> getCoreIconFolderPath() {
        return getCoreFolderPath().map(path -> path.resolve(TEMPLATE_ICONS_PATH));
    }

    /**
     * getThemeIconFolderPath.
     *
     * @return an Optional containing the path to the theme CMS template icons folder, or empty if no theme folder is configured
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
