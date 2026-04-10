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
package io.goobi.viewer.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import io.goobi.viewer.controller.config.filter.IFilterConfiguration;
import io.goobi.viewer.controller.json.JsonMetadataConfiguration;
import io.goobi.viewer.controller.model.FeatureSetConfiguration;
import io.goobi.viewer.controller.model.LabeledValue;
import io.goobi.viewer.controller.model.ManifestLinkConfiguration;
import io.goobi.viewer.controller.model.ProviderConfiguration;
import io.goobi.viewer.controller.model.ViewAttributes;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.DownloadBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.cms.Highlight;
import io.goobi.viewer.model.export.ExportFieldConfiguration;
import io.goobi.viewer.model.job.ITaskType;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.maps.GeoMapMarker.MarkerType;
import io.goobi.viewer.model.maps.GeomapItemFilter;
import io.goobi.viewer.model.maps.View;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataListElement;
import io.goobi.viewer.model.metadata.MetadataListSeparator;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.metadata.MetadataView.MetadataViewLocation;
import io.goobi.viewer.model.misc.EmailRecipient;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.model.security.CopyrightIndicatorLicense;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus.Status;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.authentication.BibliothecaProvider;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LitteraProvider;
import io.goobi.viewer.model.security.authentication.LocalAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.authentication.VuFindProvider;
import io.goobi.viewer.model.security.authentication.XServiceProvider;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.transkribus.TranskribusUtils;
import io.goobi.viewer.model.translations.admin.TranslationGroup;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.model.translations.admin.TranslationGroupItem;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.collections.DcSortingList;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.faces.model.SelectItem;

/**
 * Central configuration holder that reads and provides access to all viewer configuration settings from the config XML file.
 */
public class Configuration extends AbstractConfiguration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    public static final String CONFIG_FILE_NAME = "config_viewer.xml";

    public static final String METADATA_LIST_TYPE_PAGE = "page";
    public static final String METADATA_LIST_TYPE_SEARCH_HIT = "searchHit";

    private static final String XML_PATH_ATTRIBUTE_CONDITION = "[@condition]";
    private static final String XML_PATH_ATTRIBUTE_DEFAULT = "[@default]";
    private static final String XML_PATH_ATTRIBUTE_DESCRIPTION = "[@description]";
    private static final String XML_PATH_ATTRIBUTE_ICON = "[@icon]";
    private static final String XML_PATH_ATTRIBUTE_LABEL = "[@label]";
    private static final String XML_PATH_ATTRIBUTE_NAME = "[@name]";
    private static final String XML_PATH_ATTRIBUTE_TYPE = "[@type]";
    private static final String XML_PATH_ATTRIBUTE_URL = "[@url]";

    private static final String XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE = "search.advanced.searchFields.template";
    private static final String XML_PATH_SEARCH_SORTING_FIELD = "search.sorting.field";
    private static final String XML_PATH_TOC_TITLEBARLABEL_TEMPLATE = "toc.titleBarLabel.template";
    private static final String XML_PATH_USER_AUTH_PROVIDERS_PROVIDER = "user.authenticationProviders.provider(";

    static final String VALUE_DEFAULT = "_DEFAULT";

    private Set<String> stopwords;

    /**
     * Creates a new Configuration instance.
     *
     * @param configFilePath path to the default configuration XML file
     */
    public Configuration(String configFilePath) {
        // Load default config file
        builder =
                new ReloadingFileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setBasePath(Configuration.class.getClassLoader().getResource("").getFile())
                                .setFileName(configFilePath)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setThrowExceptionOnMissing(false));
        if (builder.getFileHandler().getFile().exists()) {
            try {
                builder.getConfiguration();
                logger.info("Default configuration file '{}' loaded.", builder.getFileHandler().getFile().getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            builder.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    event -> builder.getReloadingController().checkForReloading(null));
        } else {
            logger.error("Default configuration file not found: {}; Base path is {}", builder.getFileHandler().getFile().getAbsoluteFile(),
                    builder.getFileHandler().getBasePath());
        }

        // Load local config file
        String fileName = FilenameUtils.getName(configFilePath);
        File fileLocal = new File(getConfigLocalPath() + fileName);
        builderLocal =
                new ReloadingFileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(fileLocal.getAbsolutePath())
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setThrowExceptionOnMissing(false));
        if (builderLocal.getFileHandler().getFile().exists()) {
            try {
                builderLocal.getConfiguration();
                logger.info("Local configuration file '{}' loaded.", fileLocal.getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            logger.trace("adding event listener");
            builderLocal.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    event -> {
                        // logger.trace("request event");
                        if (builderLocal.getReloadingController().checkForReloading(null)) {
                            if (System.currentTimeMillis() - localConfigDisabledTimestamp > 1000) {
                                localConfigDisabled = false;
                                logger.info("Local configuration file '{}' reloaded.", fileLocal.getAbsolutePath());
                            }
                        }
                    });
        }

        // Load stopwords
        try {
            stopwords = loadStopwords(getStopwordsFilePath());
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            stopwords = HashSet.newHashSet(0);
        } catch (IOException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            stopwords = HashSet.newHashSet(0);
        }
    }

    /**
     * loadStopwords.
     *
     * @param stopwordsFilePath path to the stopwords file to read
     * @return the set of stopwords loaded from the file
     * @throws java.io.IOException if any.
     * @should load all stopwords
     * @should remove parts starting with pipe
     * @should not add empty stopwords
     * @should throw IllegalArgumentException if stopwordsFilePath empty
     * @should throw FileNotFoundException if file does not exist
     */
    protected static Set<String> loadStopwords(final String stopwordsFilePath) throws IOException {
        if (StringUtils.isEmpty(stopwordsFilePath)) {
            throw new IllegalArgumentException("stopwordsFilePath may not be null or empty");
        }

        String useStopwordsFilePath = FileTools.adaptPathForWindows(stopwordsFilePath);
        Set<String> ret = new HashSet<>();
        try (FileReader fr = new FileReader(useStopwordsFilePath); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (StringUtils.isNotBlank(line) && line.charAt(0) != '#') {
                    int pipeIndex = line.indexOf('|');
                    if (pipeIndex != -1) {
                        line = line.substring(0, pipeIndex).trim();
                    }
                    if (!line.isEmpty() && Character.getNumericValue(line.charAt(0)) != -1) {
                        ret.add(line);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Returns the stopwords loading during initialization.
     *
     * @should return all stopwords
     * @return the set of stopwords loaded during initialization
     */
    public Set<String> getStopwords() {
        return stopwords;
    }

    /*********************************** direct config results ***************************************/

    /**
     * getConfigLocalPath.
     *
     * @return the path to the local config_viewer.xml file.
     * @should return environment variable value if available
     * @should add trailing slash
     */
    public String getConfigLocalPath() {
        String configLocalPath = System.getProperty("configFolder");
        if (StringUtils.isEmpty(configLocalPath)) {
            configLocalPath = getConfig().getString("configFolder", "/opt/digiverso/viewer/config/");
        }
        if (!configLocalPath.endsWith("/")) {
            configLocalPath += "/";
        }
        configLocalPath = FileTools.adaptPathForWindows(configLocalPath);
        return configLocalPath;
    }

    /**
     * getLocalRessourceBundleFile.
     *
     * @return the absolute path to the local German message properties file
     */
    public String getLocalRessourceBundleFile() {
        return getConfigLocalPath() + "messages_de.properties";
    }

    /**
     * getViewerThumbnailsPerPage.
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerThumbnailsPerPage() {
        return getLocalInt("viewer.thumbnailsPerPage", 10);
    }

    /**
     * getViewerMaxImageWidth.
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageWidth() {
        return getLocalInt("viewer.maxImageWidth", 2000);
    }

    /**
     * getViewerMaxImageHeight.
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageHeight() {
        return getLocalInt("viewer.maxImageHeight", 2000);
    }

    /**
     * getViewerMaxImageScale.
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageScale() {
        return getLocalInt("viewer.maxImageScale", 500);
    }

    /**
     * isRememberImageZoom.
     *
     * @return true if the image zoom level is remembered between page navigation, false otherwise
     * @should return correct value
     */
    public boolean isRememberImageZoom() {
        return getLocalBoolean("viewer.rememberImageZoom[@enabled]", false);
    }

    /**
     * isRememberImageRotation.
     *
     * @return true if the image rotation is remembered between page navigation, false otherwise
     * @should return correct value
     */
    public boolean isRememberImageRotation() {
        return getLocalBoolean("viewer.rememberImageRotation[@enabled]", false);
    }

    /**
     * getDfgViewerUrl.
     *
     * @return the configured DFG Viewer base URL
     * @should return correct value
     */
    public String getDfgViewerUrl() {
        return getLocalString("urls.dfg-viewer", "https://dfg-viewer.de/v2?set[mets]=");
    }

    /**
     * 
     * @return the configured Solr field used as the DFG Viewer source file field
     * @should return correct value
     */
    public String getDfgViewerSourcefileField() {
        return getLocalString("urls.dfg-viewer[@sourcefileField]");
    }

    /**
     * 
     * @param prefix Optional prefix for filtering
     * @return List of type attribute values of matching lists
     * @should return all metadataList types if prefix empty
     * @should filter by prefix correctly
     */
    public List<String> getMetadataListTypes(String prefix) {
        List<HierarchicalConfiguration<ImmutableNode>> metadataLists = getLocalConfigurationsAt("metadata.metadataList");
        if (metadataLists == null) {
            logger.error("no metadata lists found");
            return new ArrayList<>(); // must be a mutable list!
        }
        // TODO Combine local and global types?

        List<String> ret = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> metadataList : metadataLists) {
            String type = metadataList.getString(XML_PATH_ATTRIBUTE_TYPE);
            if (StringUtils.isEmpty(prefix) || type.startsWith(prefix)) {
                ret.add(type);
            }
        }

        return ret;
    }

    /**
     * 
     * @param type metadata list type attribute value
     * @param template template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @param topstructValueFallbackDefaultValue default value for topstructValueFallback attribute
     * @return List of metadata configurations
     * @should throw IllegalArgumentException if type null
     * @should return empty list if no metadata lists configured
     * @should return empty list if metadataList contains no templates
     * @should return empty list if list type not found
     */
    public List<Metadata> getMetadataConfigurationForTemplate(String type, String template, boolean fallbackToDefaultTemplate,
            boolean topstructValueFallbackDefaultValue) {
        return getMetadataListItemsForTemplate(type, template, fallbackToDefaultTemplate, topstructValueFallbackDefaultValue).stream()
                .filter(item -> item instanceof Metadata)
                .map(item -> (Metadata) item)
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param type metadata list type attribute value
     * @param template template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @param topstructValueFallbackDefaultValue default value for topstructValueFallback attribute
     * @return List of metadata configurations
     * @should throw IllegalArgumentException if type null
     * @should return empty list if no metadata lists configured
     * @should return empty list if metadataList contains no templates
     * @should return empty list if list type not found
     */
    public List<MetadataListElement> getMetadataListItemsForTemplate(String type, String template, boolean fallbackToDefaultTemplate,
            boolean topstructValueFallbackDefaultValue) {
        // logger.trace("getMetadataConfigurationForTemplate: {}/{}", type, template); //NOSONAR Debug
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> allMetadataLists = new ArrayList<>();

        // Local lists
        List<HierarchicalConfiguration<ImmutableNode>> metadataLists = getLocalConfigurationsAt("metadata.metadataList");
        if (metadataLists != null) {
            allMetadataLists.addAll(metadataLists);
        }
        // Global lists
        metadataLists = getLocalConfigurationsAt(getConfig(), null, "metadata.metadataList");
        if (metadataLists != null) {
            allMetadataLists.addAll(metadataLists);
        }

        if (allMetadataLists.isEmpty()) {
            logger.trace("no metadata lists found");
            return new ArrayList<>(); // must be a mutable list!
        }

        for (HierarchicalConfiguration<ImmutableNode> metadataList : allMetadataLists) {
            if (type.equals(metadataList.getString(XML_PATH_ATTRIBUTE_TYPE))) {
                List<HierarchicalConfiguration<ImmutableNode>> templateList = metadataList.configurationsAt("template");
                if (templateList.isEmpty()) {
                    logger.trace("{}  templates found for type {}", templateList.size(), type);
                    return new ArrayList<>(); // must be a mutable list!
                }

                return getMetadataListItemsForTemplate(template, templateList, fallbackToDefaultTemplate, topstructValueFallbackDefaultValue);
            }
        }

        return new ArrayList<>(); // must be a mutable list!
    }

    /**
     * 
     * @param type metadata list type attribute value
     * @return Map&lt;String, List&lt;Metadata&gt;&gt;
     * @should return empty map if type null
     * @should return correct config
     */
    public Map<String, List<Metadata>> getMetadataTemplates(String type) {
        try {
            return getMetadataTemplates(type, true, true);
        } catch (IllegalArgumentException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 
     * @param type metadata list type attribute value
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @param topstructValueFallbackDefaultValue default value for topstructValueFallback attribute
     * @return Map&lt;String, List&lt;Metadata&gt;&gt;
     */
    public Map<String, List<Metadata>> getMetadataTemplates(String type, boolean fallbackToDefaultTemplate,
            boolean topstructValueFallbackDefaultValue) {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> allMetadataLists = new ArrayList<>();

        // Local lists
        List<HierarchicalConfiguration<ImmutableNode>> metadataLists = getLocalConfigurationsAt("metadata.metadataList");
        if (metadataLists != null) {
            allMetadataLists.addAll(metadataLists);
        }
        // Global lists
        metadataLists = getLocalConfigurationsAt(getConfig(), null, "metadata.metadataList");
        if (metadataLists != null) {
            allMetadataLists.addAll(metadataLists);
        }

        if (allMetadataLists.isEmpty()) {
            logger.trace("no metadata lists found");
            return new HashMap<>(); // must be a mutable list!
        }

        Map<String, List<Metadata>> map = new HashMap<>();
        for (HierarchicalConfiguration<ImmutableNode> metadataList : allMetadataLists) {
            if (type.equals(metadataList.getString(XML_PATH_ATTRIBUTE_TYPE))) {
                List<HierarchicalConfiguration<ImmutableNode>> templateList = metadataList.configurationsAt("template");
                for (HierarchicalConfiguration<ImmutableNode> templateConfig : templateList) {
                    String template = templateConfig.getString("[@name]", VALUE_DEFAULT);
                    map.put(template, getMetadataForTemplate(template, templateList, fallbackToDefaultTemplate, topstructValueFallbackDefaultValue));
                }

            }
        }
        return map;
    }

    /**
     * Returns the list of configured metadata for search hit elements.
     *
     * @param template template name to look up
     * @should return correct template configuration
     * @should return default template configuration if requested not found
     * @should return default template if template is null
     * @return a list of configured Metadata definitions for search hit elements of the given template
     */
    public List<Metadata> getSearchHitMetadataForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.searchHitMetadataList.template");
        if (templateList != null && !templateList.isEmpty()) {
            logger.warn("Old <searchHitMetadataList> configuration found - please migrate to <metadataList type=\"searchHit\">.");
            return getMetadataForTemplate(template, templateList, true, true);
        }

        return getMetadataConfigurationForTemplate(METADATA_LIST_TYPE_SEARCH_HIT, template, true, true);
    }

    /**
     * Returns the list of configured metadata for pages.
     *
     * @param template template name to look up
     * @should return correct template configuration
     * @should return default template configuration if requested not found
     * @should return default template if template is null
     * @return a list of configured Metadata definitions for page elements of the given template
     */
    public List<Metadata> getPageMetadataForTemplate(String template) {
        return getMetadataConfigurationForTemplate(METADATA_LIST_TYPE_PAGE, template, true, true);
    }

    /**
     * Returns the list of configured metadata for {@link Highlight}s which reference a record.
     *
     * @param template template name to look up
     * @should return default template configuration if requested not found
     * @return a list of configured Metadata definitions for highlighted record references of the given template
     */
    public List<Metadata> getHighlightMetadataForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.highlightMetadataList.template");
        if (templateList != null && !templateList.isEmpty()) {
            logger.warn("Old <searchHitMetadataList> configuration found - please migrate to <metadataList type=\"searchHit\">.");
            return getMetadataForTemplate(template, templateList, true, true);
        }

        return getMetadataConfigurationForTemplate("highlight", template, true, true);
    }

    /**
     *
     * @return the list of configured metadata view configurations
     * @should return all configured values
     */
    public List<MetadataView> getMetadataViews() {
        List<HierarchicalConfiguration<ImmutableNode>> metadataPageList = getLocalConfigurationsAt("metadata.metadataView");
        if (metadataPageList == null) {
            metadataPageList = getLocalConfigurationsAt("metadata.mainMetadataList");
            if (metadataPageList != null) {
                logger.warn("Old <mainMetadataList> configuration found - please migrate to <metadataView>.");
                return Collections.singletonList(new MetadataView());
            }
            return new ArrayList<>();
        }

        List<MetadataView> ret = new ArrayList<>(metadataPageList.size());
        for (HierarchicalConfiguration<ImmutableNode> metadataView : metadataPageList) {
            int index = metadataView.getInt("[@index]", 0);
            String label = metadataView.getString(XML_PATH_ATTRIBUTE_LABEL);
            String url = metadataView.getString(XML_PATH_ATTRIBUTE_URL, "");
            String condition = metadataView.getString(XML_PATH_ATTRIBUTE_CONDITION);
            MetadataViewLocation location = MetadataViewLocation.getByName(metadataView.getString("[@location]", "sidebar"));
            MetadataView view = new MetadataView().setIndex(index).setLabel(label).setUrl(url).setCondition(condition).setLocation(location);
            ret.add(view);
        }

        return ret;
    }

    /**
     *
     * @param index zero-based index of the metadataView element to use
     * @param template template name to look up
     * @return List of configured <code>Metadata</code> fields for the given template
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     */
    public List<Metadata> getMainMetadataForTemplate(int index, String template) {
        return getMainMetadataListItemsForTemplate(index, template).stream()
                .filter(item -> item instanceof Metadata)
                .map(item -> (Metadata) item)
                .toList();
    }

    /**
     *
     * @param index zero-based index of the metadataView element to use
     * @param template template name to look up
     * @return List of configured <code>Metadata</code> fields for the given template
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     */
    public List<MetadataListElement> getMainMetadataListItemsForTemplate(int index, String template) {
        logger.trace("getMainMetadataForTemplate: {}", template);
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.metadataView(" + index + ").template");
        if (templateList == null) {
            templateList = getLocalConfigurationsAt("metadata.metadataView.template");
            if (templateList == null) {
                templateList = getLocalConfigurationsAt("metadata.mainMetadataList.template");
                // Old configuration fallback
                if (templateList != null) {
                    logger.warn("Old <mainMetadataList> configuration found - please migrate to <metadataView>.");
                } else {
                    return new ArrayList<>();
                }
            }
        }

        return getMetadataListItemsForTemplate(template, templateList, true, false);
    }

    /**
     * Returns the list of configured metadata for the sidebar.
     *
     * @param template Template name
     * @return List of configured metadata for configured fields
     * @should return correct template configuration
     * @should return empty list if template not found
     * @should return empty list if template is null
     */
    public List<Metadata> getSidebarMetadataForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.sideBarMetadataList.template");
        if (templateList != null && !templateList.isEmpty()) {
            logger.warn("Old <sideBarMetadataList> configuration found - please migrate to <metadataList type=\"sideBar\">.");
            return getMetadataForTemplate(template, templateList, false, false);
        }

        return getMetadataConfigurationForTemplate("sideBar", template, false, false);
    }

    /**
     * Returns the list of configured metadata for the archives.
     *
     * @return List of configured metadata for configured fields
     * @should return default template configuration
     */
    public List<Metadata> getArchiveMetadata() {
        return getMetadataConfigurationForTemplate("archive", StringConstants.DEFAULT_NAME, true, false);
    }

    /**
     * Reads metadata configuration for the given template name if it's contained in the given template list. Includes non-metadata elements in the
     * list
     *
     * @param template Requested template name
     * @param templateList List of templates in which to look
     * @param fallbackToDefaultTemplate If true, the _DEFAULT template will be loaded if the given template is not found
     * @param topstructValueFallbackDefaultValue If true, the default value for the parameter attribute "topstructValueFallback" will be the value
     *            passed here
     * @return the list of configured metadata list elements for the resolved template
     */
    private static List<MetadataListElement> getMetadataListItemsForTemplate(String template,
            List<HierarchicalConfiguration<ImmutableNode>> templateList,
            boolean fallbackToDefaultTemplate, boolean topstructValueFallbackDefaultValue) {
        if (templateList == null) {
            return new ArrayList<>();
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return new ArrayList<>();
        }

        return getMetadataListItemsForTemplate(usingTemplate, topstructValueFallbackDefaultValue);
    }

    /**
     * Reads metadata configuration for the given template name if it's contained in the given template list.
     *
     * @param template Requested template name
     * @param templateList List of templates in which to look
     * @param fallbackToDefaultTemplate If true, the _DEFAULT template will be loaded if the given template is not found
     * @param topstructValueFallbackDefaultValue If true, the default value for the parameter attribute "topstructValueFallback" will be the value
     *            passed here
     * @return the list of configured Metadata objects for the resolved template
     */
    private static List<Metadata> getMetadataForTemplate(String template, List<HierarchicalConfiguration<ImmutableNode>> templateList,
            boolean fallbackToDefaultTemplate, boolean topstructValueFallbackDefaultValue) {
        return getMetadataListItemsForTemplate(template, templateList, fallbackToDefaultTemplate, topstructValueFallbackDefaultValue).stream()
                .filter(item -> item instanceof Metadata)
                .map(item -> (Metadata) item)
                .toList();
    }

    /**
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     *
     * @param usingTemplate the template configuration node to read metadata from
     * @param topstructValueFallbackDefaultValue Default value for topstructValueFallback, if not explicitly configured
     * @return the list of configured metadata list elements for the given template configuration node
     */
    private static List<MetadataListElement> getMetadataListItemsForTemplate(HierarchicalConfiguration<ImmutableNode> usingTemplate,
            boolean topstructValueFallbackDefaultValue) {
        if (usingTemplate == null) {
            return new ArrayList<>();
        }
        List<HierarchicalConfiguration<ImmutableNode>> elements = usingTemplate.childConfigurationsAt(".");
        if (elements == null) {
            logger.warn("Template '{}' contains no metadata elements.", usingTemplate.getRootElementName());
            return new ArrayList<>();
        }

        List<MetadataListElement> ret = new ArrayList<>(elements.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : elements) {
            if ("fold".equals(sub.getRootElementName())) {
                ret.add(new MetadataListSeparator());
            } else if ("metadata".equals(sub.getRootElementName())) {
                Metadata md = getMetadataFromSubnodeConfig(sub, topstructValueFallbackDefaultValue, 0);
                if (md != null) {
                    ret.add(md);
                }
            }
        }

        return ret;
    }

    /**
     * Selects template from given list and optionally returns default template configuration.
     * 
     * @param templateList list of template configuration nodes to search
     * @param template template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the matching template configuration node, or the default template if fallback is enabled; null if not found
     */
    static HierarchicalConfiguration<ImmutableNode> selectTemplate(List<HierarchicalConfiguration<ImmutableNode>> templateList, String template,
            boolean fallbackToDefaultTemplate) {
        if (templateList == null) {
            return null;
        }

        HierarchicalConfiguration<ImmutableNode> ret = null;
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (HierarchicalConfiguration<ImmutableNode> subElement : templateList) {
            if (subElement.getString(XML_PATH_ATTRIBUTE_NAME).equals(template)) {
                ret = subElement;
                break;
            } else if (StringConstants.DEFAULT_NAME.equals(subElement.getString(XML_PATH_ATTRIBUTE_NAME))) {
                defaultTemplate = subElement;
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (ret == null && fallbackToDefaultTemplate) {
            ret = defaultTemplate;
        }

        return ret;
    }

    /**
     * Creates a {@link Metadata} instance from the given subnode configuration.
     *
     * @param sub The subnode configuration
     * @param topstructValueFallbackDefaultValue default value for topstructValueFallback attribute
     * @param indentation indentation level for nested metadata
     * @return the resulting {@link Metadata} instance
     * @should load metadata config attributes correctly
     * @should load parameters correctly
     * @should load replace rules correctly
     * @should load child metadata configurations recursively
     */
    static Metadata getMetadataFromSubnodeConfig(HierarchicalConfiguration<ImmutableNode> sub, boolean topstructValueFallbackDefaultValue,
            int indentation) {
        if (sub == null) {
            throw new IllegalArgumentException("sub may not be null");
        }

        String label = sub.getString(XML_PATH_ATTRIBUTE_LABEL);
        String masterValue = sub.getString("[@value]");
        String citationTemplate = sub.getString("[@citationTemplate]");
        boolean group = sub.getBoolean("[@group]", false);
        String key = sub.getString("[@key]", label);
        boolean singleString = sub.getBoolean("[@singleString]", true);
        boolean topstructOnly = sub.getBoolean("[@topstructOnly]", false);
        int number = sub.getInt("[@number]", -1);
        int type = sub.getInt(XML_PATH_ATTRIBUTE_TYPE, 0);
        boolean hideIfOnlyMetadataField = sub.getBoolean("[@hideIfOnlyMetadataField]", false);
        String labelField = sub.getString("[@labelField]");
        String sortField = sub.getString("[@sortField]");
        String separator = sub.getString("[@separator]");
        String filterQuery = sub.getString("filterQuery", "");
        List<HierarchicalConfiguration<ImmutableNode>> params = sub.configurationsAt("param");
        List<MetadataParameter> paramList = null;
        if (params != null) {
            paramList = new ArrayList<>(params.size());
            for (HierarchicalConfiguration<ImmutableNode> sub2 : params) {
                paramList.add(MetadataParameter.createFromConfig(sub2, topstructValueFallbackDefaultValue));
            }
        }

        Metadata ret = new Metadata(label, key, masterValue, paramList)
                .setType(type)
                .setGroup(group)
                .setNumber(number)
                .setSingleString(singleString)
                .setHideIfOnlyMetadataField(hideIfOnlyMetadataField)
                .setTopstructOnly(topstructOnly)
                .setCitationTemplate(citationTemplate)
                .setLabelField(labelField)
                .setSortField(sortField)
                .setSeparator(separator)
                .setIndentation(indentation)
                .setFilterQuery(filterQuery);

        // Recursively add nested metadata configurations
        List<HierarchicalConfiguration<ImmutableNode>> children = sub.configurationsAt("metadata");
        if (children != null && !children.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> child : children) {
                Metadata childMetadata = getMetadataFromSubnodeConfig(child, topstructValueFallbackDefaultValue, indentation + 1);
                childMetadata.setParentMetadata(ret);
                ret.getChildMetadata().add(childMetadata);
            }
        }

        return ret;
    }

    /**
     * getNormdataFieldsForTemplate.
     *
     * @param template Template name
     * @return List of normdata fields configured for the given template name
     * @should return correct template configuration
     */
    public List<String> getNormdataFieldsForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.normdataList.template");
        if (templateList == null) {
            return new ArrayList<>();
        }

        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        for (HierarchicalConfiguration<ImmutableNode> subElement : templateList) {
            if (subElement.getString(XML_PATH_ATTRIBUTE_NAME).equals(template)) {
                usingTemplate = subElement;
                break;
            }
        }
        if (usingTemplate == null) {
            return new ArrayList<>();
        }

        return getLocalList(usingTemplate, null, "field", null);
    }

    /**
     * getTocLabelConfiguration.
     *
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @param template template name to look up
     * @return a list of Metadata definitions used for generating TOC entry labels for the given template
     */
    public List<Metadata> getTocLabelConfiguration(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.labelConfig.template");
        if (templateList == null) {
            return new ArrayList<>();
        }

        return getMetadataForTemplate(template, templateList, true, false);
    }

    public float getGeomapClusterDistanceMultiplier() {
        return getLocalFloat("maps.cluster.distanceMultiplier", 1.0f);
    }

    public int getGeomapClusterRadius() {
        return getLocalInt("maps.cluster.radius", 80);
    }

    public Integer getGeomapDisableClusteringAtZoom() {
        String value = getLocalString("maps.cluster.disableAtZoom", "");
        if (StringTools.isInteger(value)) {
            return Integer.parseInt(value);
        } else {
            return null;
        }
    }

    public MetadataListElement getGeoMapFeatureConfiguration(String option, String template) {
        return getGeomapFeatureConfigurations(option).getOrDefault(template, new Metadata());
    }

    public String getMetadataListForGeomapMarkerConfig(String option) {

        if (StringUtils.isBlank(option)) {
            return "";
        }

        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");

        return options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .findAny()
                .map(config -> config.getString("marker[@metadataList]", ""))
                .orElse("");
    }

    public String getMetadataListForGeomapItemConfig(String option) {

        if (StringUtils.isBlank(option)) {
            return "";
        }

        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");

        return options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .findAny()
                .map(config -> config.getString("item[@metadataList]", ""))
                .orElse("");
    }

    public Map<String, Metadata> getGeomapFeatureConfigurations(String option) {
        if (StringUtils.isBlank(option)) {
            return Collections.emptyMap();
        }

        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");

        return options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .findAny()
                .map(config -> config.getString("marker[@metadataList]", ""))
                .filter(StringUtils::isNotBlank)
                .map(mdListName -> getMetadataTemplates(mdListName))
                .map(map -> map.entrySet()
                        .stream()
                        .filter(e -> !e.getValue().isEmpty())
                        //map to single Metadata list
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0))))
                .orElse(new HashMap<>());
    }

    public Map<String, Metadata> getGeomapItemConfigurations(String option) {
        if (StringUtils.isBlank(option)) {
            return Collections.emptyMap();
        }

        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");

        return options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .findAny()
                .map(config -> config.getString("item[@metadataList]", ""))
                .filter(StringUtils::isNotBlank)
                .map(mdListName -> getMetadataTemplates(mdListName))
                .map(map -> map.entrySet()
                        .stream()
                        .filter(e -> !e.getValue().isEmpty())
                        //map to single Metadata list
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0))))
                .orElse(new HashMap<>());
    }

    /**
     * @return the list of configured geo map feature title options as select items
     */
    public List<SelectItem> getGeomapFeatureTitleOptions() {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("maps.metadata.option");
        if (configs != null && !configs.isEmpty()) {
            return configs.stream()
                    .map(config -> {
                        String value = config.getString("[@name]", null);
                        String label = config.getString("[@label]", value); //NOSONAR specific path
                        return new SelectItem(value, label);
                    })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public View getGeomapDefaultView() {
        double zoom = getLocalFloat("maps.view.zoom", 5f);
        double lng = getLocalFloat("maps.view.center.lng", 11.073397f);
        double lat = getLocalFloat("maps.view.center.lat", 49.451993f);
        return new View(zoom, lng, lat);
    }

    public GeomapItemFilter getGeomapFilter(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return getGeomapFilters().stream().filter(f -> name.equals(f.getName())).findAny().orElse(null);
    }

    public List<GeomapItemFilter> getGeomapFilters() {
        HierarchicalConfiguration<ImmutableNode> filtersConfig = this.getLocalConfigurationAt("maps.filters");
        List<HierarchicalConfiguration<ImmutableNode>> filterConfigs = filtersConfig.configurationsAt("filter");

        List<GeomapItemFilter> filters = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> config : filterConfigs) {
            String name = config.getString("name", "_DEFAULT");
            String label = config.getString("label", name);
            boolean visible = config.getBoolean("[@visible]", true);
            List<LabeledValue> fields = config.configurationsAt("field").stream().map(c -> {
                String field = c.getString(".");
                String fieldLabel = c.getString("[@label]", "");
                String styleClass = c.getString("[@styleClass]", "");
                return new LabeledValue(field, fieldLabel, styleClass);
            })
                    .collect(Collectors.toList());
            filters.add(new GeomapItemFilter(name, label, visible, fields));
        }
        return filters;
    }

    public List<FeatureSetConfiguration> getRecordGeomapFeatureSetConfigs(String templateName) {
        HierarchicalConfiguration<ImmutableNode> template = selectTemplate(getLocalConfigurationsAt("maps.record.template"), templateName, true);
        if (template != null) {
            List<HierarchicalConfiguration<ImmutableNode>> featureSetConfigs = template.configurationsAt("featureSets.featureSet");
            return featureSetConfigs.stream().map(FeatureSetConfiguration::new).collect(Collectors.toList());
        }

        FeatureSetConfiguration config = new FeatureSetConfiguration("docStruct", "MD_TITLE",
                DataManager.getInstance().getConfiguration().getRecordGeomapMarker(templateName), "", "_DEFAULT", "_DEFAULT",
                "");

        return List.of(config);
    }

    private static Map<String, MetadataListElement> loadGeomapLabelConfigurations(List<HierarchicalConfiguration<ImmutableNode>> templateList) {
        if (templateList == null) {
            return Collections.emptyMap();
        }
        Map<String, MetadataListElement> map = new HashMap<>();
        for (HierarchicalConfiguration<ImmutableNode> template : templateList) {
            String name = template.getString("[@name]", "_DEFAULT");
            MetadataListElement md = getMetadataForTemplate(name, templateList, true, false).stream().findAny().orElse(null);
            if (md != null) {
                map.put(name, md);
            }
        }
        return map;
    }

    /**
     * Returns number of elements displayed per paginator page in a table of contents for anchors and groups. Values below 1 disable pagination (all
     * elements are displayed on the single page).
     *
     * @should return correct value
     * @return a int.
     */
    public int getTocAnchorGroupElementsPerPage() {
        return getLocalInt("toc.tocAnchorGroupElementsPerPage", 0);
    }

    /**
     * Return the layout type for TOCs of anchor records. Defaults to 'list'
     *
     * @return the configured TOC anchor layout type; defaults to {@code "list"}
     */
    public String getTocAnchorLayout() {
        return getLocalString("toc.tocAnchorLayout", "list");
    }

    /**
     * Returns a regex such that all download files which filenames fit this regex should not be visible in the downloads widget. If an empty string
     * is returned, all downloads should remain visible
     *
     * @return a regex or an empty string if no downloads should be hidden
     */
    public List<IFilterConfiguration> getAdditionalFilesDisplayFilters() {
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("additional-files");
        if (widgetConfig != null) {
            return widgetConfig.configurationsAt("filter")
                    .stream()
                    .map(conf -> {
                        try {
                            return IFilterConfiguration.fromConfiguration(conf);
                        } catch (ConfigurationException e) {
                            logger.error("Error reading configuration for additionalFilesDisplayFilters ", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        return Collections.emptyList();

    }

    /**
     *
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetCitationCitationRecommendation() {
        return getSidebarWidgetBooleanValue("citation", "citationRecommendation[@enabled]", true);
    }

    /**
     *
     * @return List of available citation style names
     * @should return all configured values
     */
    public List<String> getSidebarWidgetCitationCitationRecommendationStyles() {
        List<String> ret = new ArrayList<>();
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("citation");
        if (widgetConfig != null) {
            for (Object o : widgetConfig.getList("citationRecommendation.styles.style", new ArrayList<>())) {
                ret.add((String) o);
            }
        }

        return ret;
    }

    /**
     *
     * @return the configured citation recommendation source metadata
     * @should return correct configuration
     */
    public Metadata getSidebarWidgetCitationCitationRecommendationSource() {
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("citation");
        if (widgetConfig != null) {
            HierarchicalConfiguration<ImmutableNode> sub = null;
            try {
                sub = widgetConfig.configurationAt("citationRecommendation.source.metadata");
            } catch (IllegalArgumentException e) {
                // no or multiple occurrences
            }
            if (sub != null) {
                return getMetadataFromSubnodeConfig(sub, false, 0);
            }
        }

        return new Metadata();
    }

    /**
     *
     * @return Map containing mappings DOCSTRCT -> citeproc type
     * @should return all configured values
     */
    public Map<String, String> getSidebarWidgetCitationCitationRecommendationDocstructMapping() {
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("citation");
        if (widgetConfig != null) {
            Map<String, String> ret = new HashMap<>();
            widgetConfig.configurationsAt("citationRecommendation.source.csltypes.csltype")
                    .forEach(conf -> ret.put(conf.getString("[@docstrct]"), conf.getString(".")));
            return ret;
        }

        return Collections.emptyMap();
    }

    /**
     *
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetCitationCitationLinks() {
        return getSidebarWidgetBooleanValue("citation", "citationLinks[@enabled]", true);
    }

    /**
     *
     * @return the list of configured citation links for the citation widget
     * @should return all configured values
     */
    public List<CitationLink> getSidebarWidgetCitationCitationLinks() {
        List<CitationLink> ret = new ArrayList<>();
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("citation");
        if (widgetConfig != null) {
            List<HierarchicalConfiguration<ImmutableNode>> links = widgetConfig.configurationsAt("citationLinks.links.link");
            if (links == null || links.isEmpty()) {
                return new ArrayList<>();
            }

            for (HierarchicalConfiguration<ImmutableNode> sub : links) {
                String type = sub.getString(XML_PATH_ATTRIBUTE_TYPE);
                String level = sub.getString("[@for]");
                String label = sub.getString(XML_PATH_ATTRIBUTE_LABEL);
                String field = sub.getString("[@field]");
                String pattern = sub.getString("[@pattern]");
                String action = sub.getString("[@action]", "clipboard");
                boolean topstructValueFallback = sub.getBoolean("[@topstructValueFallback]", false);
                try {
                    ret.add(new CitationLink(type, level, action, label).setField(field)
                            .setPattern(pattern)
                            .setTopstructValueFallback(topstructValueFallback));
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return ret;
    }

    /**
     * Returns a list of configured page download options.
     *
     * @return List of configured <code>DownloadOption</code> items
     * @should return all configured elements
     */
    public List<DownloadOption> getSidebarWidgetDownloadsPageDownloadOptions() {
        List<DownloadOption> ret = new ArrayList<>();
        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("downloads");
        if (widgetConfig != null) {
            List<HierarchicalConfiguration<ImmutableNode>> configs = widgetConfig.configurationsAt("page.downloadOptions.option");
            if (configs != null && !configs.isEmpty()) {
                ret = new ArrayList<>(configs.size());
                for (HierarchicalConfiguration<ImmutableNode> config : configs) {
                    ret.add(new DownloadOption().setLabel(config.getString(XML_PATH_ATTRIBUTE_LABEL))
                            .setFormat(config.getString("[@format]"))
                            .setBoxSizeInPixel(config.getString("[@boxSizeInPixel]")));
                }
            }
        }

        return ret;
    }

    /**
     *
     * @return true if the page download options are enabled in the downloads widget, false otherwise
     * @should return correct value
     */
    public boolean isDisplayWidgetDownloadsDownloadOptions() {
        return getSidebarWidgetBooleanValue("downloads", "page.downloadOptions[@enabled]", true);
    }

    /**
     * 
     * @return true if the PDF page range selector is enabled in the downloads widget, false otherwise
     */
    public boolean isDisplaySidebarWidgetDownloadsPdfPageRange() {
        return getSidebarWidgetBooleanValue("downloads", "pdfPageRange[@enabled]", false);
    }

    /**
     * 
     * @param view Record view name
     * @return List of sidebar widget names to display in the given view (in the intended order)
     * @should return correct values
     * @should fall back to view name prefix if no exact match found
     */
    public List<String> getSidebarWidgetsForView(String view) {
        logger.trace("getSidebarWidgetsForView: {}", view);
        List<String> ret = new ArrayList<>();
        if (StringUtils.isEmpty(view)) {
            return ret;
        }

        HierarchicalConfiguration<ImmutableNode> viewConfig = getSidebarViewConfiguration(view.toLowerCase());
        if (viewConfig != null) {
            for (HierarchicalConfiguration<ImmutableNode> widget : viewConfig.configurationsAt("displayWidget")) {
                ret.add(widget.getString(XML_PATH_ATTRIBUTE_NAME));
            }
        }

        return ret;
    }

    /**
     * 
     * @param view Record view name
     * @param widget Widget name
     * @return true if widget configured as collapsible; false otherwise; default is false
     * @should return correct value
     */
    public boolean isSidebarWidgetForViewCollapsible(String view, String widget) {
        if (StringUtils.isEmpty(view) || StringUtils.isEmpty(widget)) {
            return false;
        }

        HierarchicalConfiguration<ImmutableNode> viewConfig = getSidebarViewConfiguration(view.toLowerCase());
        if (viewConfig != null) {
            for (HierarchicalConfiguration<ImmutableNode> widgetConfig : viewConfig.configurationsAt("displayWidget")) {
                if (widget.equals(widgetConfig.getString(XML_PATH_ATTRIBUTE_NAME))) {
                    return widgetConfig.getBoolean("[@collapsible]", false);
                }
            }
        }

        return false;
    }

    /**
     * 
     * @param view Record view name
     * @param widget Widget name
     * @return true if widget configured as collapsed by default; false otherwise; default is false
     * @should return correct value
     */
    public boolean isSidebarWidgetForViewCollapsedByDefault(String view, String widget) {
        if (StringUtils.isEmpty(view) || StringUtils.isEmpty(widget)) {
            return false;
        }

        HierarchicalConfiguration<ImmutableNode> viewConfig = getSidebarViewConfiguration(view.toLowerCase());
        if (viewConfig != null) {
            for (HierarchicalConfiguration<ImmutableNode> widgetConfig : viewConfig.configurationsAt("displayWidget")) {
                if (widget.equals(widgetConfig.getString(XML_PATH_ATTRIBUTE_NAME))) {
                    return widgetConfig.getBoolean("[@collapsedByDefault]", false);
                }
            }
        }

        return false;
    }

    /**
     * isBrowsingMenuEnabled.
     *
     * @should return correct value
     * @return true if the browsing menu is enabled, false otherwise
     */
    public boolean isBrowsingMenuEnabled() {
        return getLocalBoolean("metadata.browsingMenu[@enabled]", false);
    }

    /**
     * getBrowsingMenuIndexSizeThreshold.
     *
     * @return Solr doc count threshold for browsing term calculation
     * @should return correct value
     */
    public int getBrowsingMenuIndexSizeThreshold() {
        return getLocalInt("metadata.browsingMenu.indexSizeThreshold", 100000);
    }

    /**
     * getBrowsingMenuHitsPerPage.
     *
     * @should return correct value
     * @return a int.
     */
    public int getBrowsingMenuHitsPerPage() {
        return getLocalInt("metadata.browsingMenu.hitsPerPage", 50);
    }

    /**
     * Returns the list of index fields to be used for term browsing.
     *
     * @return a list of configured index fields to be used for term browsing in the browsing menu
     * @should return all configured elements
     */
    public List<BrowsingMenuFieldConfig> getBrowsingMenuFields() {
        List<HierarchicalConfiguration<ImmutableNode>> fields = getLocalConfigurationsAt("metadata.browsingMenu.luceneField");
        if (fields != null && !fields.isEmpty()) {
            logger.warn("Old <luceneField> configuration found - please migrate to <field>.");
        } else {
            fields = getLocalConfigurationsAt("metadata.browsingMenu.field");
        }
        if (fields == null) {
            return new ArrayList<>();
        }

        List<BrowsingMenuFieldConfig> ret = new ArrayList<>(fields.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : fields) {
            String field = sub.getString(".");
            String sortField = sub.getString("[@sortField]");
            String filterQuery = sub.getString("[@filterQuery]");
            boolean translate = sub.getBoolean("[@translate]", false);
            boolean recordsAndAnchorsOnly = sub.getBoolean("[@recordsAndAnchorsOnly]", false);
            boolean alwaysApplyFilter = sub.getBoolean("[@alwaysApplyFilter]", false);
            boolean skipInWidget = sub.getBoolean("[@skipInWidget]", false);
            BrowsingMenuFieldConfig bmfc =
                    new BrowsingMenuFieldConfig(field, sortField, filterQuery)
                            .setTranslate(translate)
                            .setAlwaysApplyFilter(alwaysApplyFilter)
                            .setSkipInWidget(skipInWidget)
                            .setRecordsAndAnchorsOnly(recordsAndAnchorsOnly);
            ret.add(bmfc);
        }

        return ret;
    }

    /**
     *
     * @return the configured leading characters to ignore when sorting the browsing menu
     * @should return correct value
     */
    public String getBrowsingMenuSortingIgnoreLeadingChars() {
        return getLocalString("metadata.browsingMenu.sorting.ignoreLeadingChars");
    }

    /**
     * getDocstrctWhitelistFilterQuery.
     *
     * @should return correct value
     * @return the configured Solr filter query restricting indexed document structure types
     */
    public String getDocstrctWhitelistFilterQuery() {
        return getLocalString("search.docstrctWhitelistFilterQuery", SearchHelper.DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY);
    }

    /**
     * getCollectionSplittingChar.
     *
     * @param field collection Solr field name to look up
     * @should return correct value
     * @return the configured hierarchy splitting character for the given collection field
     */
    public String getCollectionSplittingChar(String field) {
        HierarchicalConfiguration<ImmutableNode> subConfig = getCollectionConfiguration(field);
        if (subConfig != null) {
            return subConfig.getString("splittingCharacter", ".");
        }

        return getLocalString("collections.splittingCharacter", ".");
    }

    /**
     * Returns the config block for the given field.
     *
     * @param field collection Solr field name
     * @return the configuration block for the given collection Solr field, or null if not found
     */
    private HierarchicalConfiguration<ImmutableNode> getCollectionConfiguration(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> collectionList = getLocalConfigurationsAt("collections.collection");
        if (collectionList == null) {
            return null;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : collectionList) {
            if (subElement.getString("[@field]").equals(field)) {
                return subElement;

            }
        }

        return null;
    }

    /**
     * 
     * @return the list of configured collection Solr field names
     */
    public List<String> getConfiguredCollectionFields() {
        List<String> list = getLocalList("collections.collection[@field]");
        if (list == null || list.isEmpty()) {
            return Collections.singletonList("DC");
        }

        return list;
    }

    /**
     * Gets all configured sortOrders for collections in the given field, mapped against a regex matching the collection(s).
     *
     * <p>Whether subcollections should be sorted according to the sortOrder.
     * 
     * @param field the solr fild on which the collection is based
     * @return a map of regular expressions matching collection names and associated sortOrders
     */
    public Map<String, String> getCollectionSortOrders(String field) {

        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection != null) {
            List<HierarchicalConfiguration<ImmutableNode>> sortOrders = collection.configurationsAt("sorting.sortOrder");
            return sortOrders.stream().collect(Collectors.toMap(conf -> conf.getString("[@collections]"), conf -> conf.getString(".")));
        }

        return Collections.emptyMap();
    }

    /**
     * getCollectionSorting.
     *
     * @param field collection Solr field name to look up
     * @return a list of DcSortingList objects defining the configured collection sort order for the given field
     * @should return all configured elements
     */
    public List<DcSortingList> getCollectionSorting(String field) {

        List<DcSortingList> superlist = new ArrayList<>();
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return superlist;
        }

        superlist.add(new DcSortingList(getLocalList("sorting.collection")));
        List<HierarchicalConfiguration<ImmutableNode>> listConfigs = collection.configurationsAt("sorting.sortingList");
        for (HierarchicalConfiguration<ImmutableNode> listConfig : listConfigs) {
            String sortAfter = listConfig.getString("[@sortAfter]", null);
            List<String> collectionList = getLocalList(listConfig, null, "collection", Collections.<String> emptyList());
            superlist.add(new DcSortingList(sortAfter, collectionList));
        }
        return superlist;
    }

    /**
     * Returns collection names to be omitted from search results, listings etc.
     *
     * @param field collection Solr field name to look up
     * @return a list of collection names to be omitted from search results and listings for the given field
     * @should return all configured elements
     */
    public List<String> getCollectionBlacklist(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return new ArrayList<>();
        }
        return getLocalList(collection, null, "blacklist.collection", Collections.<String> emptyList());
    }

    /**
     * Returns the index field by which records in the collection with the given name are to be sorted in a listing.
     *
     * @param field collection Solr field name to look up
     * @return a map of collection name patterns to their configured default sort fields for the given collection field
     * @should return correct field for collection
     * @should give priority to exact matches
     * @should return hyphen if collection not found
     */
    public Map<String, String> getCollectionDefaultSortFields(String field) {
        Map<String, String> map = new HashMap<>();
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return map;
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = collection.configurationsAt("defaultSortFields.field");
        if (fields == null) {
            return map;
        }

        for (HierarchicalConfiguration<ImmutableNode> sub : fields) {
            String key = sub.getString("[@collection]");
            String value = sub.getString("");
            map.put(key, value);
        }
        return map;
    }

    /**
     * getCollectionDisplayNumberOfVolumesLevel.
     *
     * @param field collection Solr field name to look up
     * @should return correct value
     * @return a int.
     */
    public int getCollectionDisplayNumberOfVolumesLevel(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return 0;
        }
        return collection.getInt("displayNumberOfVolumesLevel", 0);
    }

    /**
     * getCollectionDisplayDepthForSearch.
     *
     * @param field collection Solr field name to look up
     * @should return correct value
     * @should return -1 if no collection config was found
     * @return a int.
     */
    public int getCollectionDisplayDepthForSearch(String field) {

        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return -1;
        }
        return collection.getInt("displayDepthForSearch", -1);
    }

    /**
     * getCollectionHierarchyField.
     *
     * @return the first configured collection Solr field for which hierarchy breadcrumbs are enabled, or null if none
     * @should return first field where hierarchy enabled
     */
    public String getCollectionHierarchyField() {

        for (String field : getConfiguredCollections()) {
            if (isAddCollectionHierarchyToBreadcrumbs(field)) {
                return field;
            }
        }

        return null;
    }

    /**
     * isAddCollectionHierarchyToBreadcrumbs.
     *
     * @param field collection Solr field name to look up
     * @should return correct value
     * @should return false if no collection config was found
     * @return true if the collection hierarchy is added to the breadcrumb trail for the given field, false otherwise
     */
    public boolean isAddCollectionHierarchyToBreadcrumbs(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return false;
        }
        return collection.getBoolean("addHierarchyToBreadcrumbs", false);
    }

    /**
     * getSolrUrl.
     *
     * @should return correct value
     * @return the configured Solr base URL without a trailing slash
     */
    public String getSolrUrl() {
        String value = getLocalString("urls.solr", "http://localhost:8089/solr");
        if (value.charAt(value.length() - 1) == '/') {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * getDownloadUrl.
     *
     * @should return correct value
     * @return the configured download base URL with a trailing slash
     * @deprecated because download uri is now built from request in {@link DownloadBean}
     */
    @Deprecated(since = "25.11")
    public String getDownloadUrl() {
        String urlString = getLocalString("urls.download", "http://localhost:8080/viewer/download/");
        if (!urlString.endsWith("/")) {
            urlString = urlString + "/";
        }
        if (!urlString.endsWith("download/")) {
            urlString = urlString + "download/";
        }
        return urlString;
    }

    /**
     * Get the base url of the viewer. This is the url up to the context path. The returned url always ends with a '/'
     * 
     * @return The base viewer url
     */
    public String getViewerBaseUrl() {
        String urlString = getLocalString("urls.base");
        if (urlString == null) {
            urlString = getRestApiUrl().replaceAll("api/v1/?", "");
        } else if (!urlString.endsWith("/")) {
            urlString = urlString + "/";
        }
        return urlString;
    }

    /**
     * getRestApiUrl.
     *
     * @return The url to the viewer REST API as configured in the config_viewer. The url always ends with "/"
     */
    public String getRestApiUrl() {
        String urlString = getLocalString("urls.rest");
        if (urlString == null) {
            urlString = "localhost:8080/default-viewer/rest";
        }

        if (!urlString.endsWith("/")) {
            urlString += "/";
        }

        return urlString;
    }

    /**
     * url to rest api url for record media files. Always ends with a slash
     *
     * @return the configured IIIF API base URL, always ending with a slash
     */
    public String getIIIFApiUrl() {
        String urlString = getLocalString("urls.iiif", getRestApiUrl());
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }

    /**
     * 
     * @return true if the IIIF API URL is used for CMS media file URLs instead of the default URL, false otherwise
     */
    public boolean isUseIIIFApiUrlForCmsMediaUrls() {
        return getLocalBoolean("urls.iiif[@useForCmsMedia]", true);
    }

    /**
     * getSourceFileUrl.
     *
     * @should return correct value
     * @return the configured URL for accessing source (METS/LIDO) metadata files
     */
    public String getSourceFileUrl() {
        return getLocalString("urls.metadata.sourcefile");
    }

    /**
     * getMarcUrl.
     *
     * @should return correct value
     * @return the configured URL for accessing MARC metadata records
     */
    public String getMarcUrl() {
        return getLocalString("urls.metadata.marc");
    }

    /**
     * getDcUrl.
     *
     * @should return correct value
     * @return the configured URL for accessing Dublin Core metadata records
     */
    public String getDcUrl() {
        return getLocalString("urls.metadata.dc");
    }

    /**
     * getEseUrl.
     *
     * @should return correct value
     * @return the configured URL for accessing ESE metadata records
     */
    public String getEseUrl() {
        return getLocalString("urls.metadata.ese");
    }

    /**
     * getSearchHitsPerPageValues.
     *
     * @should return all values
     * @return List of configured values
     */
    public List<Integer> getSearchHitsPerPageValues() {
        List<String> values = getLocalList("search.hitsPerPage.value");
        if (values.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> ret = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                ret.add(Integer.valueOf(value));
            } catch (NumberFormatException e) {
                logger.error("Configured hits per page value not a number: {}", value);
            }
        }

        return ret;
    }

    /**
     * 
     * @return true if search hit numbers are displayed next to each result, false otherwise
     * @should return correct value
     */
    public boolean isDisplaySearchHitNumbers() {
        return getLocalBoolean("search.displayHitNumbers[@enabled]", false);
    }

    public int getSearchChildHitsInitialLoadLimit() {
        return getLocalInt("search.childHits.initialLoadLimit", 5);
    }

    public int getSearchChildHitsToLoadOnExpand() {
        return getLocalInt("search.childHits.loadOnExpand", 20);
    }

    /**
     * getSearchHitsPerPageDefaultValue.
     *
     * @should return correct value
     * @return value element that is marked as default value; 10 if none found
     */
    public int getSearchHitsPerPageDefaultValue() {
        List<HierarchicalConfiguration<ImmutableNode>> values = getLocalConfigurationsAt("search.hitsPerPage.value");
        if (values.isEmpty()) {
            return 10;
        }
        for (HierarchicalConfiguration<ImmutableNode> sub : values) {
            if (sub.getBoolean(XML_PATH_ATTRIBUTE_DEFAULT, false)) {
                return sub.getInt(".");
            }
        }

        return 10;
    }

    /**
     * getFulltextFragmentLength.
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextFragmentLength() {
        return getLocalInt("search.fulltextFragmentLength", 200);
    }

    /**
     * isAdvancedSearchEnabled.
     *
     * @should return correct value
     * @return true if the advanced search is enabled, false otherwise
     */
    public boolean isAdvancedSearchEnabled() {
        return getLocalBoolean("search.advanced[@enabled]", true);
    }

    /**
     * 
     * @return List of configured template names
     * @should return all values
     */
    public List<String> getAdvancedSearchTemplateNames() {
        return getLocalList(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE + "[@name]", Collections.emptyList());
    }

    /**
     * 
     * @return _DEFAULT or the name of the first template in the list
     */
    public String getAdvancedSearchDefaultTemplateName() {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null || templateList.isEmpty()) {
            logger.error("No advanced search template configurations found.");
            return StringConstants.DEFAULT_NAME;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : templateList) {
            String name = subElement.getString(XML_PATH_ATTRIBUTE_NAME);
            if (StringConstants.DEFAULT_NAME.equals(name)) {
                logger.trace("Found _DEFAULT template.");
                return name;
            }
        }

        String firstTemplateName = templateList.get(0).getString(XML_PATH_ATTRIBUTE_NAME);
        if (StringUtils.isNotEmpty(firstTemplateName)) {
            logger.trace("Returning first template name: {}", firstTemplateName);
            return firstTemplateName;
        }

        return StringConstants.DEFAULT_NAME;
    }

    /**
     * 
     * @param template advanced search template name to look up
     * @return Value of the query attribute; empty string if none found
     * @should return correct value
     */
    public String getAdvancedSearchTemplateQuery(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, false);
        if (usingTemplate == null) {
            return null;
        }

        return usingTemplate.getString("[@query]", "");
    }

    /**
     * getAdvancedSearchFields.
     *
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @param language language code used to filter language-specific fields
     * @return a list of configured advanced search field configurations for the given template and language
     * @should return all values
     * @should return skip fields that don't match given language
     */
    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchFields(String template, boolean fallbackToDefaultTemplate, String language) {
        // logger.trace("getAdvancedSearchFields({},{})", template, fallbackToDefaultTemplate); //NOSONAR Debug
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return new ArrayList<>();
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return new ArrayList<>();
        }
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = usingTemplate.configurationsAt("field");
        if (fieldList == null) {
            return new ArrayList<>();
        }

        List<AdvancedSearchFieldConfiguration> ret = new ArrayList<>(fieldList.size());
        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            String field = subElement.getString(".");

            if (StringUtils.isEmpty(field)) {
                logger.warn("No advanced search field name defined, skipping.");
                continue;
            } else if (isLanguageVersionOtherThan(field, language != null ? language : "en")) {
                // logger.trace("Field {} belongs to different language; skipping", field); //NOSONAR Debug
                continue;
            }
            String label = subElement.getString(XML_PATH_ATTRIBUTE_LABEL, field);
            boolean hierarchical = subElement.getBoolean("[@hierarchical]", false);
            boolean range = subElement.getBoolean("[@range]", false);
            boolean datepicker = subElement.getBoolean("[@datepicker]", false);
            boolean untokenizeForPhraseSearch = subElement.getBoolean("[@untokenizeForPhraseSearch]", false);
            boolean visible = subElement.getBoolean("[@visible]", false);
            boolean allowMultipleItems = subElement.getBoolean("[@allowMultipleItems]", false);
            int displaySelectItemsThreshold = subElement.getInt("[@displaySelectItemsThreshold]", 50);
            String selectType = subElement.getString("[@selectType]", AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN);
            String replaceRegex = subElement.getString("[@replaceRegex]");
            String replaceWith = subElement.getString("[@replaceWith]");
            String preselectValue = subElement.getString("[@preselectValue]");

            ret.add(new AdvancedSearchFieldConfiguration(field)
                    .setLabel(label)
                    .setHierarchical(hierarchical)
                    .setRange(range)
                    .setDatepicker(datepicker)
                    .setUntokenizeForPhraseSearch(untokenizeForPhraseSearch)
                    .setDisabled(field.charAt(0) == '#' && field.charAt(field.length() - 1) == '#')
                    .setVisible(visible)
                    .setAllowMultipleItems(allowMultipleItems)
                    .setDisplaySelectItemsThreshold(displaySelectItemsThreshold)
                    .setSelectType(selectType)
                    .setReplaceRegex(replaceRegex)
                    .setReplaceWith(replaceWith)
                    .setPreselectValue(preselectValue));
        }

        return ret;
    }

    /**
     * isDisplayAdditionalMetadataEnabled.
     *
     * @should return correct value
     * @return true if displaying additional metadata in search hits is enabled, false otherwise
     */
    public boolean isDisplayAdditionalMetadataEnabled() {
        return getLocalBoolean("search.displayAdditionalMetadata[@enabled]", true);
    }

    /**
     * getDisplayAdditionalMetadataIgnoreFields.
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataIgnoreFields() {
        return getDisplayAdditionalMetadataFieldsByType("ignore", false);
    }

    /**
     * Returns a list of additional metadata fields thats are configured to have their values translated. Field names are normalized (i.e. things like
     * _UNTOKENIZED are removed).
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataTranslateFields() {
        return getDisplayAdditionalMetadataFieldsByType("translate", true);
    }

    /**
     * getDisplayAdditionalMetadataIgnoreFields.
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataOnelineFields() {
        return getDisplayAdditionalMetadataFieldsByType("oneline", false);
    }

    /**
     * getDisplayAdditionalMetadataSnippetFields.
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataSnippetFields() {
        return getDisplayAdditionalMetadataFieldsByType("snippet", false);
    }

    /**
     * getDisplayAdditionalMetadataNoHighlightFields.
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataNoHighlightFields() {
        return getDisplayAdditionalMetadataFieldsByType("nohighlight", false);
    }

    /**
     *
     * @param type Value of the type attribute
     * @param normalize If true; field will be normalized
     * @return List of <field> elements filtered by type
     */
    List<String> getDisplayAdditionalMetadataFieldsByType(String type, boolean normalize) {
        List<HierarchicalConfiguration<ImmutableNode>> fields = getLocalConfigurationsAt("search.displayAdditionalMetadata.field");
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (fields == null || fields.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> ret = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> node : fields) {
            if (!type.equals(node.getString(XML_PATH_ATTRIBUTE_TYPE))) {
                continue;
            }
            String value = node.getString(".");
            if (StringUtils.isNotEmpty(value)) {
                if (normalize) {
                    value = SearchHelper.normalizeField(value);
                }
                ret.add(value);
            }
        }

        return ret;
    }

    /**
     * isAdvancedSearchFieldHierarchical.
     *
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the given advanced search field is configured as hierarchical, false otherwise
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldHierarchical(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "hierarchical", template, fallbackToDefaultTemplate);
    }

    /**
     * isAdvancedSearchFieldRange.
     *
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the given advanced search field is configured as a range field, false otherwise
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldRange(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "range", template, fallbackToDefaultTemplate);
    }

    /**
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the given advanced search field is configured to use a date picker, false otherwise
     */
    public boolean isAdvancedSearchFieldDatepicker(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "datepicker", template, fallbackToDefaultTemplate);
    }

    /**
     * isAdvancedSearchFieldAllowMultipleItems.
     *
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the advanced search field is configured to allow multiple items, false otherwise
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldAllowMultipleItems(String field, String template, boolean fallbackToDefaultTemplate) {
        // logger.trace("isAdvancedSearchFieldAllowMultipleItems: {}/{}/{}", field, template, fallbackToDefaultTemplate);
        return isAdvancedSearchFieldHasAttribute(field, "allowMultipleItems", template, fallbackToDefaultTemplate);
    }

    /**
     * isAdvancedSearchFieldUntokenizeForPhraseSearch.
     *
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the advanced search field is configured to untokenize values for phrase search, false otherwise
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldUntokenizeForPhraseSearch(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "untokenizeForPhraseSearch", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param field advanced search field name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured threshold for displaying select items in the advanced search field for the given field and template
     * @should return correct value
     */
    public int getAdvancedSearchFieldDisplaySelectItemsThreshold(String field, String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return AdvancedSearchFieldConfiguration.DEFAULT_THRESHOLD;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return AdvancedSearchFieldConfiguration.DEFAULT_THRESHOLD;
        }
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = usingTemplate.configurationsAt("field");
        if (fieldList == null) {
            return AdvancedSearchFieldConfiguration.DEFAULT_THRESHOLD;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            if (subElement.getString(".").equals(field)) {
                return subElement.getInt("[@displaySelectItemsThreshold]", AdvancedSearchFieldConfiguration.DEFAULT_THRESHOLD);
            }
        }

        return AdvancedSearchFieldConfiguration.DEFAULT_THRESHOLD;
    }

    /**
     *
     * @param field advanced search field name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured select type for the advanced search field, defaulting to dropdown if not set
     * @should return correct value
     */
    public String getAdvancedSearchFieldSelectType(String field, String template, boolean fallbackToDefaultTemplate) {
        String ret = getAdvancedSearchFieldGetAttributeValue(field, "selectType", template, fallbackToDefaultTemplate);
        if (ret == null) {
            ret = AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN;
        }

        return ret;
    }

    /**
     * isAdvancedSearchFieldHierarchical.
     *
     * @param field advanced search field name to check
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return Label attribute value for the given field name
     * @should return correct value
     */
    public String getAdvancedSearchFieldSeparatorLabel(String field, String template, boolean fallbackToDefaultTemplate) {
        return getAdvancedSearchFieldGetAttributeValue(field, "label", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param field advanced search field name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured regex pattern to replace in the advanced search field value for the given field and template, or null if not configured
     * @should return correct value
     */
    public String getAdvancedSearchFieldReplaceRegex(String field, String template, boolean fallbackToDefaultTemplate) {
        return getAdvancedSearchFieldGetAttributeValue(field, "replaceRegex", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param field advanced search field name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured replacement string for the advanced search field value for the given field and template, or null if not configured
     * @should return correct value
     */
    public String getAdvancedSearchFieldReplaceWith(String field, String template, boolean fallbackToDefaultTemplate) {
        return getAdvancedSearchFieldGetAttributeValue(field, "replaceWith", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param field advanced search field name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured default boolean operator for the given advanced search field and template, or null if not configured
     * @should return correct value
     */
    public String getAdvancedSearchFieldDefaultOperator(String field, String template, boolean fallbackToDefaultTemplate) {
        return getAdvancedSearchFieldGetAttributeValue(field, "defaultOperator", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param template advanced search template name to look up
     * @return the configured default operator for the first line of the given advanced search template, or null if not configured
     * @should return correct value
     */
    public String getAdvancedSearchTemplateFirstLineDefaultOperator(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, false);
        if (usingTemplate == null) {
            return null;
        }

        return usingTemplate.getString("[@firstLineDefaultOperator]");
    }

    /**
     *
     * @param field advanced search field name
     * @param attribute XML attribute name to read
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the configured attribute value for the given advanced search field and attribute, or null if not configured
     */
    String getAdvancedSearchFieldGetAttributeValue(String field, String attribute, String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return null;
        }
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = usingTemplate.configurationsAt("field");
        if (fieldList == null) {
            return null;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            if (subElement.getString(".").equals(field)) {
                return subElement.getString("[@" + attribute + "]");
            }
        }

        return null;
    }

    /**
     *
     * @param field Advanced search field name
     * @param attribute Attribute name
     * @param template advanced search template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return true if the given advanced search field has the specified attribute configured, false otherwise
     */
    boolean isAdvancedSearchFieldHasAttribute(String field, String attribute, String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return false;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return false;
        }
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = usingTemplate.configurationsAt("field");
        if (fieldList == null) {
            return false;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            if (subElement.getString(".").equals(field)) {
                return subElement.getBoolean("[@" + attribute + "]", false);
            }
        }

        return false;
    }

    /**
     * isTimelineSearchEnabled.
     *
     * @should return correct value
     * @return true if the timeline search is enabled, false otherwise
     */
    public boolean isTimelineSearchEnabled() {
        return getLocalBoolean("search.timeline[@enabled]", true);
    }

    /**
     * isCalendarSearchEnabled.
     *
     * @should return correct value
     * @return true if the calendar search is enabled, false otherwise
     */
    public boolean isCalendarSearchEnabled() {
        return getLocalBoolean("search.calendar[@enabled]", true);
    }

    /**
     * getStaticQuerySuffix.
     *
     * @should return correct value
     * @return the configured static Solr query suffix appended to all search queries
     */
    public String getStaticQuerySuffix() {
        return getLocalString("search.staticQuerySuffix");
    }

    /**
     * getPreviousVersionIdentifierField.
     *
     * @should return correct value
     * @return the configured Solr field name holding the previous version identifier
     */
    public String getPreviousVersionIdentifierField() {
        return getLocalString("search.versioning.previousVersionIdentifierField");
    }

    /**
     * getNextVersionIdentifierField.
     *
     * @should return correct value
     * @return the configured Solr field name holding the next version identifier
     */
    public String getNextVersionIdentifierField() {
        return getLocalString("search.versioning.nextVersionIdentifierField");
    }

    /**
     * getVersionLabelField.
     *
     * @should return correct value
     * @return the configured Solr field name holding the version label
     */
    public String getVersionLabelField() {
        return getLocalString("search.versioning.versionLabelField");
    }

    /**
     * getIndexedMetsFolder.
     *
     * @should return correct value
     * @return the configured folder name for indexed METS files
     */
    public String getIndexedMetsFolder() {
        return getLocalString("indexedMetsFolder", "indexed_mets");
    }

    /**
     * getIndexedLidoFolder.
     *
     * @should return correct value
     * @return the configured folder name for indexed LIDO files
     */
    public String getIndexedLidoFolder() {
        return getLocalString("indexedLidoFolder", "indexed_lido");
    }

    /**
     * getIndexedEadFolder.
     *
     * @should return correct value
     * @return the configured folder name for indexed EAD files
     */
    public String getIndexedEadFolder() {
        return getLocalString("indexedEadFolder", "indexed_ead");
    }

    /**
     * getIndexedDenkxwebFolder.
     *
     * @should return correct value
     * @return the configured folder name for indexed DenkXweb files
     */
    public String getIndexedDenkxwebFolder() {
        return getLocalString("indexedDenkxwebFolder", "indexed_denkxweb");
    }

    /**
     * getIndexedDublinCoreFolder.
     *
     * @should return correct value
     * @return the configured folder name for indexed Dublin Core files
     */
    public String getIndexedDublinCoreFolder() {
        return getLocalString("indexedDublinCoreFolder", "indexed_dublincore");
    }

    /**
     * getPageSelectionFormat.
     *
     * @should return correct value
     * @return the configured format string for page selection display
     */
    public String getPageSelectionFormat() {
        return getLocalString("viewer.pageSelectionFormat", "{pageno}:{pagenolabel}");
    }

    /**
     * getMediaFolder.
     *
     * @should return correct value
     * @return the configured path to the media folder
     */
    public String getMediaFolder() {
        return getLocalString("mediaFolder");
    }

    /**
     * getPdfFolder.
     *
     * @should return correct value
     * @return the configured folder name for generated PDF files
     */
    public String getPdfFolder() {
        return getLocalString("pdfFolder", "pdf");
    }

    /**
     * getVocabulariesFolder.
     *
     * @return the configured folder name for vocabulary files
     */
    public String getVocabulariesFolder() {
        return getLocalString("vocabularies", "vocabularies");
    }

    /**
     * getOrigContentFolder.
     *
     * @should return correct value
     * @return the configured folder name for original source content files
     */
    public String getOrigContentFolder() {
        return getLocalString("origContentFolder", "source");
    }

    /**
     * getCmsMediaFolder.
     *
     * @should return correct value
     * @return the configured folder name for CMS media files
     */
    public String getCmsMediaFolder() {
        return getLocalString("cmsMediaFolder", "cms_media");
    }

    /**
     * getCmsTextFolder.
     *
     * @should return correct value
     * @return the configured folder name for CMS text content files
     */
    public String getCmsTextFolder() {
        return getLocalString("cmsTextFolder");
    }

    /**
     * getAltoFolder.
     *
     * @should return correct value
     * @return the configured folder name for ALTO OCR files
     */
    public String getAltoFolder() {
        return getLocalString("altoFolder", "alto");
    }

    /**
     * getAltoCrowdsourcingFolder.
     *
     * @should return correct value
     * @return the configured folder name for crowdsourced ALTO OCR files
     */
    public String getAltoCrowdsourcingFolder() {
        return getLocalString("altoCrowdsourcingFolder", "alto_crowd");
    }

    /**
     * getAbbyyFolder.
     *
     * @should return correct value
     * @return the configured folder name for ABBYY recognition result files
     */
    public String getAbbyyFolder() {
        return getLocalString("abbyyFolder", "abbyy");
    }

    /**
     * getFulltextFolder.
     *
     * @should return correct value
     * @return the configured folder name for plain full-text files
     */
    public String getFulltextFolder() {
        return getLocalString("fulltextFolder", "fulltext");
    }

    /**
     * getFulltextCrowdsourcingFolder.
     *
     * @should return correct value
     * @return the configured folder name for crowdsourced plain full-text files
     */
    public String getFulltextCrowdsourcingFolder() {
        return getLocalString("fulltextCrowdsourcingFolder", "fulltext_crowd");
    }

    /**
     * getTeiFolder.
     *
     * @should return correct value
     * @return the configured folder name for TEI document files
     */
    public String getTeiFolder() {
        return getLocalString("teiFolder", "tei");
    }

    /**
     * getCmdiFolder.
     *
     * @should return correct value
     * @return the configured folder name for CMDI metadata files
     */
    public String getCmdiFolder() {
        return getLocalString("cmdiFolder", "cmdi");
    }

    /**
     * getAnnotationFolder.
     *
     * @should return correct value
     * @return the configured folder name for annotation files
     */
    public String getAnnotationFolder() {
        return getLocalString("annotationFolder");
    }

    /**
     * getHotfolder.
     *
     * @should return correct value
     * @return the configured path to the Goobi indexer hotfolder
     */
    public String getHotfolder() {
        return getLocalString("hotfolder");
    }

    /**
     * getTempFolder.
     *
     * @should return correct value
     * @return the path to the temporary viewer working directory within the system temp directory
     */
    public String getTempFolder() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "viewer").toString();
    }

    /**
     * 
     * @return the list of configured Solr field names used for URN resolver lookups
     * @should return all configured elements
     */
    public List<String> getUrnResolverFields() {
        return getLocalList("urnresolver.field", Collections.singletonList(SolrConstants.URN));
    }

    /**
     * isUrnDoRedirect.
     *
     * @should return correct value
     * @return true if URN resolver should redirect instead of forward, false otherwise
     */
    public boolean isUrnDoRedirect() {
        return getLocalBoolean("urnresolver.doRedirectInsteadofForward", false);
    }

    /**
     * isUserRegistrationEnabled.
     *
     * @should return correct value
     * @return true if user self-registration is enabled, false otherwise
     */
    public boolean isUserRegistrationEnabled() {
        return getLocalBoolean("user.registration[@enabled]", true);
    }

    /**
     *
     * @return the list of configured security questions for user registration
     * @should return all configured elements
     */
    public List<SecurityQuestion> getSecurityQuestions() {
        List<HierarchicalConfiguration<ImmutableNode>> nodes = getLocalConfigurationsAt("user.securityQuestions.question");
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<SecurityQuestion> ret = new ArrayList<>(nodes.size());
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            String questionKey = node.getString("[@key]");
            if (StringUtils.isEmpty(questionKey)) {
                logger.warn("Security question key not found, skipping...");
                continue;
            }
            List<Object> answerNodes = node.getList("allowedAnswer", new ArrayList<>());
            if (answerNodes.isEmpty()) {
                logger.warn("Security question '{}' has no configured answers, skipping...", questionKey);
                continue;
            }
            Set<String> allowedAnswers = HashSet.newHashSet(answerNodes.size());
            for (Object answer : answerNodes) {
                allowedAnswers.add(((String) answer).toLowerCase());
            }
            ret.add(new SecurityQuestion(questionKey, allowedAnswers));
        }

        return ret;
    }

    /**
     * isShowOpenIdConnect.
     *
     * @should return correct value
     * @return true if at least one OpenID Connect authentication provider is configured and enabled, false otherwise
     */
    public boolean isShowOpenIdConnect() {
        return getAuthenticationProviders().stream().anyMatch(provider -> OpenIdProvider.TYPE_OPENID.equalsIgnoreCase(provider.getType()));
    }

    /**
     * getAuthenticationProviders.
     *
     * @should return all properly configured elements
     * @should load user group names correctly
     * @return a list of all configured authentication providers
     */
    public List<IAuthenticationProvider> getAuthenticationProviders() {
        XMLConfiguration myConfigToUse = getConfig();
        // User local config, if available
        if (!getConfigLocal().configurationsAt("user.authenticationProviders").isEmpty()) {
            myConfigToUse = getConfigLocal();
        }

        int max = myConfigToUse.getMaxIndex("user.authenticationProviders.provider");
        List<IAuthenticationProvider> providers = new ArrayList<>(max + 1);
        for (int i = 0; i <= max; i++) {
            String label = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")" + XML_PATH_ATTRIBUTE_LABEL);
            String name = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@name]");
            String type = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@type]", "");
            String endpoint = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@endpoint]", null);
            String tokenEndpoint = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tokenEndpoint]", null);
            String jwksUri = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@jwksUri]", null);
            String redirectionEndpoint = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@redirectionEndpoint]", null);
            String scope = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@scope]", null);
            String responseType = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@responseType]", "code");
            String responseMode = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@responseMode]");
            String image = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@image]", null);
            boolean enabled = myConfigToUse.getBoolean(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@enabled]", true);
            String discoveryUri = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@discoveryUri]");
            String clientId = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@clientId]", null);
            String clientSecret = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@clientSecret]", null);
            String parameterType = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@parameterType]", null);
            String parameterName = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@parameterName]", null);
            String issuer = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@issuer]");
            long tokenCheckDelay = myConfigToUse.getLong(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tokenCheckDelay]", 0);
            String thirdPartyLoginUrl = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tPLoginUrl]", null);
            String thirdPartyLoginApiKey = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tPLoginApiKey]", null);
            String thirdPartyLoginScope = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tPLoginScope]", null);
            String thirdPartyLoginReqParamDef = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tPLoginReqParamDef]", null);
            String thirdPartyLoginClaim = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@tPLoginClaim]", null);
            long timeoutMillis = myConfigToUse.getLong(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@timeout]", 60000);

            if (enabled) {
                IAuthenticationProvider provider = null;
                switch (type.toLowerCase()) {
                    case "httpheader":
                        providers.add(new HttpHeaderProvider(name, label, endpoint, image, timeoutMillis, parameterType, parameterName));
                        break;
                    case "openid":
                        providers.add(
                                new OpenIdProvider(name, label, endpoint, image, timeoutMillis, clientId, clientSecret)
                                        .setDiscoveryUri(discoveryUri)
                                        .setTokenEndpoint(tokenEndpoint)
                                        .setRedirectionEndpoint(redirectionEndpoint)
                                        .setJwksUri(jwksUri)
                                        .setScope(scope)
                                        .setResponseType(responseType)
                                        .setResponseMode(responseMode)
                                        .setIssuer(issuer)
                                        .setTokenCheckDelay(tokenCheckDelay)
                                        .setThirdPartyVariables(thirdPartyLoginUrl, thirdPartyLoginApiKey, thirdPartyLoginScope,
                                                thirdPartyLoginReqParamDef, thirdPartyLoginClaim));
                        break;
                    case "userpassword":
                        switch (name.toLowerCase()) {
                            case "vufind":
                                provider = new VuFindProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "x-service":
                            case "xservice":
                                provider = new XServiceProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "littera":
                                provider = new LitteraProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "bibliotheca":
                                provider = new BibliothecaProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            default:
                                logger.error("Cannot add userpassword authentification provider with name {}. No implementation found", name);
                        }
                        break;
                    case "local":
                        provider = new LocalAuthenticationProvider(name);
                        break;
                    default:
                        logger.error("Cannot add authentification provider with name '{}' and type '{}'. No implementation found", name, type);
                }
                if (provider != null) {
                    // Look for user group configurations to which users shall be automatically added when logging in
                    List<String> addToUserGroupList =
                            getLocalList(myConfigToUse, null, XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ").addUserToGroup", null);
                    if (addToUserGroupList != null) {
                        provider.setAddUserToGroups(addToUserGroupList);
                        // logger.trace("{}: add to group: {}", provider.getName(), addToUserGroupList.toString()); //NOSONAR Debug
                    }
                    providers.add(provider);
                }
            }
        }
        return providers;
    }

    /**
     * getSmtpServer.
     *
     * @should return correct value
     * @return the configured SMTP server hostname or IP address
     */
    public String getSmtpServer() {
        return getLocalString("user.smtpServer");
    }

    /**
     * getSmtpUser.
     *
     * @should return correct value
     * @return the configured SMTP authentication username
     */
    public String getSmtpUser() {
        return getLocalString("user.smtpUser");
    }

    /**
     * getSmtpPassword.
     *
     * @should return correct value
     * @return the configured SMTP authentication password
     */
    public String getSmtpPassword() {
        return getLocalString("user.smtpPassword");
    }

    /**
     * getSmtpSenderAddress.
     *
     * @should return correct value
     * @return the configured SMTP sender email address
     */
    public String getSmtpSenderAddress() {
        return getLocalString("user.smtpSenderAddress");
    }

    /**
     * getSmtpSenderName.
     *
     * @should return correct value
     * @return the configured SMTP sender display name
     */
    public String getSmtpSenderName() {
        return getLocalString("user.smtpSenderName");
    }

    /**
     * getSmtpSecurity.
     *
     * @should return correct value
     * @return the configured SMTP connection security mode (e.g. none, ssl, tls)
     */
    public String getSmtpSecurity() {
        return getLocalString("user.smtpSecurity", "none");
    }

    /**
     *
     * @return Configured SMTP port number; -1 if not configured
     * @should return correct value
     */
    public int getSmtpPort() {
        return getLocalInt("user.smtpPort", -1);
    }

    /**
     * isDisplayCollectionBrowsing.
     *
     * @should return correct value
     * @return true if the collection browsing navigation is enabled, false otherwise
     */
    public boolean isDisplayCollectionBrowsing() {
        return this.getLocalBoolean("webGuiDisplay.collectionBrowsing", true);
    }

    /**
     * isDisplayUserNavigation.
     *
     * @should return correct value
     * @return true if the user account navigation is enabled, false otherwise
     */
    public boolean isDisplayUserNavigation() {
        return this.getLocalBoolean("webGuiDisplay.userAccountNavigation", true);
    }

    /**
     * isDisplayTagCloudNavigation.
     *
     * @should return correct value
     * @return true if the tag cloud navigation is enabled, false otherwise
     */
    public boolean isDisplayTagCloudNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudNavigation", true);
    }

    /**
     * isDisplayStatistics.
     *
     * @should return correct value
     * @return true if the statistics display is enabled, false otherwise
     */
    public boolean isDisplayStatistics() {
        return this.getLocalBoolean("webGuiDisplay.displayStatistics", true);
    }

    /**
     * isDisplayTimeMatrix.
     *
     * @should return correct value
     * @return true if the time matrix display is enabled, false otherwise
     */
    public boolean isDisplayTimeMatrix() {
        return this.getLocalBoolean("webGuiDisplay.displayTimeMatrix", false);
    }

    /**
     * isDisplayCrowdsourcingModuleLinks.
     *
     * @should return correct value
     * @return true if links to the crowdsourcing module are displayed, false otherwise
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return this.getLocalBoolean("webGuiDisplay.displayCrowdsourcingModuleLinks", false);
    }

    /**
     * getTheme.
     *
     * @should return correct value
     * @return the configured main theme name
     */
    public String getTheme() {
        return getSubthemeMainTheme();
    }

    /**
     * getThemeRootPath.
     *
     * @return the configured root path for theme resources
     */
    public String getThemeRootPath() {
        return getLocalString("viewer.theme.rootPath");
    }

    /**
     * getName.
     *
     * @should return correct value
     * @return the configured viewer application name
     */
    public String getName() {
        return getLocalString("viewer.name", "Goobi viewer");
    }

    /**
     * getDescription.
     *
     * @should return correct value
     * @return the configured viewer application description
     */
    public String getDescription() {
        return getLocalString("viewer.description", "Goobi viewer");
    }

    /**
     *
     * @should return correct value
     * @return true if the tag cloud is displayed on the start page, false otherwise
     */
    public boolean isDisplayTagCloudStartpage() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudStartpage", true);
    }

    /**
     * isDisplaySearchResultNavigation.
     *
     * @should return correct value
     * @return true if the search result navigation is displayed, false otherwise
     */
    public boolean isDisplaySearchResultNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displaySearchResultNavigation", true);
    }

    /**
     * Returns the config block for the given path and name attribute value.
     *
     * @param path XPath expression for the config elements
     * @param name value of the name attribute to match
     * @param globalFallback If true, search in global config if desired name not found in local
     * @return HierarchicalConfiguration<ImmutableNode>; null if none found
     */
    private HierarchicalConfiguration<ImmutableNode> getSubConfigurationByNameAttribute(String path, String name, boolean globalFallback) {
        List<HierarchicalConfiguration<ImmutableNode>> allConfigs = new ArrayList<>();

        // Local lists
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt(path);
        if (configs != null) {
            allConfigs.addAll(configs);
        }
        // Global lists
        if (globalFallback) {
            configs = getLocalConfigurationsAt(getConfig(), null, path);
            if (configs != null) {
                allConfigs.addAll(configs);
            }
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : allConfigs) {
            if (subElement.getString(XML_PATH_ATTRIBUTE_NAME, "").equals(name)) {
                return subElement;

            }
        }

        return null;
    }

    /**
     * Returns the sidebar view configuration for the given view name.
     *
     * <p>If no exact match is found, falls back to the prefix before the last underscore
     * (e.g. "metadata_codicological" → "metadata"), allowing dynamically created metadata
     * subpages to inherit the sidebar configuration of their base view.
     *
     * @param name View name
     * @return HierarchicalConfiguration or null if neither the name nor any prefix matches
     */
    private HierarchicalConfiguration<ImmutableNode> getSidebarViewConfiguration(String name) {
        HierarchicalConfiguration<ImmutableNode> config = getSubConfigurationByNameAttribute("sidebar.views.view", name, true);
        if (config != null) {
            return config;
        }
        // Fall back to base view name by stripping suffix after last underscore
        int underscoreIndex = name.lastIndexOf('_');
        if (underscoreIndex > 0) {
            return getSidebarViewConfiguration(name.substring(0, underscoreIndex));
        }
        return null;
    }

    /**
     *
     * @param name sidebar widget field name
     * @return the configuration node for the given sidebar widget, or null if not found
     */
    private HierarchicalConfiguration<ImmutableNode> getSidebarWidgetConfiguration(String name) {
        return getSubConfigurationByNameAttribute("sidebar.widgets.widget", name, true);
    }

    /**
     * 
     * @param widgetName Widget name
     * @param valuePath Path to the wanted value
     * @param defaultValue value to return if none configured
     * @return the configured boolean value for the given sidebar widget attribute path, or {@code defaultValue} if the widget is not found
     */
    private boolean getSidebarWidgetBooleanValue(String widgetName, String valuePath, boolean defaultValue) {
        HierarchicalConfiguration<ImmutableNode> widget = getSidebarWidgetConfiguration(widgetName);
        if (widget != null) {
            return widget.getBoolean(valuePath, defaultValue);
        }

        return defaultValue;
    }

    /**
     * 
     * @param widgetName Widget name
     * @param valuePath Path to the wanted value
     * @param defaultValue value to return if none configured
     * @return the configured integer value for the given sidebar widget attribute path, or {@code defaultValue} if the widget is not found
     */
    private int getSidebarWidgetIntValue(String widgetName, String valuePath, int defaultValue) {
        HierarchicalConfiguration<ImmutableNode> widget = getSidebarWidgetConfiguration(widgetName);
        if (widget != null) {
            return widget.getInteger(valuePath, defaultValue);
        }

        return defaultValue;
    }

    /**
     * 
     * @param widgetName Widget name
     * @param valuePath Path to the wanted value
     * @param defaultValue value to return if none configured
     * @return a {@link String}
     */
    private String getSidebarWidgetStringValue(String widgetName, String valuePath, String defaultValue) {
        HierarchicalConfiguration<ImmutableNode> widget = getSidebarWidgetConfiguration(widgetName);
        if (widget != null) {
            return widget.getString(valuePath, defaultValue);
        }

        return defaultValue;
    }

    /**
     * isFoldout.
     *
     * @param sidebarElement sidebar widget name to check
     * @return true if the given sidebar element is configured as foldout, false otherwise
     */
    public boolean isFoldout(String sidebarElement) {
        return getLocalBoolean("sidebar." + sidebarElement + ".foldout", false);
    }

    /**
     * isSidebarViewsWidgetObjectViewLinkVisible.
     *
     * @should return correct value
     * @return true if the object view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetObjectViewLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "object[@enabled]", true);
    }

    /**
     * Checks whether the TOC <strong>link</strong> in the sidebar views widget is enabled. To check whether the sidebar TOC
     * <strong>widget</strong> is enabled, use <code>isSidebarTocVisible()</code>.
     *
     * @should return correct value
     * @return true if the TOC view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetTocViewLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "toc[@enabled]", true);
    }

    /**
     * isSidebarViewsWidgetThumbsViewLinkVisible.
     *
     * @should return correct value
     * @return true if the thumbnails view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetThumbsViewLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "thumbs[@enabled]", true);
    }

    /**
     * isSidebarViewsWidgetMetadataViewLinkVisible.
     *
     * @should return correct value
     * @return true if the metadata view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetMetadataViewLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "metadata[@enabled]", true);
    }

    /**
     * isSidebarViewsWidgetFulltextLinkVisible.
     *
     * @should return correct value
     * @return true if the fulltext view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetFulltextLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "fulltext[@enabled]", true);
    }

    /**
     * isSidebarViewsWidgetOpacLinkVisible.
     *
     * @should return correct value
     * @return true if the OPAC view link in the sidebar views widget is visible, false otherwise
     */
    public boolean isSidebarViewsWidgetOpacLinkVisible() {
        return getSidebarWidgetBooleanValue("views", "opac[@enabled]", false);
    }

    /**
     * isSearchInItemOnlyIfFullTextAvailable.
     *
     * @should return correct value
     * @return true if search-in-item is only active when full text is available, false otherwise
     */
    public boolean isSearchInItemOnlyIfFullTextAvailable() {
        return getSidebarWidgetBooleanValue("search-in-current-item", "[@onlyIfFullTextAvailable]", false);
    }

    /**
     * Checks whether the TOC <strong>widget</strong> is enabled. To check whether the sidebar TOC <strong>link</strong> in the views
     * widget is enabled, use <code>isSidebarTocVisible()</code>.
     *
     * @should return correct value
     * @return true if the sidebar TOC widget is visible in fullscreen mode, false otherwise
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return getSidebarWidgetBooleanValue("toc", "visibleInFullscreen", true);
    }

    /**
     * getSidebarTocPageNumbersVisible.
     *
     * @should return correct value
     * @return true if page numbers are visible in the sidebar TOC, false otherwise
     */
    public boolean getSidebarTocPageNumbersVisible() {
        return getSidebarWidgetBooleanValue("toc", "pageNumbersVisible", false);
    }

    /**
     * getSidebarTocLengthBeforeCut.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return getSidebarWidgetIntValue("toc", "lengthBeforeCut", 10);
    }

    /**
     * getSidebarTocInitialCollapseLevel.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocInitialCollapseLevel() {
        return getSidebarWidgetIntValue("toc", "initialCollapseLevel", 2);
    }

    /**
     * getSidebarTocCollapseLengthThreshold.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocCollapseLengthThreshold() {
        return getSidebarWidgetIntValue("toc", "collapseLengthThreshold", 10);
    }

    /**
     * getSidebarTocLowestLevelToCollapseForLength.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLowestLevelToCollapseForLength() {
        return getSidebarWidgetIntValue("toc", "collapseLengthThreshold[@lowestLevelToTest]", 2);
    }

    /**
     * isSidebarTocTreeView.
     *
     * @should return correct value
     * @return true if the sidebar TOC uses tree view, false otherwise
     */
    public boolean isSidebarTocTreeView() {
        return getSidebarWidgetBooleanValue("toc", "useTreeView", true);
    }

    /**
     * isTocTreeView.
     *
     * @should return true for allowed docstructs
     * @should return false for other docstructs
     * @param docStructType document structure type to check against configured allowlist
     * @return true if tree view is enabled for the given document structure type, false otherwise
     */
    public boolean isTocTreeView(String docStructType) {
        List<HierarchicalConfiguration<ImmutableNode>> hcList = getLocalConfigurationsAt("toc.useTreeView");
        if (hcList == null || hcList.isEmpty()) {
            return false;
        }
        HierarchicalConfiguration<ImmutableNode> hc = hcList.get(0);
        String docStructTypes = hc.getString("[@showDocStructs]");
        boolean allowed = hc.getBoolean(".");
        if (!allowed) {
            // logger.trace("Tree view disabled"); //NOSONAR Debug
            return false;
        }

        if (docStructTypes != null) {
            String[] docStructTypesSplit = docStructTypes.split(";");
            for (String dst : docStructTypesSplit) {
                if ("_ALL".equals(dst) || dst.equals(docStructType)) {
                    logger.trace("Tree view for {} allowed", docStructType);
                    return true;
                }
            }

        }

        return false;
    }

    //

    /**
     * isDisplaySidebarBrowsingTerms.
     *
     * @return true if the browsing terms sidebar widget is enabled, false otherwise
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return getSidebarWidgetBooleanValue("browsing-terms", "[@enabled]", true);
    }

    /**
     * isSidebarRssFeedWidgetEnabled.
     *
     * @return true if the RSS feed sidebar widget is enabled, false otherwise
     * @should return correct value
     */
    public boolean isSidebarRssFeedWidgetEnabled() {
        return getSidebarWidgetBooleanValue("rss", "[@enabled]", true);
    }

    /**
     * Returns a list containing all simple facet fields.
     *
     * @should return correct order
     * @return a list of all configured facet field names
     */
    public List<String> getAllFacetFields() {
        return getLocalList("search.facets.field");
    }

    /**
     * 
     * @return the list of configured regular (non-range, non-hierarchical) facet field names
     */
    public List<String> getRegularFacetFields() {
        List<String> ret = new ArrayList<>();
        for (String field : getAllFacetFields()) {
            String type = getFacetFieldType(field);
            if (StringUtils.isEmpty(type)) {
                ret.add(field);
            }
        }

        return ret;
    }

    /**
     * getBooleanFacetFields.
     *
     * @return a list of configured boolean-type facet field names
     * @should return all values
     */
    public List<String> getBooleanFacetFields() {
        List<String> ret = new ArrayList<>();
        for (String field : getAllFacetFields()) {
            String type = getFacetFieldType(field);
            if (type != null && type.equalsIgnoreCase("boolean")) {
                ret.add(field);
            }
        }

        return ret;
    }

    /**
     * getHierarchicalFacetFields.
     *
     * @return a list of configured hierarchical-type facet field names
     * @should return all values
     */
    public List<String> getHierarchicalFacetFields() {
        List<String> ret = new ArrayList<>();
        for (String field : getAllFacetFields()) {
            String type = getFacetFieldType(field);
            if (type != null && type.equalsIgnoreCase("hierarchical")) {
                ret.add(field);
            }
        }

        return ret;
    }

    /**
     * getRangeFacetFields.
     *
     * @return List of facet fields to be used as range values
     * @should return all values
     */
    public List<String> getRangeFacetFields() {
        List<String> ret = new ArrayList<>();
        for (String field : getAllFacetFields()) {
            String type = getFacetFieldType(field);
            if (type != null && type.equalsIgnoreCase("range")) {
                ret.add(field);
            }
        }

        return ret;
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured minimum value for the range facet field, or Integer.MIN_VALUE if not configured
     * @should return correct value
     * @should return INT_MIN if no value configured
     */
    public int getRangeFacetFieldMinValue(String facetField) {
        String val = getPropertyForFacetField(facetField, "[@minValue]", null);
        if (StringUtils.isNotEmpty(val)) {
            return Integer.parseInt(val);
        }

        return Integer.MIN_VALUE;
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured maximum value for the range facet field, or Integer.MAX_VALUE if not configured
     * @should return correct value
     * @should return INT_MAX if no value configured
     */
    public int getRangeFacetFieldMaxValue(String facetField) {
        String val = getPropertyForFacetField(facetField, "[@maxValue]", null);
        if (StringUtils.isNotEmpty(val)) {
            return Integer.parseInt(val);
        }

        return Integer.MAX_VALUE;
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured display style for the facet field, or an empty string if not configured
     * @should return correct value
     */
    public String getFacetFieldStyle(String facetField) {
        return getPropertyForFacetField(facetField, "[@style]", "");
    }

    /**
     * getGeoFacetFields.
     *
     * @return a list of configured geo-type facet field names
     * @should return all values
     */
    public List<String> getGeoFacetFields() {
        List<String> ret = new ArrayList<>();
        for (String field : getAllFacetFields()) {
            String type = getFacetFieldType(field);
            if (type != null && type.equalsIgnoreCase("geo")) {
                ret.add(field);
            }
        }

        return ret;
    }

    /**
     * @param facetField facet field name
     * @return the configured spatial predicate for the geo facet field, defaulting to ISWITHIN
     * @should return correct value
     */
    public String getGeoFacetFieldPredicate(String facetField) {
        return getPropertyForFacetField(facetField, "[@predicate]", "ISWITHIN");

    }

    /**
     * @param facetField facet field name
     * @return the configured value indicating whether search hits should be shown in the geo facet map
     * @should return correct value
     */
    public boolean isShowSearchHitsInGeoFacetMap(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@displayResultsOnMap]", "true");
        return Boolean.valueOf(value.trim());
    }

    /**
     * getInitialFacetElementNumber.
     *
     * @param facetField facet field name to look up
     * @return Number of initial facet values
     * @should return correct value
     * @should return default value if field not found
     */
    public int getInitialFacetElementNumber(String facetField) {
        if (StringUtils.isBlank(facetField)) {
            return getLocalInt("search.facets.initialElementNumber", 3);
        }

        String value = getPropertyForFacetField(facetField, "[@initialElementNumber]", "-1");
        return Integer.parseInt(value.trim());
    }

    /**
     * getFacetFieldDescriptionKey.
     *
     * @param facetField facet field name to look up
     * @return Optional description message key
     * @should return correct value
     */
    public String getFacetFieldDescriptionKey(String facetField) {
        return getPropertyForFacetField(facetField, "[@descriptionKey]", null);
    }

    /**
     * getSortOrder.
     *
     * @param facetField facet field name to look up
     * @return the configured sort order for the given facet field (e.g. "default", "asc", "desc")
     */
    public String getSortOrder(String facetField) {
        return getPropertyForFacetField(facetField, "[@sortOrder]", "default");
    }

    /**
     * Returns a list of values to prioritize for the given facet field.
     *
     * @param field facet field name to look up
     * @return List of priority values; empty list if none found for the given field
     * @should return return all configured elements for regular fields
     * @should return return all configured elements for hierarchical fields
     */
    public List<String> getPriorityValuesForFacetField(String field) {
        if (StringUtils.isBlank(field)) {
            return new ArrayList<>();
        }

        String priorityValues = getPropertyForFacetField(field, "[@priorityValues]", "");
        if (priorityValues == null) {
            return new ArrayList<>();
        }
        String[] priorityValuesSplit = priorityValues.split(";");

        return Arrays.asList(priorityValuesSplit);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured label field name for the facet field, or null if not configured
     * @should return correct value
     * @should return null if no value found
     */
    public String getLabelFieldForFacetField(String facetField) {
        return getPropertyForFacetField(facetField, "[@labelField]", null);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured value indicating whether facet field labels should be translated
     * @should return correct value
     */
    public boolean isTranslateFacetFieldLabels(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@translateLabels]", "true");
        return Boolean.parseBoolean(value);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured grouping length for the facet field, or -1 if not configured
     * @should return correct value
     */
    public int getGroupToLengthForFacetField(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@groupToLength]", "-1");
        return Integer.parseInt(value);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured value indicating whether the facet field should always be applied to unfiltered hits
     * @should return correct value
     */
    public boolean isAlwaysApplyFacetFieldToUnfilteredHits(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@alwaysApplyToUnfilteredHits]", "false");
        return Boolean.valueOf(value);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured value indicating whether the facet field should be skipped in the widget
     * @should return correct value
     */
    public boolean isFacetFieldSkipInWidget(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@skipInWidget]", "false");
        return Boolean.valueOf(value);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured value indicating whether a value filter should be displayed for the facet field
     * @should return correct value
     */
    public boolean isFacetFieldDisplayValueFilter(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@displayValueFilter]", "true");
        return Boolean.valueOf(value);
    }

    /**
     *
     * @param facetField facet field name
     * @return the configured type of the facet field, or an empty string if not configured
     */
    public String getFacetFieldType(String facetField) {
        return getPropertyForFacetField(facetField, XML_PATH_ATTRIBUTE_TYPE, "");
    }

    /**
     * @param facetField facet field name
     * @return the configured multi-value operator for the facet field, defaulting to AND
     * @should return correct value
     */
    public String getMultiValueOperatorForField(String facetField) {
        return getPropertyForFacetField(facetField, "[@multiValueOperator]", "AND");
    }

    /**
     * Boilerplate code for retrieving values from regular and hierarchical facet field configurations.
     *
     * @param facetField Facet field
     * @param property Element or attribute name to check
     * @param defaultValue Value that is returned if none was found
     * @return Found value or defaultValue
     */
    String getPropertyForFacetField(String facetField, String property, String defaultValue) {
        if (StringUtils.isBlank(facetField)) {
            return defaultValue;
        }

        String facetifiedField = SearchHelper.facetifyField(facetField);
        // Regular fields
        List<HierarchicalConfiguration<ImmutableNode>> facetFields = getLocalConfigurationsAt("search.facets.field");
        if (facetFields != null && !facetFields.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> fieldConfig : facetFields) {
                String nodeText = fieldConfig.getString(".", "");
                if (nodeText.equals(facetField)
                        || (facetField + SolrConstants.SUFFIX_UNTOKENIZED).equals(nodeText)
                        || nodeText.equals(facetifiedField)) {
                    String ret = fieldConfig.getString(property);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }
        // Hierarchical fields
        facetFields = getLocalConfigurationsAt("search.facets.hierarchicalField");
        if (facetFields != null && !facetFields.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> fieldConfig : facetFields) {
                String nodeText = fieldConfig.getString(".", "");
                if (nodeText.equals(facetField)
                        || (facetField + SolrConstants.SUFFIX_UNTOKENIZED).equals(nodeText)
                        || nodeText.equals(facetifiedField)) {
                    String ret = fieldConfig.getString(property);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }

        return defaultValue;
    }

    /**
     * isSortingEnabled.
     *
     * @should return correct value
     * @return true if search result sorting is enabled, false otherwise
     */
    public boolean isSortingEnabled() {
        return getLocalBoolean("search.sorting[@enabled]", true);
    }

    /**
     * getDefaultSortField.
     *
     * @param language language code for selecting language-specific sort fields
     * @return the configured default sort field for the given language, or the relevance sort constant if not configured
     * @should return correct value
     * @should return correct language value
     */
    public String getDefaultSortField(String language) {
        List<HierarchicalConfiguration<ImmutableNode>> fields = getLocalConfigurationsAt(XML_PATH_SEARCH_SORTING_FIELD);
        if (fields == null || fields.isEmpty()) {
            return SolrConstants.SORT_RELEVANCE;
        }

        for (HierarchicalConfiguration<ImmutableNode> fieldConfig : fields) {
            if (fieldConfig.getBoolean(XML_PATH_ATTRIBUTE_DEFAULT, false)) {
                String field = fieldConfig.getString(".");
                if (StringUtils.isEmpty(language) || !field.contains(SolrConstants.MIDFIX_LANG) || field.endsWith(language.toUpperCase())) {
                    return field;
                }
            }

        }

        return SolrConstants.SORT_RELEVANCE;
    }

    /**
     * getSortFields.
     *
     * @should return return all configured elements
     * @return a list of configured sort field names for search results
     */
    public List<String> getSortFields() {
        return getLocalList(XML_PATH_SEARCH_SORTING_FIELD);
    }

    /**
     *
     * @param language language code for filtering language-specific sort fields
     * @return List of {@link SearchSortingOption}s from configured sorting fields
     * @should place default sorting field on top
     * @should handle descending configurations correctly
     * @should ignore secondary fields from default config
     * @should ignore fields with mismatched language
     */
    public List<SearchSortingOption> getSearchSortingOptions(String language) {
        List<SearchSortingOption> options = new ArrayList<>();
        //default option
        String defaultField = getDefaultSortField(language);
        if (defaultField.charAt(0) == '!') {
            defaultField = defaultField.substring(1);
        }
        if (defaultField.contains(";")) {
            defaultField = defaultField.substring(0, defaultField.indexOf(";"));
        }
        List<String> fields = getSortFields();
        fields.remove(defaultField);
        fields.add(0, defaultField);
        for (final String f : fields) {
            String field = f;
            if (field.charAt(0) == '!') {
                field = field.substring(1);
            }
            if (field.contains(";")) {
                field = field.substring(0, field.indexOf(";"));
            }
            SearchSortingOption option = new SearchSortingOption(field, true);
            // Add option unless already in the list or there's a direct language mismatch
            if (!options.contains(option) && (StringUtils.isEmpty(language) || !option.getField().contains(SolrConstants.MIDFIX_LANG)
                    || option.getField().endsWith(SolrConstants.MIDFIX_LANG + language.toUpperCase()))) {
                options.add(new SearchSortingOption(field, true));
                // Add descending option for most fields
                if (!SolrConstants.SORT_RANDOM.equals(field) && !SolrConstants.SORT_RELEVANCE.equals(field)) {
                    options.add(new SearchSortingOption(field, false));
                }
            }
        }

        return options;
    }

    /**
     * getStaticSortFields.
     *
     * @should return return all configured elements
     * @return a list of configured static sort field names that are always applied to search results
     */
    public List<String> getStaticSortFields() {
        return getLocalList("search.sorting.static.field");
    }

    /**
     * @param field sort field name
     * @return an Optional containing the configured message key for ascending sort label, or empty if not configured
     */
    public Optional<String> getSearchSortingKeyAscending(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldConfigs = getLocalConfigurationsAt(XML_PATH_SEARCH_SORTING_FIELD);
        for (HierarchicalConfiguration<ImmutableNode> conf : fieldConfigs) {
            String configField = conf.getString(".");
            if (Strings.CS.equals(configField, field)) {
                return Optional.ofNullable(conf.getString("[@dropDownAscMessageKey]", null));
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param field sort field name
     * @return an Optional containing the configured message key for descending sort label, or empty if not configured
     */
    public Optional<String> getSearchSortingKeyDescending(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldConfigs = getLocalConfigurationsAt(XML_PATH_SEARCH_SORTING_FIELD);
        for (HierarchicalConfiguration<ImmutableNode> conf : fieldConfigs) {
            String configField = conf.getString(".");
            if (Strings.CS.equals(configField, field)) {
                return Optional.ofNullable(conf.getString("[@dropDownDescMessageKey]", null));
            }
        }
        return Optional.empty();
    }

    /**
     * getUrnResolverUrl.
     *
     * @should return correct value
     * @return the configured URN resolver base URL
     */
    public String getUrnResolverUrl() {
        return getLocalString("urls.urnResolver",
                new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resolver?urn=").toString());
    }

    /**
     * The maximal image size retrievable with only the permission to view thumbnails.
     *
     * @should return correct value
     * @return the maximal image width
     */
    public int getThumbnailImageAccessMaxWidth() {
        return getLocalInt("accessConditions.thumbnailImageAccessMaxWidth", getLocalInt("accessConditions.unconditionalImageAccessMaxWidth", 120));
    }

    /**
     * The maximal image size retrievable with the permission to view images but without the permission to zoom images.
     *
     * @should return correct value
     * @return the maximal image width, default ist 600
     */
    public int getUnzoomedImageAccessMaxWidth() {
        return getLocalInt("accessConditions.unzoomedImageAccessMaxWidth", 0);
    }

    /**
     * isFullAccessForLocalhost.
     *
     * @should return correct value
     * @return true if full access is granted to requests from localhost, false otherwise
     */
    public boolean isFullAccessForLocalhost() {
        return getLocalBoolean("accessConditions.fullAccessForLocalhost", false);
    }

    /**
     * isGeneratePdfInMessageQueue.
     *
     * @should return correct value
     * @return true if PDF generation is handled via the message queue, false otherwise
     */
    public boolean isGeneratePdfInMessageQueue() {
        return getLocalBoolean("pdf.generateInMessageQueue", getLocalBoolean("pdf.externalPdfGeneration", false));
    }

    /**
     * isPdfApiDisabled.
     *
     * @should return correct value
     * @return true if the PDF API is disabled, false otherwise
     */
    public boolean isPdfApiDisabled() {
        return getLocalBoolean("pdf.pdfApiDisabled", false);
    }

    /**
     * isTitlePdfEnabled.
     *
     * @should return correct value
     * @return true if PDF download for the title page is enabled, false otherwise
     */
    public boolean isTitlePdfEnabled() {
        return getLocalBoolean("pdf.titlePdfEnabled", true);
    }

    /**
     * isTocPdfEnabled.
     *
     * @should return correct value
     * @return true if PDF download for the table of contents is enabled, false otherwise
     */
    public boolean isTocPdfEnabled() {
        return getLocalBoolean("pdf.tocPdfEnabled", true);
    }

    /**
     * isMetadataPdfEnabled.
     *
     * @should return correct value
     * @return true if PDF download including metadata is enabled, false otherwise
     */
    public boolean isMetadataPdfEnabled() {
        return getLocalBoolean("pdf.metadataPdfEnabled", true);
    }

    /**
     * isPagePdfEnabled.
     *
     * @should return correct value
     * @return true if PDF download for individual pages is enabled, false otherwise
     */
    public boolean isPagePdfEnabled() {
        return getLocalBoolean("pdf.pagePdfEnabled", false);
    }

    /**
     * isDocHierarchyPdfEnabled.
     *
     * @should return correct value
     * @return true if PDF download for the full document hierarchy is enabled, false otherwise
     */
    public boolean isDocHierarchyPdfEnabled() {
        return getLocalBoolean("pdf.docHierarchyPdfEnabled", false);
    }

    /**
     * isTitleEpubEnabled.
     *
     * @should return correct value
     * @return true if EPUB download for the title is enabled, false otherwise
     */
    public boolean isTitleEpubEnabled() {
        return getLocalBoolean("epub.titleEpubEnabled", false);
    }

    /**
     * isTocEpubEnabled.
     *
     * @should return correct value
     * @return true if EPUB download for the table of contents is enabled, false otherwise
     */
    public boolean isTocEpubEnabled() {
        return getLocalBoolean("epub.tocEpubEnabled", false);
    }

    /**
     * isMetadataEpubEnabled.
     *
     * @should return correct value
     * @return true if EPUB download including metadata is enabled, false otherwise
     */
    public boolean isMetadataEpubEnabled() {
        return getLocalBoolean("epub.metadataEpubEnabled", false);
    }

    /**
     * getDownloadFolder.
     *
     * @should return correct value for pdf
     * @should return correct value for epub
     * @should return empty string if type unknown
     * @param type download type (pdf, epub, or resource)
     * @return the configured download folder path for the given type, or an empty string for unknown types
     */
    public String getDownloadFolder(String type) {
        switch (type.toLowerCase()) {
            case "pdf":
                return getLocalString("pdf.downloadFolder", "/opt/digiverso/viewer/pdf_download");
            case "epub":
                return getLocalString("epub.downloadFolder", "/opt/digiverso/viewer/epub_download");
            case "resource":
                return getLocalString("externalResource.downloadFolder", "/opt/digiverso/viewer/resource_download");
            default:
                return "";

        }
    }

    public Map<String, String> getDownloadHeader(String externalResourceUrl) {

        if (StringUtils.isBlank(externalResourceUrl)) {
            return Collections.emptyMap();
        }

        List<HierarchicalConfiguration<ImmutableNode>> configs = getAllConfigurationsAt("externalResource.urls.template");

        for (HierarchicalConfiguration<ImmutableNode> templateConfig : configs) {
            String templateUrl = templateConfig.getString("url", "");
            if (externalResourceUrl.equals(templateUrl)) {
                Map<String, String> headerMap = new HashMap<>();
                List<HierarchicalConfiguration<ImmutableNode>> headerConfigs = templateConfig.configurationsAt("httpHeader");
                for (HierarchicalConfiguration<ImmutableNode> headerConfig : headerConfigs) {
                    String key = headerConfig.getString("[@key]", "");
                    String value = headerConfig.getString("[@value]", "");
                    if (StringUtils.isNoneBlank(key, value)) {
                        headerMap.put(key, value);
                    }
                }
                return headerMap;
            }
        }
        return Collections.emptyMap();
    }

    public List<String> getExternalResourceUrlTemplates() {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getAllConfigurationsAt("externalResource.urls.template");
        List<String> templates = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> templateConfig : configs) {
            String url = templateConfig.getString(".", "");
            if (StringUtils.isNotBlank(url)) {
                templates.add(url);
            } else {
                url = templateConfig.getString("url", "");
                if (StringUtils.isNotBlank(url)) {
                    templates.add(url);
                }
            }
        }
        return templates.stream().distinct().toList();
    }

    public Duration getExternalResourceTimeBeforeDeletion() {
        int amount = getLocalInt("externalResource.deleteAfter.value", 1);
        String unitString = getLocalString("externalResource.deleteAfter.unit", ChronoUnit.DAYS.name());
        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString.toUpperCase());
            return Duration.of(amount, unit);
        } catch (IllegalArgumentException e) {
            logger.warn("Could not read temporal unit from string '{}' in config field 'externalResource.deleteAfter.unit'. Assuming days.",
                    unitString);
            return Duration.of(amount, ChronoUnit.DAYS);
        }
    }

    /**
     * getRssFeedItems.
     *
     * @should return correct value
     * @return a int.
     */
    public int getRssFeedItems() {
        return getLocalInt("rss.numberOfItems", 50);
    }

    /**
     * getRssTitle.
     *
     * @should return correct value
     * @return the configured RSS feed title
     */
    public String getRssTitle() {
        return getLocalString("rss.title", "viewer-rss");
    }

    /**
     * getRssDescription.
     *
     * @should return correct value
     * @return the configured RSS feed description
     */
    public String getRssDescription() {
        return getLocalString("rss.description", "latest imports");
    }

    /**
     * getRssCopyrightText.
     *
     * @should return correct value
     * @return the configured RSS feed copyright text
     */
    public String getRssCopyrightText() {
        return getLocalString("rss.copyright");
    }

    /**
     * getThumbnailsWidth.
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsWidth() {
        return getLocalInt("viewer.thumbnailsWidth", 100);
    }

    /**
     * getThumbnailsHeight.
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsHeight() {
        return getLocalInt("viewer.thumbnailsHeight", 120);
    }

    /**
     * getAnchorThumbnailMode.
     *
     * @should return correct value
     * @return the configured thumbnail display mode for anchor records
     */
    public String getAnchorThumbnailMode() {
        return getLocalString("viewer.anchorThumbnailMode", StringConstants.ANCHOR_THUMBNAIL_MODE_GENERIC);
    }

    /**
     * getDisplayBreadcrumbs.
     *
     * @should return correct value
     * @return true if breadcrumb navigation is displayed, false otherwise
     */
    public boolean getDisplayBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayBreadcrumbs", true);
    }

    /**
     * getDisplayMetadataPageLinkBlock.
     *
     * @should return correct value
     * @return true if the metadata page link block is displayed, false otherwise
     */
    public boolean getDisplayMetadataPageLinkBlock() {
        return this.getLocalBoolean("webGuiDisplay.displayMetadataPageLinkBlock", true);
    }

    /**
     * useTiles.
     *
     * @should return correct value
     * @return true if tiled image loading is used in the standard image view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return useTiles(new ViewAttributes(PageType.viewImage));
    }

    /**
     * useTilesFullscreen.
     *
     * @should return correct value
     * @return true if tiled image loading is used in fullscreen view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return useTiles(new ViewAttributes(PageType.viewFullscreen));
    }

    /**
     * useTiles.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if tiled image loading is used in the standard image view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return getZoomImageViewConfig(viewAttributes).getBoolean("[@tileImage]", false);
    }

    /**
     * Returns whether a navigator element should be shown in the OpenSeadragon viewer.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if navigator should be shown
     * @throws ViewerConfigurationException
     */
    public boolean showImageNavigator(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return getZoomImageViewConfig(viewAttributes).getBoolean("navigator[@enabled]", false);
    }

    /**
     * Returns whether the thumbnail gallery should be shown in image view.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if thumbnail gallery should be visible
     * @throws ViewerConfigurationException
     */
    public boolean showImageThumbnailGallery(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return getZoomImageViewConfig(viewAttributes).getBoolean("thumbnailGallery[@enabled]", false);
    }

    /**
     * getFooterHeight.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(new ViewAttributes(PageType.viewImage));
    }

    /**
     * getFullscreenFooterHeight.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFullscreenFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(new ViewAttributes(PageType.viewFullscreen));
    }

    /**
     * getFooterHeight.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return getZoomImageViewConfig(viewAttributes).getInt("[@footerHeight]", 50);
    }

    /**
     * getImageViewZoomScales.
     *
     * @return a list of configured zoom scale values for the default image view
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales() throws ViewerConfigurationException {
        return getImageViewZoomScales(new ViewAttributes(PageType.viewImage));
    }

    /**
     * getImageViewZoomScales.
     *
     * @param view page type name used to construct the ViewAttributes
     * @return a list of configured zoom scale values for the given view
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(String view) throws ViewerConfigurationException {
        return getImageViewZoomScales(new ViewAttributes(PageType.valueOf(view)));
    }

    /**
     * getImageViewZoomScales.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return a list of configured zoom scale values for the given view context
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        List<String> defaultList = new ArrayList<>();
        BaseHierarchicalConfiguration zoomImageViewConfig = getZoomImageViewConfig(viewAttributes);
        if (zoomImageViewConfig != null) {
            String[] scales = zoomImageViewConfig.getStringArray("scale");
            if (scales != null) {
                return Arrays.asList(scales);
            }
        }
        return defaultList;
    }

    /**
     * getTileSizes.
     *
     * @return the configured tile sizes for imageView as a hashmap linking each tile size to the list of resolutions to use with that size
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes() throws ViewerConfigurationException {
        return getTileSizes(new ViewAttributes(PageType.viewImage));
    }

    /**
     * getTileSizes.
     *
     * @param viewAttributes view context attributes selecting the zoom config
     * @return a map of tile sizes (resolution) to lists of scale factors for the given view context
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        Map<Integer, List<Integer>> map = new HashMap<>();
        List<HierarchicalConfiguration<ImmutableNode>> sizes = getZoomImageViewConfig(viewAttributes).configurationsAt("tileSize");
        if (sizes != null && !sizes.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> sizeConfig : sizes) {
                int size = sizeConfig.getInt("size", 0);
                String[] resolutionString = sizeConfig.getStringArray("scaleFactors");
                List<Integer> resolutions = new ArrayList<>(resolutionString.length);
                for (String res : resolutionString) {
                    try {
                        int resolution = Integer.parseInt(res);
                        resolutions.add(resolution);
                    } catch (NullPointerException | NumberFormatException e) {
                        logger.warn("Cannot parse {} as an integer", res);
                    }
                }
                map.put(size, resolutions);
            }
        }
        if (map.isEmpty()) {
            map.put(512, Arrays.asList(1, 32));
        }
        return map;
    }

    /**
     * getZoomImageViewConfig.
     *
     * @param viewAttributes view context attributes selecting the matching zoom config block
     * @return the zoom image view configuration block matching the given view context
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BaseHierarchicalConfiguration getZoomImageViewConfig(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        List<HierarchicalConfiguration<ImmutableNode>> configs = new ArrayList<>();
        configs.addAll(getLocalConfigurationsAt("viewer.zoomImageView"));
        configs.addAll(getConfig().configurationsAt("viewer.zoomImageView"));

        for (HierarchicalConfiguration<ImmutableNode> subConfig : configs) {

            if (!subConfig.configurationsAt("condition").isEmpty()) {
                HierarchicalConfiguration<ImmutableNode> condition = subConfig.configurationAt("condition");
                if (viewAttributes.matchesConfiguration(condition)) {
                    return (BaseHierarchicalConfiguration) subConfig;
                }
            } else if (!subConfig.configurationsAt("useFor").isEmpty()) { //fallback for old setup
                HierarchicalConfiguration<ImmutableNode> condition = subConfig.configurationAt("useFor");
                if (viewAttributes.matchesConfiguration(condition)) {
                    return (BaseHierarchicalConfiguration) subConfig;
                }
            } else {
                return (BaseHierarchicalConfiguration) subConfig;
            }

        }

        throw new ViewerConfigurationException("Viewer config must define at least a generic <zoomImageView>");
    }

    /**
     * getBreadcrumbsClipping.
     *
     * @should return correct value
     * @return a int.
     */
    public int getBreadcrumbsClipping() {
        return getLocalInt("webGuiDisplay.breadcrumbsClipping", 50);
    }

    /**
     * getDisplayStructType.
     *
     * @should return correct value
     * @return true if the structure type should be displayed in search hit metadata, false otherwise
     */
    public boolean getDisplayStructType() {
        return this.getLocalBoolean("search.metadata.displayStructType", true);
    }

    /**
     * getSearchHitMetadataValueNumber.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitMetadataValueNumber() {
        return getLocalInt("search.metadata.valueNumber", 1);
    }

    /**
     * getSearchHitMetadataValueLength.
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitMetadataValueLength() {
        return getLocalInt("search.metadata.valueLength", 0);
    }

    /**
     *
     * @return true if enabled or not configured; false otherwise
     * @should return correct value
     */
    public boolean isWatermarkTextConfigurationEnabled() {
        return getLocalBoolean("viewer.watermarkTextConfiguration[@enabled]", true);
    }

    /**
     * Returns the preference order of data to be used as an image footer text.
     *
     * @should return all configured elements in the correct order
     * @return a list of data source names defining the preference order for image footer text
     */
    public List<String> getWatermarkTextConfiguration() {
        return getLocalList("viewer.watermarkTextConfiguration.text");
    }

    /**
     * getWatermarkFormat.
     *
     * @return the configured image format for watermarked images (e.g. "jpg")
     */
    public String getWatermarkFormat() {
        return getLocalString("viewer.watermarkFormat", "jpg");
    }

    /**
     * getStopwordsFilePath.
     *
     * @should return correct value
     * @return the configured path to the stopwords file
     */
    public String getStopwordsFilePath() {
        return getLocalString("stopwordsFile");
    }

    /**
     * Returns the locally configured page type name for URLs (e.g. "bild" instead of default "image").
     *
     * @param type page type whose configured URL name is returned
     * @should return the correct value for the given type
     * @should return null for non configured type
     * @return the locally configured URL name for the given page type, or null if not configured
     */
    public String getPageType(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name());
    }

    /**
     * getRecordTargetPageType.
     *
     * @param publicationType publication type name to look up
     * @should return correct value
     * @should return null if docstruct not found
     * @return the configured target page type name for the given publication type, or null if not configured
     */
    public String getRecordTargetPageType(String publicationType) {
        return getLocalString("viewer.recordTargetPageTypes." + publicationType);
    }

    public String getPageTypeExitView(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name() + "[@exit]");
    }

    /**
     * getFulltextPercentageWarningThreshold.
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextPercentageWarningThreshold() {
        return getLocalInt("viewer.fulltextPercentageWarningThreshold", 30);
    }

    /**
     *
     * @return the configured fallback default language code, e.g. "en"
     * @should return correct value
     */
    public String getFallbackDefaultLanguage() {
        return getLocalString("viewer.fallbackDefaultLanguage", "en");
    }

    /**
     * getFeedbackEmailAddresses.
     *
     * @should return correct values
     * @return the list of configured feedback email recipients
     */
    public List<EmailRecipient> getFeedbackEmailRecipients() {
        List<EmailRecipient> ret = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> licenseNodes = getLocalConfigurationsAt("user.feedbackEmailAddressList.address");
        int counter = 0;
        for (HierarchicalConfiguration<ImmutableNode> node : licenseNodes) {
            String address = node.getString(".", "");
            if (StringUtils.isNotBlank(address)) {
                String id = node.getString("[@id]", "genId_" + (++counter));
                String label = node.getString(XML_PATH_ATTRIBUTE_LABEL, address);
                boolean defaultRecipient = node.getBoolean(XML_PATH_ATTRIBUTE_DEFAULT, false);
                ret.add(new EmailRecipient(id, label, address, defaultRecipient));
            }
        }

        return ret;
    }

    /**
     *
     * @return the email address of the default feedback recipient, or a placeholder string if none is configured
     */
    public String getDefaultFeedbackEmailAddress() {
        for (EmailRecipient recipient : getFeedbackEmailRecipients()) {
            if (recipient.isDefaultRecipient()) {
                return recipient.getEmailAddress();
            }
        }

        return "<NOT CONFIGURED>";
    }

    /**
     * isBookmarksEnabled.
     *
     * @should return correct value
     * @return true if the bookmarks feature is enabled, false otherwise
     */
    public boolean isBookmarksEnabled() {
        return getLocalBoolean("bookmarks[@enabled]", true);
    }

    /**
     * getPageLoaderThreshold.
     *
     * @should return correct value
     * @return a int.
     */
    public int getPageLoaderThreshold() {
        return getLocalInt("performance.pageLoaderThreshold", 1000);
    }

    /**
     * Returns the TTL (in minutes) for cached data repository name lookups.
     *
     * @return TTL in minutes; default is 10
     */
    public int getDataRepositoryCacheTTL() {
        return getLocalInt("performance.dataRepositoryCacheTTL", 10);
    }

    /**
     * isPreventProxyCaching.
     *
     * @should return correct value
     * @return true if proxy caching should be prevented, false otherwise
     */
    public boolean isPreventProxyCaching() {
        return getLocalBoolean(("performance.preventProxyCaching"), false);
    }

    /**
     * getDatabaseConnectionAttempts.
     *
     * @should return correct value
     * @return a int.
     */
    public int getDatabaseConnectionAttempts() {
        return getLocalInt("performance.databaseConnectionAttempts", 5);
    }

    /**
     * @return true if review mode is enabled for comments, false otherwise
     */
    public boolean reviewEnabledForComments() {
        return getLocalBoolean("comments.review[@enabled]", false);
    }

    /**
     * getViewerHome.
     *
     * @should return correct value
     * @return the configured viewer home directory path
     */
    public String getViewerHome() {
        return getLocalString("viewerHome");
    }

    /**
     *
     * @return the configured path to the data repositories home directory
     * @should return correct value
     */
    String getDataRepositoriesHome() {
        return getLocalString("dataRepositoriesHome", "");
    }

    /**
     * getWatermarkIdField.
     *
     * @should return correct value
     * @return a list of Solr field names used to select the watermark image for a record
     */
    public List<String> getWatermarkIdField() {
        return getLocalList("viewer.watermarkIdField", Collections.singletonList(SolrConstants.DC));

    }

    /**
     *
     * @return true if docstruct navigation is enabled, false otherwise
     * @should return correct value
     */
    public boolean isDocstructNavigationEnabled() {
        return getLocalBoolean("viewer.docstructNavigation[@enabled]", false);
    }

    /**
     *
     * @param template template name to look up
     * @param fallbackToDefaultTemplate if true, fall back to the default template when not found
     * @return the list of configured docstruct type names for the given template
     * @should return all configured values
     */
    public List<String> getDocstructNavigationTypes(String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("viewer.docstructNavigation.template");
        if (templateList == null) {
            return new ArrayList<>();
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return new ArrayList<>();
        }

        String[] ret = usingTemplate.getStringArray("docstruct");
        if (ret == null) {
            logger.warn("Template '{}' contains no docstruct elements.", usingTemplate.getRootElementName());
            return new ArrayList<>();
        }

        return Arrays.asList(ret);
    }

    /**
     * getSubthemeMainTheme.
     *
     * @should return correct value
     * @return the configured main theme name used as the base theme for subthemes
     */
    public String getSubthemeMainTheme() {
        String theme = getLocalString("viewer.theme[@mainTheme]");
        if (StringUtils.isEmpty(theme)) {
            logger.error("Theme name could not be read - {} may not be well-formed.", CONFIG_FILE_NAME);
        }
        return getLocalString("viewer.theme[@mainTheme]");
    }

    /**
     * getSubthemeDiscriminatorField.
     *
     * @should return correct value
     * @return the configured Solr field name used to discriminate between subthemes
     */
    public String getSubthemeDiscriminatorField() {
        return getLocalString("viewer.theme[@discriminatorField]", "");
    }

    /**
     * getTagCloudSampleSize.
     *
     * @should return correct value for existing fields
     * @should return INT_MAX for other fields
     * @param fieldName Solr field name to look up the sample size for
     * @return a int.
     */
    public int getTagCloudSampleSize(String fieldName) {
        return getLocalInt("tagclouds.sampleSizes." + fieldName, Integer.MAX_VALUE);
    }

    /**
     * getTocVolumeSortFieldsForTemplate.
     *
     * @param template template name to look up
     * @return a list of sort field name/order pairs configured for TOC volume sorting for the given template
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template configuration if template is null
     */
    public List<StringPair> getTocVolumeSortFieldsForTemplate(String template) {
        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return new ArrayList<>();
        }
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (HierarchicalConfiguration<ImmutableNode> subElement : templateList) {
            String templateName = subElement.getString(XML_PATH_ATTRIBUTE_NAME);
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if (StringConstants.DEFAULT_NAME.equals(templateName)) {
                    defaultTemplate = subElement;
                }
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return new ArrayList<>();
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = usingTemplate.configurationsAt("field");
        if (fields == null) {
            return new ArrayList<>();
        }

        List<StringPair> ret = new ArrayList<>(fields.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : fields) {
            String field = sub.getString(".");
            String order = sub.getString("[@order]");
            ret.add(new StringPair(field, "desc".equals(order) ? "desc" : "asc"));
        }

        return ret;
    }

    /**
     * Returns the grouping Solr field for the given anchor TOC sort configuration.
     *
     * @should return correct value
     * @param template template name to look up
     * @return the configured grouping Solr field for the given anchor TOC sort configuration
     */
    public String getTocVolumeGroupFieldForTemplate(String template) {
        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (HierarchicalConfiguration<ImmutableNode> subElement : templateList) {
            String templateName = subElement.getString(XML_PATH_ATTRIBUTE_NAME);
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if (StringConstants.DEFAULT_NAME.equals(templateName)) {
                    defaultTemplate = subElement;
                }
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return null;
        }
        String groupBy = usingTemplate.getString("[@groupBy]");
        if (StringUtils.isNotEmpty(groupBy)) {
            return groupBy;
        }

        return null;
    }

    /**
     * getDisplayTitleBreadcrumbs.
     *
     * @should return correct value
     * @return true if title breadcrumbs should be displayed, false otherwise
     */
    public boolean getDisplayTitleBreadcrumbs() {
        return getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs", false);
    }

    /**
     * isDisplayTitlePURL.
     *
     * @should return correct value
     * @return true if the persistent URL should be displayed in the title bar, false otherwise
     */
    public boolean isDisplayTitlePURL() {
        return this.getLocalBoolean("webGuiDisplay.displayTitlePURL", true);
    }

    /**
     * getTitleBreadcrumbsMaxTitleLength.
     *
     * @should return correct value
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return this.getLocalInt("webGuiDisplay.displayTitleBreadcrumbs[@maxTitleLength]", 25);
    }

    /**
     * getIncludeAnchorInTitleBreadcrumbs.
     *
     * @should return correct value
     * @return true if the anchor record should be included in title breadcrumbs, false otherwise
     */
    public boolean getIncludeAnchorInTitleBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs[@includeAnchor]", false);
    }

    /**
     * isDisplaySearchRssLinks.
     *
     * @should return correct value
     * @return true if RSS links should be displayed in search results, false otherwise
     */
    public boolean isDisplaySearchRssLinks() {
        return getLocalBoolean("rss.displaySearchRssLinks", true);
    }

    /**
     * getStartYearForTimeline.
     *
     * @should return correct value
     * @return the configured start year for the timeline view
     */
    public String getStartYearForTimeline() {
        return this.getLocalString("search.timeline.startyear", "1750");
    }

    /**
     * getEndYearForTimeline.
     *
     * @should return correct value
     * @return the configured end year for the timeline view
     */
    public String getEndYearForTimeline() {
        return this.getLocalString("search.timeline.endyear", "2014");
    }

    /**
     * getTimelineHits.
     *
     * @should return correct value
     * @return the configured maximum number of timeline hits to display
     */
    public String getTimelineHits() {
        return this.getLocalString("search.timeline.hits", "108");
    }

    /**
     * isPiwikTrackingEnabled.
     *
     * @should return correct value
     * @return true if Piwik/Matomo tracking is enabled, false otherwise
     */
    public boolean isPiwikTrackingEnabled() {
        return getLocalBoolean("piwik[@enabled]", false);
    }

    /**
     * getPiwikBaseURL.
     *
     * @should return correct value
     * @return the configured Piwik/Matomo tracking base URL
     */
    public String getPiwikBaseURL() {
        return this.getLocalString("piwik.baseURL", "");
    }

    /**
     * getPiwikSiteID.
     *
     * @should return correct value
     * @return the configured Piwik/Matomo site ID
     */
    public String getPiwikSiteID() {
        return this.getLocalString("piwik.siteID", "1");
    }

    /**
     * isSearchSavingEnabled.
     *
     * @should return correct value
     * @return true if saving searches is enabled, false otherwise
     */
    public boolean isSearchSavingEnabled() {
        return getLocalBoolean("search.searchSaving[@enabled]", true);
    }

    /**
     * getRecordGroupIdentifierFields.
     *
     * @should return all configured values
     * @return a {@link java.util.List} object.
     * @deprecated Group identifier fields are now detected implicitly via the {@code GROUPID_} prefix. This config is no longer needed.
     */
    @Deprecated(since = "26.04")
    public List<String> getRecordGroupIdentifierFields() {
        return getLocalList("toc.recordGroupIdentifierFields.field");
    }

    /**
     * getAncestorIdentifierFields.
     *
     * @should return all configured values
     * @return a list of configured Solr field names used to identify ancestor records
     */
    public List<String> getAncestorIdentifierFields() {
        return getLocalList("toc.ancestorIdentifierFields.field");
    }

    /**
     * isTocListSiblingRecords.
     *
     * @return true if sibling records should be listed in the table of contents, false otherwise
     * @should return correctValue
     */
    public boolean isTocListSiblingRecords() {
        return getLocalBoolean("toc.ancestorIdentifierFields[@listSiblingRecords]", false);
    }

    /**
     * getAncestorIdentifierFieldFilterQuery(String).
     *
     * @param field ancestor identifier field name
     * @return Configured filter query for the given field; empty string is none found
     * @should return empty string if field config not found
     * @should return correctValue
     */
    public String getAncestorIdentifierFieldFilterQuery(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = getLocalConfigurationsAt("toc.ancestorIdentifierFields.field");
        if (fieldList == null) {
            return "";
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            if (subElement.getString(".").equals(field)) {
                return subElement.getString("[@filterQuery]", "");
            }
        }

        return "";
    }

    /**
     * getSearchFilters.
     *
     * @should return all configured elements
     * @return a list of configured search filter definitions
     */
    public List<SearchFilter> getSearchFilters() {
        List<HierarchicalConfiguration<ImmutableNode>> elements = getLocalConfigurationsAt("search.filters.filter");
        if (elements == null) {
            return new ArrayList<>();
        }

        List<SearchFilter> ret = new ArrayList<>(elements.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : elements) {
            String filterString = sub.getString(".");
            if (filterString.startsWith("filter_")) {
                ret.add(new SearchFilter(filterString, filterString.substring(7), sub.getBoolean(XML_PATH_ATTRIBUTE_DEFAULT, false)));
            } else {
                logger.error("Invalid search filter definition: {}", filterString);
            }
        }

        return ret;
    }

    /**
     *
     * @return the configured default search filter, or the global "all" filter if none is marked as default
     */
    public SearchFilter getDefaultSearchFilter() {
        for (SearchFilter filter : getSearchFilters()) {
            if (filter.isDefaultFilter()) {
                return filter;
            }
        }

        return SearchHelper.SEARCH_FILTER_ALL;
    }

    /**
     * getWebApiFields.
     *
     * @param template template name to look up
     * @return {@link JsonMetadataConfiguration}
     * @should return all configured elements
     */
    public JsonMetadataConfiguration getWebApiFields(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templates = getLocalConfigurationsAt("webapi.json.template");
        if (templates == null) {
            return null;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : templates) {
            if (subElement.getString(XML_PATH_ATTRIBUTE_NAME).equals(template)) {
                String query = subElement.getString("[@query]");
                List<HierarchicalConfiguration<ImmutableNode>> fieldsNodes = subElement.configurationsAt("field");
                List<Map<String, String>> fields = new ArrayList<>(fieldsNodes.size());
                for (HierarchicalConfiguration<ImmutableNode> fieldNode : fieldsNodes) {
                    Map<String, String> fieldConfig = new HashMap<>();
                    fieldConfig.put("jsonField", fieldNode.getString("[@jsonField]", null));
                    fieldConfig.put("solrField", fieldNode.getString("[@solrField]", null));
                    fieldConfig.put("multivalue", fieldNode.getString("[@multivalue]", null));
                    fieldConfig.put("constantValue", fieldNode.getString("[@constantValue]", null));
                    fields.add(fieldConfig);
                }
                return new JsonMetadataConfiguration(template, query, fields);
            }
        }

        return null;
    }

    /**
     * getDbPersistenceUnit.
     *
     * @should return correct value
     * @return the configured JPA persistence unit name, or null if not configured
     */
    public String getDbPersistenceUnit() {
        return getLocalString("dbPersistenceUnit", null);
    }

    /**
     * A folder for temporary storage of media files. Used by DC record creation to store uploaded files
     *
     * @return "temp_media" unless otherwise configured in "tempMediaFolder"
     */
    public String getTempMediaFolder() {
        return getLocalString("tempMediaFolder", "temp_media");
    }

    public String getUserAvatarFolder() {
        return getLocalString("userAvatarFolder", "users/avatar");
    }

    /**
     * getCmsMediaDisplayWidth.
     *
     * @return a int.
     */
    public int getCmsMediaDisplayWidth() {
        return getLocalInt("cms.mediaDisplayWidth", 0);
    }

    /**
     * getCmsMediaDisplayHeight. If not configured, return 100.000. In this case the actual image size always depends on the requested width
     *
     * @return a int.
     */
    public int getCmsMediaDisplayHeight() {
        return getLocalInt("cms.mediaDisplayHeight", 100000);
    }

    /**
     * isTranskribusEnabled.
     *
     * @should return correct value
     * @return true if Transkribus integration is enabled, false otherwise
     */
    public boolean isTranskribusEnabled() {
        return getLocalBoolean("transkribus[@enabled]", false);
    }

    /**
     * getTranskribusUserName.
     *
     * @should return correct value
     * @return the configured Transkribus login username
     */
    public String getTranskribusUserName() {
        return getLocalString("transkribus.userName");
    }

    /**
     * getTranskribusPassword.
     *
     * @should return correct value
     * @return the configured Transkribus login password
     */
    public String getTranskribusPassword() {
        return getLocalString("transkribus.password");
    }

    /**
     * getTranskribusDefaultCollection.
     *
     * @should return correct value
     * @return the configured default Transkribus collection name
     */
    public String getTranskribusDefaultCollection() {
        return getLocalString("transkribus.defaultCollection");
    }

    /**
     * getTranskribusRestApiUrl.
     *
     * @should return correct value
     * @return the configured Transkribus REST API base URL
     */
    public String getTranskribusRestApiUrl() {
        return getLocalString("transkribus.restApiUrl", TranskribusUtils.TRANSRIBUS_REST_URL);
    }

    /**
     * getTranskribusAllowedDocumentTypes.
     *
     * @should return all configured elements
     * @return a list of configured document type names (docstructs) allowed for Transkribus integration
     */
    public List<String> getTranskribusAllowedDocumentTypes() {
        return getLocalList("transkribus.allowedDocumentTypes.docstruct");
    }

    /**
     * getTocIndentation.
     *
     * @should return correct value
     * @return a int.
     */
    public int getTocIndentation() {
        return getLocalInt("toc.tocIndentation", 20);
    }

    /**
     * isPageBrowseEnabled.
     *
     * @return true if page-by-page browsing is enabled, false otherwise
     * @should return correct value
     */
    public boolean isPageBrowseEnabled() {
        return getLocalBoolean("viewer.pageBrowse[@enabled]", false);
    }

    /**
     * getPageBrowseSteps.
     *
     * @return a list of configured page step sizes for page-by-page browsing navigation
     */
    public List<Integer> getPageBrowseSteps() {
        List<String> defaultList = Collections.singletonList("1");
        List<String> stringList = getLocalList("viewer.pageBrowse.pageBrowseStep", defaultList);
        List<Integer> intList = new ArrayList<>();
        for (String s : stringList) {
            try {
                intList.add(Integer.valueOf(s));
            } catch (NullPointerException | NumberFormatException e) {
                logger.error("Illegal config at 'viewer.pageBrowse.pageBrowseStep': {}", s);
            }
        }
        return intList;
    }

    /**
     *
     * @return the configured minimum number of pages required to display the page select dropdown
     * @should return correct value
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return getLocalInt("viewer.pageSelectDropdownDisplayMinPages", 3);
    }

    /**
     * getWorkflowRestUrl.
     *
     * @return The url to the Goobi workflow REST API as configured in the config_viewer. The url always ends with "/"
     * @should return correct value
     */
    public String getWorkflowRestUrl() {
        String urlString = getLocalString("urls.workflow", "localhost:8080/goobi/api/");
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }

        return urlString;
    }

    /**
     * getReCaptchaSiteKey.
     *
     * @should return correct value
     * @return the configured reCAPTCHA site key for the frontend widget
     */
    public String getReCaptchaSiteKey() {
        return getLocalString("reCaptcha.provider[@siteKey]");
    }

    /**
     * isUseReCaptcha.
     *
     * @should return correct value
     * @return true if reCAPTCHA verification is enabled, false otherwise
     */
    public boolean isUseReCaptcha() {
        return getLocalBoolean("reCaptcha[@enabled]", true);
    }

    /**
     * isSearchRisExportEnabled.
     *
     * @should return correct value
     * @return true if RIS export of search results is enabled, false otherwise
     */
    public boolean isSearchRisExportEnabled() {
        return getLocalBoolean("search.export.ris[@enabled]", false);
    }

    /**
     * isSearchExcelExportEnabled.
     *
     * @should return correct value
     * @return true if Excel export of search results is enabled, false otherwise
     */
    public boolean isSearchExcelExportEnabled() {
        return getLocalBoolean("search.export.excel[@enabled]", false);
    }

    /**
     * getSearchExcelExportFields.
     *
     * @should return all values
     * @return a list of configured export field definitions for the Excel search export
     */
    public List<ExportFieldConfiguration> getSearchExcelExportFields() {
        return getExportConfigurations("search.export.excel.field");
    }

    /**
     *
     * @param path XPath expression for the config elements
     * @return the list of configured export field configurations at the given path
     */
    List<ExportFieldConfiguration> getExportConfigurations(String path) {
        if (path == null) {
            return new ArrayList<>();
        }

        List<HierarchicalConfiguration<ImmutableNode>> nodes = getLocalConfigurationsAt(path);
        List<ExportFieldConfiguration> ret = new ArrayList<>(nodes.size());
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            String field = node.getString(".", "");
            if (StringUtils.isNotBlank(field)) {
                String label = node.getString(XML_PATH_ATTRIBUTE_LABEL);
                ret.add(new ExportFieldConfiguration(field).setLabel(label));
            }
        }

        return ret;
    }

    /**
     * getExcelDownloadTimeout.
     *
     * @return a int.
     */
    public int getExcelDownloadTimeout() {
        return getLocalInt("search.export.excel.timeout", 120);
    }

    /**
     * Return true if double page navigation is enabled for the given {@link PageType} and {@link ImageType}. Default is false
     *
     * @should return correct value
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if double page navigation is enabled for the given view attributes, false otherwise
     * @throws ViewerConfigurationException
     */
    public boolean isDoublePageNavigationEnabled(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return !isSequencePageNavigationEnabled(viewAttributes)
                && getZoomImageViewConfig(viewAttributes).getBoolean("doublePageNavigation[@enabled]", false);
    }

    /**
     * Return true if double page navigation should be used per default for the given {@link PageType} and {@link ImageType}. Default is false
     *
     * @should return correct value
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if double page navigation is enabled and set as the default mode for the given view attributes, false otherwise
     * @throws ViewerConfigurationException
     */
    public boolean isDoublePageNavigationDefault(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return isDoublePageNavigationEnabled(viewAttributes)
                && getZoomImageViewConfig(viewAttributes).getBoolean("doublePageNavigation[@default]", false);
    }

    /**
     * Return true if sequence page navigation is enabled for the given {@link PageType} and {@link ImageType}. Default is false
     *
     * @should return correct value
     * @param viewAttributes view context attributes selecting the zoom config
     * @return true if sequence page navigation is enabled for the given view attributes, false otherwise
     * @throws ViewerConfigurationException
     */
    public boolean isSequencePageNavigationEnabled(ViewAttributes viewAttributes) throws ViewerConfigurationException {
        return getZoomImageViewConfig(viewAttributes).getString("[@type]", "default").equalsIgnoreCase("sequence");
    }

    /**
     * getRestrictedImageUrls.
     *
     * @return a list of URL patterns for external image content that requires access restriction
     */
    public List<String> getRestrictedImageUrls() {
        return getLocalList("viewer.externalContent.restrictedUrls.url", new ArrayList<>());
    }

    public List<String> getIIIFLicenses() {
        return getLocalList("webapi.iiif.license", new ArrayList<>());
    }

    public boolean useExternalManifestUrls() {
        return getLocalBoolean("webapi.iiif.externalManifests[@enabled]", false);
    }

    public String getExternalManifestSolrField() {
        return getLocalString("webapi.iiif.externalManifests[@field]", "");
    }

    /**
     * getIIIFMetadataFields.
     *
     * @return a list of configured Solr field names to be included as metadata in IIIF manifests
     */
    public List<String> getIIIFMetadataFields() {
        return getLocalList("webapi.iiif.metadataFields.field", new ArrayList<>());
    }

    /**
     * getIIIFEventFields.
     *
     * @return the list of all configured event fields for IIIF manifests All fields must contain a "/" to separate the event type and the actual
     *         field name If no "/" is present in the configured field it is prepended to the entry to indicate that this field should be taken from
     *         all events
     */
    public List<String> getIIIFEventFields() {
        List<String> fields = getLocalList("webapi.iiif.metadataFields.event", new ArrayList<>());
        fields = fields.stream().map(field -> field.contains("/") ? field : "/" + field).collect(Collectors.toList());
        return fields;
    }

    /**
     * getIIIFMetadataLabel.
     *
     * @param field the value of the field
     * @return The attribute "label" of any children of webapi.iiif.metadataFields
     * @should return correct values
     */
    public String getIIIFMetadataLabel(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldsConfig = getLocalConfigurationsAt("webapi.iiif.metadataFields");
        if (fieldsConfig == null || fieldsConfig.isEmpty()) {
            return "";
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = fieldsConfig.get(0).childConfigurationsAt("");
        for (HierarchicalConfiguration<ImmutableNode> fieldNode : fields) {
            String value = fieldNode.getString(".");
            if (value != null && value.equals(field)) {
                return fieldNode.getString(XML_PATH_ATTRIBUTE_LABEL, "");
            }
        }
        return "";
    }

    /**
     * Configured in webapi.iiif.discovery.activitiesPerPage. Default value is 100
     *
     * @return The number of activities to display per collection page in the IIIF discovery api
     */
    public int getIIIFDiscoveryAvtivitiesPerPage() {
        return getLocalInt("webapi.iiif.discovery.activitiesPerPage", 100);
    }

    /**
     * getIIIFLogo.
     *
     * @return the list of configured IIIF logo URLs
     */
    public List<String> getIIIFLogo() {
        return getLocalList("webapi.iiif.logo", new ArrayList<>());
    }

    /**
     * getIIIFNavDateField.
     *
     * @return the configured Solr field name used as the IIIF navigation date field, or null if not configured
     */
    public String getIIIFNavDateField() {
        return getLocalString("webapi.iiif.navDateField", null);
    }

    /**
     * getIIIFAttribution.
     *
     * @return the list of configured IIIF attribution strings
     */
    public List<String> getIIIFAttribution() {
        return getLocalList("webapi.iiif.attribution", new ArrayList<>());
    }

    /**
     * getIIIFDescriptionFields.
     *
     * @return a list of configured Solr field names used as description in IIIF manifests
     */
    public List<String> getIIIFDescriptionFields() {
        return getLocalList("webapi.iiif.descriptionFields.field", new ArrayList<>());
    }

    public List<String> getIIIFLabelFields() {
        return getLocalList("webapi.iiif.labelFields.field", new ArrayList<>());
    }

    public List<Locale> getIIIFTranslationLocales() {
        List<Locale> list = getLocalList("webapi.iiif.translations.locale", new ArrayList<>())
                .stream()
                .map(Locale::forLanguageTag)
                .filter(l -> StringUtils.isNotBlank(l.getLanguage()))
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            return ViewerResourceBundle.getAllLocales();
        }

        return list;
    }

    /**
     *
     * @return The SOLR field containing a rights url for a IIIF3 manifest if one is configured
     */
    public String getIIIFRightsField() {
        return getLocalString("webapi.iiif.rights", null);
    }

    /**
     * Uses {@link #getIIIFAttribution()} as fallback.
     *
     * @return the message key to use for the IIIF3 requiredStatement value if the statement should be added to manifests.
     */
    public String getIIIFRequiredValue() {
        return getLocalString("webapi.iiif.requiredStatement.value", getIIIFAttribution().stream().findFirst().orElse(null));
    }

    /**
     *
     * @return the message key to use for the IIIF3 requiredStatement label. Default is "Attribution"
     */
    public String getIIIFRequiredLabel() {
        return getLocalString("webapi.iiif.requiredStatement.label", "Attribution");
    }

    /**
     *
     * @return The list of configurations for IIIF3 providers
     * @throws PresentationException if a provider or a homepage configuration misses the url or label element
     */
    public List<ProviderConfiguration> getIIIFProvider() throws PresentationException {
        List<ProviderConfiguration> provider = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("webapi.iiif.provider");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            provider.add(new ProviderConfiguration(config));
        }

        return provider;
    }

    /**
     * @param vr {@link VariableReplacer}
     * @return The list of configurations for IIIF3 providers
     * @throws PresentationException if a provider or a homepage configuration misses the url or label element
     */
    public List<ProviderConfiguration> getIIIFProvider(VariableReplacer vr) throws PresentationException {
        List<ProviderConfiguration> provider = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("webapi.iiif.provider");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            provider.add(new ProviderConfiguration(config, vr));
        }

        return provider;
    }

    /**
     *
     * @return true if the PDF rendering link is visible in IIIF manifests, false otherwise
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPDF() {
        return getLocalBoolean("webapi.iiif.rendering.pdf[@enabled]", true);
    }

    /**
     *
     * @return true if the viewer rendering link is visible in IIIF manifests, false otherwise
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingViewer() {
        return getLocalBoolean("webapi.iiif.rendering.viewer[@enabled]", true);
    }

    public String getLabelIIIFRenderingPDF() {
        return getLocalString("webapi.iiif.rendering.pdf.label", null);
    }

    public String getLabelIIIFRenderingViewer() {
        return getLocalString("webapi.iiif.rendering.viewer.label", null);
    }

    /**
     *
     * @return true if the plain text rendering link is visible in IIIF manifests, false otherwise
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPlaintext() {
        return getLocalBoolean("webapi.iiif.rendering.plaintext[@enabled]", true);
    }

    /**
     *
     * @return true if the ALTO rendering link is visible in IIIF manifests, false otherwise
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingAlto() {
        return getLocalBoolean("webapi.iiif.rendering.alto[@enabled]", true);
    }

    public String getLabelIIIFRenderingPlaintext() {
        return getLocalString("webapi.iiif.rendering.plaintext.label", null);
    }

    public String getLabelIIIFRenderingAlto() {
        return getLocalString("webapi.iiif.rendering.alto.label", null);
    }

    public boolean isVisibleIIIFSeeAlsoMets() {
        return getLocalBoolean("webapi.iiif.seeAlso.mets[@enabled]", true);
    }

    public String getLabelIIIFSeeAlsoMets() {
        return getLocalString("webapi.iiif.seeAlso.mets.label", "METS/MODS");
    }

    public boolean isVisibleIIIFSeeAlsoLido() {
        return getLocalBoolean("webapi.iiif.seeAlso.lido[@enabled]", true);
    }

    public String getLabelIIIFSeeAlsoLido() {
        return getLocalString("webapi.iiif.seeAlso.lido.label", "LIDO");
    }

    public List<ManifestLinkConfiguration> getIIIFSeeAlsoMetadataConfigurations() {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("webapi.iiif.seeAlso.metadata");
        List<ManifestLinkConfiguration> links = new ArrayList<>(configs.size());
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            String label = config.getString(XML_PATH_ATTRIBUTE_LABEL, "");
            String format = config.getString("[@format]", "");
            MetadataParameter param = MetadataParameter.createFromConfig(config.configurationAt("param"), true);
            Metadata md = new Metadata("", "", Arrays.asList(param));
            links.add(new ManifestLinkConfiguration(label, format, md));
        }
        return links;
    }

    /**
     * getSitelinksField.
     *
     * @should return correct value
     * @return the configured Solr field name used as the sitelinks field
     */
    public String getSitelinksField() {
        return getLocalString("sitelinks.sitelinksField");
    }

    /**
     *
     * @return true if sitelinks are enabled, false otherwise
     * @should return correct value
     */
    public boolean isSitelinksEnabled() {
        return getLocalBoolean("sitelinks[@enabled]", true);
    }

    /**
     * getSitelinksFilterQuery.
     *
     * @should return correct value
     * @return the configured Solr filter query for sitelinks
     */
    public String getSitelinksFilterQuery() {
        return getLocalString("sitelinks.sitelinksFilterQuery");
    }

    /**
     * getConfiguredCollections.
     *
     * @return a list of configured collection Solr field names
     */
    public List<String> getConfiguredCollections() {
        return getLocalList("collections.collection[@field]", new ArrayList<>());

    }

    /**
     * getWebApiToken.
     *
     * @should return correct value
     * @return the configured web API authorization token
     */
    public String getWebApiToken() {
        return getLocalString("webapi.authorization.token", "");
    }

    /**
     * isAllowRedirectCollectionToWork.
     *
     * @return true if opening a collection containing only a single work should redirect to that work
     * @should return correct value
     */
    public boolean isAllowRedirectCollectionToWork() {
        return getLocalBoolean("collections.redirectToWork", true);
    }

    /**
     * getTwitterUserName.
     *
     * @return the configured Twitter username for embedding, or null if not configured
     * @should return correct value
     */
    public String getTwitterUserName() {
        return getLocalString("embedding.twitter.userName");
    }

    /**
     * getLimitImageHeightUpperRatioThreshold.
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightUpperRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@upperRatioThreshold]", 0.3f);
    }

    /**
     * getLimitImageHeightLowerRatioThreshold.
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightLowerRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@lowerRatioThreshold]", 3f);
    }

    /**
     * isLimitImageHeight.
     *
     * @should return correct value
     * @return true if image height should be limited based on the configured ratio thresholds, false otherwise
     */
    public boolean isLimitImageHeight() {
        return getLocalBoolean("viewer.limitImageHeight", true);
    }

    /**
     * isAddCORSHeader.
     *
     * @should return correct value
     * @return true if CORS headers should be added to API responses, false otherwise
     */
    public boolean isAddCORSHeader() {
        return getLocalBoolean("webapi.cors[@enabled]", false);
    }

    /**
     * Gets the value configured in webapi.cors. Default is "*"
     *
     * @should return correct value
     * @return the configured CORS header value for API responses
     */
    public String getCORSHeaderValue() {
        return getLocalString("webapi.cors", "*");
    }

    /**
     * @return true if the IIIF image content location should be disclosed in responses, false otherwise
     */
    public boolean isDiscloseImageContentLocation() {
        return getLocalBoolean("webapi.iiif.discloseContentLocation", true);
    }

    /**
     *
     * @return the configured display style of the copyright indicator widget
     * @should return correct value
     */
    public String getCopyrightIndicatorStyle() {
        return getSidebarWidgetStringValue("copyright", "[@style]", "widget");
    }

    /**
     *
     * @return the configured Solr field name used for the copyright indicator status
     * @should return correct value
     */
    public String getCopyrightIndicatorStatusField() {
        return getSidebarWidgetStringValue("copyright", "status[@field]", null);
    }

    /**
     *
     * @param value field value to match against configured entries
     * @return the configured copyright indicator status matching the given field value, or null if not found
     * @should return correct value
     */
    public CopyrightIndicatorStatus getCopyrightIndicatorStatusForValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("copyright");
        if (widgetConfig != null) {
            List<HierarchicalConfiguration<ImmutableNode>> configs = widgetConfig.configurationsAt("status.value");
            for (HierarchicalConfiguration<ImmutableNode> config : configs) {
                String content = config.getString("[@content]");
                if (value.equals(content)) {
                    String statusName = config.getString("[@status]");
                    Status status = CopyrightIndicatorStatus.Status.getByName(statusName);
                    if (status == null) {
                        logger.warn("No copyright indicator status found for configured name: {}", statusName);
                        status = Status.OPEN;
                    }
                    String description = config.getString(XML_PATH_ATTRIBUTE_DESCRIPTION);
                    return new CopyrightIndicatorStatus(status, description);
                }
            }
        }

        return null;
    }

    /**
     *
     * @param value field value to match against configured entries
     * @return the configured copyright indicator license matching the given field value, or null if not found
     * @should return correct value
     */
    public CopyrightIndicatorLicense getCopyrightIndicatorLicenseForValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        HierarchicalConfiguration<ImmutableNode> widgetConfig = getSidebarWidgetConfiguration("copyright");
        if (widgetConfig != null) {
            List<HierarchicalConfiguration<ImmutableNode>> configs = widgetConfig.configurationsAt("license.value");
            for (HierarchicalConfiguration<ImmutableNode> config : configs) {
                String content = config.getString("[@content]");
                if (value.equals(content)) {
                    String description = config.getString(XML_PATH_ATTRIBUTE_DESCRIPTION);
                    String[] icons = config.getStringArray("icon");
                    // Filter out empty strings that Apache Commons Configuration may return when no <icon> elements are present
                    List<String> iconList = icons != null
                            ? Arrays.stream(icons).filter(s -> s != null && !s.isBlank()).collect(Collectors.toList())
                            : new ArrayList<>();
                    return new CopyrightIndicatorLicense(description, iconList);
                }
            }
        }

        return null;
    }

    /**
     *
     * @return the configured Solr field name used for the copyright indicator license
     * @should return correct value
     */
    public String getCopyrightIndicatorLicenseField() {
        return getSidebarWidgetStringValue("copyright", "license[@field]", null);
    }

    public boolean isDisplaySocialMediaShareLinks() {
        return getLocalBoolean("webGuiDisplay.displaySocialMediaShareLinks", false);
    }

    public boolean isDisplayAnchorLabelInTitleBar(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_TOC_TITLEBARLABEL_TEMPLATE);
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getBoolean("displayAnchorTitle", false);
        }

        return false;
    }

    public String getAnchorLabelInTitleBarPrefix(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_TOC_TITLEBARLABEL_TEMPLATE);
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@prefix]", "");
        }

        return "";
    }

    public String getAnchorLabelInTitleBarSuffix(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_TOC_TITLEBARLABEL_TEMPLATE);
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@suffix]", " ");
        }

        return " ";
    }

    public String getMapBoxToken() {
        return getLocalString("maps.mapbox.token", "");
    }

    public String getMapBoxUser() {
        return getLocalString("maps.mapbox.user", "");
    }

    public String getMapBoxStyleId() {
        return getLocalString("maps.mapbox.styleId", "");
    }

    public boolean isDisplayAddressSearchInMap() {
        return getLocalBoolean("maps.mapbox.addressSearch[@enabled]", true);
    }

    /**
     * @param name geo map marker name to look up
     * @return the configured GeoMapMarker with the given name, or null if not found
     */
    public GeoMapMarker getGeoMapMarker(String name) {
        return getGeoMapMarkers().stream().filter(m -> name.equalsIgnoreCase(m.getName())).findAny().orElse(null);
    }

    /**
     *
     * @return a list of solr field names containing GeoJson data used to create markers in maps
     */
    public List<String> getGeoMapMarkerFields() {
        return getLocalList("maps.coordinateFields.field", Arrays.asList("MD_GEOJSON_POINT", "NORM_COORDS_GEOJSON"));
    }

    public boolean useHeatmapForCMSMaps() {
        return getLocalBoolean("maps.cms.heatmap[@enabled]", false);
    }

    public boolean useHeatmapForMapSearch() {
        return getLocalBoolean("maps.search.heatmap[@enabled]", false);
    }

    public boolean useHeatmapForFacetting() {
        return getLocalBoolean("maps.facet.heatmap[@enabled]", false);
    }

    public GeoMapMarker getMarkerForMapSearch() {
        HierarchicalConfiguration<ImmutableNode> config = getLocalConfigurationAt("maps.search.marker");
        return readGeoMapMarker(config);
    }

    public GeoMapMarker getMarkerForFacetting() {
        HierarchicalConfiguration<ImmutableNode> config = getLocalConfigurationAt("maps.facet.marker");
        return readGeoMapMarker(config);
    }

    public String getSelectionColorForMapSearch() {
        return getLocalString("maps.search.selection[@color]", "#d9534f");
    }

    public String getSelectionColorForFacetting() {
        return getLocalString("maps.facet.selection[@color]", "#d9534f");
    }

    public boolean includeCoordinateFieldsFromMetadataDocs() {
        return getLocalBoolean("maps.coordinateFields[@includeMetadataDocs]", false);
    }

    public List<GeoMapMarker> getGeoMapMarkers() {

        List<GeoMapMarker> markers = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("maps.markers.marker");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            markers.add(readGeoMapMarker(config));
        }
        return markers;

    }

    public String getRecordGeomapMarker(String templateName) {
        HierarchicalConfiguration<ImmutableNode> template = selectTemplate(getLocalConfigurationsAt("maps.record.template"), templateName, true);
        if (template != null) {
            List<HierarchicalConfiguration<ImmutableNode>> configs = template.configurationsAt("marker");
            return configs.stream()
                    .findAny()
                    .map(config -> config.getString(".", ""))
                    .orElse("");
        }
        return "";
    }

    /**
     * @param config configuration node for the marker element
     * @return the GeoMapMarker parsed from the given configuration node, or an empty default marker if config is null
     */
    public static GeoMapMarker readGeoMapMarker(HierarchicalConfiguration<ImmutableNode> config) {
        GeoMapMarker marker = null;
        if (config != null) {
            String name = config.getString(".", "default");
            marker = new GeoMapMarker(name);
            marker.setExtraClasses(config.getString("[@extraClasses]", marker.getExtraClasses()));
            marker.setIcon(config.getString(XML_PATH_ATTRIBUTE_ICON, marker.getIcon()));
            marker.setIconColor(config.getString("[@iconColor]", marker.getIconColor()));
            marker.setIconRotate(config.getInt("[@iconRotate]", marker.getIconRotate()));
            marker.setMarkerColor(config.getString("[@markerColor]", marker.getMarkerColor()));
            marker.setHighlightColor(config.getString("[@highlightColor]", marker.getHighlightColor()));
            marker.setNumber(config.getString("[@number]", marker.getNumber()));
            marker.setPrefix(config.getString("[@prefix]", marker.getPrefix()));
            marker.setShape(config.getString("[@shape]", marker.getShape()));
            marker.setSvg(config.getBoolean("[@svg]", marker.isSvg()));
            marker.setShadow(config.getBoolean("[@shadow]", marker.isShadow()));
            marker.setUseDefault(config.getBoolean("[@useDefaultIcon]", marker.isUseDefault()));
            marker.setHighlightIcon(config.getString("[@highlightIcon]", marker.getHighlightIcon()));
            marker.setType(Optional.ofNullable(MarkerType.getTypeByName(config.getString("[@type]", MarkerType.EXTRA_MARKERS.getName())))
                    .orElse(MarkerType.EXTRA_MARKERS));
            marker.setClassName(config.getString("[@class]", ""));
            return marker;
        }
        return new GeoMapMarker("");
    }

    /**
     * Find the template with the given name in the templateList. If no such template exists, find the template with name _DEFAULT. Failing that,
     * return null;
     *
     * @param templateList list of template configuration nodes to search
     * @param name template name to match
     * @return the matching template configuration node, falling back to _DEFAULT, or null if neither is found
     */
    private static HierarchicalConfiguration<ImmutableNode> getMatchingConfig(List<HierarchicalConfiguration<ImmutableNode>> templateList,
            String name) {
        if (name == null || templateList == null) {
            return null;
        }

        HierarchicalConfiguration<ImmutableNode> conf = null;
        HierarchicalConfiguration<ImmutableNode> defaultConf = null;
        for (HierarchicalConfiguration<ImmutableNode> subConf : templateList) {
            if (name.equalsIgnoreCase(subConf.getString(XML_PATH_ATTRIBUTE_NAME))) {
                conf = subConf;
                break;
            } else if (StringConstants.DEFAULT_NAME.equalsIgnoreCase(subConf.getString(XML_PATH_ATTRIBUTE_NAME))) {
                defaultConf = subConf;
            }
        }
        if (conf != null) {
            return conf;
        }

        return defaultConf;
    }

    /**
     *
     * @return the list of configured license descriptions
     */
    public List<LicenseDescription> getLicenseDescriptions() {
        List<LicenseDescription> licenses = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> licenseNodes = getLocalConfigurationsAt("metadata.licenses.license");
        for (HierarchicalConfiguration<ImmutableNode> node : licenseNodes) {
            String url = node.getString(XML_PATH_ATTRIBUTE_URL, "");
            if (StringUtils.isNotBlank(url)) {
                String label = node.getString(XML_PATH_ATTRIBUTE_LABEL, url);
                LicenseDescription license = new LicenseDescription(url);
                license.setLabel(label);
                licenses.add(license);
            }
        }

        return licenses;
    }

    /**
     *
     * @return the configured lazy loading threshold for archive tree nodes
     * @should return correct value
     */
    public int getArchivesLazyLoadingThreshold() {
        return getLocalInt("archives[@lazyLoadingThreshold]", 100);
    }

    public boolean isExpandArchiveEntryOnSelection() {
        return getLocalBoolean("archives.expandOnSelect", false);
    }

    public Map<String, String> getArchiveNodeTypes() {
        List<HierarchicalConfiguration<ImmutableNode>> nodeTypes = getLocalConfigurationsAt("archives.nodeTypes.node");
        nodeTypes.get(0).getString(getReCaptchaSiteKey());
        return nodeTypes.stream()
                .collect(Collectors.toMap(node -> node.getString(XML_PATH_ATTRIBUTE_NAME), node -> node.getString(XML_PATH_ATTRIBUTE_ICON)));
    }

    public Pair<String, String> getDefaultArchiveNodeType() {
        List<HierarchicalConfiguration<ImmutableNode>> nodeTypes = getLocalConfigurationsAt("archives.nodeTypes.node");
        return nodeTypes.stream()
                .filter(node -> node.getBoolean(XML_PATH_ATTRIBUTE_DEFAULT, false))
                .findFirst()
                .map(node -> Pair.of(node.getString("[@name]", ""), node.getString(XML_PATH_ATTRIBUTE_ICON, "")))
                .orElse(Pair.of("", ""));
    }

    /**
     *
     * @return true if user-generated content annotations should be displayed below the image, false otherwise
     */
    public boolean isDisplayUserGeneratedContentBelowImage() {
        return getLocalBoolean("webGuiDisplay.displayUserGeneratedContentBelowImage", false);
    }

    /**
     * config: <code>&#60;iiif use-version="3.0"&#62;&#60;/iiif&#62;</code>.
     *
     * @return the configured IIIF API version to use for manifests
     */
    public String getIIIFVersionToUse() {
        return getLocalString("webapi.iiif[@use-version]", "2.1.1");
    }

    /**
     *
     * @return the list of configured translation groups
     * @should read config items correctly
     */
    public List<TranslationGroup> getTranslationGroups() {
        List<TranslationGroup> ret = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> groupNodes = getLocalConfigurationsAt("translations.group");
        int id = 0;
        for (HierarchicalConfiguration<ImmutableNode> groupNode : groupNodes) {
            String typeValue = groupNode.getString(XML_PATH_ATTRIBUTE_TYPE);
            if (StringUtils.isBlank(typeValue)) {
                logger.warn("translations/group/@type may not be empty.");
                continue;
            }
            TranslationGroupType type = TranslationGroupType.getByName(typeValue);
            if (type == null) {
                logger.warn("Unknown translations/group/@type: {}", typeValue);
                continue;
            }
            String name = groupNode.getString(XML_PATH_ATTRIBUTE_NAME);
            if (StringUtils.isBlank(name)) {
                logger.warn("translations/group/@name may not be empty.");
                continue;
            }
            String description = groupNode.getString(XML_PATH_ATTRIBUTE_DESCRIPTION);
            List<HierarchicalConfiguration<ImmutableNode>> keyNodes = groupNode.configurationsAt("key");
            TranslationGroup group = TranslationGroup.create(id, type, name, description, keyNodes.size());
            for (HierarchicalConfiguration<ImmutableNode> keyNode : keyNodes) {
                String value = keyNode.getString(".");
                if (StringUtils.isBlank(value)) {
                    logger.warn("translations/group/key may not be empty.");
                    continue;
                }
                boolean regex = keyNode.getBoolean("[@regex]", false);
                group.getItems().add(TranslationGroupItem.create(type, value, regex));
            }
            ret.add(group);
            id++;
        }

        return ret;
    }

    /**
     *
     * @return true if crowdsourcing annotation text should be displayed in the image view, false otherwise
     */
    public boolean isDisplayAnnotationTextInImage() {
        return getLocalBoolean("webGuiDisplay.displayAnnotationTextInImage", true);
    }

    /**
     *
     * @return true if active facets should be used as expand queries in search, false otherwise
     * @should return correct value
     */
    public boolean isUseFacetsAsExpandQuery() {
        return getLocalBoolean("search.useFacetsAsExpandQuery[@enabled]", false);
    }

    /**
     *
     * @return the list of configured facet query values allowed as expand queries
     * @should return all configured elements
     */
    public List<String> getAllowedFacetsForExpandQuery() {
        return getLocalList("search.useFacetsAsExpandQuery.facetQuery");
    }

    /**
     *
     * @return true if search result groups are enabled, false otherwise
     * @should return correct value
     */
    public boolean isSearchResultGroupsEnabled() {
        return getLocalBoolean("search.resultGroups[@enabled]", false);
    }

    /**
     *
     * @return the list of configured search result groups
     * @should return all configured elements
     */
    public List<SearchResultGroup> getSearchResultGroups() {
        List<SearchResultGroup> ret = new ArrayList<>();

        List<HierarchicalConfiguration<ImmutableNode>> groupNodes = getLocalConfigurationsAt("search.resultGroups.group");
        for (HierarchicalConfiguration<ImmutableNode> groupNode : groupNodes) {
            String name = groupNode.getString(XML_PATH_ATTRIBUTE_NAME);
            if (StringUtils.isBlank(name)) {
                logger.warn("search/resultGroups/group/@name may not be empty.");
                continue;
            }
            String query = groupNode.getString("[@query]");
            if (StringUtils.isBlank(query)) {
                logger.warn("search/resultGroups/group/@query may not be empty.");
                continue;
            }
            int previewHitCount = groupNode.getInt("[@previewHitCount]", 10);
            ret.add(new SearchResultGroup(name, query, previewHitCount));
        }

        return ret;
    }

    /**
     *
     * @return true if content upload is enabled, false otherwise
     * @should return correct value
     */
    public boolean isContentUploadEnabled() {
        return getLocalBoolean("upload[@enabled]", false);
    }

    /**
     *
     * @return the configured authentication token for content upload
     * @should return correct value
     */
    public String getContentUploadToken() {
        return getLocalString("upload.token");
    }

    /**
     *
     * @return the configured docstruct type used for newly uploaded content records
     * @should return correct value
     */
    public String getContentUploadDocstruct() {
        return getLocalString("upload.docstruct", "monograph");
    }

    /**
     *
     * @return the configured Goobi workflow process template name used for content upload
     * @should return correct value
     */
    public String getContentUploadTemplateName() {
        return getLocalString("upload.templateName");
    }

    /**
     *
     * @return the configured Goobi workflow process property name indicating that an upload was rejected
     * @should return correct value
     */
    public String getContentUploadRejectionPropertyName() {
        return getLocalString("upload.rejectionPropertyName");
    }

    /**
     *
     * @return the configured Goobi workflow process property name containing the rejection reason for an upload
     * @should return correct value
     */
    public String getContentUploadRejectionReasonPropertyName() {
        return getLocalString("upload.rejectionReasonPropertyName");
    }

    public String getCrowdsourcingCampaignItemOrder() {
        return getLocalString("campaigns.itemOrder", "fixed");
    }

    public int getGeomapAnnotationZoom() {
        return getLocalInt("campaigns.annotations.geoCoordinates.zoom", 7);
    }

    public int getCrowdsourcingCampaignGeomapZoom() {
        return getLocalInt("campaigns.geoMap.zoom", 7);
    }

    public String getCrowdsourcingCampaignGeomapLngLat() {
        return getLocalString("campaigns.geoMap.lngLat", "11.073397, 49.451993");
    }

    public String getCrowdsourcingCampaignGeomapTilesource() {
        return getLocalString("campaigns.geoMap.tilesource", "mapbox");

    }

    public boolean isStatisticsEnabled() {
        return getLocalBoolean("statistics[@enabled]", false);
    }

    public String getCrawlerDetectionRegex() {
        return getLocalString("statistics.crawlerDetection[@regex]",
                ".*[bB]ot.*|.*Yahoo! Slurp.*|.*Feedfetcher-Google.*|.*Apache-HttpClient.*|.*[Ss]pider.*|.*[Cc]rawler.*|.*nagios.*|.*Yandex.*");
    }

    /**
     *
     * @return true if the in-application configuration file editor is enabled, false otherwise
     * @should return correct value
     */
    public boolean isConfigEditorEnabled() {
        return getLocalBoolean("configEditor[@enabled]", false);
    }

    /**
     *
     * @return the configured number of backup files to keep for edited configuration files
     * @should return correct value
     */
    public int getConfigEditorBackupFiles() {
        return getLocalInt("configEditor[@backupFiles]", 0);
    }

    /**
     *
     * @return the list of configured directories accessible through the configuration file editor
     * @should return all configured elements
     */
    public List<String> getConfigEditorDirectories() {
        return getLocalList("configEditor.directory", new ArrayList<>());
    }

    /**
     * 
     * @return true if enabled; false otherwise
     * @should return correct value
     */
    public boolean isProxyEnabled() {
        return getLocalBoolean("proxy[@enabled]", false);
    }

    /**
     *
     * @return the configured HTTP proxy URL
     * @should return correct value
     */
    public String getProxyUrl() {
        return getLocalString("proxy.proxyUrl");
    }

    /**
     * 
     * @return Configured port number; 0 if none found
     * @should return correct value
     */
    public int getProxyPort() {
        return getLocalInt("proxy.proxyPort", 0);
    }

    /**
     *
     * @param url URL whose host is checked against the proxy whitelist
     * @return true if the host of the given URL is on the configured proxy whitelist, false otherwise
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @should return true if host whitelisted
     */
    public boolean isHostProxyWhitelisted(String url) throws MalformedURLException, URISyntaxException {
        URL urlAsURL = new URI(url).toURL();
        return getProxyWhitelist().contains(urlAsURL.getHost());
    }

    /**
     *
     * @return the list of configured host names that bypass the proxy
     */
    public List<String> getProxyWhitelist() {
        return getLocalList("proxy.whitelist.host");
    }

    /**
     *
     * @return the list of configured host names allowed as redirect targets after HTTP header login
     */
    public List<String> getHttpHeaderLoginRedirectWhitelist() {
        return getLocalList("user.authenticationProviders.redirectWhitelist.host");
    }

    // active mq configuration //

    public boolean isStartInternalMessageBroker() {
        return getLocalBoolean("activeMQ[@enabled]", true);
    }

    public int getNumberOfParallelMessages() {
        return getLocalInt("activeMQ[@numberOfParallelMessages]", 1);
    }

    public int getActiveMQMessagePurgeInterval() {
        return getLocalInt("activeMQ.deleteCompletedTasksAfterDays", 90);
    }

    public String getQuartzSchedulerCronExpression(String taskName) {
        try {
            ITaskType type = TaskType.getByName(taskName.toUpperCase());
            return getLocalString("quartz.scheduler." + taskName.toLowerCase() + ".cronExpression", type.getDefaultCronExpression());

        } catch (IllegalArgumentException e) {
            return getLocalString("quartz.scheduler." + taskName.toLowerCase() + ".cronExpression", getQuartzSchedulerCronExpression());
        }
    }

    public String getQuartzSchedulerCronExpression() {
        return getLocalString("quartz.scheduler.cronExpression", "0 0 0 * * ?");
    }

    public boolean isDeveloperPageActive() {
        return getLocalBoolean("deveoper[@enabled]", false);
    }

    public String getDeveloperScriptPath(String purpose) {
        List<HierarchicalConfiguration<ImmutableNode>> scriptNodes = getLocalConfigurationsAt("developer.script");
        return scriptNodes.stream()
                .filter(node -> node.getString("[@purpose]", "").equals(purpose))
                .map(node -> node.getString(".", ""))
                .findAny()
                .orElse("");
    }

    /**
     * @param field Solr field name to check for language suffix
     * @param language language code to compare against the field suffix
     * @return true if the field has a language suffix (_LANG_XX) that does not match the given language, false otherwise
     */
    public static boolean isLanguageVersionOtherThan(String field, String language) {
        return field.matches(".*_LANG_[A-Z][A-Z]") && !field.matches(".*_LANG_" + language.toUpperCase());
    }

    public Optional<String> getStringFormat(String type, Locale locale) {

        String path = String.format("viewer.formats.%s.%s", type, locale.getLanguage());
        return Optional.ofNullable(getLocalString(path, null));
    }

    public String getThemePullScriptPath() {
        return getLocalString("developer.scripts.pullTheme", "{config-folder-path}/script_theme-pull.sh {theme-path}/../../../../");
    }

    /**
     * 
     * @return boolean
     * @should return correct value
     */
    public boolean isPullThemeEnabled() {
        return getLocalBoolean("developer.scripts.pullTheme[@enabled]", true);
    }

    public String getCreateDeveloperPackageScriptPath() {
        return getLocalString("developer.scripts.createDeveloperPackage",
                "{config-folder-path}/script_create_package.sh -d viewer -f {base-path} -w /var/www/  -s {solr-url}");
    }

    public String getMediaTypeHandling(String mimeType) {
        String defaultDisposition = "attachment";
        String defaultMimeType = "default";
        return getLocalConfigurationsAt("viewer.mediaTypes.type").stream()
                .filter(conf -> conf.getString("[@mimeType]", defaultMimeType).equals(mimeType))
                .map(conf -> conf.getString("contentDisposition", defaultDisposition))
                .findFirst()
                .orElse(defaultDisposition);

    }

    public String getMediaTypeRedirectUrl(String mimeType) {
        String defaultMimeType = "default";
        return getLocalConfigurationsAt("viewer.mediaTypes.type").stream()
                .filter(conf -> conf.getString("[@mimeType]", defaultMimeType).equals(mimeType))
                .map(conf -> conf.getString("redirectHandling", ""))
                .findFirst()
                .orElse("");

    }

    public String getSearchHitStyleClass() {
        return getLocalString("search.hitStyleClass", "docstructtype__{record.DOCSTRCT}");
    }

    public String getRecordViewStyleClass() {
        return getLocalString("viewer.viewStyleClass", "docstructtype__{record.DOCSTRCT}");
    }

    public Duration getDownloadPdfTimeToLive() {
        int num = getLocalInt("pdf.expireAfter", 14);
        String unitString = getLocalString("pdf.expireAfter[@unit]", "DAYS");
        TimeUnit unit = TimeUnit.DAYS;
        try {
            unit = TimeUnit.valueOf(unitString);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid value in configuration pdf.expireAfter[@unit]: {}", unitString);
        }
        return Duration.of((long) num, unit.toChronoUnit());

    }

}
