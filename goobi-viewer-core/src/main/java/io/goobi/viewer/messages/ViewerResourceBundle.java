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

import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

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

    private static final Logger logger = LogManager.getLogger(ViewerResourceBundle.class);

    private static final Object LOCK = new Object();

    private static final String BUNDLE_NAME = "messages";

    private static Map<Locale, ResourceBundle> defaultBundles = new ConcurrentHashMap<>();
    /** Constant <code>localBundles</code> */
    protected static Map<Locale, ResourceBundle> localBundles = new ConcurrentHashMap<>();
    /** Constant <code>reloadNeededMap</code> */
    protected static Map<String, Boolean> reloadNeededMap = new ConcurrentHashMap<>();
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
                    path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    while (true) {
                        final WatchKey wk = watchService.take();
                        for (WatchEvent<?> event : wk.pollEvents()) {
                            final Path changed = (Path) event.context();
                            final String fileName = changed.getFileName().toString();
                            if (fileName.startsWith("messages_")) {
                                logger.trace("File has been modified: {}", fileName);
                                final String language = fileName.substring(9, 11);
                                reloadNeededMap.put(language, true);
                                logger.debug("File '{}' (language: {}) has been modified, triggering bundle reload...",
                                        changed.getFileName(), language);
                            }
                        }
                        if (!wk.reset()) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
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
            synchronized (LOCK) {
                if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
                    defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
                    if (defaultLocale == null) {
                        logger.error("Default locale not found. Is faces-config.xml missing in the theme?");
                    }
                    // logger.trace(defaultLocale.getLanguage()); //NOSONAR Debug
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
     * Returns a locale for the configured fallback language. Does not use FacesContext.
     *
     * @return Locale for the language code returned by the configuration getter
     * @should return locale for configured fallback language
     * @should return English if no fallback language configured
     */
    public static Locale getFallbackLocale() {
        String fallbackLanguage = DataManager.getInstance().getConfiguration().getFallbackDefaultLanguage();
        return Locale.forLanguageTag(fallbackLanguage);
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
            synchronized (LOCK) {
                // Bundle could have been initialized by a different thread in the meanwhile
                defaultBundles.computeIfAbsent(locale, k -> ResourceBundle.getBundle(BUNDLE_NAME, locale));
            }
        }
        // Reload local bundle if the locale is different or the corresponding messages files has been modified
        if (!localBundles.containsKey(locale) || (reloadNeededMap.containsKey(locale.getLanguage()) && reloadNeededMap.get(locale.getLanguage()))) {
            synchronized (LOCK) {
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
                // logger.debug("URL: " + file.getParentFile().toURI().toURL()); //NOSONAR Debug
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
     * @param removeRemainingPlaceholders If true, any placeholders in the value not replaced by params are removed
     * @param params One or more parameter values to replace the placeholders
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslationWithParameters(final String key, final Locale locale, boolean removeRemainingPlaceholders,
            final String... params) {
        String ret = getTranslation(key, locale);
        if (params != null) {
            ret = replaceParameters(ret, removeRemainingPlaceholders, params);
        }

        return ret;
    }

    public String translate(String key, String... params) {
        return getTranslationWithParameters(key, getLocale(), false, params);
    }

    /**
     *
     * @param msg
     * @param removeRemainingPlaceholders If true, any placeholders in the value not replaced by params are removed
     * @param params
     * @return msg with replaced parameters
     * @should return null if msg is null
     * @should replace parameters correctly
     * @should remove remaining placeholders correctly
     */
    static String replaceParameters(final String msg, boolean removeRemainingPlaceholders, String... params) {
        String ret = msg;
        if (ret != null && params != null) {
            for (int i = 0; i < params.length; ++i) {
                ret = ret.replace(new StringBuilder("{").append(i).append("}").toString(), params[i]);
            }
            if (removeRemainingPlaceholders) {
                ret = ret.replaceAll("\\{\\d+\\}", "").trim();
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
     * @return Translated message key
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
     * @param inLocale Desired locale
     * @param useFallback If true, get default locale translation if there is none for the given locale
     * @param returnKeyIfNoneFound If true, the key will be returned as translation value; null otherwise
     * @param reversePriority If true, the global bundle will be checked first, then the local
     * @param cleanup If true, elements such as 'zzz' will be removed from the translation
     * @return Translated message key
     */
    public static String getTranslation(final String key, final Locale inLocale, boolean useFallback, boolean returnKeyIfNoneFound,
            boolean reversePriority, boolean cleanup) {
        //        logger.trace("Translation for: {}", key); //NOSONAR Debug
        Locale locale = checkAndLoadResourceBundles(inLocale); // If locale is null, the return value will be the current locale
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
    protected static String getTranslation(final String key, ResourceBundle fallbackBundle, ResourceBundle preferredBundle, boolean cleanup) {
        if (key == null) {
            return null;
        }

        String useKey = key;
        // Remove trailing asterisk
        if (useKey.endsWith("*")) {
            useKey = useKey.substring(0, useKey.length() - 1);
        }

        if (preferredBundle != null) {
            String value = getTranslationFromBundle(useKey, preferredBundle);
            if (value != null) {
                return cleanup ? cleanUpTranslation(value) : value;
            }
        }
        if (fallbackBundle != null) {
            String value = getTranslationFromBundle(useKey, fallbackBundle);
            if (value != null) {
                return cleanup ? cleanUpTranslation(value) : value;
            }
            try {
                if (fallbackBundle.containsKey(useKey)) {
                    return cleanup ? cleanUpTranslation(fallbackBundle.getString(useKey)) : fallbackBundle.getString(useKey);
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
    private static String getTranslationFromBundle(final String key, ResourceBundle bundle) {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("bundle may not be null");
        }

        String useKey = key;
        if (bundle.containsKey(useKey)) {
            return bundle.getString(useKey);
        }

        // Remove leading _LANG_XX
        if (useKey.contains(SolrConstants.MIDFIX_LANG)) {
            String translation = getTranslationFromBundleUsingCleanedUpKeys(useKey, bundle);
            if (translation != null) {
                return translation;
            }
            // Fall back to translations without the language part
            useKey = useKey.replaceAll(SolrConstants.MIDFIX_LANG + "[A-Z{][A-Z}]", "");
        }

        return getTranslationFromBundleUsingCleanedUpKeys(useKey, bundle);

    }

    /**
     *
     * @param key
     * @param bundle
     * @return Translated message key
     */
    private static String getTranslationFromBundleUsingCleanedUpKeys(String key, ResourceBundle bundle) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }

        // Remove trailing _DD (collection names for drill-down)
        if (key.endsWith(SolrConstants.SUFFIX_DD)) {
            String newKey = key.replace(SolrConstants.SUFFIX_DD, "");
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove trailing _UNTOKENIZED
        if (key.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
            String newKey = key.replace(SolrConstants.SUFFIX_UNTOKENIZED, "");
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading MD_ (metadata fields)
        if (key.startsWith("MD_")) {
            String newKey = key.substring(3);
            if (newKey.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                newKey = newKey.replace(SolrConstants.SUFFIX_UNTOKENIZED, "");
            }
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading SORT_
        if (key.startsWith(SolrConstants.PREFIX_SORT)) {
            String newKey = key.replace(SolrConstants.PREFIX_SORT, "");
            if (bundle.containsKey("MD_" + newKey)) {
                return bundle.getString("MD_" + newKey);
            }
            if (bundle.containsKey(newKey)) {
                return bundle.getString(newKey);
            }
        }
        // Remove leading FACET_
        if (key.startsWith(SolrConstants.PREFIX_FACET)) {
            String newKey = key.replace(SolrConstants.PREFIX_FACET, "");
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
                if (key != null && StringUtils.isNotBlank(translation) && (allowKeyAsTranslation || !key.equals(translation))) {
                    translations.put(locale.getLanguage(), translation);
                }
            }
        }
        if (translations.isEmpty()) {
            return new SimpleMetadataValue(key);
        }

        return new MultiLanguageMetadataValue(translations);
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
                // logger.trace("No faces context instance available"); //NOSONAR Debug
                return Arrays.asList(Locale.ENGLISH, Locale.GERMAN);
            }
        }
        return allLocales;
    }

    /**
     *
     * @param servletContext
     * @return List of all configured {@link Locale}s
     */
    public static List<Locale> getAllLocales(ServletContext servletContext) {
        if (allLocales == null) {
            allLocales = getLocalesFromFacesConfig(servletContext);
        }
        return allLocales;
    }

    /**
     * 
     * @param servletContext
     * @return Default {@link Locale}
     */
    public static Locale getDefaultLocale(ServletContext servletContext) {
        if (defaultLocale == null) {
            defaultLocale = getDefaultLocaleFromFacesConfig(servletContext);
        }
        return defaultLocale;
    }

    public static List<Locale> getFacesLocales() {
        List<Locale> locales = new ArrayList<>();
        try {
            FacesContext.getCurrentInstance().getApplication().getSupportedLocales().forEachRemaining(locales::add);
        } catch (NullPointerException e) {
            // logger.warn("No faces context instance available");
            return Arrays.asList(Locale.ENGLISH, Locale.GERMAN);
        }
        return locales;
    }

    /**
     * Get locales configured in faces-config, ordered by appearance in file
     *
     * @param servletContext
     * @return a list of Locale objects, or null if the list could not be retrieved
     */
    public static List<Locale> getLocalesFromFacesConfig(ServletContext servletContext) {
        if (servletContext == null) {
            return getFacesLocales();
        }

        try {
            String webContentRoot = servletContext.getRealPath("resources/themes");
            Path facesConfigPath = Paths.get(webContentRoot).resolve("faces-config.xml");
            if (Files.exists(facesConfigPath)) {
                return getLocalesFromFile(facesConfigPath);
            }
            throw new FileNotFoundException("Unable to locate faces-config at " + facesConfigPath);
        } catch (Exception e) {
            logger.error("Error getting locales from faces-config", e);
            return getFacesLocales();
        }
    }

    /**
     * 
     * @param servletContext
     * @return Default {@link Locale}
     */
    public static Locale getDefaultLocaleFromFacesConfig(ServletContext servletContext) {
        if (servletContext == null) {
            return getDefaultLocale();
        }

        try {
            String webContentRoot = servletContext.getRealPath("resources/themes");
            Path facesConfigPath = Paths.get(webContentRoot).resolve("faces-config.xml");
            if (Files.exists(facesConfigPath)) {
                return getDefaultLocaleFromFile(facesConfigPath);
            }
            throw new FileNotFoundException("Unable to locate faces-config at " + facesConfigPath);
        } catch (Exception e) {
            logger.error("Error getting locales from faces-config", e);
            return getDefaultLocale();
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
                logger.trace("Local message file already exists: {}", path.toAbsolutePath());
                continue;
            }
            try {
                Files.createFile(path);
                // BufferedWriter defaults to UTF-8
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write("");
                }
                logger.info("Created local message file: {}", path.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Could not create local message file: {}", e.getMessage());
            }
        }
    }

    /**
     * @param facesConfigPath
     * @return {@link Locale}s configured in given file path
     * @throws IOException
     * @throws JDOMException
     */
    public static List<Locale> getLocalesFromFile(Path facesConfigPath) throws IOException, JDOMException {
        Document doc = XmlTools.readXmlFile(facesConfigPath);
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace javaee = Namespace.getNamespace("ee", "http://java.sun.com/xml/ns/javaee");
        List<Namespace> namespaces = Arrays.asList(xsi, javaee);
        //doc.getNamespacesInScope();
        List<Element> localeElements = XmlTools.evaluateToElements("//ee:locale-config/ee:supported-locale", doc.getRootElement(), namespaces);
        return localeElements.stream().map(Element::getText).map(Locale::forLanguageTag).collect(Collectors.toList());
    }

    /**
     * 
     * @param facesConfigPath
     * @return Default {@link Locale} configured in given file path
     * @throws IOException
     * @throws JDOMException
     */
    public static Locale getDefaultLocaleFromFile(Path facesConfigPath) throws IOException, JDOMException {
        Document doc = XmlTools.readXmlFile(facesConfigPath);
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace javaee = Namespace.getNamespace("ee", "http://java.sun.com/xml/ns/javaee");
        List<Namespace> namespaces = Arrays.asList(xsi, javaee);
        //doc.getNamespacesInScope();
        List<Element> localeElements = XmlTools.evaluateToElements("//ee:locale-config/ee:default-locale", doc.getRootElement(), namespaces);
        return localeElements.stream().map(Element::getText).map(Locale::forLanguageTag).findFirst().orElse(getDefaultLocale());
    }

    /**
     * @param servletContext
     */
    public static void init(ServletContext servletContext) {
        getAllLocales(servletContext);
        getDefaultLocale(servletContext);
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
     * @return true if file updated successfully; false otherwise
     * @should preserve spaces
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
                if (!file.createNewFile()) {
                    logger.error("File could not be createad: {}", file.getAbsolutePath());
                    return false;
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        try {
            ConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                            .configure(new Parameters().properties()
                                    .setFile(file)
                                    .setListDelimiterHandler(new DisabledListDelimiterHandler())
                                    .setThrowExceptionOnMissing(false));

            PropertiesConfiguration config = builder.getConfiguration();

            if (StringUtils.isNotEmpty(value)) {
                // Update value in file
                String oldValue = config.getProperty(key) != null ? config.getProperty(key).toString() : null;
                config.setProperty(key, value);
                logger.trace("value set ({}): {}:{}->{}", file.getName(), key, oldValue,
                        config.getProperty(key));
            } else {
                // Delete value in file if cleared in entry
                config.clearProperty(key);
                logger.trace("value removed ({}): {}", file.getName(), key);
            }

            FileHandler fh = new FileHandler(config);
            fh.save(file);
            logger.trace("File written: {}", file.getAbsolutePath());
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
