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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;

public class ViewerResourceBundle extends ResourceBundle {

    private static final Logger logger = LoggerFactory.getLogger(ViewerResourceBundle.class);

    private static final Object lock = new Object();
    private static final Map<Locale, ResourceBundle> defaultBundles = new ConcurrentHashMap<>();
    protected static final Map<Locale, ResourceBundle> localBundles = new ConcurrentHashMap<>();
    protected static volatile Locale defaultLocale;

    /**
     * Loads default resource bundles if not yet loaded.
     */
    private static void checkAndLoadDefaultResourceBundles() {
        if (defaultLocale == null) {
            synchronized (lock) {
                if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
                    defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
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
        // Reload local bundle if the locale is different
        if (!localBundles.containsKey(locale)) {
            synchronized (lock) {
                // Bundle could have been initialized by a different thread in the meanwhile
                if (!localBundles.containsKey(locale)) {
                    logger.trace("Reloading local resource bundle for '{}'...", locale.getLanguage());
                    ResourceBundle localBundle = loadLocalResourceBundle(locale);
                    if (localBundle != null) {
                        localBundles.put(locale, localBundle);
                    } else {
                        logger.warn("Could not load local resource bundle.");
                    }
                }
            }
        }

        return locale;
    }

    /**
     * 
     * @param locale
     * @return
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
                // some error while loading bundle from file system; use
                // default bundle now ...
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

    /**
     * 
     * @param text
     * @param locale
     * @return
     */
    public static String getTranslation(final String key, Locale locale) {
        //        logger.trace("Translation for: {}", key);
        checkAndLoadDefaultResourceBundles();
        locale = checkAndLoadResourceBundles(locale); // If locale is null, the return value will be the current locale
        String value = getTranslation(key, defaultBundles.get(locale), localBundles.get(locale));
        if (StringUtils.isEmpty(value) && defaultBundles.containsKey(defaultLocale) && !defaultLocale.equals(locale)) {
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
     * @return
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
     * @return
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
     * @return
     */
    private static String cleanUpTranslation(String value) {
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
}
