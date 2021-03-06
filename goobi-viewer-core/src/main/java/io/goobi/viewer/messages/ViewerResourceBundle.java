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
package io.goobi.viewer.messages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * ViewerResourceBundle class.
 * </p>
 */
public class ViewerResourceBundle extends ResourceBundle {

    private static final Logger logger = LoggerFactory.getLogger(ViewerResourceBundle.class);

    private static final Object lock = new Object();

    private static final String BUNDLE_NAME = "messages";

    private static final Map<Locale, ResourceBundle> defaultBundles = new ConcurrentHashMap<>();
    /** Constant <code>localBundles</code> */
    protected static final Map<Locale, ResourceBundle> localBundles = new ConcurrentHashMap<>();
    /** Constant <code>reloadNeededMap</code> */
    protected static final Map<String, Boolean> reloadNeededMap = new ConcurrentHashMap<>();
    /** Constant <code>defaultLocale</code> */
    protected static volatile Locale defaultLocale;
    private static List<Locale> allLocales = null;

    /**
     * <p>
     * Constructor for ViewerResourceBundle.
     * </p>
     */
    public ViewerResourceBundle() {
        registerFileChangedService(Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath()));
    }

    public ViewerResourceBundle(Path localConfigPath) {
        registerFileChangedService(localConfigPath);
    }

    /**
     * Registers a WatchService that checks for modified messages.properties files and tags them for reloading.
     * 
     * @param path
     * @throws IOException
     * @throws InterruptedException
     */
    private static void registerFileChangedService(Path path) {
        logger.trace("registerFileChangedService: {}", path);
        Thread fileChangedObserver = new Thread(new Runnable() {

            @Override
            public void run() {
                try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    while (true) {
                        final WatchKey wk = watchService.take();
                        for (WatchEvent<?> event : wk.pollEvents()) {
                            final Path changed = (Path) event.context();
                            final String fileName = changed.getFileName().toString();
                            logger.trace("File has been modified: {}", fileName);
                            if (fileName.startsWith("messages_")) {
                                final String language = fileName.substring(9, 11);
                                reloadNeededMap.put(language, true);
                                logger.debug("File '{}' (language: {}) has been modified, triggering bundle reload...",
                                        changed.getFileName().toString(), language);
                            }
                        }
                        if (!wk.reset()) {
                            break;
                        }
                        // Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });

        fileChangedObserver.start();
    }

    /**
     * Loads default resource bundles if not yet loaded.
     */
    private static void checkAndLoadDefaultResourceBundles() {
        if (defaultLocale == null) {
            synchronized (lock) {
                if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
                    defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
                    if (defaultLocale == null) {
                        logger.error("Default locale not found. Is faces-config.xml missing in the theme?");
                    }
                    // logger.trace(defaultLocale.getLanguage());
                }
                checkAndLoadResourceBundles(defaultLocale);
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>defaultLocale</code>.
     * </p>
     *
     * @return a {@link java.util.Locale} object.
     */
    public static Locale getDefaultLocale() {
        if (defaultLocale == null) {
            checkAndLoadDefaultResourceBundles();
        }
        return defaultLocale == null ? Locale.ENGLISH : defaultLocale;
    }

    /**
     * Loads resource bundles for the current locale and reloads them if the locale has since changed.
     * 
     * @param inLocale
     * @return The selected locale
     */
    private static Locale checkAndLoadResourceBundles(Locale inLocale) {
        Locale locale = getThisOrFallback(inLocale);
        // Reload default bundle if the locale is different
        if (!defaultBundles.containsKey(locale)) {
            synchronized (lock) {
                // Bundle could have been initialized by a different thread in the meanwhile
                if (!defaultBundles.containsKey(locale)) {
                    defaultBundles.put(locale, ResourceBundle.getBundle(BUNDLE_NAME, locale));
                }
            }
        }
        // Reload local bundle if the locale is different or the corresponding messages files has been modified
        if (!localBundles.containsKey(locale) || (reloadNeededMap.containsKey(locale.getLanguage()) && reloadNeededMap.get(locale.getLanguage()))) {
            synchronized (lock) {
                // Bundle could have been initialized by a different thread in the meanwhile
                if (!localBundles.containsKey(locale)
                        || (reloadNeededMap.containsKey(locale.getLanguage()) && reloadNeededMap.get(locale.getLanguage()))) {
                    logger.debug("Reloading local resource bundle for '{}'...", locale.getLanguage());
                    try {
                        ResourceBundle localBundle = loadLocalResourceBundle(locale);
                        if (localBundle != null) {
                            localBundles.put(locale, localBundle);
                        } else {
                            localBundles.put(locale, defaultBundles.get(locale));
                            logger.warn("Could not load local resource bundle.");
                        }
                    } finally {
                        reloadNeededMap.remove(locale.getLanguage());
                    }
                }
            }
        }

        return locale;
    }

    /**
     * @param inLocale
     * @return the passed inLocale if it is not null. Otherwise the current locale from the faces context, or ENGLISH if no faces context exists
     */
    private static Locale getThisOrFallback(Locale inLocale) {
        Locale locale;
        if (inLocale != null && getAllLocales().contains(inLocale)) {
            locale = inLocale;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        } else {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    /**
     * 
     * @param locale
     * @return The resource bundle
     */
    private static ResourceBundle loadLocalResourceBundle(final Locale locale) {
        File file = new File(DataManager.getInstance().getConfiguration().getLocalRessourceBundleFile());
        if (file.isFile()) {
            try {
                URL resourceURL = file.getParentFile().toURI().toURL();
                // logger.debug("URL: " + file.getParentFile().toURI().toURL());
                URLClassLoader urlLoader = new URLClassLoader(new URL[] { resourceURL });
                return ResourceBundle.getBundle(BUNDLE_NAME, locale, urlLoader);
            } catch (Exception e) {
                // some error while loading bundle from file system; use default bundle now ...
            }
        } else {
            logger.debug("Local messages file not found: {}", file.getAbsolutePath());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * This is the method that is called for HTML translations.
     */
    @Override
    protected Object handleGetObject(final String key) {
        return getTranslation(key, FacesContext.getCurrentInstance().getViewRoot().getLocale());
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslation(final String key, Locale locale) {
        return getTranslation(key, locale, true, true);
    }

    /**
     * <p>
     * getTranslationWithParameters.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @param params One or more parameter values to replace the placeholders
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslationWithParameters(final String key, final Locale locale, final String... params) {
        String ret = getTranslation(key, locale);
        if (params != null) {
            ret = replaceParameters(ret, params);
        }

        return ret;
    }

    /**
     * 
     * @param msg
     * @param params
     * @return
     * @should return null if msg is null
     * @should replace parameters correctly
     */
    static String replaceParameters(final String msg, String... params) {
        String ret = msg;
        if (ret != null && params != null) {
            for (int i = 0; i < params.length; ++i) {
                ret = ret.replace(new StringBuilder("{").append(i).append("}").toString(), params[i]);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @param useFallback If true, get default locale translation if there is none for the given locale
     * @return Translated message key
     */
    public static String getTranslation(final String key, Locale locale, boolean useFallback) {
        return getTranslation(key, locale, useFallback, useFallback);
    }

    /**
     * 
     * @param key
     * @param locale
     * @param useFallback
     * @param cleanup
     * @return
     */
    public static String getTranslation(final String key, Locale locale, boolean useFallback, boolean cleanup) {
        return getTranslation(key, locale, useFallback, false, cleanup);
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param key Message key to translate
     * @param locale Desired locale
     * @param useFallback If true, get default locale translation if there is none for the given locale
     * @param reversePriority If true, the global bundle will be checked first, then the local
     * @param cleanup If true, elements such as 'zzz' will be removed from the translation
     * @return Translated message key
     */
    public static String getTranslation(final String key, Locale locale, boolean useFallback, boolean reversePriority, boolean cleanup) {
        return getTranslation(key, locale, useFallback, true, reversePriority, cleanup);
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     * 
     * @param key Message key to translate
     * @param locale Desired locale
     * @param useFallback If true, get default locale translation if there is none for the given locale
     * @param returnKeyIfNoneFound If true, the key will be returned as translation value; null otherwise
     * @param reversePriority If true, the global bundle will be checked first, then the local
     * @param cleanup If true, elements such as 'zzz' will be removed from the translation
     * @return Translated message key
     */
    public static String getTranslation(final String key, Locale locale, boolean useFallback, boolean returnKeyIfNoneFound, boolean reversePriority,
            boolean cleanup) {
        //        logger.trace("Translation for: {}", key);
        locale = checkAndLoadResourceBundles(locale); // If locale is null, the return value will be the current locale
        Map<Locale, ResourceBundle> bundles1 = reversePriority ? localBundles : defaultBundles;
        Map<Locale, ResourceBundle> bundles2 = reversePriority ? defaultBundles : localBundles;
        String value = getTranslation(key, bundles1.get(locale), bundles2.get(locale), cleanup);
        if (useFallback && StringUtils.isEmpty(value) && defaultLocale != null && bundles1.containsKey(defaultLocale)
                && !defaultLocale.equals(locale)) {
            value = getTranslation(key, bundles1.get(defaultLocale), bundles2.get(defaultLocale), cleanup);
        }
        if (value == null && returnKeyIfNoneFound) {
            value = key;
        }

        return value;
    }

    /**
     * Translation method with ResourceBundle parameters. It can be overridden from inheriting classes which may pass their own bundles.
     *
     * @param key Message key
     * @param fallbackBundle Fallback bundle if no value is found in preferredBundle
     * @param preferredBundle Check for a translation in this bundle first
     * @param cleanup If true, elements such as 'zzz' will be removed from the translation
     * @return Translated message key
     */
    protected static String getTranslation(String key, ResourceBundle fallbackBundle, ResourceBundle preferredBundle, boolean cleanup) {
        if (key == null) {
            return null;
        }

        // Remove trailing asterisk
        if (key.endsWith("*")) {
            key = key.substring(0, key.length() - 1);
        }

        if (preferredBundle != null) {
            String value = getTranslationFromBundle(key, preferredBundle);
            if (value != null) {
                return cleanup ? cleanUpTranslation(value) : value;
            }
        }
        if (fallbackBundle != null) {
            String value = getTranslationFromBundle(key, fallbackBundle);
            if (value != null) {
                return cleanup ? cleanUpTranslation(value) : value;
            }
            try {
                if (fallbackBundle.containsKey(key)) {
                    return cleanup ? cleanUpTranslation(fallbackBundle.getString(key)) : fallbackBundle.getString(key);
                }
            } catch (MissingResourceException e) {
                // There is a MissingResourceException when calling this from the RSS feed
            }
        } else {
            logger.error("Global resource bundle is null");
        }

        return null;
    }

    /**
     * 
     * @param key
     * @param bundle
     * @return Translated message key
     */
    private static String getTranslationFromBundle(String key, ResourceBundle bundle) {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("bundle may not be null");
        }

        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }

        // Remove leading _LANG_XX
        if (key.contains(SolrConstants._LANG_)) {
            String translation = getTranslationFromBundleUsingCleanedUpKeys(key, bundle);
            if (translation != null) {
                return translation;
            }
            // Fall back to translations without the language part
            key = key.replaceAll(SolrConstants._LANG_ + "[A-Z][A-Z]", "");
        }

        return getTranslationFromBundleUsingCleanedUpKeys(key, bundle);

    }

    /**
     * 
     * @param key
     * @param bundle
     * @return
     */
    private static String getTranslationFromBundleUsingCleanedUpKeys(String key, ResourceBundle bundle) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }

        // Remove trailing _DD (collection names for drill-down)
        if (key.endsWith(SolrConstants._DRILLDOWN_SUFFIX)) {
            String newKey = key.replace(SolrConstants._DRILLDOWN_SUFFIX, "");
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove trailing _UNTOKENIZED
        if (key.endsWith(SolrConstants._UNTOKENIZED)) {
            String newKey = key.replace(SolrConstants._UNTOKENIZED, "");
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading MD_ (metadata fields)
        if (key.startsWith("MD_")) {
            String newKey = key.substring(3);
            if (newKey.endsWith(SolrConstants._UNTOKENIZED)) {
                newKey = newKey.replace(SolrConstants._UNTOKENIZED, "");
            }
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading SORT_
        if (key.startsWith("SORT_")) {
            String newKey = key.replace("SORT_", "");
            if (bundle.containsKey("MD_" + newKey)) {
                return bundle.getString("MD_" + newKey);
            }
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading FACET_
        if (key.startsWith("FACET_")) {
            String newKey = key.replace("FACET_", "");
            if (bundle.containsKey("MD_" + newKey)) {
                return bundle.getString("MD_" + newKey);
            }
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }

        return null;
    }

    /**
     * Removes the "zzz" marker from the given string.
     *
     * @param value a {@link java.lang.String} object.
     * @return Cleaned-up value
     */
    public static String cleanUpTranslation(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("zzz")) {
            return value.replace(" zzz", "").replace("zzz", "");
        }
        return value;
    }

    /**
     * <p>
     * getMessagesValues.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @param keyPrefix a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getMessagesValues(Locale locale, String keyPrefix) {
        ResourceBundle rb = loadLocalResourceBundle(locale);
        if (rb == null) {
            return Collections.emptyList();
        }

        List<String> res = new ArrayList<>();
        for (String key : rb.keySet()) {
            if (key.startsWith(keyPrefix)) {
                res.add(key);
            }
        }
        Collections.sort(res);

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getKeys() {
        return null;
    }

    /**
     * <p>
     * Getter for the field <code>allLocales</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @should return English if no other locales found
     */
    public static List<Locale> getAllLocales() {
        if (allLocales == null) {
            try {
                ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
                return getAllLocales(servletContext);
            } catch (NullPointerException e) {
                logger.warn("No faces context instance available");
                allLocales = Arrays.asList(Locale.GERMAN, Locale.ENGLISH);
            }
        }
        return allLocales;
    }

    public static List<Locale> getAllLocales(ServletContext servletContext) {
        if (allLocales == null) {
            allLocales = getLocalesFromFacesConfig(servletContext);
        }
        return allLocales;
    }

    public static List<Locale> getFacesLocales() {
        List<Locale> locales = new ArrayList<>();
        try {
            FacesContext.getCurrentInstance().getApplication().getSupportedLocales().forEachRemaining(locales::add);
        } catch (NullPointerException e) {
            logger.warn("No faces context instance available");
            return Arrays.asList(Locale.GERMAN, Locale.ENGLISH);
        }
        return locales;
    }

    /**
     * Returns a Multilanguage metadata value containing all found translations for the {@code key}, or the key itself if not translations were found
     *
     * @param key the message key
     * @return A Multilanguage metadata value containing all found translations for the {@code key}, or the key itself if not translations were found
     */
    public static IMetadataValue getTranslations(String key) {
        return getTranslations(key, true);
    }

    public static IMetadataValue getTranslations(String key, boolean allowKeyAsTranslation) {
        return getTranslations(key, getAllLocales(), allowKeyAsTranslation);
    }

    public static IMetadataValue getTranslations(String key, List<Locale> locales, boolean allowKeyAsTranslation) {
        Map<String, String> translations = new HashMap<>();
        if (locales != null) {
            for (Locale locale : locales) {
                String translation = ViewerResourceBundle.getTranslation(key, locale, false, true);
                if (key != null && StringUtils.isNotBlank(translation)) {
                    if (allowKeyAsTranslation || !key.equals(translation)) {
                        translations.put(locale.getLanguage(), translation);
                    }
                }
            }
        }
        if (translations.isEmpty()) {
            return new SimpleMetadataValue(key);
        }

        return new MultiLanguageMetadataValue(translations);
    }

    /**
     * Get locales configured in faces-config, ordered by apprearance in file
     * 
     * @return a list of Locale objects, or null if the list could not be retrieved
     */
    public static List<Locale> getLocalesFromFacesConfig(ServletContext servletContext) {
        try {
            String webContentRoot = servletContext.getRealPath("resources/themes");
            Path facesConfigPath = Paths.get(webContentRoot).resolve("faces-config.xml");
            if (Files.exists(facesConfigPath)) {
                return getLocalesFromFile(facesConfigPath);
            }
            throw new FileNotFoundException("Unable to locate faces-config at " + facesConfigPath);
        } catch (Throwable e) {
            logger.error("Error getting locales from faces-config", e);
            return getFacesLocales();
        }
    }

    /**
     * Creates a local messages_xx.properties file for every locale in the Faces context, if not already present.
     */
    public static void createLocalMessageFiles() {
        createLocalMessageFiles(getAllLocales());
    }

    /**
     * Creates a local messages_xx.properties file for every locale in the given list, if not already present.
     * 
     * @param locales
     * @should create files correctly
     */
    static void createLocalMessageFiles(List<Locale> locales) {
        if (locales == null) {
            return;
        }

        for (Locale locale : getAllLocales()) {
            Path path =
                    Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath(), "messages_" + locale.getLanguage() + ".properties");
            if (Files.exists(path)) {
                logger.trace("Local message file already exists: {}", path.toAbsolutePath().toString());
                continue;
            }
            try {
                Files.createFile(path);
                // BufferedWriter defaults to UTF-8
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write("");
                }
                logger.info("Created local message file: {}", path.toAbsolutePath().toString());
            } catch (IOException e) {
                logger.error("Could not create local message file: {}", e.getMessage());
            }
        }
    }

    /**
     * @param facesConfigPath
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JDOMException
     */
    public static List<Locale> getLocalesFromFile(Path facesConfigPath) throws FileNotFoundException, IOException, JDOMException {
        Document doc = XmlTools.readXmlFile(facesConfigPath);
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace javaee = Namespace.getNamespace("ee", "http://java.sun.com/xml/ns/javaee");
        List<Namespace> namespaces = Arrays.asList(xsi, javaee);//doc.getNamespacesInScope();
        List<Element> localeElements = XmlTools.evaluateToElements("//ee:locale-config/ee:supported-locale", doc.getRootElement(), namespaces);
        return localeElements.stream().map(ele -> ele.getText()).map(Locale::forLanguageTag).collect(Collectors.toList());
    }

    /**
     * @param servletContext
     */
    public static void init(ServletContext servletContext) {
        getAllLocales(servletContext);
    }

    /**
     * 
     * @return All message keys in the bundle
     */
    public static Set<String> getAllKeys() {
        ResourceBundle bundle = getBundle(BUNDLE_NAME);
        if (bundle == null) {
            logger.error("Reource bundle '{}' not found.", BUNDLE_NAME);
            return Collections.emptySet();
        }
        return bundle.keySet();
    }

    /**
     * 
     * @return Set of message keys from local messages_*.properties
     */
    public static Set<String> getAllLocalKeys() {
        Set<String> ret = new HashSet<>();
        for (Locale locale : getAllLocales()) {
            ResourceBundle bundle = localBundles.get(locale);
            if (bundle == null) {
                logger.error("Reource bundle '{}' not found.", BUNDLE_NAME);
                continue;
            }
            ret.addAll(bundle.keySet());
        }

        return ret;
    }

    /**
     * 
     * @param key Message key
     * @param value Message value
     * @param language ISO 639-1 language code
     * @return
     */
    public static boolean updateLocalMessageKey(String key, String value, String language) {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("key may not be empty");
        }
        if (StringUtils.isEmpty(language)) {
            throw new IllegalArgumentException("language may not be empty");
        }

        // Load config
        File file = getLocalTranslationFile(language);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        PropertiesConfiguration config = new PropertiesConfiguration();
        try {
            config.load(file);
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        if (StringUtils.isNotEmpty(value)) {
            // Update value in file
            String oldValue = config.getProperty(key) != null ? config.getProperty(key).toString() : null;
            config.setProperty(key, value);
            logger.trace("value set ({}): {}:{}->{}", config.getFile().getName(), key, oldValue,
                    config.getProperty(key));
        } else {
            // Delete value in file if cleared in entry
            config.clearProperty(key);
            logger.trace("value removed ({}): {}", config.getFile().getName(), key);
        }

        try {
            config.setFile(getLocalTranslationFile(language));
            config.save();
            logger.trace("File written: {}", config.getFile().getAbsolutePath());
            return true;
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    public static File getLocalTranslationFile(String language) {
        return new File(DataManager.getInstance().getConfiguration().getConfigLocalPath(),
                "messages_" + language + ".properties");
    }
}
