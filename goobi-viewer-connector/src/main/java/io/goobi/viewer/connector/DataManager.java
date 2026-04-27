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
package io.goobi.viewer.connector;

import io.goobi.viewer.connector.utils.Configuration;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.model.translations.language.LanguageHelper;

/**
 * <p>
 * DataManager class.
 * </p>
 *
 */
public final class DataManager {

    private static final Object LOCK = new Object();

    private static DataManager instance = null;

    private Configuration configuration;

    private SolrSearchIndex searchIndex;

    private LanguageHelper languageHelper = io.goobi.viewer.controller.DataManager.getInstance().getLanguageHelper();

    /**
     * <p>
     * Getter for the field <code>instance</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.connector.DataManager} object.
     */
    public static DataManager getInstance() {
        DataManager dm = instance;
        if (dm == null) {
            synchronized (LOCK) {
                // Another thread might have initialized instance by now
                dm = instance;
                if (dm == null) {
                    dm = new DataManager();
                    instance = dm;
                }
            }
        }

        return dm;
    }

    private DataManager() {
    }

    /**
     * <p>
     * Getter for the field <code>configuration</code>.
     * </p>
     *
     * @return the configuration
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (LOCK) {
                configuration = new Configuration(Configuration.DEFAULT_CONFIG_FILE);
            }
        }

        return configuration;
    }

    /**
     * <p>
     * Getter for the field <code>searchIndex</code>.
     * </p>
     *
     * @return the searchIndex
     */
    public SolrSearchIndex getSearchIndex() {
        if (searchIndex == null) {
            synchronized (LOCK) {
                searchIndex = new SolrSearchIndex(null, false);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * <p>
     * Getter for the field <code>languageHelper</code>.
     * </p>
     *
     * @return the languageHelper
     */
    public LanguageHelper getLanguageHelper() {
        if (languageHelper == null) {
            synchronized (LOCK) {
                String configFolder = getConfiguration().getViewerConfigFolder();
                if (!configFolder.endsWith("/")) {
                    configFolder += '/';
                }
                languageHelper = new LanguageHelper(configFolder + "languages.xml");
            }
        }

        return languageHelper;
    }

    /**
     * Sets custom Configuration object (used for unit testing).
     *
     * @param configuration a {@link io.goobi.viewer.connector.utils.Configuration} object.
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     *
     * @param searchIndex a {@link io.goobi.viewer.connector.utils.SolrSearchIndex} object.
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }
}
