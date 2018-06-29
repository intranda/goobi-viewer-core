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
package de.intranda.digiverso.presentation.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.lang.StringUtils;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter.MetadataParameterType;
import de.intranda.digiverso.presentation.model.search.SearchFilter;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.OpenIdProvider;
import de.intranda.digiverso.presentation.model.viewer.BrowsingMenuFieldConfig;
import de.intranda.digiverso.presentation.model.viewer.DcSortingList;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;

public final class Configuration extends AbstractConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private Set<String> stopwords;

    public Configuration(String configFilePath) {
        // Load default config file
        try {
            config = new XMLConfiguration();
            config.setReloadingStrategy(new FileChangedReloadingStrategy());
            //            config.setDelimiterParsingDisabled(true);
            config.load(configFilePath);
            if (!config.getFile().exists()) {
                logger.error("Default configuration file not found: {}", config.getFile().getAbsolutePath());
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
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            stopwords = new HashSet<>(0);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            stopwords = new HashSet<>(0);
        }
    }

    /**
     * 
     * @param stopwordsFilePath
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     * @should load all stopwords
     * @should remove parts starting with pipe
     * @should not add empty stopwords
     * @should throw IllegalArgumentException if stopwordsFilePath empty
     * @should throw FileNotFoundException if file does not exist
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
     * @return
     * @should return all stopwords
     */
    public Set<String> getStopwords() {
        return stopwords;
    }

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
     * @return the path to the local config_viewer.xml file.
     */
    public String getConfigLocalPath() {
        String configLocalPath = config.getString("configFolder", "/opt/digiverso/config/");
        if (!configLocalPath.endsWith("/")) {
            configLocalPath += "/";
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0 && configLocalPath.startsWith("/opt/")) {
            configLocalPath = configLocalPath.replace("/opt", "C:");
        }
        return configLocalPath;
    }

    public String getLocalRessourceBundleFile() {
        return getConfigLocalPath() + "messages_de.properties";
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getViewerThumbnailsPerPage() {
        return getLocalInt("viewer.thumbnailsPerPage", 10);
    }

    /**
     * width of the image returned by content server for normal image view
     * 
     * @return
     * @should return correct value
     */
    public int getDefaultImageWidth() {
        return getLocalInt("viewer.defaultImageWidth", 900);
    }

    /**
     * width of the image returned by content server for normal image view
     * 
     * @return
     * @should return correct value
     */
    public int getDefaultImageHeight() {
        return getLocalInt("viewer.defaultImageHeight", 1500);
    }

    /**
     * width of the image returned by content server for fullscreen view
     * 
     * @return
     * @should return correct value
     */
    public int getDefaultImageFullscreenWidth() {
        return getLocalInt("viewer.defaultImageFullscreenWidth", 1000);
    }

    /**
     * height of the image returned by content server for fullscreen view
     * 
     * @return
     * @should return correct value
     */
    public int getDefaultImageFullscreenHeight() {
        return getLocalInt("viewer.defaultImageFullscreenHeight", 1400);
    }

    /**
     * Display width of the image in normal image view
     * 
     * @return
     */
    @Deprecated
    public int getViewImageWidth() {
        return getLocalInt("viewer.zoomImageView[@width]", 600);
    }

    /**
     * Display height of the image in normal image view
     * 
     * @return
     */
    @Deprecated
    public int getViewImageHeight() {
        return getLocalInt("viewer.zoomImageView[@height]", 800);
    }

    /**
     * Display width of the image in fullscreen view
     * 
     * @return
     */
    @Deprecated
    public int getFullscreenImageWidth() {
        return getLocalInt("viewer.zoomFullscreenView[@width]", 1024);
    }

    /**
     * Display height of the image in fullscreen view
     * 
     * @return
     */
    @Deprecated
    public int getFullscreenImageHeight() {
        return getLocalInt("viewer.zoomFullscreenView[@height]", 768);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getViewerMaxImageWidth() {
        return getLocalInt("viewer.maxImageWidth", 2000);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getViewerMaxImageHeight() {
        return getLocalInt("viewer.maxImageHeight", 2000);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getViewerMaxImageScale() {
        return getLocalInt("viewer.maxImageScale", 500);
    }

    public boolean isRememberImageZoom() {
        return getLocalBoolean("viewer.rememberImageZoom", false);
    }

    public boolean isRememberImageRotation() {
        return getLocalBoolean("viewer.rememberImageRotation", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getViewerDfgViewerUrl() {
        return getLocalString("urls.dfg-viewer", "http://dfg-viewer.de/v2?set[mets]=");
    }

    /**
     * Returns the list of configured metadata for the title bar component.
     * 
     * @return
     * @should return all configured metadata elements
     */
    @SuppressWarnings("rawtypes")
    public List<Metadata> getTitleBarMetadata() {
        List<Metadata> ret = new ArrayList<>();

        List elements = getLocalConfigurationsAt("metadata.titleBarMetadataList.metadata");
        if (elements != null) {
            for (Iterator it = elements.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                String label = sub.getString("[@label]");
                String masterValue = sub.getString("[@value]");
                boolean group = sub.getBoolean("[@group]", false);
                int type = sub.getInt("[@type]", 0);
                List params = sub.configurationsAt("param");
                List<MetadataParameter> paramList = null;
                if (params != null) {
                    paramList = new ArrayList<>();
                    for (Iterator it2 = params.iterator(); it2.hasNext();) {
                        HierarchicalConfiguration sub2 = (HierarchicalConfiguration) it2.next();
                        String fieldType = sub2.getString("[@type]");
                        String source = sub2.getString("[@source]", null);
                        String key = sub2.getString("[@key]");
                        String overrideMasterValue = sub2.getString("[@value]");
                        String defaultValue = sub2.getString("[@defaultValue]");
                        String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                        String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                        boolean addUrl = sub2.getBoolean("[@url]", false);
                        boolean dontUseTopstructValue = sub2.getBoolean("[@dontUseTopstructValue]", false);
                        paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                defaultValue, prefix, suffix, addUrl, dontUseTopstructValue));
                    }
                }
                ret.add(new Metadata(label, masterValue, type, paramList, group));
            }
        }
        return ret;
    }

    /**
     * Returns the list of configured metadata for search hit elements.
     * 
     * @param template
     * @return
     * @should return correct template configuration
     * @should return default template configuration if requested not found
     * @should return default template if template is null
     */
    @SuppressWarnings({ "rawtypes", })
    public List<Metadata> getSearchHitMetadataForTemplate(String template) {
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("metadata.searchHitMetadataList.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;

            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@name]").equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                    defaultTemplate = subElement;
                }
            }

            // If the requested template does not exist in the config, use _DEFAULT
            if (usingTemplate == null) {
                usingTemplate = defaultTemplate;
            }

        }

        return getMetadataForTemplate(usingTemplate);
    }

    /**
     * Returns the list of configured metadata for the main metadata page.
     * 
     * @param template
     * @return
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Metadata> getMainMetadataForTemplate(String template) {
        logger.trace("getMainMetadataForTemplate: {}", template);
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("metadata.mainMetadataList.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;

            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@name]").equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                    defaultTemplate = subElement;
                }
            }

            // If the requested template does not exist in the config, use _DEFAULT
            if (usingTemplate == null) {
                usingTemplate = defaultTemplate;
            }

        }

        return getMetadataForTemplate(usingTemplate);

    }

    /**
     * Returns the list of configured metadata for the sidebar.
     * 
     * @return
     * @should return correct template configuration
     * @should return empty list if template not found
     * @should return empty list if template is null
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Metadata> getSidebarMetadataForTemplate(String template) {
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("metadata.sideBarMetadataList.template");
        if (templateList != null) {
            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@name]").equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                }
            }
        }

        return getMetadataForTemplate(usingTemplate);
    }

    /**
     * 
     * @return
     * @should return correct template configuration
     * @should return default template configuration if template not found
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Metadata> getTocLabelConfiguration(String template) {
        //        List templateList = getLocalConfigurationsAt("toc.labelConfig");
        //        if (templateList != null) {
        //            for (Iterator it = templateList.iterator(); it.hasNext();) {
        //                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
        //                List<Metadata> metadata = getMetadataForTemplate(subElement);
        //                if (metadata != null && !metadata.isEmpty()) {
        //                    return metadata.get(0);
        //                }
        //            }
        //        }

        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("toc.labelConfig.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;

            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@name]").equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                    defaultTemplate = subElement;
                }
            }

            // If the requested template does not exist in the config, use _DEFAULT
            if (usingTemplate == null) {
                usingTemplate = defaultTemplate;
            }

        }

        return getMetadataForTemplate(usingTemplate);
    }

    /**
     * Returns number of elements displayed per paginator page in a table of contents for anchors and groups. Values below 1 disable pagination (all
     * elements are displayed on the single page).
     * 
     * @return
     * @should return correct value
     */
    public int getTocAnchorGroupElementsPerPage() {
        return getLocalInt("toc.tocAnchorGroupElementsPerPage", 0);
    }

    /**
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     * 
     * @param templateList
     * @param template
     * @return
     */
    @SuppressWarnings("rawtypes")
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration usingTemplate) {
        List<Metadata> ret = new ArrayList<>();

        if (usingTemplate != null) {
            //                logger.debug("template requested: " + template + ", using: " + usingTemplate.getString("[@name]"));
            List elements = usingTemplate.configurationsAt("metadata");
            if (elements != null) {
                for (Iterator it2 = elements.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration sub = (HierarchicalConfiguration) it2.next();
                    String label = sub.getString("[@label]");
                    String masterValue = sub.getString("[@value]");
                    boolean group = sub.getBoolean("[@group]", false);
                    int number = sub.getInt("[@number]", -1);
                    int type = sub.getInt("[@type]", 0);
                    List params = sub.configurationsAt("param");
                    List<MetadataParameter> paramList = null;
                    if (params != null) {
                        paramList = new ArrayList<>(params.size());
                        for (Iterator it3 = params.iterator(); it3.hasNext();) {
                            HierarchicalConfiguration sub2 = (HierarchicalConfiguration) it3.next();
                            String fieldType = sub2.getString("[@type]");
                            String source = sub2.getString("[@source]", null);
                            String key = sub2.getString("[@key]");
                            String overrideMasterValue = sub2.getString("[@value]");
                            String defaultValue = sub2.getString("[@defaultValue]");
                            String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                            String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                            boolean addUrl = sub2.getBoolean("[@url]", false);
                            boolean dontUseTopstructValue = sub2.getBoolean("[@dontUseTopstructValue]", false);
                            paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                    defaultValue, prefix, suffix, addUrl, dontUseTopstructValue));
                        }
                    }
                    ret.add(new Metadata(label, masterValue, type, paramList, group, number));
                }
            }
        }

        return ret;
    }

    /**
     * 
     * @param eleTemplate
     * @return
     */
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
                                paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                        defaultValue, prefix, suffix, addUrl, dontUseTopstructValue));
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
     * Returns the list of structure elements allowed to be shown in search results, collection listings, etc.
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getDocStructWhiteList() {
        return getLocalList("metadata.docStructWhiteList.docStruct");
    }

    /**
     * Returns the list of structure elements that are to be displayed as museum items (as opposed to library items), e.g. with event metadata etc.
     * 
     * @return
     * @should return all configured elements
     */
    @Deprecated
    public List<String> getMuseumDocstructTypes() {
        return getLocalList("metadata.museumDocstructTypes.docStruct");
    }

    /**
     * Returns the list of structure elements allowed to be shown in calendar view
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getCalendarDocStructTypes() {
        return getLocalList("metadata.calendarDocstructTypes.docStruct");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isBrowsingMenuEnabled() {
        return getLocalBoolean("metadata.browsingMenu.enabled", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getBrowsingMenuHitsPerPage() {
        return getLocalInt("metadata.browsingMenu.hitsPerPage", 50);
    }

    /**
     * Returns the list of index fields to be used for term browsing.
     * 
     * @return
     * @should return all configured elements
     */
    @SuppressWarnings("rawtypes")
    public List<BrowsingMenuFieldConfig> getBrowsingMenuFields() {
        List<BrowsingMenuFieldConfig> ret = new ArrayList<>();

        List fields = getLocalConfigurationsAt("metadata.browsingMenu.luceneField");
        if (fields != null) {
            for (Iterator it = fields.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                String field = sub.getString(".");
                String sortField = sub.getString("[@sortField]");
                String docstructFilterString = sub.getString("[@docstructFilters]");
                boolean recordsAndAnchorsOnly = sub.getBoolean("[@recordsAndAnchorsOnly]", false);
                BrowsingMenuFieldConfig bmf = new BrowsingMenuFieldConfig(field, sortField, docstructFilterString, recordsAndAnchorsOnly);
                ret.add(bmf);
            }
        }

        return ret;
    }

    /**
     * Returns the collection config block for the given field.
     *
     * @param field
     * @return
     */
    @SuppressWarnings("rawtypes")
    private HierarchicalConfiguration getCollectionConfiguration(String field) {
        List collectionList = getLocalConfigurationsAt("collections.collection");
        if (collectionList != null) {
            for (Iterator it = collectionList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString("[@field]").equals(field)) {
                    return subElement;

                }
            }
        }

        return null;
    }

    /**
     *
     * @param field
     * @return
     * @should return all configured elements
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
     * @param field
     * @return
     * @should return correct value
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
     * @param field
     * @return
     * @should return all configured elements
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
     * @param field
     * @param name
     * @return
     * @should return correct field for collection
     * @should give priority to exact matches
     * @should return hyphen if collection not found
     */
    @SuppressWarnings("rawtypes")
    public String getCollectionDefaultSortField(String field, String name) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return "-";
        }

        List fields = collection.configurationsAt("defaultSortFields.field");
        if (fields != null) {
            String exactMatch = null;
            String inheritedMatch = null;
            for (Iterator it = fields.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
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
        }

        return "-";
    }

    /**
     *
     * @param field
     * @return
     * @should return correct value
     */
    public int getCollectionDisplayNumberOfVolumesLevel(String field) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return 0;
        }
        return collection.getInt("displayNumberOfVolumesLevel", 0);
    }

    /**
     *
     * @param field
     * @return
     * @should return correct value
     * @should return -1 if no collection config was found
     */
    public int getCollectionDisplayDepthForSearch(String field) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return -1;
        }
        return collection.getInt("displayDepthForSearch", -1);
    }

    /**
     *
     * @param field
     * @return
     * @should return all configured elements
     */
    public List<String> getSelectedRecords(String field) {
        HierarchicalConfiguration collection = getCollectionConfiguration(field);
        if (collection == null) {
            return null;
        }
        return getLocalList(collection, null, "selectedRecords.pi", Collections.<String> emptyList());
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSolrUrl() {
        String value = getLocalString("urls.solr", "http://localhost:80889/solr");
        if (value.charAt(value.length() - 1) == '/') {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentServerWrapperUrl() {
        return getLocalString("urls.contentServerWrapper");
    }

    /**
     *
     * @return
     * @should return correct value
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
     *
     * @return
     * @should return correct value
     */
    public String getContentRestApiUrl() {
        return getRestApiUrl() + "content/";

    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getIiifUrl() {
        return getRestApiUrl();
    }

    /**
     * @return
     */
    public String getRestApiUrl() {
        String urlString = getLocalString("urls.rest",
                getLocalString("urls.contentRestApi", getLocalString("urls.iiif", "http://localhost:8080/viewer/rest/").replace("/iiif", ""))
                        .replace("/content", ""));
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }

        return urlString;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getContentServerRealUrl() {
        return getLocalString("urls.contentServer");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getMetsUrl() {
        return getLocalString("urls.metadata.mets");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getMarcUrl() {
        return getLocalString("urls.metadata.marc");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getDcUrl() {
        return getLocalString("urls.metadata.dc");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getEseUrl() {
        return getLocalString("urls.metadata.ese");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSearchHitsPerPage() {
        return getLocalInt("search.hitsPerPage", 10);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getFulltextFragmentLength() {
        return getLocalInt("search.fulltextFragmentLength", 200);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isAdvancedSearchEnabled() {
        return getLocalBoolean("search.advanced.enabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getAdvancedSearchDefaultItemNumber() {
        return getLocalInt("search.advanced.defaultItemNumber", 2);
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<String> getAdvancedSearchFields() {
        return getLocalList("search.advanced.searchFields.field");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isAggregateHits() {
        return getLocalBoolean("search.aggregateHits", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayAdditionalMetadataEnabled() {
        return getLocalBoolean("search.displayAdditionalMetadata.enabled", true);
    }

    /**
     * 
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataIgnoreFields() {
        return getLocalList("search.displayAdditionalMetadata.ignoreField", Collections.emptyList());
    }

    /**
     * 
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataTranslateFields() {
        return getLocalList("search.displayAdditionalMetadata.translateField", Collections.emptyList());
    }

    /**
     * 
     * @param field
     * @return
     * @should return correct value
     */
    @SuppressWarnings("rawtypes")
    public boolean isAdvancedSearchFieldHierarchical(String field) {
        List fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList != null) {

            for (Iterator it = fieldList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString(".").equals(field)) {
                    return subElement.getBoolean("[@hierarchical]", false);
                }
            }
        }

        return false;
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isTimelineSearchEnabled() {
        return getLocalBoolean("search.timeline.enabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isCalendarSearchEnabled() {
        return getLocalBoolean("search.calendar.enabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getStaticQuerySuffix() {
        return getLocalString("search.staticQuerySuffix");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getPreviousVersionIdentifierField() {
        return getLocalString("search.versioning.previousVersionIdentifierField");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getNextVersionIdentifierField() {
        return getLocalString("search.versioning.nextVersionIdentifierField");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getVersionLabelField() {
        return getLocalString("search.versioning.versionLabelField");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getIndexedMetsFolder() {
        return getLocalString("indexedMetsFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getIndexedLidoFolder() {
        return getLocalString("indexedLidoFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSplittingCharacter() {
        return getLocalString("viewer.splittingCharacter");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getMediaFolder() {
        return getLocalString("mediaFolder");
    }

    public String getVocabulariesFolder() {
        return getLocalString("vocabularies", "vocabularies");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getOrigContentFolder() {
        return getLocalString("origContentFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getOverviewFolder() {
        return getLocalString("overviewFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getAltoFolder() {
        return getLocalString("altoFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getAbbyyFolder() {
        return getLocalString("abbyyFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getFulltextFolder() {
        return getLocalString("fulltextFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTeiFolder() {
        return getLocalString("teiFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getCmdiFolder() {
        return getLocalString("cmdiFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getWcFolder() {
        return getLocalString("wcFolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getHotfolder() {
        return getLocalString("hotfolder");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    @SuppressWarnings("static-method")
    public String getTempFolder() {
        return System.getProperty("java.io.tmpdir") + "viewer";
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isUrnDoRedirect() {
        return getLocalBoolean("urnresolver.doRedirectInsteadofForward", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isUserRegistrationEnabled() {
        return getLocalBoolean("user.userRegistrationEnabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isShowOpenIdConnect() {
        return getLocalBoolean("user.openIdConnect[@show]", true);
    }

    /**
     * 
     * @return
     * @should return all properly configured elements
     */
    public List<OpenIdProvider> getOpenIdConnectProviders() {
        XMLConfiguration myConfigToUse = config;
        // User local config, if available
        if (configLocal.getString("user.openIdConnect[@show]") != null) {
            myConfigToUse = configLocal;
        }

        List<OpenIdProvider> providers = new ArrayList<>();
        int max = myConfigToUse.getMaxIndex("user.openIdConnect.provider");
        for (int i = 0; i <= max; i++) {
            String name = myConfigToUse.getString("user.openIdConnect.provider(" + i + ")[@name]");
            String endpoint = myConfigToUse.getString("user.openIdConnect.provider(" + i + ")[@endpoint]", null);
            String image = myConfigToUse.getString("user.openIdConnect.provider(" + i + ")[@image]", null);
            boolean useTextField = myConfigToUse.getBoolean("user.openIdConnect.provider(" + i + ")[@useTextField]", false);
            String clientId = myConfigToUse.getString("user.openIdConnect.provider(" + i + ")[@clientId]", null);
            String clientSecret = myConfigToUse.getString("user.openIdConnect.provider(" + i + ")[@clientSecret]", null);
            if (StringUtils.isNotEmpty(clientId) && StringUtils.isNotEmpty(clientId)) {
                providers.add(new OpenIdProvider(name, endpoint, image, useTextField, clientId, clientSecret));
            } else {
                logger.warn("OpenID Connect provider config incomplete: {}", name);
            }
        }
        return providers;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpServer() {
        return getLocalString("user.smtpServer");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpUser() {
        return getLocalString("user.smtpUser");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpPassword() {
        return getLocalString("user.smtpPassword");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpSenderAddress() {
        return getLocalString("user.smtpSenderAddress");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpSenderName() {
        return getLocalString("user.smtpSenderName");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSmtpSecurity() {
        return getLocalString("user.smtpSecurity", "none");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayCollectionBrowsing() {
        return this.getLocalBoolean("webGuiDisplay.collectionBrowsing", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayUserNavigation() {
        return this.getLocalBoolean("webGuiDisplay.userAccountNavigation", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayTagCloudNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudNavigation", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayStatistics() {
        return this.getLocalBoolean("webGuiDisplay.displayStatistics", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isDisplayTimeMatrix() {
        return this.getLocalBoolean("webGuiDisplay.displayTimeMatrix", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTheme() {
        return getSubthemeMainTheme();
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getThemeRootPath() {
        return getLocalString("viewer.theme.rootPath");
    }

    /**
     * TagCloud auf der Startseite anzeigen lassen
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayTagCloudStartpage() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudStartpage", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplaySearchResultNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displaySearchResultNavigation", true);
    }

    public boolean isFoldout(String sidebarElement) {
        return getLocalBoolean("sidebar." + sidebarElement + ".foldout", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarOverviewLinkVisible() {
        return getLocalBoolean("sidebar.overview.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSidebarOverviewLinkCondition() {
        return getLocalString("sidebar.overview.condition");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarPageLinkVisible() {
        return getLocalBoolean("sidebar.page.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarCalendarLinkVisible() {
        return getLocalBoolean("sidebar.calendar.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarTocLinkVisible() {
        return getLocalBoolean("sidebar.toc.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarThumbsLinkVisible() {
        return getLocalBoolean("sidebar.thumbs.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarMetadataLinkVisible() {
        return getLocalBoolean("sidebar.metadata.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isShowSidebarEventMetadata() {
        return getLocalBoolean("sidebar.metadata.showEventMetadata", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarFulltextLinkVisible() {
        return getLocalBoolean("sidebar.fulltext.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarDfgLinkVisible() {
        return getLocalBoolean("sidebar.dfg.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarOpacLinkVisible() {
        return getLocalBoolean("sidebar.opac.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarTocVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc.visible", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean getSidebarTocPageNumbersVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc.pageNumbersVisible", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSidebarTocLengthBeforeCut() {
        return this.getLocalInt("sidebar.sidebarToc.lengthBeforeCut", 10);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSidebarTocInitialCollapseLevel() {
        return this.getLocalInt("sidebar.sidebarToc.initialCollapseLevel", 2);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSidebarTocCollapseLengthThreshold() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold", 10);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSidebarTocLowestLevelToCollapseForLength() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold[@lowestLevelToTest]", 2);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSidebarTocTreeView() {
        return getLocalBoolean("sidebar.sidebarToc.useTreeView", true);
    }

    /**
     * 
     * @return
     * @should return true for allowed docstructs
     * @should return false for other docstructs
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

        logger.trace("Tree view for {} not allowed", docStructType);
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
     * 
     * @return
     * @should return all values
     */
    public List<String> getDrillDownFields() {
        return getLocalList("search.drillDown.field");
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<String> getHierarchicalDrillDownFields() {
        return getLocalList("search.drillDown.hierarchicalField");
    }

    /**
     * 
     * @return
     * @should return correct value
     * @should return default value if field not found
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
     * @param field
     * @return
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
     * @param field
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
     * 
     * @return
     * @should return correct value
     */
    public boolean isSortingEnabled() {
        return getLocalBoolean("search.sorting.enabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getDefaultSortField() {
        return getLocalString("search.sorting.defaultSortField", null);
    }

    /**
     * 
     * @return
     * @should return return all configured elements
     */
    public List<String> getSortFields() {
        return getLocalList("search.sorting.luceneField");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getUrnResolverUrl() {
        return getLocalString("urls.urnResolver",
                new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resolver?urn=").toString());
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getUnconditionalImageAccessMaxWidth() {
        return getLocalInt("accessConditions.unconditionalImageAccessMaxWidth", 120);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isFullAccessForLocalhost() {
        return getLocalBoolean("accessConditions.fullAccessForLocalhost", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isPdfApiDisabled() {
        return getLocalBoolean("pdf.pdfApiDisabled", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isTitlePdfEnabled() {
        return getLocalBoolean("pdf.titlePdfEnabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isTocPdfEnabled() {
        return getLocalBoolean("pdf.tocPdfEnabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isMetadataPdfEnabled() {
        return getLocalBoolean("pdf.metadataPdfEnabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isPagePdfEnabled() {
        return getLocalBoolean("pdf.pagePdfEnabled", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDocHierarchyPdfEnabled() {
        return getLocalBoolean("pdf.docHierarchyPdfEnabled", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getRssFeedItems() {
        return getLocalInt("rss.numberOfItems", 50);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getRssTitle() {
        return getLocalString("rss.title", "viewer-rss");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getRssDescription() {
        return getLocalString("rss.description", "latest imports");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getRssCopyrightText() {
        return getLocalString("rss.copyright");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getRulesetFilePath() {
        return getLocalString("content.ruleset");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getDefaultCollection() {
        return getLocalString("content.defaultCollection");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getThumbnailsWidth() {
        return getLocalInt("viewer.thumbnailsWidth", 100);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getThumbnailsHeight() {
        return getLocalInt("viewer.thumbnailsHeight", 120);
    }

    public int getThumbnailsCompression() {
        return getLocalInt("viewer.thumbnailsCompression", 85);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getAnchorThumbnailMode() {
        return getLocalString("viewer.anchorThumbnailMode", "GENERIC");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getMultivolumeThumbnailWidth() {
        return getLocalInt("toc.multiVolumeThumbnailsWidth", 50);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getMultivolumeThumbnailHeight() {
        return getLocalInt("toc.multiVolumeThumbnailsHeight", 60);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean getDisplayBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayBreadcrumbs", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean getDisplayMetadataPageLinkBlock() {
        return this.getLocalBoolean("webGuiDisplay.displayMetadataPageLinkBlock", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isAddDublinCoreMetaTags() {
        return getLocalBoolean("metadata.addDublinCoreMetaTags", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isAddHighwirePressMetaTags() {
        return getLocalBoolean("metadata.addHighwirePressMetaTags", false);
    }

    /**
     * 
     * @return
     * @throws ConfigurationException
     * @should return correct value
     */
    public boolean useTiles() throws ConfigurationException {
        return useTiles(PageType.viewImage, null);
    }

    /**
     * 
     * @return
     * @throws ConfigurationException
     * @should return correct value
     */
    public boolean useTilesFullscreen() throws ConfigurationException {
        return useTiles(PageType.viewFullscreen, null);
    }

    public boolean useTiles(PageType view, ImageType image) throws ConfigurationException {
        return getZoomImageViewConfig(view, image).getBoolean("[@tileImage]", false);
    }

    public int getFooterHeight() throws ConfigurationException {
        return getFooterHeight(PageType.viewImage, null);
    }

    public int getFullscreenFooterHeight() throws ConfigurationException {
        return getFooterHeight(PageType.viewFullscreen, null);
    }

    public int getFooterHeight(PageType view, ImageType image) throws ConfigurationException {
        return getZoomImageViewConfig(view, image).getInt("[@footerHeight]", 50);
    }

    public String getImageViewType() throws ConfigurationException {
        return getZoomImageViewType(PageType.viewImage, null);
    }

    public String getZoomFullscreenViewType() throws ConfigurationException {
        return getZoomImageViewType(PageType.viewFullscreen, null);
    }

    public String getZoomImageViewType(PageType view, ImageType image) throws ConfigurationException {
        return getZoomImageViewConfig(view, image).getString("[@type]");
    }

    public boolean useOpenLayers() throws ConfigurationException {
        return "openlayers".equalsIgnoreCase(getImageViewType());
    }

    public boolean useOpenLayersFullscreen() throws ConfigurationException {
        return "openlayers".equalsIgnoreCase(getZoomFullscreenViewType());
    }

    public boolean useOpenSeadragon() throws ConfigurationException {
        return "openseadragon".equalsIgnoreCase(getImageViewType());
    }

    public List<String> getImageViewZoomScales() throws ConfigurationException {
        return getImageViewZoomScales(PageType.viewImage, null);
    }

    public List<String> getImageViewZoomScales(String view) throws ConfigurationException {
        return getImageViewZoomScales(PageType.valueOf(view), null);
    }

    public List<String> getImageViewZoomScales(PageType view, ImageType image) throws ConfigurationException {
        List<String> defaultList = new ArrayList<>();
        //        defaultList.add("600");
        //        defaultList.add("900");
        //        defaultList.add("1500");
        return Arrays.asList(getZoomImageViewConfig(view, image).getStringArray("scale"));
    }

    /**
     * 
     * @return the configured tile sizes for imageView as a hashmap linking each tile size to the list of resolutions to use with that size
     * @throws ConfigurationException
     */
    public Map<Integer, List<Integer>> getTileSizes() throws ConfigurationException {
        return getTileSizes(PageType.viewImage, null);
    }

    public Map<Integer, List<Integer>> getTileSizes(PageType view, ImageType image) throws ConfigurationException {
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

    public SubnodeConfiguration getZoomImageViewConfig(PageType pageType, ImageType imageType) throws ConfigurationException {
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
        throw new ConfigurationException("Viewer config must define at least a generic <zoomImageView>");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    @Deprecated
    public Integer getImageViewZoomScale() {
        return getLocalInt("viewer.zoomImageView[@zoomScale]", 3);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    @Deprecated
    public Integer getFullscreenZoomScale() {
        return getLocalInt("viewer.zoomFullscreenView[@zoomScale]", 3);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getBreadcrumbsClipping() {
        return getLocalInt("webGuiDisplay.breadcrumbsClipping", 50);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayTopstructLabel() {
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayTopstructLabel", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean getDisplayStructType() {
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayStructType", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisableMenuBrowsingOnSearchList() {
        return getLocalBoolean("webGuiDisplay.disableMenuBrowsingOnSearchList", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSearchHitMetadataValueNumber() {
        return getLocalInt("metadata.searchHitMetadataList.valueNumber", 1);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getSearchHitMetadataValueLength() {
        return getLocalInt("metadata.searchHitMetadataList.valueLength", 0);
    }

    /**
     * Returns the preference order of data to be used as an image footer text.
     * 
     * @return
     * @should return all configured elements in the correct order
     */
    public List<String> getWatermarkTextConfiguration() {
        List<String> list = getLocalList("viewer.watermarkTextConfiguration.text");
        return list;
    }

    public String getWatermarkFormat() {
        return getLocalString("viewer.watermarkFormat", "jpg");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isOriginalContentDownload() {
        return getLocalBoolean("content.originalContentDownload", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getStopwordsFilePath() {
        return getLocalString("stopwordsFile");
    }

    /**
     * Returns the locally configured page type name for URLs (e.g. "bild" instead of default "image").
     * 
     * @param type
     * @return
     * @should return the correct value for the given type
     * @should return null for non configured type
     */
    public String getPageType(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name());
    }

    /**
     * 
     * @param docstruct
     * @return
     * @should return correct value
     * @should return null if docstruct not found
     */
    public String getDocstructTargetPageType(String docstruct) {
        return getLocalString("viewer.docstructTargetPageTypes." + docstruct);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getFulltextPercentageWarningThreshold() {
        return getLocalInt("viewer.fulltextPercentageWarningThreshold", 30);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isUseViewerLocaleAsRecordLanguage() {
        return getLocalBoolean("viewer.useViewerLocaleAsRecordLanguage", false);
    }

    /**
     * 
     * @return
     */
    @Deprecated
    public boolean isTocAlwaysDisplayDocstruct() {
        return getLocalBoolean("toc.alwaysDisplayDocstruct", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getFeedbackEmailAddress() {
        return getLocalString("user.feedbackEmailAddress");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isBookshelvesEnabled() {
        return getLocalBoolean("bookshelves.bookshelvesEnabled", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isForceJpegConversion() {
        return getLocalBoolean("viewer.forceJpegConversion", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getPageLoaderThreshold() {
        return getLocalInt("performance.pageLoaderThreshold", 1000);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isPreventProxyCaching() {
        return getLocalBoolean(("performance.preventProxyCaching"), false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isUserCommentsEnabled() {
        return getLocalBoolean(("userComments.enabled"), false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getUserCommentsConditionalQuery() {
        return getLocalString("userComments.conditionalQuery");
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getUserCommentsNotificationEmailAddresses() {
        return getLocalList("userComments.notificationEmailAddress");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getViewerHome() {
        return getLocalString("viewerHome");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    @Deprecated
    public String getDataRepositoriesHome() {
        return getLocalString("dataRepositoriesHome", "");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getWatermarkIdField() {
        return getLocalString("viewer.watermarkIdField", SolrConstants.DC);

    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSubthemesEnabled() {
        return getLocalBoolean("viewer.theme[@subTheme]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSubthemeMainTheme() {
        String theme = getLocalString("viewer.theme[@mainTheme]");
        if (StringUtils.isEmpty(theme)) {
            logger.error("Theme name could not be read - config_viewer.xml may not be well-formed.");
        }
        return getLocalString("viewer.theme[@mainTheme]");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSubthemeDiscriminatorField() {
        return getLocalString("viewer.theme[@discriminatorField]");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSubthemeAutoSwitch() {
        return getLocalBoolean("viewer.theme[@autoSwitch]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSubthemeAddFilterQuery() {
        return getLocalBoolean("viewer.theme[@addFilterQuery]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isSubthemeFilterQueryVisible() {
        return getLocalBoolean("viewer.theme[@filterQueryVisible]", false);
    }

    //    /**
    //     * 
    //     * @return
    //     * @should return all configured elements
    //     */
    //    @SuppressWarnings("rawtypes")
    //    public Map<String, String> getSubthemeMap() {
    //        Map<String, String> ret = new HashMap<>();
    //
    //        List subThemes = getLocalConfigurationsAt("viewer.theme.subTheme");
    //
    //        if (subThemes != null) {
    //            for (Iterator it = subThemes.iterator(); it.hasNext();) {
    //                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
    //                String key = sub.getString("[@discriminatorValue]");
    //                String value = sub.getString("[@themeFolder]");
    //                ret.put(key, value);
    //            }
    //        }
    //
    //        return ret;
    //    }

    /**
     * 
     * @return
     * @should return correct value for existing fields
     * @should return INT_MAX for other fields
     */
    public int getTagCloudSampleSize(String fieldName) {
        return getLocalInt("tagclouds.sampleSizes." + fieldName, Integer.MAX_VALUE);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isUseExternalCS() {
        return getLocalBoolean("urls.contentServer[@external]", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getPreviewHeightPercentage() {
        return getLocalInt("viewer.previewImageView[@previewHeightPercentage]", 10);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getPreviewWidth() {
        return getLocalInt("viewer.previewImageView[@previewWidth]", 600);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getPreviewThumbnailWidth() {
        return getLocalInt("viewer.previewImageView[@thumbnailWidth]", 200);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getPreviewThumbnailHeight() {
        return getLocalInt("viewer.previewImageView[@thumbnailHeight]", 240);
    }

    /**
     * 
     * @return
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template configuration if template is null
     */
    @SuppressWarnings("rawtypes")
    public List<StringPair> getTocVolumeSortFieldsForTemplate(String template) {
        List<StringPair> ret = new ArrayList<>();

        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;
            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
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
        }

        if (usingTemplate != null) {
            List fields = usingTemplate.configurationsAt("field");
            if (fields != null) {
                for (Iterator it2 = fields.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration sub = (HierarchicalConfiguration) it2.next();
                    String field = sub.getString(".");
                    String order = sub.getString("[@order]");
                    ret.add(new StringPair(field, "desc".equals(order) ? "desc" : "asc"));
                }
            }
        }

        return ret;
    }

    /**
     * Returns the grouping Solr field for the given annchor TOC sort configuration.
     * 
     * @return
     * @should return correct value
     */
    @SuppressWarnings("rawtypes")
    public String getTocVolumeGroupFieldForTemplate(String template) {
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;
            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
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
        }

        if (usingTemplate != null) {
            String groupBy = usingTemplate.getString("[@groupBy]");
            if (StringUtils.isNotEmpty(groupBy)) {
                return groupBy;
            }
        }

        return null;
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean getDisplayTitleBreadcrumbs() {
        return getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isDisplayTitlePURL() {
        return this.getLocalBoolean("webGuiDisplay.displayTitlePURL", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return this.getLocalInt("webGuiDisplay.displayTitleBreadcrumbs[@maxTitleLength]", 25);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean getIncludeAnchorInTitleBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs[@includeAnchor]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplaySearchRssLinks() {
        return getLocalBoolean("rss.displaySearchRssLinks", true);
    }

    /**
     * @return
     * @should return correct value
     */
    @Deprecated
    public boolean getDisplayBibdataBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayBibdataBreadcrumbs", false);
    }

    /**
     * @return
     * @should return correct value
     */
    @Deprecated
    public int getBibdataBreadcrumbsMaxTitleLength() {
        return this.getLocalInt("webGuiDisplay.displayBibdataBreadcrumbs[@maxTitleLength]", 0);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean showThumbnailsInToc() {
        return this.getLocalBoolean("toc.multiVolumeThumbnailsEnabled", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public String getStartYearForTimeline() {
        return this.getLocalString("search.timeline.startyear", "1750");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getEndYearForTimeline() {
        return this.getLocalString("search.timeline.endyear", "2014");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getTimelineHits() {
        return this.getLocalString("search.timeline.hits", "108");
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isPiwikTrackingEnabled() {
        return getLocalBoolean("piwik.enabled", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public String getPiwikBaseURL() {
        return this.getLocalString("piwik.baseURL", "");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getPiwikSiteID() {
        return this.getLocalString("piwik.siteID", "1");

    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSearchSavingEnabled() {
        return getLocalBoolean("search.searchSavingEnabled", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isBoostTopLevelDocstructs() {
        return getLocalBoolean("search.boostTopLevelDocstructs", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isGroupDuplicateHits() {
        return getLocalBoolean("search.groupDuplicateHits", true);
    }

    /**
     * @return
     * @should return all configured values
     */
    public List<String> getRecordGroupIdentifierFields() {
        return getLocalList("toc.recordGroupIdentifierFields.field");
    }

    /**
     * @return
     * @should return all configured values
     */
    public List<String> getAncestorIdentifierFields() {
        return getLocalList("toc.ancestorIdentifierFields.field");
    }

    /**
     * @return
     * @should return correctValue
     */
    public boolean isTocListSiblingRecords() {
        return getLocalBoolean("toc.ancestorIdentifierFields[@listSiblingRecords]", false);
    }

    /**
     * @return
     * @should return all configured elements
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
     * @return
     * @should return all configured elements
     */
    @SuppressWarnings("rawtypes")
    public List<Map<String, String>> getWebApiFields() {
        List<Map<String, String>> ret = new ArrayList<>();

        List elements = getLocalConfigurationsAt("webapi.fields.field");
        if (elements != null) {
            for (Iterator it = elements.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                Map<String, String> fieldConfig = new HashMap<>();
                fieldConfig.put("jsonField", sub.getString("[@jsonField]", null));
                fieldConfig.put("luceneField", sub.getString("[@luceneField]", null));
                fieldConfig.put("multivalue", sub.getString("[@multivalue]", null));
                ret.add(fieldConfig);
            }
        }

        return ret;
    }

    /**
     * @return
     * @should return correct value
     */
    public String getDbPersistenceUnit() {
        return getLocalString("dbPersistenceUnit", null);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isCmsEnabled() {
        return getLocalBoolean("cms.enabled", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean useCustomNavBar() {
        return getLocalBoolean("cms.useCustomNavBar", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public String getCmsTemplateFolder() {
        return getLocalString("cms.templateFolder", "resources/cms/templates/");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getCmsMediaFolder() {
        return getLocalString("cms.mediaFolder", "cms_media");
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getCmsClassifications() {
        return getLocalList("cms.classifications.classification");
    }

    public int getCmsMediaDisplayWidth() {
        return getLocalInt("cms.mediaDisplayWidth", 0);
    }

    public int getCmsMediaDisplayHeight() {
        return getLocalInt("cms.mediaDisplayHeight", 0);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isTranskribusEnabled() {
        return getLocalBoolean("transkribus[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTranskribusUserName() {
        return getLocalString("transkribus.userName");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTranskribusPassword() {
        return getLocalString("transkribus.password");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTranskribusDefaultCollection() {
        return getLocalString("transkribus.defaultCollection");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTranskribusRestApiUrl() {
        return getLocalString("transkribus.restApiUrl", TranskribusUtils.TRANSRIBUS_REST_URL);
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<String> getTranskribusAllowedDocumentTypes() {
        return getLocalList("transkribus.allowedDocumentTypes.docstruct");
    }

    /**
     * @return
     * @should return correct value
     */
    public int getTocIndentation() {
        return getLocalInt("toc.tocIndentation", 20);
    }

    /**
     * @return
     * @should return correct value
     */
    public int getSidebarTocIndentation() {
        return getLocalInt("sidebar.sidebarToc.tocIndentation", 10);
    }

    public boolean isPageBrowseEnabled() {
        return getLocalBoolean("viewer.pageBrowse.enabled", false);
    }

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
     * @return
     * @should return correct value
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
     * @return
     * @should return correct value
     */
    public String getTaskManagerUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getTaskManagerServiceUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "service";
    }

    /**
     * @return
     * @should return correct value
     */
    public String getTaskManagerRestUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "rest";
    }

    /**
     * @return
     * @should return correct value
     */
    public String getReCaptchaSiteKey() {
        return getLocalString("reCaptcha.provider[@siteKey]");
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isUseReCaptcha() {
        return getLocalBoolean("reCaptcha[@show]", true);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isTocEpubEnabled() {
        return getLocalBoolean("epub.tocEpubEnabled", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isMetadataEpubEnabled() {
        return getLocalBoolean("epub.metadataEpubEnabled", false);
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isTitleEpubEnabled() {
        return getLocalBoolean("epub.titleEpubEnabled", false);
    }

    public boolean isGeneratePdfInTaskManager() {
        return getLocalBoolean("pdf.externalPdfGeneration", false);
    }

    /**
     * @should return true if the search field to search the current item/work is configured to be visible
     */
    public boolean isSearchInItemEnabled() {
        return getLocalBoolean("sidebar.searchInItem.visible", true);
    }

    public String getDefaultBrowseIcon(String field) {
        HierarchicalConfiguration subConfig = getCollectionConfiguration(field);
        if (subConfig != null) {
            return subConfig.getString("defaultBrowseIcon", "");
        }

        return getLocalString("collection.defaultBrowseIcon", getLocalString("collections.defaultBrowseIcon", ""));
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSearchExcelExportEnabled() {
        return getLocalBoolean("search.export.excel.enabled", false);
    }

    /**
     * 
     * @return
     * @should return all values
     */
    public List<String> getSearchExcelExportFields() {
        return getLocalList("search.export.excel.field", new ArrayList<String>(0));
    }

    /**
     * @return
     */
    public int getExcelDownloadTimeout() {
        return getLocalInt("search.export.excel.timeout", 120);
    }

    /**
     * @return
     */
    public boolean isDisplayEmptyTocInSidebar() {
        return getLocalBoolean("sidebar.sidebarToc.visibleIfEmpty", true);
    }

    /**
     * @return
     */
    public boolean isDoublePageModeEnabled() {
        return getLocalBoolean("viewer.doublePageMode.enabled", false);
    }

    public List<String> getRestrictedImageUrls() {
        return getLocalList("viewer.externalContent.restrictedUrls.url", Collections.emptyList());
    }

    public List<String> getIIIFMetadataFields() {
        return getLocalList("webapi.iiif.metadataFields.field", Collections.emptyList());
    }

    public String getIIIFLogo() {
        return getLocalString("webapi.iiif.logo", null);
    }

    /**
     * @return
     */
    public String getIIIFNavDateField() {
        return getLocalString("webapi.iiif.navDateField", null);
    }

    public String getIIIFAttribution() {
        return getLocalString("webapi.iiif.attribution", "provided by Goobi viewer");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getSitelinksField() {
        return getLocalString("sitemap.sitelinksField");
    }

    /**
     * @return
     * @should return correct value
     */
    public String getSitelinksFilterQuery() {
        return getLocalString("sitemap.sitelinksFilterQuery");
    }

    /**
     * 
     */
    public List<String> getConfiguredCollections() {
        return getLocalList("collections.collection[@field]", Collections.EMPTY_LIST);

    }
}
