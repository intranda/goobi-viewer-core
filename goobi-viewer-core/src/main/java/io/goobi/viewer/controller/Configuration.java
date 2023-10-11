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
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import io.goobi.viewer.controller.model.FeatureSetConfiguration;
import io.goobi.viewer.controller.model.LabeledValue;
import io.goobi.viewer.controller.model.ManifestLinkConfiguration;
import io.goobi.viewer.controller.model.ProviderConfiguration;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.cms.Highlight;
import io.goobi.viewer.model.export.ExportFieldConfiguration;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.maps.GeoMapMarker.MarkerType;
import io.goobi.viewer.model.maps.View;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataView;
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
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.collections.DcSortingList;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * Configuration class.
 * </p>
 */
public class Configuration extends AbstractConfiguration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    public static final String CONFIG_FILE_NAME = "config_viewer.xml";

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
     * <p>
     * Constructor for Configuration.
     * </p>
     *
     * @param configFilePath a {@link java.lang.String} object.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) {
                            if (builder.getReloadingController().checkForReloading(null)) {
                                //
                            }
                        }
                    });
        } else {
            logger.error("Default configuration file not found: {}; Base path is {}", builder.getFileHandler().getFile().getAbsoluteFile(),
                    builder.getFileHandler().getBasePath());
        }

        // Load local config file
        File fileLocal = new File(getConfigLocalPath() + CONFIG_FILE_NAME);
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
            builderLocal.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) {
                            if (builderLocal.getReloadingController().checkForReloading(null)) {
                                //
                            }
                        }
                    });
        }

        // Load stopwords
        try {
            stopwords = loadStopwords(getStopwordsFilePath());
        } catch (

        FileNotFoundException e) {
            logger.error(e.getMessage());
            stopwords = new HashSet<>(0);
        } catch (IOException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            stopwords = new HashSet<>(0);
        }
    }

    /**
     * <p>
     * loadStopwords.
     * </p>
     *
     * @param stopwordsFilePath a {@link java.lang.String} object.
     * @return a {@link java.util.Set} object.
     * @throws java.io.IOException if any.
     * @should load all stopwords
     * @should remove parts starting with pipe
     * @should not add empty stopwords
     * @should throw IllegalArgumentException if stopwordsFilePath empty
     * @should throw FileNotFoundException if file does not exist
     */
    protected static Set<String> loadStopwords(String stopwordsFilePath) throws IOException {
        if (StringUtils.isEmpty(stopwordsFilePath)) {
            throw new IllegalArgumentException("stopwordsFilePath may not be null or empty");
        }

        if (StringUtils.isEmpty(stopwordsFilePath)) {
            logger.warn("'stopwordsFile' not configured. Stop words cannot be filtered from search queries.");
            return new HashSet<>();
        }

        Set<String> ret = new HashSet<>();
        try (FileReader fr = new FileReader(stopwordsFilePath); BufferedReader br = new BufferedReader(fr)) {
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
     * @return a {@link java.util.Set} object.
     */
    public Set<String> getStopwords() {
        return stopwords;
    }

    /*********************************** direct config results ***************************************/

    /**
     * <p>
     * getConfigLocalPath.
     * </p>
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
     * <p>
     * getLocalRessourceBundleFile.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocalRessourceBundleFile() {
        return getConfigLocalPath() + "messages_de.properties";
    }

    /**
     * <p>
     * getViewerThumbnailsPerPage.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerThumbnailsPerPage() {
        return getLocalInt("viewer.thumbnailsPerPage", 10);
    }

    /**
     * <p>
     * getViewerMaxImageWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageWidth() {
        return getLocalInt("viewer.maxImageWidth", 2000);
    }

    /**
     * <p>
     * getViewerMaxImageHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageHeight() {
        return getLocalInt("viewer.maxImageHeight", 2000);
    }

    /**
     * <p>
     * getViewerMaxImageScale.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageScale() {
        return getLocalInt("viewer.maxImageScale", 500);
    }

    /**
     * <p>
     * isRememberImageZoom.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isRememberImageZoom() {
        return getLocalBoolean("viewer.rememberImageZoom[@enabled]", false);
    }

    /**
     * <p>
     * isRememberImageRotation.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isRememberImageRotation() {
        return getLocalBoolean("viewer.rememberImageRotation[@enabled]", false);
    }

    /**
     * <p>
     * getDfgViewerUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDfgViewerUrl() {
        return getLocalString("urls.dfg-viewer", "https://dfg-viewer.de/v2?set[mets]=");
    }

    /**
     * 
     * @return
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
     * @param type
     * @param template
     * @param fallbackToDefaultTemplate
     * @param topstructValueFallbackDefaultValue
     * @return List of metadata configurations
     * @should throw IllegalArgumentException if type null
     * @should return empty list if no metadata lists configured
     * @should return empty list if metadataList contains no templates
     * @should return empty list if list type not found
     */
    public List<Metadata> getMetadataConfigurationForTemplate(String type, String template, boolean fallbackToDefaultTemplate,
            boolean topstructValueFallbackDefaultValue) {
        // logger.trace("getMetadataConfigurationForTemplate: {}/{}", type, template); //NOSONAR Sometimes used for debugging
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> metadataLists = getLocalConfigurationsAt("metadata.metadataList");
        if (metadataLists == null) {
            logger.trace("no metadata lists found");
            return new ArrayList<>(); // must be a mutable list!
        }

        for (HierarchicalConfiguration<ImmutableNode> metadataList : metadataLists) {
            if (type.equals(metadataList.getString(XML_PATH_ATTRIBUTE_TYPE))) {
                List<HierarchicalConfiguration<ImmutableNode>> templateList = metadataList.configurationsAt("template");
                if (templateList.isEmpty()) {
                    logger.trace("{}  templates found for type {}", templateList.size(), type);
                    return new ArrayList<>(); // must be a mutable list!
                }

                return getMetadataForTemplate(template, templateList, fallbackToDefaultTemplate, topstructValueFallbackDefaultValue);
            }
        }

        return new ArrayList<>(); // must be a mutable list!
    }

    /**
     * Returns the list of configured metadata for search hit elements.
     *
     * @param template a {@link java.lang.String} object.
     * @should return correct template configuration
     * @should return default template configuration if requested not found
     * @should return default template if template is null
     * @return a {@link java.util.List} object.
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
     * Returns the list of configured metadata for {@link Highlight}s which reference a record.
     *
     * @param template a {@link java.lang.String} object.
     * @should return default template configuration if requested not found
     * @return a {@link java.util.List} object.
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
     * @return
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
            MetadataView view = new MetadataView().setIndex(index).setLabel(label).setUrl(url).setCondition(condition);
            ret.add(view);
        }

        return ret;
    }

    /**
     *
     * @param index
     * @param template
     * @return List of configured <code>Metadata</code> fields for the given template
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     */
    public List<Metadata> getMainMetadataForTemplate(int index, String template) {
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

        return getMetadataForTemplate(template, templateList, true, false);
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
     * Reads metadata configuration for the given template name if it's contained in the given template list.
     *
     * @param template Requested template name
     * @param templateList List of templates in which to look
     * @param fallbackToDefaultTemplate If true, the _DEFAULT template will be loaded if the given template is not found
     * @param topstructValueFallbackDefaultValue If true, the default value for the parameter attribute "topstructValueFallback" will be the value
     *            passed here
     * @return
     */
    private static List<Metadata> getMetadataForTemplate(String template, List<HierarchicalConfiguration<ImmutableNode>> templateList,
            boolean fallbackToDefaultTemplate, boolean topstructValueFallbackDefaultValue) {
        if (templateList == null) {
            return new ArrayList<>();
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return new ArrayList<>();
        }

        return getMetadataForTemplate(usingTemplate, topstructValueFallbackDefaultValue);
    }

    /**
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     *
     * @param usingTemplate
     * @param topstructValueFallbackDefaultValue Default value for topstructValueFallback, if not explicitly configured
     * @return
     */
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration<ImmutableNode> usingTemplate,
            boolean topstructValueFallbackDefaultValue) {
        if (usingTemplate == null) {
            return new ArrayList<>();
        }
        List<HierarchicalConfiguration<ImmutableNode>> elements = usingTemplate.configurationsAt("metadata");
        if (elements == null) {
            logger.warn("Template '{}' contains no metadata elements.", usingTemplate.getRootElementName());
            return new ArrayList<>();
        }

        List<Metadata> ret = new ArrayList<>(elements.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : elements) {
            Metadata md = getMetadataFromSubnodeConfig(sub, topstructValueFallbackDefaultValue, 0);
            if (md != null) {
                ret.add(md);
            }
        }

        return ret;
    }

    /**
     * Selects template from given list and optionally returns default template configuration.
     * 
     * @param templateList
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
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
     * Creates a {@link Metadata} instance from the given subnode configuration
     *
     * @param sub The subnode configuration
     * @param topstructValueFallbackDefaultValue
     * @param indentation
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
        boolean singleString = sub.getBoolean("[@singleString]", true);
        boolean topstructOnly = sub.getBoolean("[@topstructOnly]", false);
        int number = sub.getInt("[@number]", -1);
        int type = sub.getInt(XML_PATH_ATTRIBUTE_TYPE, 0);
        boolean hideIfOnlyMetadataField = sub.getBoolean("[@hideIfOnlyMetadataField]", false);
        String labelField = sub.getString("[@labelField]");
        String sortField = sub.getString("[@sortField]");
        String separator = sub.getString("[@separator]");
        List<HierarchicalConfiguration<ImmutableNode>> params = sub.configurationsAt("param");
        List<MetadataParameter> paramList = null;
        if (params != null) {
            paramList = new ArrayList<>(params.size());
            for (HierarchicalConfiguration<ImmutableNode> sub2 : params) {
                paramList.add(MetadataParameter.createFromConfig(sub2, topstructValueFallbackDefaultValue));
            }
        }

        Metadata ret = new Metadata(label, masterValue, paramList)
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
                .setIndentation(indentation);

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
     * <p>
     * getNormdataFieldsForTemplate.
     * </p>
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
     * <p>
     * getTocLabelConfiguration.
     * </p>
     *
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getTocLabelConfiguration(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.labelConfig.template");
        if (templateList == null) {
            return new ArrayList<>();
        }

        return getMetadataForTemplate(template, templateList, true, false);
    }

    public Map<String, Metadata> getGeomapFeatureConfigurations(String option) {
        if (StringUtils.isBlank(option)) {
            return Collections.emptyMap();
        }

        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");
        List<HierarchicalConfiguration<ImmutableNode>> templates = options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .map(config -> config.configurationsAt("title.template"))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return loadGeomapLabelConfigurations(templates);
    }

    public Map<String, Metadata> getGeomapEntityConfigurations(String option) {
        List<HierarchicalConfiguration<ImmutableNode>> options = getLocalConfigurationsAt("maps.metadata.option");
        List<HierarchicalConfiguration<ImmutableNode>> templates = options.stream()
                .filter(config -> option.equals(config.getString("[@name]", "_DEFAULT")))
                .map(config -> config.configurationsAt("entity.title.template"))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return loadGeomapLabelConfigurations(templates);
    }

    /**
     * 
     * @return
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

    public Map<String, List<LabeledValue>> getGeomapFilters() {
        List<HierarchicalConfiguration<ImmutableNode>> filterConfigs = this.getLocalConfigurationsAt("maps.filters.filter");
        Map<String, List<LabeledValue>> filters = new HashMap<>();
        for (HierarchicalConfiguration<ImmutableNode> config : filterConfigs) {
            String groupName = config.getString("featureGroup", "");
            List<LabeledValue> fields = config.configurationsAt("field").stream().map(c -> {
                String field = c.getString(".");
                String label = c.getString("[@label]", "");
                String styleClass = c.getString("[@styleClass]", "");
                return new LabeledValue(field, label, styleClass);
            })
                    .collect(Collectors.toList());
            filters.put(groupName, fields);
        }
        return filters;
    }

    public List<FeatureSetConfiguration> getRecordGeomapFeatureSetConfigs(String templateName) {
        HierarchicalConfiguration<ImmutableNode> template = selectTemplate(getLocalConfigurationsAt("maps.record.template"), templateName, true);
        if (template != null) {
            List<HierarchicalConfiguration<ImmutableNode>> featureSetConfigs = template.configurationsAt("featureSets.featureSet");
            return featureSetConfigs.stream().map(FeatureSetConfiguration::new).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static Map<String, Metadata> loadGeomapLabelConfigurations(List<HierarchicalConfiguration<ImmutableNode>> templateList) {
        if (templateList == null) {
            return Collections.emptyMap();
        }
        Map<String, Metadata> map = new HashMap<>();
        for (HierarchicalConfiguration<ImmutableNode> template : templateList) {
            String name = template.getString("[@name]", "_DEFAULT");
            Metadata md = getMetadataForTemplate(name, templateList, true, false).stream().findAny().orElse(null);
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
     * <p>
     * isDisplaySidebarBrowsingTerms.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return getLocalBoolean("sidebar.sidebarBrowsingTerms[@enabled]", true);
    }

    /**
     * <p>
     * isDisplaySidebarRssFeed.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarRssFeed() {
        return getLocalBoolean("sidebar.sidebarRssFeed[@enabled]", true);
    }

    /**
     * <p>
     * isOriginalContentDownload.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetDownloads() {
        return getLocalBoolean("sidebar.sidebarWidgetDownloads[@enabled]", false);
    }

    /**
     * <p>
     * Returns a regex such that all download files which filenames fit this regex should not be visible in the downloads widget. If an empty string
     * is returned, all downloads should remain visible
     * </p>
     *
     * @return a regex or an empty string if no downloads should be hidden
     */
    public String getHideDownloadFileRegex() {
        return getLocalString("sidebar.sidebarWidgetDownloads.hideFileRegex", "");
    }

    /**
     * <p>
     * isDisplayWidgetUsage.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetUsage() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage[@enabled]", true);
    }

    /**
     *
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetUsageCitationRecommendation() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.citationRecommendation[@enabled]", true);
    }

    /**
     *
     * @return List of available citation style names
     * @should return all configured values
     */
    public List<String> getSidebarWidgetUsageCitationRecommendationStyles() {
        return getLocalList("sidebar.sidebarWidgetUsage.citationRecommendation.styles.style", new ArrayList<>());
    }

    /**
     *
     * @return
     */
    public Metadata getSidebarWidgetUsageCitationRecommendationSource() {
        HierarchicalConfiguration<ImmutableNode> sub = null;
        try {
            sub = getLocalConfigurationAt("sidebar.sidebarWidgetUsage.citationRecommendation.source.metadata");
        } catch (IllegalArgumentException e) {
            // no or multiple occurrences
        }
        if (sub != null) {
            return getMetadataFromSubnodeConfig(sub, false, 0);
        }

        return new Metadata();
    }

    /**
     *
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetUsageCitationLinks() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.citationLinks[@enabled]", true);
    }

    /**
     *
     * @return
     * @should return all configured values
     */
    public List<CitationLink> getSidebarWidgetUsageCitationLinks() {
        List<HierarchicalConfiguration<ImmutableNode>> links = getLocalConfigurationsAt("sidebar.sidebarWidgetUsage.citationLinks.links.link");
        if (links == null || links.isEmpty()) {
            return new ArrayList<>();
        }

        List<CitationLink> ret = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> sub : links) {
            String type = sub.getString(XML_PATH_ATTRIBUTE_TYPE);
            String level = sub.getString("[@for]");
            String label = sub.getString(XML_PATH_ATTRIBUTE_LABEL);
            String field = sub.getString("[@field]");
            String pattern = sub.getString("[@pattern]");
            boolean topstructValueFallback = sub.getBoolean("[@topstructValueFallback]", false);
            try {
                ret.add(new CitationLink(type, level, label).setField(field)
                        .setPattern(pattern)
                        .setTopstructValueFallback(topstructValueFallback));
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
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
    public List<DownloadOption> getSidebarWidgetUsagePageDownloadOptions() {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("sidebar.sidebarWidgetUsage.page.downloadOptions.option");
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }

        List<DownloadOption> ret = new ArrayList<>(configs.size());
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            ret.add(new DownloadOption().setLabel(config.getString(XML_PATH_ATTRIBUTE_LABEL))
                    .setFormat(config.getString("[@format]"))
                    .setBoxSizeInPixel(config.getString("[@boxSizeInPixel]")));
        }

        return ret;
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isDisplayWidgetUsageDownloadOptions() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.page.downloadOptions[@enabled]", true);
    }

    /**
     * Returns the list of structure elements allowed to be shown in calendar view
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCalendarDocStructTypes() {
        return getLocalList("metadata.calendarDocstructTypes.docStruct");
    }

    /**
     * <p>
     * isBrowsingMenuEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBrowsingMenuEnabled() {
        return getLocalBoolean("metadata.browsingMenu[@enabled]", false);
    }

    /**
     * <p>
     * getBrowsingMenuIndexSizeThreshold.
     * </p>
     *
     * @return Solr doc count threshold for browsing term calculation
     * @should return correct value
     */
    public int getBrowsingMenuIndexSizeThreshold() {
        return getLocalInt("metadata.browsingMenu.indexSizeThreshold", 100000);
    }

    /**
     * <p>
     * getBrowsingMenuHitsPerPage.
     * </p>
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
     * @return a {@link java.util.List} object.
     * @should return all configured elements
     */
    public List<BrowsingMenuFieldConfig> getBrowsingMenuFields() {
        List<HierarchicalConfiguration<ImmutableNode>> fields = getLocalConfigurationsAt("metadata.browsingMenu.field");
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
            BrowsingMenuFieldConfig bmfc =
                    new BrowsingMenuFieldConfig(field, sortField, filterQuery, translate, recordsAndAnchorsOnly, alwaysApplyFilter);
            ret.add(bmfc);
        }

        return ret;
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public String getBrowsingMenuSortingIgnoreLeadingChars() {
        return getLocalString("metadata.browsingMenu.sorting.ignoreLeadingChars");
    }

    /**
     * <p>
     * getDocstrctWhitelistFilterQuery.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDocstrctWhitelistFilterQuery() {
        return getLocalString("search.docstrctWhitelistFilterQuery", SearchHelper.DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY);
    }

    /**
     * <p>
     * getCollectionSplittingChar.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a {@link java.lang.String} object.
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
     * @param field
     * @return
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
     * @return
     */
    public List<String> getConfiguredCollectionFields() {
        List<String> list = getLocalList("collections.collection[@field]");
        if (list == null || list.isEmpty()) {
            return Collections.singletonList("DC");
        }

        return list;
    }

    /**
     * <p>
     * getCollectionSorting.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
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
     * @param field a {@link java.lang.String} object
     * @return a {@link java.util.List} object.
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
     * @param field a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getCollectionDisplayNumberOfVolumesLevel.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
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
     * <p>
     * getCollectionDisplayDepthForSearch.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
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
     * <p>
     * getCollectionHierarchyField.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * isAddCollectionHierarchyToBreadcrumbs.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @should return false if no collection config was found
     * @return a boolean.
     */
    public boolean isAddCollectionHierarchyToBreadcrumbs(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return false;
        }
        return collection.getBoolean("addHierarchyToBreadcrumbs", false);
    }

    /**
     * <p>
     * getSolrUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSolrUrl() {
        String value = getLocalString("urls.solr", "http://localhost:8089/solr");
        if (value.charAt(value.length() - 1) == '/') {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * <p>
     * getDownloadUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
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
     * <p>
     * getRestApiUrl.
     * </p>
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
     * @return
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
     * @return
     */
    public boolean isUseIIIFApiUrlForCmsMediaUrls() {
        return getLocalBoolean("urls.iiif[@useForCmsMedia]", true);
    }

    /**
     * <p>
     * getSourceFileUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSourceFileUrl() {
        return getLocalString("urls.metadata.sourcefile");
    }

    /**
     * <p>
     * getMarcUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMarcUrl() {
        return getLocalString("urls.metadata.marc");
    }

    /**
     * <p>
     * getDcUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDcUrl() {
        return getLocalString("urls.metadata.dc");
    }

    /**
     * <p>
     * getEseUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseUrl() {
        return getLocalString("urls.metadata.ese");
    }

    /**
     * <p>
     * getSearchHitsPerPageValues.
     * </p>
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
     * @return Configured value; default value if none found
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
     * <p>
     * getSearchHitsPerPageDefaultValue.
     * </p>
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
     * <p>
     * getFulltextFragmentLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextFragmentLength() {
        return getLocalInt("search.fulltextFragmentLength", 200);
    }

    /**
     * <p>
     * isAdvancedSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchEnabled() {
        return getLocalBoolean("search.advanced[@enabled]", true);
    }

    /**
     * <p>
     * getAdvancedSearchFields.
     * </p>
     *
     * @param template
     * @param fallbackToDefaultTemplate
     * @param language
     * @return a {@link java.util.List} object.
     * @should return all values
     * @should return skip fields that don't match given language
     */
    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchFields(String template, boolean fallbackToDefaultTemplate, String language) {
        logger.trace("getAdvancedSearchFields({},{})", template, fallbackToDefaultTemplate);
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
                // logger.trace("Field {} belongs to different language; skipping", field);
                continue;
            }
            String label = subElement.getString(XML_PATH_ATTRIBUTE_LABEL, field);
            boolean hierarchical = subElement.getBoolean("[@hierarchical]", false);
            boolean range = subElement.getBoolean("[@range]", false);
            boolean untokenizeForPhraseSearch = subElement.getBoolean("[@untokenizeForPhraseSearch]", false);
            boolean visible = subElement.getBoolean("[@visible]", false);
            int displaySelectItemsThreshold = subElement.getInt("[@displaySelectItemsThreshold]", 50);
            String selectType = subElement.getString("[@selectType]", AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN);

            ret.add(new AdvancedSearchFieldConfiguration(field)
                    .setLabel(label)
                    .setHierarchical(hierarchical)
                    .setRange(range)
                    .setUntokenizeForPhraseSearch(untokenizeForPhraseSearch)
                    .setDisabled(field.charAt(0) == '#' && field.charAt(field.length() - 1) == '#')
                    .setVisible(visible)
                    .setDisplaySelectItemsThreshold(displaySelectItemsThreshold)
                    .setSelectType(selectType));
        }

        return ret;
    }

    /**
     * <p>
     * isDisplayAdditionalMetadataEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayAdditionalMetadataEnabled() {
        return getLocalBoolean("search.displayAdditionalMetadata[@enabled]", true);
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataIgnoreFields.
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataIgnoreFields() {
        return getDisplayAdditionalMetadataFieldsByType("ignore", false);
    }

    /**
     * <p>
     * Returns a list of additional metadata fields thats are configured to have their values translated. Field names are normalized (i.e. things like
     * _UNTOKENIZED are removed).
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataTranslateFields() {
        return getDisplayAdditionalMetadataFieldsByType("translate", true);
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataIgnoreFields.
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataOnelineFields() {
        return getDisplayAdditionalMetadataFieldsByType("oneline", false);
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataSnippetFields.
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataSnippetFields() {
        return getDisplayAdditionalMetadataFieldsByType("snippet", false);
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataNoHighlightFields.
     * </p>
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
     * <p>
     * isAdvancedSearchFieldHierarchical.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param template
     * @param fallbackToDefaultTemplate
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldHierarchical(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "hierarchical", template, fallbackToDefaultTemplate);
    }

    /**
     * <p>
     * isAdvancedSearchFieldRange.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param template
     * @param fallbackToDefaultTemplate
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldRange(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "range", template, fallbackToDefaultTemplate);
    }

    /**
     * <p>
     * isAdvancedSearchFieldUntokenizeForPhraseSearch.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldUntokenizeForPhraseSearch(String field, String template, boolean fallbackToDefaultTemplate) {
        return isAdvancedSearchFieldHasAttribute(field, "untokenizeForPhraseSearch", template, fallbackToDefaultTemplate);
    }

    /**
     *
     * @param field
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
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
     * @param field
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
     * @should return correct value
     */
    public String getAdvancedSearchFieldSelectType(String field, String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt(XML_PATH_SEARCH_ADVANCED_SEARCHFIELDS_TEMPLATE);
        if (templateList == null) {
            return AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN;
        }
        HierarchicalConfiguration<ImmutableNode> usingTemplate = selectTemplate(templateList, template, fallbackToDefaultTemplate);
        if (usingTemplate == null) {
            return AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN;
        }
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = usingTemplate.configurationsAt("field");
        if (fieldList == null) {
            return AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN;
        }

        for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
            if (subElement.getString(".").equals(field)) {
                return subElement.getString("[@selectType]", AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN);
            }
        }

        return AdvancedSearchFieldConfiguration.SELECT_TYPE_DROPDOWN;
    }

    /**
     * <p>
     * isAdvancedSearchFieldHierarchical.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param template
     * @param fallbackToDefaultTemplate
     * @return Label attribute value for the given field name
     * @should return correct value
     */
    public String getAdvancedSearchFieldSeparatorLabel(String field, String template, boolean fallbackToDefaultTemplate) {
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
                return subElement.getString(XML_PATH_ATTRIBUTE_LABEL, "");
            }
        }

        return null;
    }

    /**
     *
     * @param field Advanced search field name
     * @param attribute Attribute name
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
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
     * <p>
     * isTimelineSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTimelineSearchEnabled() {
        return getLocalBoolean("search.timeline[@enabled]", true);
    }

    /**
     * <p>
     * isCalendarSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCalendarSearchEnabled() {
        return getLocalBoolean("search.calendar[@enabled]", true);
    }

    /**
     * <p>
     * getStaticQuerySuffix.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStaticQuerySuffix() {
        return getLocalString("search.staticQuerySuffix");
    }

    /**
     * <p>
     * getPreviousVersionIdentifierField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPreviousVersionIdentifierField() {
        return getLocalString("search.versioning.previousVersionIdentifierField");
    }

    /**
     * <p>
     * getNextVersionIdentifierField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getNextVersionIdentifierField() {
        return getLocalString("search.versioning.nextVersionIdentifierField");
    }

    /**
     * <p>
     * getVersionLabelField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getVersionLabelField() {
        return getLocalString("search.versioning.versionLabelField");
    }

    /**
     * <p>
     * getIndexedMetsFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedMetsFolder() {
        return getLocalString("indexedMetsFolder", "indexed_mets");
    }

    /**
     * <p>
     * getIndexedLidoFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedLidoFolder() {
        return getLocalString("indexedLidoFolder", "indexed_lido");
    }

    /**
     * <p>
     * getIndexedDenkxwebFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedDenkxwebFolder() {
        return getLocalString("indexedDenkxwebFolder", "indexed_denkxweb");
    }

    /**
     * <p>
     * getIndexedDublinCoreFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedDublinCoreFolder() {
        return getLocalString("indexedDublinCoreFolder", "indexed_dublincore");
    }

    /**
     * <p>
     * getPageSelectionFormat.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPageSelectionFormat() {
        return getLocalString("viewer.pageSelectionFormat", "{pageno}:{pagenolabel}");
    }

    /**
     * <p>
     * getMediaFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMediaFolder() {
        return getLocalString("mediaFolder");
    }

    /**
     * <p>
     * getPdfFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPdfFolder() {
        return getLocalString("pdfFolder", "pdf");
    }

    /**
     * <p>
     * getVocabulariesFolder.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVocabulariesFolder() {
        return getLocalString("vocabularies", "vocabularies");
    }

    /**
     * <p>
     * getOrigContentFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getOrigContentFolder() {
        return getLocalString("origContentFolder", "source");
    }

    /**
     * <p>
     * getCmsMediaFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsMediaFolder() {
        return getLocalString("cmsMediaFolder", "cms_media");
    }

    /**
     * <p>
     * getCmsTextFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsTextFolder() {
        return getLocalString("cmsTextFolder");
    }

    /**
     * <p>
     * getAltoFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAltoFolder() {
        return getLocalString("altoFolder", "alto");
    }

    /**
     * <p>
     * getAltoCrowdsourcingFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAltoCrowdsourcingFolder() {
        return getLocalString("altoCrowdsourcingFolder", "alto_crowd");
    }

    /**
     * <p>
     * getAbbyyFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAbbyyFolder() {
        return getLocalString("abbyyFolder", "abbyy");
    }

    /**
     * <p>
     * getFulltextFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextFolder() {
        return getLocalString("fulltextFolder", "fulltext");
    }

    /**
     * <p>
     * getFulltextCrowdsourcingFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextCrowdsourcingFolder() {
        return getLocalString("fulltextCrowdsourcingFolder", "fulltext_crowd");
    }

    /**
     * <p>
     * getTeiFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTeiFolder() {
        return getLocalString("teiFolder", "tei");
    }

    /**
     * <p>
     * getCmdiFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmdiFolder() {
        return getLocalString("cmdiFolder", "cmdi");
    }

    /**
     * <p>
     * getAnnotationFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAnnotationFolder() {
        return getLocalString("annotationFolder");
    }

    /**
     * <p>
     * getHotfolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getHotfolder() {
        return getLocalString("hotfolder");
    }

    /**
     * <p>
     * getTempFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTempFolder() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "viewer").toString();
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getUrnResolverFields() {
        return getLocalList("urnresolver.field", Collections.singletonList(SolrConstants.URN));
    }

    /**
     * <p>
     * isUrnDoRedirect.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUrnDoRedirect() {
        return getLocalBoolean("urnresolver.doRedirectInsteadofForward", false);
    }

    /**
     * <p>
     * isUserRegistrationEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUserRegistrationEnabled() {
        return getLocalBoolean("user.registration[@enabled]", true);
    }

    /**
     *
     * @return
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
            Set<String> allowedAnswers = new HashSet<>(answerNodes.size());
            for (Object answer : answerNodes) {
                allowedAnswers.add(((String) answer).toLowerCase());
            }
            ret.add(new SecurityQuestion(questionKey, allowedAnswers));
        }

        return ret;
    }

    /**
     * <p>
     * isShowOpenIdConnect.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isShowOpenIdConnect() {
        return getAuthenticationProviders().stream().anyMatch(provider -> OpenIdProvider.TYPE_OPENID.equalsIgnoreCase(provider.getType()));
    }

    /**
     * <p>
     * getAuthenticationProviders.
     * </p>
     *
     * @should return all properly configured elements
     * @should load user group names correctly
     * @return a {@link java.util.List} object.
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
            String image = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@image]", null);
            boolean enabled = myConfigToUse.getBoolean(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@enabled]", true);
            String clientId = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@clientId]", null);
            String clientSecret = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@clientSecret]", null);
            String parameterType = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@parameterType]", null);
            String parameterName = myConfigToUse.getString(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@parameterName]", null);
            long timeoutMillis = myConfigToUse.getLong(XML_PATH_USER_AUTH_PROVIDERS_PROVIDER + i + ")[@timeout]", 60000);

            if (enabled) {
                IAuthenticationProvider provider = null;
                switch (type.toLowerCase()) {
                    case "httpheader":
                        providers.add(new HttpHeaderProvider(name, label, endpoint, image, timeoutMillis, parameterType, parameterName));
                        break;
                    case "openid":
                        providers.add(new OpenIdProvider(name, label, endpoint, image, timeoutMillis, clientId, clientSecret));
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
                        // logger.trace("{}: add to group: {}", provider.getName(), addToUserGroupList.toString());
                    }
                    providers.add(provider);
                }
            }
        }
        return providers;
    }

    /**
     * <p>
     * getSmtpServer.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpServer() {
        return getLocalString("user.smtpServer");
    }

    /**
     * <p>
     * getSmtpUser.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpUser() {
        return getLocalString("user.smtpUser");
    }

    /**
     * <p>
     * getSmtpPassword.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpPassword() {
        return getLocalString("user.smtpPassword");
    }

    /**
     * <p>
     * getSmtpSenderAddress.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpSenderAddress() {
        return getLocalString("user.smtpSenderAddress");
    }

    /**
     * <p>
     * getSmtpSenderName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpSenderName() {
        return getLocalString("user.smtpSenderName");
    }

    /**
     * <p>
     * getSmtpSecurity.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
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
     * <p>
     * isDisplayCollectionBrowsing.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayCollectionBrowsing() {
        return this.getLocalBoolean("webGuiDisplay.collectionBrowsing", true);
    }

    /**
     * <p>
     * isDisplayUserNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayUserNavigation() {
        return this.getLocalBoolean("webGuiDisplay.userAccountNavigation", true);
    }

    /**
     * <p>
     * isDisplayTagCloudNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTagCloudNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudNavigation", true);
    }

    /**
     * <p>
     * isDisplayStatistics.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayStatistics() {
        return this.getLocalBoolean("webGuiDisplay.displayStatistics", true);
    }

    /**
     * <p>
     * isDisplayTimeMatrix.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTimeMatrix() {
        return this.getLocalBoolean("webGuiDisplay.displayTimeMatrix", false);
    }

    /**
     * <p>
     * isDisplayCrowdsourcingModuleLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return this.getLocalBoolean("webGuiDisplay.displayCrowdsourcingModuleLinks", false);
    }

    /**
     * <p>
     * getTheme.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTheme() {
        return getSubthemeMainTheme();
    }

    /**
     * <p>
     * getThemeRootPath.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getThemeRootPath() {
        return getLocalString("viewer.theme.rootPath");
    }

    /**
     * <p>
     * getName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return getLocalString("viewer.name", "Goobi viewer");
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return getLocalString("viewer.description", "Goobi viewer");
    }

    /**
     * TagCloud auf der Startseite anzeigen lassen
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTagCloudStartpage() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudStartpage", true);
    }

    /**
     * <p>
     * isDisplaySearchResultNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchResultNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displaySearchResultNavigation", true);
    }

    /**
     * <p>
     * isFoldout.
     * </p>
     *
     * @param sidebarElement a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isFoldout(String sidebarElement) {
        return getLocalBoolean("sidebar." + sidebarElement + ".foldout", false);
    }

    /**
     * <p>
     * isSidebarPageLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarPageViewLinkVisible() {
        return getLocalBoolean("sidebar.page[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarCalendarViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarCalendarViewLinkVisible() {
        return getLocalBoolean("sidebar.calendar[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>link</strong> in the sidebar views widget is enabled. To check whether the sidebar TOC
     * <strong>widget</strong> is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocViewLinkVisible() {
        return getLocalBoolean("sidebar.toc[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarThumbsViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarThumbsViewLinkVisible() {
        return getLocalBoolean("sidebar.thumbs[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarMetadataViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarMetadataViewLinkVisible() {
        return getLocalBoolean("sidebar.metadata[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarFulltextLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarFulltextLinkVisible() {
        return getLocalBoolean("sidebar.fulltext[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>widget</strong> is enabled. To check whether the sidebar TOC <strong>link</strong> in the views
     * widget is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>widget</strong> is enabled. To check whether the sidebar TOC <strong>link</strong> in the views
     * widget is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return this.getLocalBoolean("sidebar.sidebarToc.visibleInFullscreen", true);
    }

    /**
     * <p>
     * isSidebarOpacLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarOpacLinkVisible() {
        return this.getLocalBoolean("sidebar.opac[@enabled]", false);
    }

    /**
     * <p>
     * getSidebarTocPageNumbersVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getSidebarTocPageNumbersVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc.pageNumbersVisible", false);
    }

    /**
     * <p>
     * getSidebarTocLengthBeforeCut.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return this.getLocalInt("sidebar.sidebarToc.lengthBeforeCut", 10);
    }

    /**
     * <p>
     * getSidebarTocInitialCollapseLevel.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocInitialCollapseLevel() {
        return this.getLocalInt("sidebar.sidebarToc.initialCollapseLevel", 2);
    }

    /**
     * <p>
     * getSidebarTocCollapseLengthThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocCollapseLengthThreshold() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold", 10);
    }

    /**
     * <p>
     * getSidebarTocLowestLevelToCollapseForLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLowestLevelToCollapseForLength() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold[@lowestLevelToTest]", 2);
    }

    /**
     * <p>
     * isSidebarTocTreeView.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocTreeView() {
        return getLocalBoolean("sidebar.sidebarToc.useTreeView", true);
    }

    /**
     * <p>
     * isTocTreeView.
     * </p>
     *
     * @should return true for allowed docstructs
     * @should return false for other docstructs
     * @param docStructType a {@link java.lang.String} object.
     * @return a boolean.
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
            logger.trace("Tree view disabled");
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

    /**
     * <p>
     * Returns a list containing all simple facet fields.
     * </p>
     *
     * @should return correct order
     * @return a {@link java.util.List} object.
     */
    public List<String> getAllFacetFields() {
        return getLocalList("search.facets.field");
    }

    /**
     * 
     * @return
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
     * <p>
     * getHierarchicalFacetFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
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
     * <p>
     * getRangeFacetFields.
     * </p>
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

    public String getFacetFieldStyle(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = getLocalConfigurationsAt("search.facets.field");
        if (fieldList != null) {
            for (HierarchicalConfiguration<ImmutableNode> subElement : fieldList) {
                if (subElement.getString(".").equals(field)) {
                    return subElement.getString("[@style]", "");
                }
            }
        }

        return "";
    }

    /**
     * <p>
     * getGeoFacetFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
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
     * @param facetField
     * @return
     * @should return correct value
     */
    public String getGeoFacetFieldPredicate(String facetField) {
        return getPropertyForFacetField(facetField, "[@predicate]", "ISWITHIN");

    }

    /**
     * @param facetField
     * @return
     * @should return correct value
     */
    public boolean isShowSearchHitsInGeoFacetMap(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@displayResultsOnMap]", "true");
        return Boolean.valueOf(value.trim());
    }

    /**
     * <p>
     * getInitialFacetElementNumber.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
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
     * <p>
     * getSortOrder.
     * </p>
     *
     * @param facetField a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSortOrder(String facetField) {
        return getPropertyForFacetField(facetField, "[@sortOrder]", "default");
    }

    /**
     * Returns a list of values to prioritize for the given facet field.
     *
     * @param field a {@link java.lang.String} object.
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
     * @param facetField
     * @return
     * @should return correct value
     * @should return null if no value found
     */
    public String getLabelFieldForFacetField(String facetField) {
        return getPropertyForFacetField(facetField, "[@labelField]", null);
    }

    /**
     *
     * @param facetField
     * @return
     * @should return correct value
     */
    public boolean isTranslateFacetFieldLabels(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@translateLabels]", "true");
        return Boolean.parseBoolean(value);
    }

    /**
     * 
     * @param facetField
     * @return
     * @should return correct value
     */
    public int getGroupToLengthForFacetField(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@groupToLength]", "-1");
        return Integer.parseInt(value);
    }

    /**
     *
     * @param facetField
     * @return
     * @should return correct value
     */
    public boolean isAlwaysApplyFacetFieldToUnfilteredHits(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@alwaysApplyToUnfilteredHits]", "false");
        return Boolean.valueOf(value);
    }

    /**
     *
     * @param facetField
     * @return
     * @should return correct value
     */
    public boolean isFacetFieldSkipInWidget(String facetField) {
        String value = getPropertyForFacetField(facetField, "[@skipInWidget]", "false");
        return Boolean.valueOf(value);
    }

    /**
     * 
     * @param facetField
     * @return
     */
    public String getFacetFieldType(String facetField) {
        return getPropertyForFacetField(facetField, XML_PATH_ATTRIBUTE_TYPE, "");
    }

    /**
     * @param facetField
     * @return
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
     * <p>
     * isSortingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSortingEnabled() {
        return getLocalBoolean("search.sorting[@enabled]", true);
    }

    /**
     * <p>
     * getDefaultSortField.
     * </p>
     *
     * @param language
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getSortFields.
     * </p>
     *
     * @should return return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getSortFields() {
        return getLocalList(XML_PATH_SEARCH_SORTING_FIELD);
    }

    /**
     * 
     * @param language
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
        for (String field : fields) {
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
     * <p>
     * getStaticSortFields.
     * </p>
     *
     * @should return return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getStaticSortFields() {
        return getLocalList("search.sorting.static.field");
    }

    /**
     * @return
     */
    public Optional<String> getSearchSortingKeyAscending(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldConfigs = getLocalConfigurationsAt(XML_PATH_SEARCH_SORTING_FIELD);
        for (HierarchicalConfiguration<ImmutableNode> conf : fieldConfigs) {
            String configField = conf.getString(".");
            if (StringUtils.equals(configField, field)) {
                return Optional.ofNullable(conf.getString("[@dropDownAscMessageKey]", null));
            }
        }
        return Optional.empty();
    }

    public Optional<String> getSearchSortingKeyDescending(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldConfigs = getLocalConfigurationsAt(XML_PATH_SEARCH_SORTING_FIELD);
        for (HierarchicalConfiguration<ImmutableNode> conf : fieldConfigs) {
            String configField = conf.getString(".");
            if (StringUtils.equals(configField, field)) {
                return Optional.ofNullable(conf.getString("[@dropDownDescMessageKey]", null));
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getUrnResolverUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getUrnResolverUrl() {
        return getLocalString("urls.urnResolver",
                new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resolver?urn=").toString());
    }

    /**
     * The maximal image size retrievable with only the permission to view thumbnails
     *
     * @should return correct value
     * @return the maximal image width
     */
    public int getThumbnailImageAccessMaxWidth() {
        return getLocalInt("accessConditions.thumbnailImageAccessMaxWidth", getLocalInt("accessConditions.unconditionalImageAccessMaxWidth", 120));
    }

    /**
     * The maximal image size retrievable with the permission to view images but without the permission to zoom images
     *
     * @should return correct value
     * @return the maximal image width, default ist 600
     */
    public int getUnzoomedImageAccessMaxWidth() {
        return getLocalInt("accessConditions.unzoomedImageAccessMaxWidth", 0);
    }

    /**
     * <p>
     * isFullAccessForLocalhost.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isFullAccessForLocalhost() {
        return getLocalBoolean("accessConditions.fullAccessForLocalhost", false);
    }

    /**
     * <p>
     * isGeneratePdfInTaskManager.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isGeneratePdfInTaskManager() {
        return getLocalBoolean("pdf.externalPdfGeneration", false);
    }

    /**
     * <p>
     * isPdfApiDisabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPdfApiDisabled() {
        return getLocalBoolean("pdf.pdfApiDisabled", false);
    }

    /**
     * <p>
     * isTitlePdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTitlePdfEnabled() {
        return getLocalBoolean("pdf.titlePdfEnabled", true);
    }

    /**
     * <p>
     * isTocPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTocPdfEnabled() {
        return getLocalBoolean("pdf.tocPdfEnabled", true);
    }

    /**
     * <p>
     * isMetadataPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isMetadataPdfEnabled() {
        return getLocalBoolean("pdf.metadataPdfEnabled", true);
    }

    /**
     * <p>
     * isPagePdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPagePdfEnabled() {
        return getLocalBoolean("pdf.pagePdfEnabled", false);
    }

    /**
     * <p>
     * isDocHierarchyPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDocHierarchyPdfEnabled() {
        return getLocalBoolean("pdf.docHierarchyPdfEnabled", false);
    }

    /**
     * <p>
     * isTitleEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTitleEpubEnabled() {
        return getLocalBoolean("epub.titleEpubEnabled", false);
    }

    /**
     * <p>
     * isTocEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTocEpubEnabled() {
        return getLocalBoolean("epub.tocEpubEnabled", false);
    }

    /**
     * <p>
     * isMetadataEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isMetadataEpubEnabled() {
        return getLocalBoolean("epub.metadataEpubEnabled", false);
    }

    /**
     * <p>
     * getDownloadFolder.
     * </p>
     *
     * @should return correct value for pdf
     * @should return correct value for epub
     * @should return empty string if type unknown
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDownloadFolder(String type) {
        switch (type.toLowerCase()) {
            case "pdf":
                return getLocalString("pdf.downloadFolder", "/opt/digiverso/viewer/pdf_download");
            case "epub":
                return getLocalString("epub.downloadFolder", "/opt/digiverso/viewer/epub_download");
            default:
                return "";

        }
    }

    /**
     * <p>
     * getRssFeedItems.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getRssFeedItems() {
        return getLocalInt("rss.numberOfItems", 50);
    }

    /**
     * <p>
     * getRssTitle.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssTitle() {
        return getLocalString("rss.title", "viewer-rss");
    }

    /**
     * <p>
     * getRssDescription.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssDescription() {
        return getLocalString("rss.description", "latest imports");
    }

    /**
     * <p>
     * getRssCopyrightText.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssCopyrightText() {
        return getLocalString("rss.copyright");
    }

    /**
     * <p>
     * getThumbnailsWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsWidth() {
        return getLocalInt("viewer.thumbnailsWidth", 100);
    }

    /**
     * <p>
     * getThumbnailsHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsHeight() {
        return getLocalInt("viewer.thumbnailsHeight", 120);
    }

    /**
     * <p>
     * getAnchorThumbnailMode.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorThumbnailMode() {
        return getLocalString("viewer.anchorThumbnailMode", "GENERIC");
    }

    /**
     * <p>
     * getDisplayBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayBreadcrumbs", true);
    }

    /**
     * <p>
     * getDisplayMetadataPageLinkBlock.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayMetadataPageLinkBlock() {
        return this.getLocalBoolean("webGuiDisplay.displayMetadataPageLinkBlock", true);
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return useTiles(PageType.viewImage, null);
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return useTiles(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getBoolean("[@tileImage]", false);
    }

    /**
     * whether to show a navigator element in the openseadragon viewe
     * 
     * @param view get settings for this pageType
     * @param image get settings for this image type
     * @return true if navigator should be shown
     * @throws ViewerConfigurationException
     */
    public boolean showImageNavigator(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getBoolean("navigator[@enabled]", false);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(PageType.viewImage, null);
    }

    /**
     * <p>
     * getFullscreenFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFullscreenFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getInt("[@footerHeight]", 50);
    }

    /**
     * <p>
     * getImageViewType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getImageViewType() throws ViewerConfigurationException {
        return getZoomImageViewType(PageType.viewImage, null);
    }

    /**
     * <p>
     * getZoomFullscreenViewType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getZoomFullscreenViewType() throws ViewerConfigurationException {
        return getZoomImageViewType(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getZoomImageViewType.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getZoomImageViewType(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getString(XML_PATH_ATTRIBUTE_TYPE);
    }

    /**
     * <p>
     * useOpenSeadragon.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useOpenSeadragon() throws ViewerConfigurationException {
        return "openseadragon".equalsIgnoreCase(getImageViewType());
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales() throws ViewerConfigurationException {
        return getImageViewZoomScales(PageType.viewImage, null);
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @param view a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(String view) throws ViewerConfigurationException {
        return getImageViewZoomScales(PageType.valueOf(view), null);
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(PageType view, ImageType image) throws ViewerConfigurationException {
        List<String> defaultList = new ArrayList<>();
        BaseHierarchicalConfiguration zoomImageViewConfig = getZoomImageViewConfig(view, image);
        if (zoomImageViewConfig != null) {
            String[] scales = zoomImageViewConfig.getStringArray("scale");
            if (scales != null) {
                return Arrays.asList(scales);
            }
        }
        return defaultList;
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @return the configured tile sizes for imageView as a hashmap linking each tile size to the list of resolutions to use with that size
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes() throws ViewerConfigurationException {
        return getTileSizes(PageType.viewImage, null);
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(PageType view, ImageType image) throws ViewerConfigurationException {
        Map<Integer, List<Integer>> map = new HashMap<>();
        List<HierarchicalConfiguration<ImmutableNode>> sizes = getZoomImageViewConfig(view, image).configurationsAt("tileSize");
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
     * <p>
     * getZoomImageViewConfig.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param imageType a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link org.apache.commons.configuration2.SubnodeConfiguration} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BaseHierarchicalConfiguration getZoomImageViewConfig(PageType pageType, ImageType imageType) throws ViewerConfigurationException {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("viewer.zoomImageView");

        for (HierarchicalConfiguration<ImmutableNode> subConfig : configs) {

            if (pageType != null) {
                List<Object> views = subConfig.getList("useFor.view");
                if (views.isEmpty() || views.contains(pageType.name()) || views.contains(pageType.getName())) {
                    //match
                } else {
                    continue;
                }
            }

            if (imageType != null && imageType.getFormat() != null) {
                List<Object> mimeTypes = subConfig.getList("useFor.mimeType");
                if (mimeTypes.isEmpty() || mimeTypes.contains(imageType.getFormat().getMimeType())) {
                    //match
                } else {
                    continue;
                }
            }

            return (BaseHierarchicalConfiguration) subConfig;
        }
        throw new ViewerConfigurationException("Viewer config must define at least a generic <zoomImageView>");
    }

    /**
     * <p>
     * getBreadcrumbsClipping.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getBreadcrumbsClipping() {
        return getLocalInt("webGuiDisplay.breadcrumbsClipping", 50);
    }

    /**
     * <p>
     * getDisplayStructType.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayStructType() {
        return this.getLocalBoolean("search.metadata.displayStructType", true);
    }

    /**
     * <p>
     * getSearchHitMetadataValueNumber.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitMetadataValueNumber() {
        return getLocalInt("search.metadata.valueNumber", 1);
    }

    /**
     * <p>
     * getSearchHitMetadataValueLength.
     * </p>
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
     * @return a {@link java.util.List} object.
     */
    public List<String> getWatermarkTextConfiguration() {
        return getLocalList("viewer.watermarkTextConfiguration.text");
    }

    /**
     * <p>
     * getWatermarkFormat.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWatermarkFormat() {
        return getLocalString("viewer.watermarkFormat", "jpg");
    }

    /**
     * <p>
     * getStopwordsFilePath.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStopwordsFilePath() {
        return getLocalString("stopwordsFile");
    }

    /**
     * Returns the locally configured page type name for URLs (e.g. "bild" instead of default "image").
     *
     * @param type a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @should return the correct value for the given type
     * @should return null for non configured type
     * @return a {@link java.lang.String} object.
     */
    public String getPageType(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name());
    }

    /**
     * <p>
     * getRecordTargetPageType.
     * </p>
     *
     * @param publicationType a {@link java.lang.String} object.
     * @should return correct value
     * @should return null if docstruct not found
     * @return a {@link java.lang.String} object.
     */
    public String getRecordTargetPageType(String publicationType) {
        return getLocalString("viewer.recordTargetPageTypes." + publicationType);
    }

    public String getPageTypeExitView(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name() + "[@exit]");
    }

    /**
     * <p>
     * getFulltextPercentageWarningThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextPercentageWarningThreshold() {
        return getLocalInt("viewer.fulltextPercentageWarningThreshold", 30);
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public String getFallbackDefaultLanguage() {
        return getLocalString("viewer.fallbackDefaultLanguage", "en");
    }

    /**
     * <p>
     * getFeedbackEmailAddresses.
     * </p>
     *
     * @should return correct values
     * @return a {@link java.lang.String} object.
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
     * @return
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
     * <p>
     * isBookmarksEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBookmarksEnabled() {
        return getLocalBoolean("bookmarks[@enabled]", true);
    }

    /**
     * <p>
     * getPageLoaderThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getPageLoaderThreshold() {
        return getLocalInt("performance.pageLoaderThreshold", 1000);
    }

    /**
     * <p>
     * isPreventProxyCaching.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPreventProxyCaching() {
        return getLocalBoolean(("performance.preventProxyCaching"), false);
    }

    /**
     * <p>
     * isSolrCompressionEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSolrCompressionEnabled() {
        return getLocalBoolean(("performance.solr.compressionEnabled"), true);
    }

    /**
     * <p>
     * isSolrBackwardsCompatible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSolrBackwardsCompatible() {
        return getLocalBoolean(("performance.solr.backwardsCompatible"), false);
    }

    /**
     * @return
     */
    public boolean reviewEnabledForComments() {
        return getLocalBoolean("comments.review[@enabled]", false);
    }

    /**
     * <p>
     * getViewerHome.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getViewerHome() {
        return getLocalString("viewerHome");
    }

    /**
     *
     * @return
     * @should return correct value
     */
    String getDataRepositoriesHome() {
        return getLocalString("dataRepositoriesHome", "");
    }

    /**
     * <p>
     * getWatermarkIdField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.util.List} object.
     */
    public List<String> getWatermarkIdField() {
        return getLocalList("viewer.watermarkIdField", Collections.singletonList(SolrConstants.DC));

    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isDocstructNavigationEnabled() {
        return getLocalBoolean("viewer.docstructNavigation[@enabled]", false);
    }

    /**
     *
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
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
     * <p>
     * getSubthemeMainTheme.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSubthemeMainTheme() {
        String theme = getLocalString("viewer.theme[@mainTheme]");
        if (StringUtils.isEmpty(theme)) {
            logger.error("Theme name could not be read - {} may not be well-formed.", CONFIG_FILE_NAME);
        }
        return getLocalString("viewer.theme[@mainTheme]");
    }

    /**
     * <p>
     * getSubthemeDiscriminatorField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSubthemeDiscriminatorField() {
        return getLocalString("viewer.theme[@discriminatorField]", "");
    }

    /**
     * <p>
     * getTagCloudSampleSize.
     * </p>
     *
     * @should return correct value for existing fields
     * @should return INT_MAX for other fields
     * @param fieldName a {@link java.lang.String} object.
     * @return a int.
     */
    public int getTagCloudSampleSize(String fieldName) {
        return getLocalInt("tagclouds.sampleSizes." + fieldName, Integer.MAX_VALUE);
    }

    /**
     * <p>
     * getTocVolumeSortFieldsForTemplate.
     * </p>
     *
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
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
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getDisplayTitleBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayTitleBreadcrumbs() {
        return getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs", false);
    }

    /**
     * <p>
     * isDisplayTitlePURL.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTitlePURL() {
        return this.getLocalBoolean("webGuiDisplay.displayTitlePURL", true);
    }

    /**
     * <p>
     * getTitleBreadcrumbsMaxTitleLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return this.getLocalInt("webGuiDisplay.displayTitleBreadcrumbs[@maxTitleLength]", 25);
    }

    /**
     * <p>
     * getIncludeAnchorInTitleBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getIncludeAnchorInTitleBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs[@includeAnchor]", false);
    }

    /**
     * <p>
     * isDisplaySearchRssLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchRssLinks() {
        return getLocalBoolean("rss.displaySearchRssLinks", true);
    }

    /**
     * <p>
     * getStartYearForTimeline.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStartYearForTimeline() {
        return this.getLocalString("search.timeline.startyear", "1750");
    }

    /**
     * <p>
     * getEndYearForTimeline.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEndYearForTimeline() {
        return this.getLocalString("search.timeline.endyear", "2014");
    }

    /**
     * <p>
     * getTimelineHits.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTimelineHits() {
        return this.getLocalString("search.timeline.hits", "108");
    }

    /**
     * <p>
     * isPiwikTrackingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPiwikTrackingEnabled() {
        return getLocalBoolean("piwik[@enabled]", false);
    }

    /**
     * <p>
     * getPiwikBaseURL.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikBaseURL() {
        return this.getLocalString("piwik.baseURL", "");
    }

    /**
     * <p>
     * getPiwikSiteID.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikSiteID() {
        return this.getLocalString("piwik.siteID", "1");

    }

    /**
     * <p>
     * isSearchSavingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSearchSavingEnabled() {
        return getLocalBoolean("search.searchSaving[@enabled]", true);
    }

    /**
     * <p>
     * getRecordGroupIdentifierFields.
     * </p>
     *
     * @should return all configured values
     * @return a {@link java.util.List} object.
     */
    public List<String> getRecordGroupIdentifierFields() {
        return getLocalList("toc.recordGroupIdentifierFields.field");
    }

    /**
     * <p>
     * getAncestorIdentifierFields.
     * </p>
     *
     * @should return all configured values
     * @return a {@link java.util.List} object.
     */
    public List<String> getAncestorIdentifierFields() {
        return getLocalList("toc.ancestorIdentifierFields.field");
    }

    /**
     * <p>
     * isTocListSiblingRecords.
     * </p>
     *
     * @should return correctValue
     * @return a boolean.
     */
    public boolean isTocListSiblingRecords() {
        return getLocalBoolean("toc.ancestorIdentifierFields[@listSiblingRecords]", false);
    }

    /**
     * <p>
     * getSearchFilters.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
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
     * @return
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
     * <p>
     * getWebApiFields.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<Map<String, String>> getWebApiFields() {
        List<HierarchicalConfiguration<ImmutableNode>> elements = getLocalConfigurationsAt("webapi.fields.field");
        if (elements == null) {
            return new ArrayList<>();
        }

        List<Map<String, String>> ret = new ArrayList<>(elements.size());
        for (HierarchicalConfiguration<ImmutableNode> sub : elements) {
            Map<String, String> fieldConfig = new HashMap<>();
            fieldConfig.put("jsonField", sub.getString("[@jsonField]", null));
            fieldConfig.put("luceneField", sub.getString("[@solrField]", null)); // deprecated
            fieldConfig.put("solrField", sub.getString("[@solrField]", null));
            fieldConfig.put("multivalue", sub.getString("[@multivalue]", null));
            ret.add(fieldConfig);
        }

        return ret;
    }

    /**
     * <p>
     * getDbPersistenceUnit.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getCmsMediaDisplayWidth.
     * </p>
     *
     * @return a int.
     */
    public int getCmsMediaDisplayWidth() {
        return getLocalInt("cms.mediaDisplayWidth", 0);
    }

    /**
     * <p>
     * getCmsMediaDisplayHeight. If not configured, return 100.000. In this case the actual image size always depends on the requested width
     * </p>
     *
     * @return a int.
     */
    public int getCmsMediaDisplayHeight() {
        return getLocalInt("cms.mediaDisplayHeight", 100000);
    }

    /**
     * <p>
     * isTranskribusEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTranskribusEnabled() {
        return getLocalBoolean("transkribus[@enabled]", false);
    }

    /**
     * <p>
     * getTranskribusUserName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusUserName() {
        return getLocalString("transkribus.userName");
    }

    /**
     * <p>
     * getTranskribusPassword.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusPassword() {
        return getLocalString("transkribus.password");
    }

    /**
     * <p>
     * getTranskribusDefaultCollection.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusDefaultCollection() {
        return getLocalString("transkribus.defaultCollection");
    }

    /**
     * <p>
     * getTranskribusRestApiUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusRestApiUrl() {
        return getLocalString("transkribus.restApiUrl", TranskribusUtils.TRANSRIBUS_REST_URL);
    }

    /**
     * <p>
     * getTranskribusAllowedDocumentTypes.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getTranskribusAllowedDocumentTypes() {
        return getLocalList("transkribus.allowedDocumentTypes.docstruct");
    }

    /**
     * <p>
     * getTocIndentation.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getTocIndentation() {
        return getLocalInt("toc.tocIndentation", 20);
    }

    /**
     * <p>
     * isPageBrowseEnabled.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isPageBrowseEnabled() {
        return getLocalBoolean("viewer.pageBrowse[@enabled]", false);
    }

    /**
     * <p>
     * getPageBrowseSteps.
     * </p>
     *
     * @return a {@link java.util.List} object.
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
     * @return
     * @should return correct value
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return getLocalInt("viewer.pageSelectDropdownDisplayMinPages", 3);
    }

    /**
     * <p>
     * getWorkflowRestUrl.
     * </p>
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
     * <p>
     * getTaskManagerServiceUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTaskManagerServiceUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "service";
    }

    /**
     * <p>
     * getTaskManagerRestUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTaskManagerRestUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "rest";
    }

    /**
     * <p>
     * getReCaptchaSiteKey.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getReCaptchaSiteKey() {
        return getLocalString("reCaptcha.provider[@siteKey]");
    }

    /**
     * <p>
     * isUseReCaptcha.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUseReCaptcha() {
        return getLocalBoolean("reCaptcha[@enabled]", true);
    }

    /**
     * <p>
     * isSearchInItemEnabled.
     * </p>
     *
     * @should return true if the search field to search the current item/work is configured to be visible
     * @return a boolean.
     */
    public boolean isSearchInItemEnabled() {
        return getLocalBoolean("sidebar.searchInItem[@enabled]", true);
    }

    /**
     * <p>
     * isSearchRisExportEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSearchRisExportEnabled() {
        return getLocalBoolean("search.export.ris[@enabled]", false);
    }

    /**
     * <p>
     * isSearchExcelExportEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSearchExcelExportEnabled() {
        return getLocalBoolean("search.export.excel[@enabled]", false);
    }

    /**
     * <p>
     * getSearchExcelExportFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<ExportFieldConfiguration> getSearchExcelExportFields() {
        return getExportConfigurations("search.export.excel.field");
    }

    /**
     *
     * @param path
     * @return
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
     * <p>
     * getExcelDownloadTimeout.
     * </p>
     *
     * @return a int.
     */
    public int getExcelDownloadTimeout() {
        return getLocalInt("search.export.excel.timeout", 120);
    }

    /**
     * <p>
     * isDoublePageNavigationEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDoublePageNavigationEnabled() {
        return getLocalBoolean("viewer.doublePageNavigation[@enabled]", false);
    }

    /**
     * <p>
     * getRestrictedImageUrls.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getRestrictedImageUrls() {
        return getLocalList("viewer.externalContent.restrictedUrls.url", new ArrayList<>());
    }

    public List<String> getIIIFLicenses() {
        return getLocalList("webapi.iiif.license", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFMetadataFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIIIFMetadataFields() {
        return getLocalList("webapi.iiif.metadataFields.field", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFEventFields.
     * </p>
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
     * <p>
     * getIIIFMetadataLabel.
     * </p>
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
     * <p>
     * getIIIFLogo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public List<String> getIIIFLogo() {
        return getLocalList("webapi.iiif.logo", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFNavDateField.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFNavDateField() {
        return getLocalString("webapi.iiif.navDateField", null);
    }

    /**
     * <p>
     * getIIIFAttribution.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public List<String> getIIIFAttribution() {
        return getLocalList("webapi.iiif.attribution", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFDescriptionFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
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
     * Uses {@link #getIIIFAttribution()} as fallback;
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
     *
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPDF() {
        return getLocalBoolean("webapi.iiif.rendering.pdf[@enabled]", true);
    }

    /**
     *
     * @return
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
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPlaintext() {
        return getLocalBoolean("webapi.iiif.rendering.plaintext[@enabled]", true);
    }

    /**
     *
     * @return
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
     * <p>
     * getSitelinksField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSitelinksField() {
        return getLocalString("sitelinks.sitelinksField");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSitelinksEnabled() {
        return getLocalBoolean("sitelinks[@enabled]", true);
    }

    /**
     * <p>
     * getSitelinksFilterQuery.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSitelinksFilterQuery() {
        return getLocalString("sitelinks.sitelinksFilterQuery");
    }

    /**
     * <p>
     * getConfiguredCollections.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getConfiguredCollections() {
        return getLocalList("collections.collection[@field]", new ArrayList<>());

    }

    /**
     * <p>
     * getWebApiToken.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getWebApiToken() {
        return getLocalString("webapi.authorization.token", "");
    }

    /**
     * <p>
     * isAllowRedirectCollectionToWork.
     * </p>
     *
     * @return true if opening a collection containing only a single work should redirect to that work
     * @should return correct value
     */
    public boolean isAllowRedirectCollectionToWork() {
        return getLocalBoolean("collections.redirectToWork", true);
    }

    /**
     * <p>
     * getTwitterUserName.
     * </p>
     *
     * @return Configured value; null if none configured
     * @should return correct value
     */
    public String getTwitterUserName() {
        return getLocalString("embedding.twitter.userName");
    }

    /**
     * <p>
     * getLimitImageHeightUpperRatioThreshold.
     * </p>
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightUpperRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@upperRatioThreshold]", 0.3f);
    }

    /**
     * <p>
     * getLimitImageHeightLowerRatioThreshold.
     * </p>
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightLowerRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@lowerRatioThreshold]", 3f);
    }

    /**
     * <p>
     * isLimitImageHeight.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isLimitImageHeight() {
        return getLocalBoolean("viewer.limitImageHeight", true);
    }

    /**
     * <p>
     * isAddCORSHeader.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddCORSHeader() {
        return getLocalBoolean("webapi.cors[@enabled]", false);
    }

    /**
     * <p>
     * Gets the value configured in webapi.cors. Default is "*"
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCORSHeaderValue() {
        return getLocalString("webapi.cors", "*");
    }

    /**
     * @return
     */
    public boolean isDiscloseImageContentLocation() {
        return getLocalBoolean("webapi.iiif.discloseContentLocation", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isCopyrightIndicatorEnabled() {
        return getLocalBoolean("sidebar.copyrightIndicator[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getCopyrightIndicatorStyle() {
        return getLocalString("sidebar.copyrightIndicator[@style]", "widget");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getCopyrightIndicatorStatusField() {
        return getLocalString("sidebar.copyrightIndicator.status[@field]");
    }

    /**
     * 
     * @param value
     * @should return correct value
     */
    public CopyrightIndicatorStatus getCopyrightIndicatorStatusForValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("sidebar.copyrightIndicator.status.value");
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

        return null;
    }

    /**
     * 
     * @param value
     * @should return correct value
     */
    public CopyrightIndicatorLicense getCopyrightIndicatorLicenseForValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("sidebar.copyrightIndicator.license.value");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            String content = config.getString("[@content]");
            if (value.equals(content)) {
                String description = config.getString(XML_PATH_ATTRIBUTE_DESCRIPTION);
                String[] icons = config.getStringArray("icon");
                return new CopyrightIndicatorLicense(description, icons != null ? Arrays.asList(icons) : new ArrayList<>());
            }
        }

        return null;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getCopyrightIndicatorLicenseField() {
        return getLocalString("sidebar.copyrightIndicator.license[@field]");
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
     * @param marker
     * @return
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

    public String getRecordGeomapMarker(String templateName, String type) {
        HierarchicalConfiguration<ImmutableNode> template = selectTemplate(getLocalConfigurationsAt("maps.record.template"), templateName, true);
        if (template != null) {
            List<HierarchicalConfiguration<ImmutableNode>> configs = template.configurationsAt("marker");
            return configs.stream()
                    .filter(config -> config.getString("[@type]", "").equals(type))
                    .findAny()
                    .map(config -> config.getString(".", ""))
                    .orElse("");
        } else {
            return "";
        }
    }

    /**
     * @param config
     * @param marker
     * @return
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
     * @param templateList
     * @param template
     * @return
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
     * @return
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
     * @return
     */
    public String getBaseXUrl() {
        return getLocalString("urls.basex");
    }

    public boolean isArchivesEnabled() {
        return getLocalBoolean("archives[@enabled]", false);
    }

    public Map<String, String> getArchiveNodeTypes() {
        List<HierarchicalConfiguration<ImmutableNode>> nodeTypes = getLocalConfigurationsAt("archives.nodeTypes.node");
        nodeTypes.get(0).getString(getReCaptchaSiteKey());
        return nodeTypes.stream()
                .collect(Collectors.toMap(node -> node.getString(XML_PATH_ATTRIBUTE_NAME), node -> node.getString(XML_PATH_ATTRIBUTE_ICON)));
    }

    /**
     * @return
     */
    public HierarchicalConfiguration<ImmutableNode> getArchiveMetadataConfig() {
        return getLocalConfigurationAt("archives.metadataList");
    }

    public boolean isDisplayUserGeneratedContentBelowImage() {
        return getLocalBoolean("webGuiDisplay.displayUserGeneratedContentBelowImage", false);
    }

    /**
     * config: <code>&#60;iiif use-version="3.0"&#62;&#60;/iiif&#62;</code>
     *
     * @return
     */
    public String getIIIFVersionToUse() {
        return getLocalString("webapi.iiif[@use-version]", "2.1.1");
    }

    /**
     *
     * @return
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
     * @return
     */
    public boolean isDisplayAnnotationTextInImage() {
        return getLocalBoolean("webGuiDisplay.displayAnnotationTextInImage", true);
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isUseFacetsAsExpandQuery() {
        return getLocalBoolean("search.useFacetsAsExpandQuery[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getAllowedFacetsForExpandQuery() {
        return getLocalList("search.useFacetsAsExpandQuery.facetQuery");
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isSearchResultGroupsEnabled() {
        return getLocalBoolean("search.resultGroups[@enabled]", false);
    }

    /**
     * 
     * @return
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
            boolean useAsAdvancedSearchTemplate = groupNode.getBoolean("[@useAsAdvancedSearchTemplate]", false);
            ret.add(new SearchResultGroup(name, query, previewHitCount, useAsAdvancedSearchTemplate));
        }

        return ret;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isContentUploadEnabled() {
        return getLocalBoolean("upload[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentUploadToken() {
        return getLocalString("upload.token");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentUploadDocstruct() {
        return getLocalString("upload.docstruct", "monograph");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentUploadTemplateName() {
        return getLocalString("upload.templateName");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentUploadRejectionPropertyName() {
        return getLocalString("upload.rejectionPropertyName");
    }

    /**
     * 
     * @return
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
     * @return
     * @should return correct value
     */
    public boolean isConfigEditorEnabled() {
        return getLocalBoolean("configEditor[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getConfigEditorBackupFiles() {
        return getLocalInt("configEditor[@backupFiles]", 0);
    }

    /**
     * 
     * @return
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
     * @return
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
     * @param url
     * @return
     * @throws MalformedURLException
     * @should return true if host whitelisted
     */
    public boolean isHostProxyWhitelisted(String url) throws MalformedURLException {
        URL urlAsURL = new URL(url);
        return getProxyWhitelist().contains(urlAsURL.getHost());
    }

    /**
     * 
     * @return
     */
    public List<String> getProxyWhitelist() {
        return getLocalList("proxy.whitelist.host");
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
            TaskType type = TaskType.valueOf(taskName.toUpperCase());
            return getLocalString("quartz.scheduler." + taskName.toLowerCase() + ".cronExpression", type.getDefaultCronExpression());

        } catch (IllegalArgumentException e) {
            return getLocalString("quartz.scheduler." + taskName.toLowerCase() + ".cronExpression", getQuartzSchedulerCronExpression());
        }
    }

    public String getQuartzSchedulerCronExpression() {
        return getLocalString("quartz.scheduler.cronExpression", "0 0 0 * * ?");
    }

    /**
     * @param field
     * @param language
     * @return
     */
    public static boolean isLanguageVersionOtherThan(String field, String language) {
        return field.matches(".*_LANG_[A-Z][A-Z]") && !field.matches(".*_LANG_" + language.toUpperCase());
    }

    public Optional<String> getStringFormat(String type, Locale locale) {

        String path = String.format("viewer.formats.%s.%s", type, locale.getLanguage());
        return Optional.ofNullable(getLocalString(path, null));
    }

    public boolean isGeomapCachingEnabled() {
        return getLocalBoolean("maps.caching[@enabled]", false);
    }

    public long getCMSGeomapCachingUpdateInterval() {
        return getLocalInt("maps.caching.updateInterval", 5);
    }

    public long getCMSGeomapCachingTimeToLive() {
        return getLocalInt("maps.caching.timeToLive", 6);
    }

}
