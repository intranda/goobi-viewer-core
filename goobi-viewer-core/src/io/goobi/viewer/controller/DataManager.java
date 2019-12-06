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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.language.LanguageHelper;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.dao.impl.JPADAO;
import io.goobi.viewer.dao.update.DatabaseUpdater;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ModuleMissingException;
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.authentication.IOAuthResponseListener;
import io.goobi.viewer.model.security.authentication.OAuthResponseListener;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.modules.interfaces.DefaultURLBuilder;
import io.goobi.viewer.modules.interfaces.IURLBuilder;

public final class DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

    private static final Object lock = new Object();

    private static volatile DataManager instance = null;

    private final List<IModule> modules = new ArrayList<>();

    private final Map<String, Map<String, String>> sessionMap = new LinkedHashMap<>();

    private Configuration configuration;

    private LanguageHelper languageHelper;

    private SolrSearchIndex searchIndex;

    private IDAO dao;

    private SessionStoreBookmarkManager bookmarkManager;

    private IOAuthResponseListener oAuthResponseListener;

    private IURLBuilder defaultUrlBuilder = new DefaultURLBuilder();

    private Map<String, List<Campaign>> recordCampaignMap = null;

    public static DataManager getInstance() {
        DataManager dm = instance;
        if (dm == null) {
            synchronized (lock) {
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
     * @return the modules
     */
    public List<IModule> getModules() {
        return modules;
    }

    /**
     * @return the urlBuilder
     */
    public IURLBuilder getUrlBuilder() {
        return getModules().stream()
                .map(module -> module.getURLBuilder())
                .filter(optional -> optional.isPresent())
                .map(optional -> optional.get())
                .findFirst()
                .orElse(defaultUrlBuilder);
    }

    /**
     * 
     * @return
     * @throws ModuleMissingException
     */
    public IModule getModule(String id) throws ModuleMissingException {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        for (IModule module : modules) {
            if (module.getId().equals(id)) {
                return module;
            }
        }

        throw new ModuleMissingException("Module not loaded: " + id);
    }

    /**
     * 
     * @param id
     * @return
     */
    public boolean isModuleLoaded(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        for (IModule module : modules) {
            if (module.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * @param module
     * @should not add module if it's already registered
     */
    public boolean registerModule(IModule module) {
        if (module == null) {
            throw new IllegalArgumentException("module may not be null");
        }

        for (IModule m : modules) {
            if (m.getId().equals(module.getId())) {
                logger.warn(
                        "Module rejected because a module with the same ID is already registered.\nRegistered module: {} ({}) v{}\nRejected module: {} ({}) v{}",
                        m.getId(), m.getName(), m.getVersion(), module.getId(), module.getName(), module.getVersion());
                return false;
            }
        }

        modules.add(module);
        logger.info("Module registered: {} ({}) v{}", module.getId(), module.getName(), module.getVersion());
        return true;
    }

    /**
     * 
     * @return
     */
    public int getSessionCount() {
        return sessionMap.size();
    }

    /**
     * @return the sessionMap
     */
    public Map<String, Map<String, String>> getSessionMap() {
        return sessionMap;
    }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (lock) {
                configuration = new Configuration("config_viewer.xml");
            }
        }

        return configuration;
    }

    /**
     * @return the languageHelper
     */
    public LanguageHelper getLanguageHelper() {
        if (languageHelper == null) {
            synchronized (lock) {
                languageHelper = new LanguageHelper("languages.xml");
            }
        }

        return languageHelper;
    }

    /**
     * @return the searchIndex
     */
    public SolrSearchIndex getSearchIndex() {
        if (searchIndex == null) {
            synchronized (lock) {
                searchIndex = new SolrSearchIndex(null);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * @return the dao
     */
    public IDAO getDao() throws DAOException {
        if (dao == null) {
            synchronized (lock) {
                dao = new JPADAO(getConfiguration().getDbPersistenceUnit());
                new DatabaseUpdater(dao).update();
            }
        }

        return dao;
    }

    /**
     * Sets custom Configuration object (used for unit testing).
     * 
     * @param dao
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     * 
     * @param dao
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }

    /**
     * Sets custom IDAO object (used for unit testing).
     * 
     * @param dao
     */
    public void injectDao(IDAO dao) {
        this.dao = dao;
    }

    public SessionStoreBookmarkManager getBookmarkManager() {
        if (this.bookmarkManager == null) {
            synchronized (lock) {
                this.bookmarkManager = new SessionStoreBookmarkManager();
            }
        }
        return this.bookmarkManager;
    }

    public void injectBookmarkManager(SessionStoreBookmarkManager bookmarkManager) {
        this.bookmarkManager = bookmarkManager;
    }

    public void injectOAuthResponseListener(IOAuthResponseListener listener) {
        if (listener != null) {
            this.oAuthResponseListener = listener;
        }
    }

    public IOAuthResponseListener getOAuthResponseListener() {
        if (oAuthResponseListener == null) {
            synchronized (lock) {
                oAuthResponseListener = new OAuthResponseListener();
            }
        }

        return oAuthResponseListener;
    }

    /**
     * @return the recordCampaignMap
     */
    public Map<String, List<Campaign>> getRecordCampaignMap() {
        return recordCampaignMap;
    }

    /**
     * @param recordCampaignMap the recordCampaignMap to set
     */
    public void setRecordCampaignMap(Map<String, List<Campaign>> recordCampaignMap) {
        this.recordCampaignMap = recordCampaignMap;
    }
}
