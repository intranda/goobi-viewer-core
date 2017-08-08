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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;

public class ViewerResourceBundle extends ResourceBundle {

    private static final Logger logger = LoggerFactory.getLogger(ViewerResourceBundle.class);

    private static ResourceBundle bundle = null;
    private static ResourceBundle localBundle = null;

    /**
     * 
     * @param inLocale
     */
    private static synchronized void loadResourceBundle(Locale inLocale) {
        Locale locale;
        if (inLocale != null) {
            locale = inLocale;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        } else {
            locale = Locale.ENGLISH;
        }
        if (bundle == null || !bundle.getLocale().equals(locale)) {
            bundle = ResourceBundle.getBundle("de.intranda.digiverso.presentation.messages.messages", locale);
        }
        if (localBundle == null || !localBundle.getLocale().equals(locale)) {
            logger.trace("Reloading local resource bundle for '{}'...", locale.getLanguage());
            localBundle = loadLocalResourceBundle(locale);
        }
    }

    /**
     * 
     * @param locale
     * @return
     */
    private static ResourceBundle loadLocalResourceBundle(Locale locale) {
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
    protected Object handleGetObject(String key) {
        return getTranslation(key, FacesContext.getCurrentInstance().getViewRoot().getLocale());
    }

    /**
     * 
     * @param text
     * @param locale
     * @return
     */
    public static String getTranslation(String text, Locale locale) {
        //        logger.trace("Translation for: {}", text);
        loadResourceBundle(locale);
        return getTranslation(text, bundle, localBundle);
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
                    return cleanUpTranslation(fallbackBundle.getString(key));
                } catch (MissingResourceException e) {
                    // There is a MissingResourceException when calling this from the RSS feed
                }
            }
        } else {
            logger.warn("globalBundle is null");
        }

        return key;
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
     * Removes the " zzz" marker from the given string.
     * 
     * @param value
     * @return
     */
    private static String cleanUpTranslation(String value) {
        if (value == null) {
            return null;
        }
        return value.replace(" zzz", "");
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
