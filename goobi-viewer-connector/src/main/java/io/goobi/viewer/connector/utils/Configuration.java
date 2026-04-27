/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Namespace;

import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.oai.model.metadata.Metadata;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.controller.StringConstants;

/**
 * <p>
 * Configuration class.
 * </p>
 *
 */
public final class Configuration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    /** Constant <code>DEFAULT_CONFIG_FILE="config_oai.xml"</code> */
    public static final String DEFAULT_CONFIG_FILE = "config_oai.xml";

    private static final String XML_PATH_ATTRIBUTE_NAME = "[@name]";

    protected ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder;
    protected ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builderLocal;

    /**
     * <p>
     * Constructor for Configuration.
     * </p>
     *
     * @param configPath a {@link java.lang.String} object.
     */
    public Configuration(String configPath) {
        // Load default configuration
        builder =
                new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setBasePath(Configuration.class.getClassLoader().getResource("").getFile())
                                .setFileName(configPath)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setThrowExceptionOnMissing(false));
        if (builder.getFileHandler().getFile().exists()) {
            try {
                builder.getConfiguration();
                logger.info("Default Connector configuration file '{}' loaded.", builder.getFileHandler().getFile().getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            builder.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    event -> builder.getReloadingController().checkForReloading(null));
        } else {
            logger.error("Default Connector configuration file not found: {}; Base path is {}",
                    builder.getFileHandler().getFile().getAbsoluteFile(),
                    builder.getFileHandler().getBasePath());
        }

        // Load local config file
        File fileLocal = new File(getViewerConfigFolder() + DEFAULT_CONFIG_FILE);
        builderLocal =
                new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(fileLocal.getAbsolutePath())
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setThrowExceptionOnMissing(false));
        if (builderLocal.getFileHandler().getFile().exists()) {
            try {
                builderLocal.getConfiguration();
                logger.info("Local Connector configuration file '{}' loaded.", fileLocal.getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error("{} ({})", e.getMessage(), fileLocal.getAbsolutePath(), e);
            }
            builderLocal.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    event -> builderLocal.getReloadingController().checkForReloading(null));
        }
    }

    /**
     * 
     * @return {@link XMLConfiguration} that is synced with the current state of the config file
     */
    protected XMLConfiguration getConfig() {
        try {
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }

        return new XMLConfiguration();
    }

    /**
     * 
     * @return {@link XMLConfiguration} that is synced with the current state of the config file
     */
    protected XMLConfiguration getConfigLocal() {
        if (builderLocal != null) {
            try {
                return builderLocal.getConfiguration();
            } catch (ConfigurationException e) {
                // logger.error(e.getMessage()); //NOSONAR Debug
            }
        }

        return new XMLConfiguration();
    }

    /**
     * ns is needed as parameter for every XML element, otherwise the standard-ns is printed out in every element the standard return String.
     * http://www.openarchives.org/OAI/2.0/
     *
     * @return the Standard Namespace for the xml response
     */
    public Namespace getStandardNameSpace() {
        String namespace = getOaiIdentifier().get("xmlns");
        return Namespace.getNamespace(namespace);
    }

    /**
     * 
     * @param inPath
     * @return List<HierarchicalConfiguration<ImmutableNode>>
     */
    private List<HierarchicalConfiguration<ImmutableNode>> getLocalConfigurationsAt(String inPath) {
        List<HierarchicalConfiguration<ImmutableNode>> ret = getConfigLocal().configurationsAt(inPath);
        if (ret == null || ret.isEmpty()) {
            ret = getConfig().configurationsAt(inPath);
        }

        return ret;
    }

    /**
     * 
     * @param inPath
     * @param defaultList
     * @return List<String>
     */
    private List<String> getLocalList(String inPath, List<String> defaultList) {
        return getLocalList(getConfigLocal(), getConfig(), inPath, defaultList);
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return a boolean
     */
    private boolean getLocalBoolean(String inPath, boolean inDefault) {
        return getConfigLocal().getBoolean(inPath, getConfig().getBoolean(inPath, inDefault));

    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return an int
     */
    private int getLocalInt(String inPath, int inDefault) {
        try {
            return getConfigLocal().getInt(inPath, getConfig().getInt(inPath, inDefault));
        } catch (ConversionException e) {
            logger.error("{}. Using default value {} instead.", e.getMessage(), inDefault);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return {@link String}
     */
    private String getLocalString(String inPath, String inDefault) {
        return getConfigLocal().getString(inPath, getConfig().getString(inPath, inDefault));
    }

    /**
     * 
     * @param config Preferred configuration
     * @param altConfig Alternative configuration
     * @param inPath XML path
     * @param defaultList List of default values to return if none found in config
     * @return List<String>
     */
    private static List<String> getLocalList(HierarchicalConfiguration<ImmutableNode> config, HierarchicalConfiguration<ImmutableNode> altConfig,
            String inPath,
            List<String> defaultList) {
        if (config == null) {
            throw new IllegalArgumentException("config may not be null");
        }
        List<Object> objects = config.getList(inPath, altConfig == null ? null : altConfig.getList(inPath, defaultList));
        if (objects != null && !objects.isEmpty()) {
            List<String> ret = new ArrayList<>(objects.size());
            for (Object obj : objects) {
                ret.add((String) obj);
            }
            return ret;
        }

        return new ArrayList<>(0);
    }

    /**
     * This method returns a HashMap with information for the OAI header and identify verb.
     *
     * @return {@link java.util.HashMap}
     */
    public Map<String, String> getIdentifyTags() {
        Map<String, String> identifyTags = new HashMap<>();

        identifyTags.put("repositoryName", getLocalString("identifyTags.repositoryName", null));
        identifyTags.put("baseURL", getLocalString("identifyTags.baseURL", null));
        identifyTags.put("protocolVersion", getLocalString("identifyTags.protocolVersion", null));
        identifyTags.put("adminEmail", getLocalString("identifyTags.adminEmail", null));
        identifyTags.put("deletedRecord", getLocalString("identifyTags.deletedRecord", null));
        identifyTags.put("granularity", getLocalString("identifyTags.granularity", null));
        identifyTags.put("description", getLocalString("identifyTags.description", null));

        return identifyTags;
    }

    /**
     * This method generates a HashMap with information for OAI header.
     *
     * @return {@link java.util.HashMap}
     * @should read config values correctly
     */
    public Map<String, String> getOaiIdentifier() {
        Map<String, String> oaiIdentifier = new HashMap<>();
        oaiIdentifier.put("xmlns", getLocalString("oai-identifier.namespace", "http://www.openarchives.org/OAI/2.0/"));
        oaiIdentifier.put("repositoryIdentifier", getLocalString("oai-identifier.repositoryIdentifier", null));

        return oaiIdentifier;
    }

    /**
     * <p>
     * getViewerConfigFolder.
     * </p>
     *
     * @return Configured viewerConfigFolder in the default config file; /opt/digiverso/viewer/config/ if no value configured
     * @should add trailing slash
     * @should return environment variable value if available
     */
    public String getViewerConfigFolder() {
        String configLocalPath = System.getProperty("configFolder");
        if (StringUtils.isEmpty(configLocalPath)) {
            configLocalPath = getConfig().getString("viewerConfigFolder", "/opt/digiverso/viewer/config/");
        }
        if (!configLocalPath.endsWith("/")) {
            configLocalPath += "/";
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0 && configLocalPath.startsWith("/opt/")) {
            configLocalPath = "C:" + configLocalPath;
        }
        return configLocalPath;
    }

    /**
     * <p>
     * getOaiFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getOaiFolder() {
        return getLocalString("oaiFolder", "/opt/digiverso/viewer/oai/");
    }

    /**
     * <p>
     * getResumptionTokenFolder.
     * </p>
     *
     * @return folder for resumptionToken
     * @should return correct value
     */
    public String getResumptionTokenFolder() {
        return getLocalString("resumptionTokenFolder", "/opt/digiverso/viewer/oai/token/");
    }

    /**
     * <p>
     * getIndexUrl.
     * </p>
     *
     * @return where to find the index
     * @should return correct value
     */
    public String getIndexUrl() {
        return getLocalString("solr.solrUrl", "http://localhost:8080/solr");
    }

    /**
     * <p>
     * getHitsPerToken.
     * </p>
     *
     * @return number of hits per page/token
     * @should return correct value
     */
    public int getHitsPerToken() {
        return getLocalInt("solr.hitsPerToken", 20);
    }

    /**
     * <p>
     * getHitsPerTokenForMetadataFormat.
     * </p>
     *
     * @return number of hits per page/token
     * @should return correct value
     * @should return default value for unknown formats
     * @param metadataFormat a {@link java.lang.String} object.
     */
    public int getHitsPerTokenForMetadataFormat(String metadataFormat) {
        return getLocalInt(metadataFormat + ".hitsPerToken", getHitsPerToken());
    }

    /**
     * <p>
     * getVersionDisriminatorFieldForMetadataFormat.
     * </p>
     *
     * @return number of hits per page/token
     * @should return correct value
     * @param metadataFormat a {@link java.lang.String} object.
     */
    public String getVersionDisriminatorFieldForMetadataFormat(String metadataFormat) {
        return getLocalString(metadataFormat + ".versionDiscriminatorField", null);
    }

    /**
     * <p>
     * isMetadataFormatEnabled.
     * </p>
     *
     * @return number of hits per page/token
     * @should return correct value
     * @should return false for unknown formats
     * @param metadataFormat a {@link java.lang.String} object.
     */
    public boolean isMetadataFormatEnabled(String metadataFormat) {
        return getLocalBoolean(metadataFormat + ".enabled", false);
    }

    /**
     * <p>
     * getSetSpecFieldsForMetadataFormat.
     * </p>
     *
     * @param metadataFormat a {@link java.lang.String} object.
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getSetSpecFieldsForMetadataFormat(String metadataFormat) {
        if (metadataFormat == null) {
            throw new IllegalArgumentException("metadataFormat may not be null");
        }

        return getLocalList(metadataFormat + ".setSpec.field", new ArrayList<>(0));
    }

    /**
     * 
     * @param metadataFormat Metadata format
     * @param accessCondition Access condition name
     * @return Mapped value; null if none found
     * @should return correct value
     */
    public String getAccessConditionMappingForMetadataFormat(String metadataFormat, String accessCondition) {
        if (metadataFormat == null) {
            throw new IllegalArgumentException("metadataFormat may not be null");
        }
        if (accessCondition == null) {
            return null;
        }

        List<HierarchicalConfiguration<ImmutableNode>> elements = getLocalConfigurationsAt(metadataFormat + ".accessConditions.mapping");
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            String key = sub.getString("[@accessCondition]", null);
            if (accessCondition.equals(key)) {
                return sub.getString(".");
            }

        }

        return null;
    }

    /**
     * <p>
     * getHarvestUrl.
     * </p>
     *
     * @return Harvest servlet URL
     * @should return correct value
     */
    public String getHarvestUrl() {
        return getLocalString("harvestUrl", "http://localhost:8080/viewer/harvest");
    }

    /**
     * <p>
     * getRestApiUrl.
     * </p>
     *
     * @return REST API URL
     * @should return correct value
     */
    public String getRestApiUrl() {
        return getLocalString("restApiUrl", "http://localhost:8080/viewer/api/v1/");
    }

    /**
     * Returns mappings for the ESE "type" element.
     *
     * @should return all values
     * @return a {@link java.util.Map} object.
     */
    @SuppressWarnings("rawtypes")
    public Map<String, String> getEseTypes() {
        Map<String, String> ret = new HashMap<>();

        List<HierarchicalConfiguration<ImmutableNode>> types = getLocalConfigurationsAt("ese.types.docstruct");
        if (types != null) {
            for (Iterator it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                ret.put(sub.getString(XML_PATH_ATTRIBUTE_NAME), sub.getString("[@type]"));
            }
        }

        return ret;
    }

    /**
     * <p>
     * getMods2MarcXsl.
     * </p>
     *
     * @return Path to the MODS2MARC XSLT stylesheet.
     * @should return correct value
     */
    public String getMods2MarcXsl() {
        return getLocalString("marcxml.marcStylesheet", null);
    }

    /**
     * <p>
     * getUrnResolverUrl.
     * </p>
     *
     * @return URN resolver URL.
     * @should return correct value
     */
    public String getUrnResolverUrl() {
        return getLocalString("urnResolverUrl", "http://localhost:8080/viewer/resolver?urn=");
    }

    /**
     * <p>
     * getPiResolverUrl.
     * </p>
     *
     * @return PI resolver URL.
     * @should return correct value
     */
    public String getPiResolverUrl() {
        return getLocalString("piResolverUrl", "http://localhost:8080/viewer/piresolver?id=");
    }

    /**
     * <p>
     * getDocumentResolverUrl.
     * </p>
     *
     * @return METS/LIDO resolver URL.
     * @should return correct value
     */
    public String getDocumentResolverUrl() {
        return getLocalString("documentResolverUrl", "http://localhost:8080/viewer/metsresolver?id=");
    }

    /**
     * Returns a list of additional docstruct types "type" element.
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    @SuppressWarnings("rawtypes")
    public List<String> getAdditionalDocstructTypes() {
        List<String> ret = new ArrayList<>();

        List<HierarchicalConfiguration<ImmutableNode>> docstructs = getLocalConfigurationsAt("epicur.additionalDocstructTypes.docstruct");
        if (docstructs != null) {
            for (Iterator it = docstructs.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
                ret.add(sub.getString("."));
                logger.trace("loaded additional docstruct type: {}", sub.getString("."));
            }
        }

        return ret;
    }

    /**
     * <p>
     * getUrnPrefixBlacklist.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getUrnPrefixBlacklist() {
        return getLocalList("epicur.blacklist.urnPrefix", new ArrayList<>(0));
    }

    /**
     * <p>
     * getEseProviderField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseProviderField() {
        return getLocalString("ese.providerField", null);
    }

    /**
     * <p>
     * getEseDataProviderField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseDataProviderField() {
        return getLocalString("ese.dataProviderField", null);
    }

    /**
     * <p>
     * getEseDefaultProvider.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseDefaultProvider() {
        return getLocalString("ese.defaultProvider", null);
    }

    /**
     * <p>
     * getEseRightsField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseRightsField() {
        return getLocalString("ese.rightsField", null);
    }

    /**
     * <p>
     * getEseDefaultRightsUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseDefaultRightsUrl() {
        return getLocalString("ese.defaultRightsUrl", null);
    }

    /**
     * <p>
     * getAllValuesSets.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<Set> getAllValuesSets() {
        List<HierarchicalConfiguration<ImmutableNode>> types = getLocalConfigurationsAt("sets.allValuesSet");
        if (types != null) {
            List<Set> ret = new ArrayList<>(types.size());
            for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration<ImmutableNode> sub = it.next();
                Set set = new Set(sub.getString("."), null, null);
                set.setTranslate(sub.getBoolean("[@translate]", false));
                ret.add(set);
            }
            return ret;
        }
        return new ArrayList<>(0);
    }

    /**
     * <p>
     * getAdditionalSets.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<Set> getAdditionalSets() {
        List<HierarchicalConfiguration<ImmutableNode>> types = getLocalConfigurationsAt("sets.set");
        if (types != null) {
            List<Set> ret = new ArrayList<>(types.size());
            for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = types.iterator(); it.hasNext();) {
                HierarchicalConfiguration<ImmutableNode> sub = it.next();
                Set set = new Set(sub.getString("[@setName]"), sub.getString("[@setSpec]"), sub.getString("[@setQuery]"));
                ret.add(set);
            }
            return ret;
        }
        return new ArrayList<>(0);
    }

    /**
     * <p>
     * getBaseURL.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getBaseURL() {
        return getLocalString("identifyTags.baseURL", null);
    }

    /**
     * <p>
     * isBaseUrlUseInRequestElement.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBaseUrlUseInRequestElement() {
        return getLocalBoolean("identifyTags.baseURL[@useInRequestElement]", false);
    }

    /**
     * <p>
     * getLocalRessourceBundleFile.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocalRessourceBundleFile() {
        return getOaiFolder() + "messages_de.properties";
    }

    /**
     * <p>
     * getDefaultLocale.
     * </p>
     *
     * @should return correct value
     * @should return English if locale not found
     * @return a {@link java.util.Locale} object.
     */
    public Locale getDefaultLocale() {
        String language = getLocalString("defaultLocale", "en");
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        return locale;
    }

    /**
     * <p>
     * getMetadataConfiguration.
     * </p>
     *
     * @param metadataFormat a {@link java.lang.String} object.
     * @param template a {@link java.lang.String} object
     * @return a {@link java.util.List} object.
     * @should return correct template configuration
     * @should return default template configuration if template not found
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Metadata> getMetadataConfiguration(String metadataFormat, String template) {
        HierarchicalConfiguration usingTemplate = null;
        List templateList = getLocalConfigurationsAt(metadataFormat + ".fields.template");
        if (templateList != null) {
            HierarchicalConfiguration defaultTemplate = null;

            for (Iterator it = templateList.iterator(); it.hasNext();) {
                HierarchicalConfiguration subElement = (HierarchicalConfiguration) it.next();
                if (subElement.getString(XML_PATH_ATTRIBUTE_NAME).equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(subElement.getString(XML_PATH_ATTRIBUTE_NAME))) {
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
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     * 
     * @param usingTemplate
     * @return List<Metadata>
     */
    @SuppressWarnings("rawtypes")
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration usingTemplate) {
        List<Metadata> ret = new ArrayList<>();

        if (usingTemplate != null) {
            List elements = usingTemplate.configurationsAt("metadata");
            if (elements != null) {
                for (Iterator it2 = elements.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration sub = (HierarchicalConfiguration) it2.next();
                    String label = sub.getString("[@label]");
                    String masterValue = sub.getString("[@value]");
                    boolean group = sub.getBoolean("[@group]", false);
                    boolean multivalued = sub.getBoolean("[@multivalued]", false);
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
                            String prefix = sub2.getString("[@prefix]", "").replace(StringConstants.PLACEHOLDER_SPACE, " ");
                            String suffix = sub2.getString("[@suffix]", "").replace(StringConstants.PLACEHOLDER_SPACE, " ");
                            boolean addUrl = sub2.getBoolean("[@url]", false);
                            boolean dontUseTopstructValue = sub2.getBoolean("[@dontUseTopstructValue]", false);
                            paramList.add(new MetadataParameter(MetadataParameterType.getByString(fieldType), source, key, overrideMasterValue,
                                    defaultValue, prefix, suffix, addUrl, dontUseTopstructValue));
                        }
                    }
                    ret.add(new Metadata(label, masterValue, type, paramList, group, number, multivalued));
                }
            }
        }

        return ret;
    }

    /**
     * Overrides values in the config file (for unit test purposes).
     *
     * @param property Property path (e.g. "accessConditions.fullAccessForLocalhost")
     * @param value New value to set
     */
    public void overrideValue(String property, Object value) {
        getConfig().setProperty(property, value);
    }
}
