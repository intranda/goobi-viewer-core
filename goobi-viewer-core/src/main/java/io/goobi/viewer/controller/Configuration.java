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
package io.goobi.viewer.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.authentication.BibliothecaProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LitteraProvider;
import io.goobi.viewer.model.security.authentication.LocalAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.authentication.VuFindProvider;
import io.goobi.viewer.model.security.authentication.XServiceProvider;
import io.goobi.viewer.model.viewer.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.viewer.DcSortingList;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * <p>
 * Configuration class.
 * </p>
 */
public final class Configuration extends AbstractConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private Set<String> stopwords;

    /**
     * <p>
     * Constructor for Configuration.
     * </p>
     *
     * @param configFilePath a {@link java.lang.String} object.
     */
    public Configuration(String configFilePath) {
        // Load default config file
        try {
            config = new XMLConfiguration();
            config.setReloadingStrategy(new FileChangedReloadingStrategy());
            //            config.setDelimiterParsingDisabled(true);
            config.load(configFilePath);
            if (config.getFile() == null || !config.getFile().exists()) {
                logger.error("Default configuration file not found: {}", Paths.get(configFilePath).toAbsolutePath());
                throw new ConfigurationException();
            }
            logger.info("Default configuration file '{}' loaded.", config.getFile().getAbsolutePath());
        } catch (ConfigurationException e) {
            logger.error("ConfigurationException", e);
            config = new XMLConfiguration();
        }

        // Load local config file
        try {
            File fileLocal = new File(getConfigLocalPath() + "config_viewer.xml");
            if (fileLocal.exists()) {
                configLocal = new XMLConfiguration();
                configLocal.setReloadingStrategy(new FileChangedReloadingStrategy());
                //                configLocal.setDelimiterParsingDisabled(true);
                configLocal.load(fileLocal);
                logger.info("Local configuration file '{}' loaded.", fileLocal.getAbsolutePath());
            } else {
                configLocal = new XMLConfiguration();
            }
        } catch (ConfigurationException e) {
            logger.error("ConfigurationException", e);
            // If failed loading the local file, use default for both
            configLocal = config;
        }

        // Load stopwords
        try {
            stopwords = loadStopwords(getStopwordsFilePath());
        } catch (IOException | IllegalArgumentException e) {
            logger.warn(e.getMessage());
            stopwords = new HashSet<>(0);
        }
    }

    /**
     * <p>
     * loadStopwords.
     * </p>
     *
     * @param stopwordsFilePath a {@link java.lang.String} object.
     * @should load all stopwords
     * @should remove parts starting with pipe
     * @should not add empty stopwords
     * @should throw IllegalArgumentException if stopwordsFilePath empty
     * @should throw FileNotFoundException if file does not exist
     * @return a {@link java.util.Set} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     */
    protected static Set<String> loadStopwords(String stopwordsFilePath) throws FileNotFoundException, IOException {
        if (StringUtils.isEmpty(stopwordsFilePath)) {
            throw new IllegalArgumentException("stopwordsFilePath may not be null or empty");
        }
        Set<String> ret = new HashSet<>();

        if (StringUtils.isNotEmpty(stopwordsFilePath)) {
            try (FileReader fr = new FileReader(stopwordsFilePath); BufferedReader br = new BufferedReader(fr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (StringUtils.isNotBlank(line)) {
                        if (line.charAt(0) != '#') {
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
            }
        } else {
            logger.warn("'stopwordsFile' not configured. Stop words cannot be filtered from search queries.");
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

    /**
     * <p>
     * reloadingRequired.
     * </p>
     *
     * @return a boolean.
     */
    public boolean reloadingRequired() {
        boolean ret = false;
        if (configLocal != null) {
            ret = configLocal.getReloadingStrategy().reloadingRequired() || config.getReloadingStrategy().reloadingRequired();
        }
        ret = config.getReloadingStrategy().reloadingRequired();
        return ret;
    }

    /*********************************** direct config results ***************************************/

    /**
     * <p>
     * getConfigLocalPath.
     * </p>
     *
     * @return the path to the local config_viewer.xml file.
     */
    public String getConfigLocalPath() {
        String configLocalPath = config.getString("configFolder", "/opt/digiverso/config/");
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
     */
    public boolean isRememberImageZoom() {
        return getLocalBoolean("viewer.rememberImageZoom", false);
    }

    /**
     * <p>
     * isRememberImageRotation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRememberImageRotation() {
        return getLocalBoolean("viewer.rememberImageRotation", false);
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
     * Returns the list of configured metadata for the title bar component. TODO Allow templates and then retire this method.
     *
     * @should return all configured metadata elements
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getTitleBarMetadata() {
        List<HierarchicalConfiguration> elements = getLocalConfigurationsAt("metadata.titleBarMetadataList.metadata");
        if (elements == null) {
            return Collections.emptyList();
        }

        List<Metadata> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration sub = it.next();
            String label = sub.getString("[@label]");
            String masterValue = sub.getString("[@value]");
            boolean group = sub.getBoolean("[@group]", false);
            int type = sub.getInt("[@type]", 0);
            List<HierarchicalConfiguration> params = sub.configurationsAt("param");
            List<MetadataParameter> paramList = null;
            if (params != null) {
                paramList = new ArrayList<>();
                for (Iterator<HierarchicalConfiguration> it2 = params.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration sub2 = it2.next();
                    String fieldType = sub2.getString("[@type]");
                    String source = sub2.getString("[@source]", null);
                    String key = sub2.getString("[@key]");
                    String overrideMasterValue = sub2.getString("[@value]");
                    String defaultValue = sub2.getString("[@defaultValue]");
                    String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                    String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                    boolean addUrl = sub2.getBoolean("[@url]", false);
                    boolean topstructValueFallback = sub2.getBoolean("[@topstructValueFallback]", false);
                    boolean topstructOnly = sub2.getBoolean("[@topstructOnly]", false);
                    paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue, defaultValue,
                            prefix, suffix, addUrl, topstructValueFallback, topstructOnly, Collections.emptyMap()));
                }
            }
            ret.add(new Metadata(label, masterValue, type, paramList, group));
        }

        return ret;
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
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("metadata.searchHitMetadataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, true, true);
    }

    /**
     * Returns the list of configured metadata for the main metadata page.
     *
     * @param template a {@link java.lang.String} object.
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMainMetadataForTemplate(String template) {
        logger.trace("getMainMetadataForTemplate: {}", template);
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("metadata.mainMetadataList.template");
        if (templateList == null) {
            return Collections.emptyList();
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
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("metadata.sideBarMetadataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, false, false);
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
    private static List<Metadata> getMetadataForTemplate(String template, List<HierarchicalConfiguration> templateList,
            boolean fallbackToDefaultTemplate, boolean topstructValueFallbackDefaultValue) {
        if (templateList == null) {
            return Collections.emptyList();
        }

        HierarchicalConfiguration usingTemplate = null;
        HierarchicalConfiguration defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            if (subElement.getString("[@name]").equals(template)) {
                usingTemplate = subElement;
                break;
            } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                defaultTemplate = subElement;
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null && fallbackToDefaultTemplate) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
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
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration usingTemplate, boolean topstructValueFallbackDefaultValue) {
        if (usingTemplate == null) {
            return Collections.emptyList();
        }
        //                logger.debug("template requested: " + template + ", using: " + usingTemplate.getString("[@name]"));
        List<HierarchicalConfiguration> elements = usingTemplate.configurationsAt("metadata");
        if (elements == null) {
            logger.warn("Template '{}' contains no metadata elements.", usingTemplate.getRoot().getName());
            return Collections.emptyList();
        }

        List<Metadata> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration sub = it.next();

            Metadata md = getMetadataFromSubnodeConfig(sub, topstructValueFallbackDefaultValue);
            if (md != null) {
                ret.add(md);
            }
        }

        return ret;
    }

    /**
     * Creates a {@link Metadata} instance from the given subnode configuration
     * 
     * @param sub The subnode configuration
     * @param topstructValueFallbackDefaultValue
     * @return the resulting {@link Metadata} instance
     * @should load replace rules correctly
     */
    // TODO
    static Metadata getMetadataFromSubnodeConfig(HierarchicalConfiguration sub, boolean topstructValueFallbackDefaultValue) {
        if (sub == null) {
            throw new IllegalArgumentException("sub may not be null");
        }

        String label = sub.getString("[@label]");
        String masterValue = sub.getString("[@value]");
        boolean group = sub.getBoolean("[@group]", false);
        int number = sub.getInt("[@number]", -1);
        int type = sub.getInt("[@type]", 0);
        List<HierarchicalConfiguration> params = sub.configurationsAt("param");
        List<MetadataParameter> paramList = null;
        if (params != null) {
            paramList = new ArrayList<>(params.size());
            for (Iterator<HierarchicalConfiguration> it2 = params.iterator(); it2.hasNext();) {
                HierarchicalConfiguration sub2 = it2.next();
                String fieldType = sub2.getString("[@type]");
                String source = sub2.getString("[@source]", null);
                String key = sub2.getString("[@key]");
                String masterValueFragment = sub2.getString("[@value]");
                String defaultValue = sub2.getString("[@defaultValue]");
                String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                String search = sub2.getString("[@search]", "").replace("_SPACE_", " ");
                String replace = sub2.getString("[@replace]", "").replace("_SPACE_", " ");
                boolean addUrl = sub2.getBoolean("[@url]", false);
                boolean topstructValueFallback = sub2.getBoolean("[@topstructValueFallback]", topstructValueFallbackDefaultValue);
                boolean topstructOnly = sub2.getBoolean("[@topstructOnly]", false);
                Map<Object, String> replaceRules = new LinkedHashMap<>();
                List<HierarchicalConfiguration> replaceRuleElements = sub2.configurationsAt("replace");
                if (replaceRuleElements != null) {
                    // Replacement rules can be applied to a character, a string or a regex
                    for (Iterator<HierarchicalConfiguration> it3 = replaceRuleElements.iterator(); it3.hasNext();) {
                        HierarchicalConfiguration sub3 = it3.next();
                        Character character = null;
                        try {
                            int charIndex = sub3.getInt("[@char]");
                            character = (char) charIndex;
                        } catch (NoSuchElementException e) {
                        }
                        String string = null;
                        try {
                            string = sub3.getString("[@string]");
                        } catch (NoSuchElementException e) {
                        }
                        String regex = null;
                        try {
                            regex = sub3.getString("[@regex]");
                        } catch (NoSuchElementException e) {
                        }
                        String replaceWith = sub3.getString("");
                        if (replaceWith == null) {
                            replaceWith = "";
                        }
                        if (character != null) {
                            replaceRules.put(character, replaceWith);
                        } else if (string != null) {
                            replaceRules.put(string, replaceWith);
                        } else if (regex != null) {
                            replaceRules.put("REGEX:" + regex, replaceWith);
                        }
                    }
                }

                paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, masterValueFragment, defaultValue,
                        prefix, suffix, addUrl, topstructValueFallback, topstructOnly, replaceRules));
            }
        }

        return new Metadata(label, masterValue, type, paramList, group, number);
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
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("metadata.normdataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        HierarchicalConfiguration usingTemplate = null;
        HierarchicalConfiguration defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            if (subElement.getString("[@name]").equals(template)) {
                usingTemplate = subElement;
                break;
            }
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
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
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.labelConfig.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, true, false);
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
     * isDisplayWidgetUsage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayWidgetUsage() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage[@display]", false);
    }

    /**
     * Get the metadata configuration for the license text in the usage widget
     *
     * @return the metadata configuration for the license text in the usage widget
     */
    public Metadata getWidgetUsageLicenceTextMetadata() {
        HierarchicalConfiguration sub = null;
        try {
            sub = getLocalConfigurationAt("sidebar.sidebarWidgetUsage.licenseText.metadata");
        } catch (IllegalArgumentException e) {
            // no or multiple occurrences 
        }
        if (sub != null) {
            Metadata md = getMetadataFromSubnodeConfig(sub, false);
            return md;
        }
        return new Metadata();
    }

    /**
     * <p>
     * isDisplaySidebarUsageWidgetLinkToJpegImage.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySidebarUsageWidgetLinkToJpegImage() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.page.displayLinkToJpegImage", false);
    }

    /**
     * <p>
     * isDisplaySidebarUsageWidgetLinkToMasterImage.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySidebarUsageWidgetLinkToMasterImage() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.page.displayLinkToMasterImage", false);
    }

    /**
     * <p>
     * getWidgetUsageMaxJpegSize.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWidgetUsageMaxJpegSize() {
        return getLocalString("sidebar.sidebarWidgetUsage.page.displayLinkToJpegImage[@maxSize]", Scale.MAX_SIZE);
    }

    /**
     * <p>
     * getWidgetUsageMaxMasterImageSize.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWidgetUsageMaxMasterImageSize() {
        return getLocalString("sidebar.sidebarWidgetUsage.page.displayLinkToMasterImage[@maxSize]", Scale.MAX_SIZE);
    }

    /**
     * <p>
     * getMetadataForTemplateFromJDOM.
     * </p>
     *
     * @param eleTemplate a {@link org.jdom2.Element} object.
     * @return a {@link java.util.List} object.
     */
    @Deprecated
    public static List<Metadata> getMetadataForTemplateFromJDOM(Element eleTemplate) {
        List<Metadata> ret = new ArrayList<>();

        if (eleTemplate != null) {
            //                logger.debug("template requested: " + template + ", using: " + usingTemplate.getString("[@name]"));
            List<Element> eleListMetadata = eleTemplate.getChildren("metadata", null);
            if (eleListMetadata != null) {
                for (Element eleMetadata : eleListMetadata) {
                    try {
                        String label = eleMetadata.getAttributeValue("label");
                        String masterValue = eleMetadata.getAttributeValue("value");
                        boolean group = eleMetadata.getAttribute("group") != null ? eleMetadata.getAttribute("group").getBooleanValue() : false;
                        int number = eleMetadata.getAttribute("number") != null ? eleMetadata.getAttribute("number").getIntValue() : -1;
                        int type = eleMetadata.getAttribute("type") != null ? eleMetadata.getAttribute("type").getIntValue() : 0;
                        List<Element> eleListParam = eleMetadata.getChildren("param", null);
                        List<MetadataParameter> paramList = null;
                        if (eleListParam != null) {
                            paramList = new ArrayList<>(eleListParam.size());
                            for (Element eleParam : eleListParam) {
                                String fieldType = eleParam.getAttributeValue("type");
                                // logger.trace("param type: " + fieldType);
                                String source = eleParam.getAttributeValue("source");
                                String key = eleParam.getAttributeValue("key");
                                String overrideMasterValue = eleParam.getAttributeValue("value");
                                String defaultValue = eleParam.getAttributeValue("defaultValue");
                                String prefix =
                                        eleParam.getAttribute("prefix") != null ? eleParam.getAttributeValue("prefix").replace("_SPACE_", " ") : "";
                                String suffix =
                                        eleParam.getAttribute("suffix") != null ? eleParam.getAttributeValue("suffix").replace("_SPACE_", " ") : "";
                                boolean addUrl = eleParam.getAttribute("url") != null ? eleParam.getAttribute("url").getBooleanValue() : false;
                                boolean dontUseTopstructValue = eleParam.getAttribute("dontUseTopstructValue") != null
                                        ? eleParam.getAttribute("dontUseTopstructValue").getBooleanValue() : false;
                                boolean topstructValueFallback = eleParam.getAttribute("topstructValueFallback") != null
                                        ? eleParam.getAttribute("topstructValueFallback").getBooleanValue() : false;
                                boolean topstructOnly = eleParam.getAttribute("topstructOnly") != null
                                        ? eleParam.getAttribute("topstructOnly").getBooleanValue() : false;

                                paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                        defaultValue, prefix, suffix, addUrl, topstructValueFallback, topstructOnly, Collections.emptyMap()));
                            }
                        }
                        ret.add(new Metadata(label, masterValue, type, paramList, group, number));
                    } catch (DataConversionException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }

        return ret;
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
        return getLocalBoolean("metadata.browsingMenu.enabled", false);
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
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<BrowsingMenuFieldConfig> getBrowsingMenuFields() {
        List<HierarchicalConfiguration> fields = getLocalConfigurationsAt("metadata.browsingMenu.luceneField");
        if (fields == null) {
            return Collections.emptyList();
        }

        List<BrowsingMenuFieldConfig> ret = new ArrayList<>(fields.size());
        for (Iterator<HierarchicalConfiguration> it = fields.iterator(); it.hasNext();) {
            HierarchicalConfiguration sub = it.next();
            String field = sub.getString(".");
            String sortField = sub.getString("[@sortField]");
            String filterQuery = sub.getString("[@filterQuery]");
            String docstructFilterString = sub.getString("[@docstructFilters]");
            boolean recordsAndAnchorsOnly = sub.getBoolean("[@recordsAndAnchorsOnly]", false);
            BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig(field, sortField, filterQuery, docstructFilterString, recordsAndAnchorsOnly);
            ret.add(bmfc);
        }

        return ret;
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
        HierarchicalConfiguration subConfig = getCollectionConfiguration(field);
        if (subConfig != null) {
            return subConfig.getString("splittingCharacter", ".");
        }

        return getLocalString("viewer.splittingCharacter", ".");
    }

    /**
     * Returns the collection config block for the given field.
     *
     * @param field
     * @return
     */
    private HierarchicalConfiguration getCollectionConfiguration(String field) {
        List<HierarchicalConfiguration> collectionList = getLocalConfigurationsAt("collections.collection");
        if (collectionList == null) {
            return null;
        }

        for (Iterator<HierarchicalConfiguration> it = collectionList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            if (subElement.getString("[@field]").equals(field)) {
                return subElement;

            }
        }

        return null;
    }

    /**
     * <p>
     * getCollectionSorting.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<DcSortingList> getCollectionSorting(String field) {
        List<DcSortingList> superlist = new ArrayList<>();
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return superlist;
        }

        superlist.add(new DcSortingList(getLocalList("sorting.collection")));
        List<HierarchicalConfiguration> listConfigs = collection.configurationsAt("sorting.sortingList");
        for (HierarchicalConfiguration listConfig : listConfigs) {
            String sortAfter = listConfig.getString("[@sortAfter]", null);
            List<String> collectionList = getLocalList(listConfig, null, "collection", Collections.<String> emptyList());
            superlist.add(new DcSortingList(sortAfter, collectionList));
        }
        return superlist;
    }

    /**
     * <p>
     * getCollectionBlacklistMode.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionBlacklistMode(String field) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return "all";
        }

        return collection.getString("blacklist.mode", "all");
    }

    /**
     * Returns collection names to be omitted from search results, listings etc.
     *
     * @param field a {@link java.lang.String} object.
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCollectionBlacklist(String field) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return null;
        }
        return getLocalList(collection, null, "blacklist.collection", Collections.<String> emptyList());
    }

    /**
     * Returns the index field by which records in the collection with the given name are to be sorted in a listing.
     *
     * @param field a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @should return correct field for collection
     * @should give priority to exact matches
     * @should return hyphen if collection not found
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionDefaultSortField(String field, String name) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return "-";
        }

        List<HierarchicalConfiguration> fields = collection.configurationsAt("defaultSortFields.field");
        if (fields == null) {
            return "-";
        }

        String exactMatch = null;
        String inheritedMatch = null;
        for (Iterator<HierarchicalConfiguration> it = fields.iterator(); it.hasNext();) {
            HierarchicalConfiguration sub = it.next();
            String key = sub.getString("[@collection]");
            if (name.equals(key)) {
                exactMatch = sub.getString("");
            } else if (key.endsWith("*") && name.startsWith(key.substring(0, key.length() - 1))) {
                inheritedMatch = sub.getString("");
            }
        }
        // Exact match is given priority so that it is possible to override the inherited sort field
        if (StringUtils.isNotEmpty(exactMatch)) {
            return exactMatch;
        }
        if (StringUtils.isNotEmpty(inheritedMatch)) {
            return inheritedMatch;
        }

        return "-";
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
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
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
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
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
     * @should return first field where hierarchy enabled
     * @return a {@link java.lang.String} object.
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
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
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
     * getContentServerWrapperUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getContentServerWrapperUrl() {
        return getLocalString("urls.contentServerWrapper");
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
     * <p>
     * getContentRestApiUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getContentRestApiUrl() {
        return getRestApiUrl() + "content/";

    }

    /**
     * <p>
     * getRestApiUrl.
     * </p>
     *
     * @return The url to the viewer rest api as configured in the config_viewer. The url always ends with "/"
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
     * <p>
     * getContentServerRealUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getContentServerRealUrl() {
        return getLocalString("urls.contentServer");
    }

    /**
     * <p>
     * getMetsUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMetsUrl() {
        return getLocalString("urls.metadata.mets");
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
     * getSearchHitsPerPage.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitsPerPage() {
        return getLocalInt("search.hitsPerPage", 10);
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
        return getLocalBoolean("search.advanced.enabled", true);
    }

    /**
     * <p>
     * getAdvancedSearchDefaultItemNumber.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getAdvancedSearchDefaultItemNumber() {
        return getLocalInt("search.advanced.defaultItemNumber", 2);
    }

    /**
     * <p>
     * getAdvancedSearchFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getAdvancedSearchFields() {
        return getLocalList("search.advanced.searchFields.field");
    }

    /**
     * <p>
     * isAggregateHits.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAggregateHits() {
        return getLocalBoolean("search.aggregateHits", true);
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
        return getLocalBoolean("search.displayAdditionalMetadata.enabled", true);
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
        return getLocalList("search.displayAdditionalMetadata.ignoreField", Collections.emptyList());
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataTranslateFields.
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataTranslateFields() {
        return getLocalList("search.displayAdditionalMetadata.translateField", Collections.emptyList());
    }

    /**
     * <p>
     * isAdvancedSearchFieldHierarchical.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchFieldHierarchical(String field) {
        List<HierarchicalConfiguration> fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList == null) {
            return false;
        }

        for (Iterator<HierarchicalConfiguration> it = fieldList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            if (subElement.getString(".").equals(field)) {
                return subElement.getBoolean("[@hierarchical]", false);
            }
        }

        return false;
    }

    /**
     * <p>
     * isAdvancedSearchFieldUntokenizeForPhraseSearch.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchFieldUntokenizeForPhraseSearch(String field) {
        List<HierarchicalConfiguration> fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList == null) {
            return false;
        }

        for (Iterator<HierarchicalConfiguration> it = fieldList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            if (subElement.getString(".").equals(field)) {
                return subElement.getBoolean("[@untokenizeForPhraseSearch]", false);
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
        return getLocalBoolean("search.timeline.enabled", true);
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
        return getLocalBoolean("search.calendar.enabled", true);
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
        return getLocalString("indexedMetsFolder");
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
        return getLocalString("indexedLidoFolder");
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
        return getLocalString("indexedDenkxwebFolder");
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
        return getLocalString("origContentFolder");
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
        return getLocalString("altoFolder");
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
        return getLocalString("abbyyFolder");
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
        return getLocalString("fulltextFolder");
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
        return getLocalString("teiFolder");
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
        return getLocalString("cmdiFolder");
    }

    /**
     * <p>
     * getWcFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getWcFolder() {
        return getLocalString("wcFolder");
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
    @SuppressWarnings("static-method")
    public String getTempFolder() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "viewer").toString();
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
        return getLocalBoolean("user.userRegistrationEnabled", true);
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
        XMLConfiguration myConfigToUse = config;
        // User local config, if available
        if (!configLocal.configurationsAt("user.authenticationProviders").isEmpty()) {
            myConfigToUse = configLocal;
        }

        int max = myConfigToUse.getMaxIndex("user.authenticationProviders.provider");
        List<IAuthenticationProvider> providers = new ArrayList<>(max + 1);
        for (int i = 0; i <= max; i++) {
            String label = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@label]");
            String name = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@name]");
            String endpoint = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@endpoint]", null);
            String image = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@image]", null);
            String type = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@type]", "");
            boolean visible = myConfigToUse.getBoolean("user.authenticationProviders.provider(" + i + ")[@show]", true);
            String clientId = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@clientId]", null);
            String clientSecret = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@clientSecret]", null);
            long timeoutMillis = myConfigToUse.getLong("user.authenticationProviders.provider(" + i + ")[@timeout]", 10000);

            if (visible) {
                IAuthenticationProvider provider = null;
                switch (type.toLowerCase()) {
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
                        logger.error("Cannot add authentification provider with name {} and type {}. No implementation found", name, type);
                }
                if (provider != null) {
                    // Look for user group configurations to which users shall be automatically added when logging in
                    List<String> addToUserGroupList =
                            getLocalList(myConfigToUse, null, "user.authenticationProviders.provider(" + i + ").addUserToGroup", null);
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
    public boolean isSidebarPageLinkVisible() {
        return getLocalBoolean("sidebar.page.visible", true);
    }

    /**
     * <p>
     * isSidebarCalendarLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarCalendarLinkVisible() {
        return getLocalBoolean("sidebar.calendar.visible", true);
    }

    /**
     * <p>
     * isSidebarTocLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocLinkVisible() {
        return getLocalBoolean("sidebar.toc.visible", true);
    }

    /**
     * <p>
     * isSidebarThumbsLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarThumbsLinkVisible() {
        return getLocalBoolean("sidebar.thumbs.visible", true);
    }

    /**
     * <p>
     * isSidebarMetadataLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarMetadataLinkVisible() {
        return getLocalBoolean("sidebar.metadata.visible", true);
    }

    /**
     * <p>
     * isShowSidebarEventMetadata.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isShowSidebarEventMetadata() {
        return getLocalBoolean("sidebar.metadata.showEventMetadata", true);
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
        return getLocalBoolean("sidebar.fulltext.visible", true);
    }

    /**
     * <p>
     * isSidebarTocVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc.visible", true);
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
        HierarchicalConfiguration hc = getLocalConfigurationAt("toc.useTreeView");
        String docStructTypes = hc.getString("[@showDocStructs]");
        boolean allowed = hc.getBoolean(".");
        if (!allowed) {
            logger.trace("Tree view disabled");
            return false;
        }

        if (docStructTypes != null) {
            String[] docStructTypesSplit = docStructTypes.split(";");
            for (String dst : docStructTypesSplit) {
                if (dst.equals("_ALL") || dst.equals(docStructType)) {
                    logger.trace("Tree view for {} allowed", docStructType);
                    return true;
                }
            }

        }

        // logger.trace("Tree view for {} not allowed", docStructType);
        return false;
    }

    /**
     * Returns the names of all configured drill-down fields in the order they appear in the list, no matter whether they're regular or hierarchical.
     *
     * @return List of regular and hierarchical fields in the order in which they appear in the config file
     * @should return correct order
     */
    public List<String> getAllDrillDownFields() {
        HierarchicalConfiguration drillDown = getLocalConfigurationAt("search.drillDown");
        List<ConfigurationNode> nodes = drillDown.getRootNode().getChildren();
        if (!nodes.isEmpty()) {
            List<String> ret = new ArrayList<>(nodes.size());
            for (ConfigurationNode node : nodes) {
                switch (node.getName()) {
                    case "field":
                    case "hierarchicalField":
                        ret.add((String) node.getValue());
                        break;
                }
            }

            return ret;
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getDrillDownFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getDrillDownFields() {
        return getLocalList("search.drillDown.field");
    }

    /**
     * <p>
     * getHierarchicalDrillDownFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getHierarchicalDrillDownFields() {
        return getLocalList("search.drillDown.hierarchicalField");
    }

    /**
     * <p>
     * getInitialDrillDownElementNumber.
     * </p>
     *
     * @should return correct value
     * @should return default value if field not found
     * @param field a {@link java.lang.String} object.
     * @return a int.
     */
    public int getInitialDrillDownElementNumber(String field) {
        if (StringUtils.isBlank(field)) {
            return getLocalInt("search.drillDown.initialElementNumber", 3);
        }

        String facetifiedField = SearchHelper.facetifyField(field);
        // Regular fields
        List<HierarchicalConfiguration> drillDownFields = getLocalConfigurationsAt("search.drillDown.field");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        return fieldConfig.getInt("[@initialElementNumber]");
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }
        // Hierarchical fields
        drillDownFields = getLocalConfigurationsAt("search.drillDown.hierarchicalField");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        return fieldConfig.getInt("[@initialElementNumber]");
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }

        // return getLocalInt("search.drillDown.initialElementNumber", 3);
        return -1;
    }

    /**
     * <p>
     * getSortOrder.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSortOrder(String field) {
        if (StringUtils.isBlank(field)) {
            return "default";
        }

        String facetifiedField = SearchHelper.facetifyField(field);

        // Regular fields
        List<HierarchicalConfiguration> drillDownFields = getLocalConfigurationsAt("search.drillDown.field");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        String sortOrder = fieldConfig.getString("[@sortOrder]");
                        if (sortOrder != null) {
                            return sortOrder;
                        }
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }
        // Hierarchical Field
        drillDownFields = getLocalConfigurationsAt("search.drillDown.hierarchicalField");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        String sortOrder = fieldConfig.getString("[@sortOrder]");
                        if (sortOrder != null) {
                            return sortOrder;
                        }
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }

        return "default";
    }

    /**
     * Returns a list of values to prioritize for the given drill-down field.
     *
     * @param field a {@link java.lang.String} object.
     * @return List of priority values; empty list if none found for the given field
     * @should return return all configured elements for regular fields
     * @should return return all configured elements for hierarchical fields
     */
    public List<String> getPriorityValuesForDrillDownField(String field) {
        if (StringUtils.isBlank(field)) {
            return Collections.emptyList();
        }

        String facetifiedField = SearchHelper.facetifyField(field);

        // Regular fields
        List<HierarchicalConfiguration> drillDownFields = getLocalConfigurationsAt("search.drillDown.field");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        String priorityValues = fieldConfig.getString("[@priorityValues]");
                        if (StringUtils.isNotEmpty(priorityValues)) {
                            String[] priorityValuesSplit = priorityValues.split(";");
                            return Arrays.asList(priorityValuesSplit);
                        }
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }

        // Hierarchical Field
        drillDownFields = getLocalConfigurationsAt("search.drillDown.hierarchicalField");
        if (drillDownFields != null && !drillDownFields.isEmpty()) {
            for (HierarchicalConfiguration fieldConfig : drillDownFields) {
                if (fieldConfig.getRootNode().getValue().equals(field)
                        || fieldConfig.getRootNode().getValue().equals(field + SolrConstants._UNTOKENIZED)
                        || fieldConfig.getRootNode().getValue().equals(facetifiedField)) {
                    try {
                        String priorityValues = fieldConfig.getString("[@priorityValues]");
                        if (StringUtils.isNotEmpty(priorityValues)) {
                            String[] priorityValuesSplit = priorityValues.split(";");
                            return Arrays.asList(priorityValuesSplit);
                        }
                    } catch (ConversionException | NoSuchElementException e) {
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getRangeFacetFields.
     * </p>
     *
     * @return List of facet fields to be used as range values
     */
    @SuppressWarnings("static-method")
    public List<String> getRangeFacetFields() {
        return Collections.singletonList(SolrConstants._CALENDAR_YEAR);
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
        return getLocalBoolean("search.sorting.enabled", true);
    }

    /**
     * <p>
     * getDefaultSortField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDefaultSortField() {
        return getLocalString("search.sorting.defaultSortField", null);
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
        return getLocalList("search.sorting.luceneField");
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
     * <p>
     * getUnconditionalImageAccessMaxWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getUnconditionalImageAccessMaxWidth() {
        return getLocalInt("accessConditions.unconditionalImageAccessMaxWidth", 120);
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
        boolean enabled = getLocalBoolean("pdf.titlePdfEnabled", true);
        return enabled;
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
     * getRulesetFilePath.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRulesetFilePath() {
        return getLocalString("content.ruleset");
    }

    /**
     * <p>
     * getDefaultCollection.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDefaultCollection() {
        return getLocalString("content.defaultCollection");
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
     * getThumbnailsCompression.
     * </p>
     *
     * @return a int.
     */
    public int getThumbnailsCompression() {
        return getLocalInt("viewer.thumbnailsCompression", 85);
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
     * getMultivolumeThumbnailWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getMultivolumeThumbnailWidth() {
        return getLocalInt("toc.multiVolumeThumbnailsWidth", 50);
    }

    /**
     * <p>
     * getMultivolumeThumbnailHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getMultivolumeThumbnailHeight() {
        return getLocalInt("toc.multiVolumeThumbnailsHeight", 60);
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
     * isAddDublinCoreMetaTags.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddDublinCoreMetaTags() {
        return getLocalBoolean("metadata.addDublinCoreMetaTags", false);
    }

    /**
     * <p>
     * isAddHighwirePressMetaTags.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddHighwirePressMetaTags() {
        return getLocalBoolean("metadata.addHighwirePressMetaTags", false);
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
        return getZoomImageViewConfig(view, image).getString("[@type]");
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
        //        defaultList.add("600");
        //        defaultList.add("900");
        //        defaultList.add("1500");

        SubnodeConfiguration zoomImageViewConfig = getZoomImageViewConfig(view, image);
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
        List<HierarchicalConfiguration> sizes = getZoomImageViewConfig(view, image).configurationsAt("tileSize");
        if (sizes != null) {
            for (HierarchicalConfiguration sizeConfig : sizes) {
                int size = sizeConfig.getInt("size", 0);
                String[] resolutionString = sizeConfig.getStringArray("scaleFactors");
                List<Integer> resolutions = new ArrayList<>();
                for (String res : resolutionString) {
                    try {
                        int resolution = Integer.parseInt(res);
                        resolutions.add(resolution);
                    } catch (NullPointerException | NumberFormatException e) {
                        logger.warn("Cannot parse " + res + " as int");
                    }
                }
                map.put(size, resolutions);
            }
        }
        if (map.isEmpty()) {
            map.put(512, Arrays.asList(new Integer[] { 1, 32 }));
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
     * @return a {@link org.apache.commons.configuration.SubnodeConfiguration} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public SubnodeConfiguration getZoomImageViewConfig(PageType pageType, ImageType imageType) throws ViewerConfigurationException {
        List<HierarchicalConfiguration> configs = getLocalConfigurationsAt("viewer.zoomImageView");

        for (HierarchicalConfiguration subConfig : configs) {

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

            return (SubnodeConfiguration) subConfig;
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
     * isDisplayTopstructLabel.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTopstructLabel() {
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayTopstructLabel", false);
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
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayStructType", true);
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
        return getLocalInt("metadata.searchHitMetadataList.valueNumber", 1);
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
        return getLocalInt("metadata.searchHitMetadataList.valueLength", 0);
    }

    /**
     * Returns the preference order of data to be used as an image footer text.
     *
     * @should return all configured elements in the correct order
     * @return a {@link java.util.List} object.
     */
    public List<String> getWatermarkTextConfiguration() {
        List<String> list = getLocalList("viewer.watermarkTextConfiguration.text");
        return list;
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
     * isOriginalContentDownload.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isOriginalContentDownload() {
        return getLocalBoolean("content.originalContentDownload", false);
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
     * getDocstructTargetPageType.
     * </p>
     *
     * @param docstruct a {@link java.lang.String} object.
     * @should return correct value
     * @should return null if docstruct not found
     * @return a {@link java.lang.String} object.
     */
    public String getDocstructTargetPageType(String docstruct) {
        return getLocalString("viewer.docstructTargetPageTypes." + docstruct);
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
     * <p>
     * isUseViewerLocaleAsRecordLanguage.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUseViewerLocaleAsRecordLanguage() {
        return getLocalBoolean("viewer.useViewerLocaleAsRecordLanguage", false);
    }

    /**
     * <p>
     * isTocAlwaysDisplayDocstruct.
     * </p>
     *
     * @return a boolean.
     */
    @Deprecated
    public boolean isTocAlwaysDisplayDocstruct() {
        return getLocalBoolean("toc.alwaysDisplayDocstruct", false);
    }

    /**
     * <p>
     * getFeedbackEmailAddress.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getFeedbackEmailAddress() {
        return getLocalString("user.feedbackEmailAddress");
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
        return getLocalBoolean("bookmarks.bookmarksEnabled", true);
    }

    /**
     * <p>
     * isForceJpegConversion.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isForceJpegConversion() {
        return getLocalBoolean("viewer.forceJpegConversion", false);
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
     * isUserCommentsEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUserCommentsEnabled() {
        return getLocalBoolean(("userComments.enabled"), false);
    }

    /**
     * <p>
     * getUserCommentsConditionalQuery.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getUserCommentsConditionalQuery() {
        return getLocalString("userComments.conditionalQuery");
    }

    /**
     * <p>
     * getUserCommentsNotificationEmailAddresses.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getUserCommentsNotificationEmailAddresses() {
        return getLocalList("userComments.notificationEmailAddress");
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
     * <p>
     * isSubthemesEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSubthemesEnabled() {
        return getLocalBoolean("viewer.theme[@subTheme]", false);
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
            logger.error("Theme name could not be read - config_viewer.xml may not be well-formed.");
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
        return getLocalString("viewer.theme[@discriminatorField]");
    }

    /**
     * <p>
     * isSubthemeAutoSwitch.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSubthemeAutoSwitch() {
        return getLocalBoolean("viewer.theme[@autoSwitch]", false);
    }

    /**
     * <p>
     * isSubthemeAddFilterQuery.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSubthemeAddFilterQuery() {
        return getLocalBoolean("viewer.theme[@addFilterQuery]", false);
    }

    /**
     * <p>
     * isSubthemeFilterQueryVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSubthemeFilterQueryVisible() {
        return getLocalBoolean("viewer.theme[@filterQueryVisible]", false);
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
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template configuration if template is null
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<StringPair> getTocVolumeSortFieldsForTemplate(String template) {
        HierarchicalConfiguration usingTemplate = null;
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return Collections.emptyList();
        }
        HierarchicalConfiguration defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            String templateName = subElement.getString("[@name]");
            String groupBy = subElement.getString("[@groupBy]");
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(templateName)) {
                    defaultTemplate = subElement;
                }
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
        }

        List<HierarchicalConfiguration> fields = usingTemplate.configurationsAt("field");
        if (fields == null) {
            return Collections.emptyList();
        }

        List<StringPair> ret = new ArrayList<>(fields.size());
        for (Iterator<HierarchicalConfiguration> it2 = fields.iterator(); it2.hasNext();) {
            HierarchicalConfiguration sub = it2.next();
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
        HierarchicalConfiguration usingTemplate = null;
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration subElement = it.next();
            String templateName = subElement.getString("[@name]");
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(templateName)) {
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
     * showThumbnailsInToc.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean showThumbnailsInToc() {
        return this.getLocalBoolean("toc.multiVolumeThumbnailsEnabled", true);
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
        return getLocalBoolean("piwik.enabled", false);
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
        return getLocalBoolean("search.searchSavingEnabled", true);
    }

    /**
     * <p>
     * isBoostTopLevelDocstructs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBoostTopLevelDocstructs() {
        return getLocalBoolean("search.boostTopLevelDocstructs", true);
    }

    /**
     * <p>
     * isGroupDuplicateHits.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isGroupDuplicateHits() {
        return getLocalBoolean("search.groupDuplicateHits", true);
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
        List<String> filterStrings = getLocalList("search.filters.filter");
        List<SearchFilter> ret = new ArrayList<>(filterStrings.size());
        for (String filterString : filterStrings) {
            if (filterString.startsWith("filter_")) {
                ret.add(new SearchFilter(filterString, filterString.substring(7)));
            } else {
                logger.error("Invalid search filter definition: {}", filterString);
            }
        }

        return ret;
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
        List<HierarchicalConfiguration> elements = getLocalConfigurationsAt("webapi.fields.field");
        if (elements == null) {
            return Collections.emptyList();
        }

        List<Map<String, String>> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration sub = it.next();
            Map<String, String> fieldConfig = new HashMap<>();
            fieldConfig.put("jsonField", sub.getString("[@jsonField]", null));
            fieldConfig.put("luceneField", sub.getString("[@luceneField]", null));
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
     * <p>
     * isCmsEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCmsEnabled() {
        return getLocalBoolean("cms.enabled", false);
    }

    /**
     * <p>
     * useCustomNavBar.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean useCustomNavBar() {
        return getLocalBoolean("cms.useCustomNavBar", false);
    }

    /**
     * <p>
     * getCmsTemplateFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsTemplateFolder() {
        return getLocalString("cms.templateFolder", "resources/cms/templates/");
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
        return getLocalString("cms.mediaFolder", "cms_media");
    }

    /**
     * <p>
     * getCmsClassifications.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCmsClassifications() {
        return getLocalList("cms.classifications.classification");
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
     * getCmsMediaDisplayHeight.
     * </p>
     *
     * @return a int.
     */
    public int getCmsMediaDisplayHeight() {
        return getLocalInt("cms.mediaDisplayHeight", 0);
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
     */
    public boolean isPageBrowseEnabled() {
        return getLocalBoolean("viewer.pageBrowse.enabled", false);
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
                intList.add(new Integer(s));
            } catch (NullPointerException | NumberFormatException e) {
                logger.error("Illegal config at 'viewer.pageBrowse.pageBrowseStep': " + s);
            }
        }
        return intList;
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
        return getLocalBoolean("reCaptcha[@show]", true);
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
        return getLocalBoolean("sidebar.searchInItem.visible", true);
    }

    /**
     * <p>
     * getDefaultBrowseIcon.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDefaultBrowseIcon(String field) {
        HierarchicalConfiguration subConfig = getCollectionConfiguration(field);
        if (subConfig != null) {
            return subConfig.getString("defaultBrowseIcon", "");
        }

        return getLocalString("collections.collection.defaultBrowseIcon", getLocalString("collections.defaultBrowseIcon", ""));
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
        return getLocalBoolean("search.export.excel.enabled", false);
    }

    /**
     * <p>
     * getSearchExcelExportFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getSearchExcelExportFields() {
        return getLocalList("search.export.excel.field", new ArrayList<String>(0));
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
     * isDisplayEmptyTocInSidebar.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayEmptyTocInSidebar() {
        return getLocalBoolean("sidebar.sidebarToc.visibleIfEmpty", true);
    }

    /**
     * <p>
     * isDoublePageModeEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDoublePageModeEnabled() {
        return getLocalBoolean("viewer.doublePageMode.enabled", false);
    }

    /**
     * <p>
     * getRestrictedImageUrls.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getRestrictedImageUrls() {
        return getLocalList("viewer.externalContent.restrictedUrls.url", Collections.emptyList());
    }

    /**
     * <p>
     * getIIIFMetadataFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIIIFMetadataFields() {
        return getLocalList("webapi.iiif.metadataFields.field", Collections.emptyList());
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
        List<String> fields = getLocalList("webapi.iiif.metadataFields.event", Collections.emptyList());
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

        HierarchicalConfiguration fieldsConfig = getLocalConfigurationAt("webapi.iiif.metadataFields");
        List<ConfigurationNode> fields = fieldsConfig.getRootNode().getChildren();
        for (ConfigurationNode fieldNode : fields) {
            if (fieldNode.getValue().equals(field)) {
                return fieldNode.getAttributes("label").stream().findFirst().map(node -> node.getValue().toString()).orElse("");
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
    public String getIIIFLogo() {
        return getLocalString("webapi.iiif.logo", null);
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
    public String getIIIFAttribution() {
        return getLocalString("webapi.iiif.attribution", "provided by Goobi viewer");
    }

    /**
     * <p>
     * getIIIFDescriptionFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIIIFDescriptionFields() {
        return getLocalList("webapi.iiif.descriptionFields.field", Collections.singletonList("MD_CONTENTDESCRIPTION"));

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
        return getLocalString("sitemap.sitelinksField");
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
        return getLocalString("sitemap.sitelinksFilterQuery");
    }

    /**
     * <p>
     * getConfiguredCollections.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getConfiguredCollections() {
        return getLocalList("collections.collection[@field]", Collections.emptyList());

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
        String token = getLocalString("webapi.authorization.token", "");
        return token;
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
        boolean redirect = getLocalBoolean("collections.redirectToWork", true);
        return redirect;
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
        String token = getLocalString("embedding.twitter.userName");
        return token;
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
        return getLocalBoolean("webapi.cors[@use]", false);
    }

    /**
     * <p>
     * getCORSHeaderValue.
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

    public String getAccessConditionDisplayField() {
        return getLocalString("webGuiDisplay.displayCopyrightInfo.accessConditionField", null);
    }

    public String getCopyrightDisplayField() {
        return getLocalString("webGuiDisplay.displayCopyrightInfo.copyrightField", null);
    }

    public boolean isDisplayCopyrightInfo() {
        return getLocalBoolean("webGuiDisplay.displayCopyrightInfo.visible", false);
    }

    public boolean isDisplaySocialMediaShareLinks() {
        return getLocalBoolean("webGuiDisplay.displaySocialMediaShareLinks", false);
    }

    public boolean isDisplayAnchorLabelInTitleBar(String template) {
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getBoolean("displayAnchorTitle", false);
        }

        return false;
    }

    public String getAnchorLabelInTitleBarPrefix(String template) {
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@prefix]", "");
        }

        return "";
    }

    public String getAnchorLabelInTitleBarSuffix(String template) {
        List<HierarchicalConfiguration> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@suffix]", " ");
        }

        return " ";
    }

    /**
     * Find the template with the given name in the templateList. If no such template exists, find the template with name _DEFAULT. Failing that,
     * return null;
     * 
     * @param templateList
     * @param template
     * @return
     */
    private static HierarchicalConfiguration getMatchingConfig(List<HierarchicalConfiguration> templateList, String name) {
        HierarchicalConfiguration conf = null;
        HierarchicalConfiguration defaultConf = null;
        if (templateList != null) {
            for (HierarchicalConfiguration subConf : templateList) {
                if (name.equalsIgnoreCase(subConf.getString("[@name]"))) {
                    conf = subConf;
                    break;
                } else if ("_DEFAULT".equalsIgnoreCase(subConf.getString("[@name]"))) {
                    defaultConf = subConf;
                }
            }
        }
        if (conf != null) {
            return conf;
        }

        return defaultConf;
    }
}
