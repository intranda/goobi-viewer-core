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
package de.intranda.digiverso.presentation.messages;

import java.io.File;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.StringTools;

public class ViewerResourceBundle extends ResourceBundle {

    private static final Logger logger = LoggerFactory.getLogger(ViewerResourceBundle.class);

    private static final Object lock = new Object();
    private static final Map<Locale, ResourceBundle> defaultBundles = new ConcurrentHashMap<>();
    protected static final Map<Locale, ResourceBundle> localBundles = new ConcurrentHashMap<>();
    protected static final Map<String, Boolean> reloadNeededMap = new ConcurrentHashMap<>();
    protected static volatile Locale defaultLocale;
    private static List<Locale> allLocales = null;

    public ViewerResourceBundle() {
        registerFileChangedService(Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath()));
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
        Thread backgroundThread = new Thread(new Runnable() {

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

        backgroundThread.start();
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
                } else {
                    defaultLocale = Locale.ENGLISH;
                }
                checkAndLoadResourceBundles(defaultLocale);
            }
        }
    }

    /**
     * Loads resource bundles for the current locale and reloads them if the locale has since changed.
     * 
     * @param inLocale
     * @return The selected locale
     */
    private static Locale checkAndLoadResourceBundles(Locale inLocale) {
        Locale locale;
        if (inLocale != null) {
            locale = inLocale;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        } else {
            locale = Locale.ENGLISH;
        }
        // Reload default bundle if the locale is different
        if (!defaultBundles.containsKey(locale)) {
            synchronized (lock) {
                // Bundle could have been initialized by a different thread in the meanwhile
                if (!defaultBundles.containsKey(locale)) {
                    defaultBundles.put(locale, ResourceBundle.getBundle("de.intranda.digiverso.presentation.messages.messages", locale));
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
     * 
     * @param locale
     * @return The resource bundle
     */
    private static ResourceBundle loadLocalResourceBundle(final Locale locale) {
        File file = new File(DataManager.getInstance().getConfiguration().getLocalRessourceBundleFile());
        if (file.exists()) {
            try {
                URL resourceURL = file.getParentFile().toURI().toURL();
                // logger.debug("URL: " + file.getParentFile().toURI().toURL());
                URLClassLoader urlLoader = new URLClassLoader(new URL[] { resourceURL });
                return ResourceBundle.getBundle("messages", locale, urlLoader);
            } catch (Exception e) {
                // some error while loading bundle from file system; use default bundle now ...
            }
        }

        return null;
    }

    /**
     * This is the method that is called for HTML translations.
     */
    @Override
    protected Object handleGetObject(final String key) {
        return getTranslation(key, FacesContext.getCurrentInstance().getViewRoot().getLocale());
    }

    public static String getTranslation(final String key, Locale locale) {
        return getTranslation(key, locale, true);
    }

    /**
     * 
     * @param text
     * @param locale
     * @param useFallback If true, get default locale translation if there is none for the given locale
     * @return Translated message key
     */
    public static String getTranslation(final String key, Locale locale, boolean useFallback) {
        //        logger.trace("Translation for: {}", key);
        checkAndLoadDefaultResourceBundles();
        locale = checkAndLoadResourceBundles(locale); // If locale is null, the return value will be the current locale
        String value = getTranslation(key, defaultBundles.get(locale), localBundles.get(locale));
        if (useFallback && StringUtils.isEmpty(value) && defaultLocale != null && defaultBundles.containsKey(defaultLocale)
                && !defaultLocale.equals(locale)) {
            value = getTranslation(key, defaultBundles.get(defaultLocale), localBundles.get(defaultLocale));
        }
        if (value == null) {
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
     * @return Translated message key
     */
    protected static String getTranslation(String key, ResourceBundle fallbackBundle, ResourceBundle preferredBundle) {
        if (key != null) {
            // Remove trailing asterisk
            if (key.endsWith("*")) {
                key = key.substring(0, key.length() - 1);
            }

            if (preferredBundle != null) {
                String value = getTranslationFromBundle(key, preferredBundle);
                if (value != null) {
                    return cleanUpTranslation(value);
                }
            }
            if (fallbackBundle != null) {
                String value = getTranslationFromBundle(key, fallbackBundle);
                if (value != null) {
                    return cleanUpTranslation(value);
                }
                try {
                    if (fallbackBundle.containsKey(key)) {
                        return cleanUpTranslation(fallbackBundle.getString(key));
                    }
                } catch (MissingResourceException e) {
                    // There is a MissingResourceException when calling this from the RSS feed
                }
            }
        } else {
            logger.warn("globalBundle is null");
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
     * @param value
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
     * 
     * @param locale
     * @param keyPrefix
     * @return
     */
    public static List<String> getMessagesValues(Locale locale, String keyPrefix) {
        ResourceBundle rb = loadLocalResourceBundle(locale);
        List<String> res = new ArrayList<>();

        for (String key : rb.keySet()) {
            if (key.startsWith(keyPrefix)) {
                res.add(key);
            }
        }

        Collections.sort(res);

        return res;
    }

    @Override
    public Enumeration<String> getKeys() {
        return null;
    }

    public static List<Locale> getAllLocales() {
        if (allLocales == null) {

            //          FacesContext.getCurrentInstance().getApplication().getSupportedLocales()
            checkAndLoadDefaultResourceBundles();
            Set<Locale> locales = new HashSet<Locale>();
            locales.addAll(defaultBundles.keySet());
            locales.addAll(localBundles.keySet());
            allLocales = new ArrayList<>(locales);

            //deprecated?
            if (allLocales.isEmpty()) {
                Path configPath = Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath());
                try (Stream<Path> messageFiles =
                        Files.list(configPath).filter(path -> path.getFileName().toString().matches("messages_[a-z]{1,3}.properties"))) {
                    allLocales = messageFiles.map(path -> StringTools
                            .findFirstMatch(path.getFileName().toString(), "(?:messages_)([a-z]{1,3})(?:.properties)", 1).orElse(null))
                            .filter(lang -> lang != null)
                            .sorted((l1, l2) -> {
                                if (l1.equals(l2)) {
                                    return 0;
                                }
                                switch (l1) {
                                    case "en":
                                        return -1;
                                    case "de":
                                        return l2.equals("en") ? 1 : -1;
                                    default:
                                        switch (l2) {
                                            case "en":
                                            case "de":
                                                return 1;
                                        }
                                }
                                return l1.compareTo(l2);
                            })
                            .map(language -> Locale.forLanguageTag(language))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error("Error reading config directory " + configPath);
                }
            }
        }

        return allLocales;
    }
}
