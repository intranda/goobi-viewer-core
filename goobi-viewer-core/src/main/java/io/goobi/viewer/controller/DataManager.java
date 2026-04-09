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
package io.goobi.viewer.controller;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.monitoring.timer.TimeAnalysis;
import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.dao.impl.JPADAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ModuleMissingException;
import io.goobi.viewer.model.archives.ArchiveManager;
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.iiif.auth.BearerTokenManager;
import io.goobi.viewer.model.security.authentication.AuthResponseListener;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.security.recordlock.RecordLockManager;
import io.goobi.viewer.model.statistics.usage.UsageStatisticsRecorder;
import io.goobi.viewer.model.translations.language.LanguageHelper;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.modules.interfaces.DefaultURLBuilder;
import io.goobi.viewer.modules.interfaces.IURLBuilder;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

/**
 * Application-scoped singleton that acts as the central access point for the DAO, configuration, and other core services.
 */
public final class DataManager {

    private static final Logger logger = LogManager.getLogger(DataManager.class);

    private static final Object LOCK = new Object();

    private static DataManager instance = null;

    private final List<IModule> modules = new ArrayList<>();

    private final Map<String, Map<String, String>> sessionMap = new LinkedHashMap<>();

    private final RecordLockManager recordLockManager = new RecordLockManager();

    private Configuration configuration;

    private LanguageHelper languageHelper;

    private SolrSearchIndex searchIndex;

    private IDAO dao;

    private SessionStoreBookmarkManager bookmarkManager;

    private AuthResponseListener<HttpAuthenticationProvider> authResponseListener;

    private IURLBuilder defaultUrlBuilder = new DefaultURLBuilder();

    private Map<String, List<Campaign>> recordCampaignMap = null;

    private String indexerVersion = "";

    private String connectorVersion = "";

    private int hotfolderFileCount = 0;

    private RestApiManager restApiManager;

    private TimeAnalysis timing = new TimeAnalysis();

    private FileResourceManager fileResourceManager = null;

    private final TaskManager restApiJobManager;

    private ArchiveManager archiveManager = null;

    private ClientApplicationManager clientManager = null;

    private SecurityManager securityManager = null;

    private UsageStatisticsRecorder usageStatisticsRecorder = null;

    private BearerTokenManager bearerTokenManager = null;

    /**
     * Getter for the field <code>instance</code>.
     *
     * @return the singleton DataManager instance
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
        restApiJobManager = new TaskManager(Duration.of(7, ChronoUnit.DAYS));
    }

    /**
     * Getter for the field <code>modules</code>.
     *
     * @return the list of all registered viewer modules
     */
    public List<IModule> getModules() {
        return modules;
    }

    /**
     * getUrlBuilder.
     *
     * @return the URL builder provided by a registered module, or the default URL builder if no module provides one
     */
    public IURLBuilder getUrlBuilder() {
        return getModules().stream()
                .map(IModule::getURLBuilder)
                .filter(Optional::isPresent)
                .map(optional -> optional.get())
                .findFirst()
                .orElse(defaultUrlBuilder);
    }

    /**
     * getModule.
     *
     * @param id unique identifier of the module to look up
     * @return the registered IModule with the given ID
     * @throws io.goobi.viewer.exceptions.ModuleMissingException if any.
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
     * isModuleLoaded.
     *
     * @param id unique identifier of the module to check
     * @return true if a module with the given ID is currently registered, false otherwise
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
     * registerModule.
     *
     * @param module module instance to register
     * @should not add module if it's already registered
     * @return true if the module was successfully registered, false if a module with the same ID is already registered
     */
    public boolean registerModule(IModule module) {
        if (module == null) {
            throw new IllegalArgumentException("module may not be null");
        }

        for (IModule m : modules) {
            if (m.getId().equals(module.getId())) {
                logger.warn(
                        "Module rejected because a module with the same ID is already registered."
                                + "\nRegistered module: {} ({}) v{}\nRejected module: {} ({}) v{}",
                        m.getId(), m.getName(), m.getVersion(), module.getId(), module.getName(), module.getVersion());
                return false;
            }
        }

        modules.add(module);
        logger.info("Module registered: {} ({}) v{}", module.getId(), module.getName(), module.getVersion());
        return true;
    }

    /**
     * getSessionCount.
     *
     * @return a int.
     */
    public int getSessionCount() {
        return sessionMap.size();
    }

    /**
     * Getter for the field <code>sessionMap</code>.
     *
     * @return the map of active HTTP session data, keyed by session ID
     */
    public Map<String, Map<String, String>> getSessionMap() {
        return sessionMap;
    }

    /**
     * Getter for the field <code>configuration</code>.
     *
     * @return the global viewer configuration instance, initialised lazily on first access
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (LOCK) {
                configuration = new Configuration(Configuration.CONFIG_FILE_NAME);
            }
        }

        return configuration;
    }

    /**
     * Getter for the field <code>languageHelper</code>.
     *
     * @return the language helper instance for ISO language code lookups, initialised lazily on first access
     */
    public LanguageHelper getLanguageHelper() {
        if (languageHelper == null) {
            synchronized (LOCK) {
                languageHelper = new LanguageHelper("languages.xml");
            }
        }

        return languageHelper;
    }

    /**
     * Getter for the field <code>searchIndex</code>.
     *
     * @return the Solr search index instance, initialised lazily on first access
     */
    public SolrSearchIndex getSearchIndex() {
        if (searchIndex == null) {
            synchronized (LOCK) {
                searchIndex = new SolrSearchIndex(null);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * Closes the Solr search index client directly, without triggering {@link SolrSearchIndex#checkReloadNeeded()}.
     *
     * <p>Use this during application shutdown instead of {@code getSearchIndex().close()} to prevent
     * a closed client from being silently replaced by a new one.
     *
     * @throws IOException if closing the client fails
     */
    public void closeSearchIndex() throws IOException {
        if (searchIndex != null) {
            searchIndex.close();
        }
    }

    /**
     * Getter for the field <code>dao</code>.
     *
     * @return the data access object for database operations, initialised lazily on first access
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IDAO getDao() throws DAOException {
        if (dao == null) {
            synchronized (LOCK) {
                dao = new JPADAO(getConfiguration().getDbPersistenceUnit());
            }
        }

        return dao;
    }

    /**
     * Sets custom Configuration object (used for unit testing).
     *
     * @param configuration configuration instance to inject
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     *
     * @param searchIndex Solr search index instance to inject
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }

    /**
     * Sets custom IDAO object (used for unit testing).
     *
     * @param dao DAO instance to inject
     */
    public void injectDao(IDAO dao) {
        this.dao = dao;
    }

    /**
     * Getter for the field <code>bookmarkManager</code>.
     *
     * @return the session-scoped bookmark manager, creating one if not yet initialized
     */
    public SessionStoreBookmarkManager getBookmarkManager() {
        if (this.bookmarkManager == null) {
            synchronized (LOCK) {
                this.bookmarkManager = new SessionStoreBookmarkManager();
            }
        }
        return this.bookmarkManager;
    }

    /**
     * injectBookmarkManager.
     *
     * @param bookmarkManager bookmark manager instance to inject
     */
    public void injectBookmarkManager(SessionStoreBookmarkManager bookmarkManager) {
        this.bookmarkManager = bookmarkManager;
    }

    /**
     * injectAuthResponseListener.
     *
     * @param listener authentication response listener to inject
     */
    public void injectAuthResponseListener(AuthResponseListener<HttpAuthenticationProvider> listener) {
        if (listener != null) {
            this.authResponseListener = listener;
        }
    }

    /**
     * Getter for the field <code>authResponseListener</code>.
     *
     * @return the authentication response listener, creating one if not yet initialized
     */
    public AuthResponseListener<HttpAuthenticationProvider> getAuthResponseListener() {
        if (authResponseListener == null) {
            synchronized (LOCK) {
                authResponseListener = new AuthResponseListener<>();
            }
        }

        return authResponseListener;
    }

    /**
     * Getter for the field <code>recordCampaignMap</code>.
     *
     * @return the map of persistent identifiers (PI) to associated crowdsourcing campaigns
     */
    public Map<String, List<Campaign>> getRecordCampaignMap() {
        return recordCampaignMap;
    }

    /**
     * Setter for the field <code>recordCampaignMap</code>.
     *
     * @param recordCampaignMap map of PI to associated crowdsourcing campaigns
     */
    public void setRecordCampaignMap(Map<String, List<Campaign>> recordCampaignMap) {
        this.recordCampaignMap = recordCampaignMap;
    }

    
    public String getIndexerVersion() {
        return indexerVersion;
    }

    /**
     * @param indexerVersion version string of the Goobi viewer indexer
     */
    public void setIndexerVersion(String indexerVersion) {
        this.indexerVersion = indexerVersion;
    }

    
    public String getConnectorVersion() {
        return connectorVersion;
    }

    /**
     * @param connectorVersion version string of the Goobi viewer connector
     */
    public void setConnectorVersion(String connectorVersion) {
        this.connectorVersion = connectorVersion;
    }

    
    public int getHotfolderFileCount() {
        return hotfolderFileCount;
    }

    /**
     * @param hotfolderFileCount number of files currently in the hotfolder
     */
    public void setHotfolderFileCount(int hotfolderFileCount) {
        this.hotfolderFileCount = hotfolderFileCount;
    }

    
    public RestApiManager getRestApiManager() {
        if (this.restApiManager == null) {
            this.restApiManager = new RestApiManager(getConfiguration());
        }
        return restApiManager;
    }

    /**
     * @param restApiManager REST API manager instance to set
     */
    public void setRestApiManager(RestApiManager restApiManager) {
        this.restApiManager = restApiManager;
    }

    
    public RecordLockManager getRecordLockManager() {
        return recordLockManager;
    }

    
    public TimeAnalysis getTiming() {
        return timing;
    }

    
    public void resetTiming() {
        this.timing = new TimeAnalysis();

    }

    public FileResourceManager getFileResourceManager() {
        try {
            return createFileResourceManager();
        } catch (NullPointerException | IllegalStateException e) {
            logger.trace("Cannot create file resource manager: {}", e.getMessage());
            return new FileResourceManager(getConfiguration().getTheme());
        }
    }

    private FileResourceManager createFileResourceManager() {
        if (FacesContext.getCurrentInstance() != null) {
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            if (servletContext != null) {
                String themeName = getConfiguration().getTheme();
                return new FileResourceManager(servletContext, themeName);
            }
        }

        throw new IllegalStateException("Must be called from within faces context");
    }

    
    public TaskManager getRestApiJobManager() {
        return restApiJobManager;
    }

    public ArchiveManager getArchiveManager() {
        if (archiveManager == null) {
            synchronized (LOCK) {
                archiveManager = new ArchiveManager();
            }
        }
        return archiveManager;
    }

    public ClientApplicationManager getClientManager() throws DAOException {
        if (this.clientManager == null) {
            synchronized (LOCK) {
                this.clientManager = new ClientApplicationManager(getDao());
            }
        }
        return this.clientManager;
    }

    
    public SecurityManager getSecurityManager() {
        if (securityManager == null) {
            synchronized (LOCK) {
                securityManager = new SecurityManager();
            }
        }

        return securityManager;
    }

    public void setClientManager(ClientApplicationManager manager) {
        this.clientManager = manager;
    }

    public UsageStatisticsRecorder getUsageStatisticsRecorder() throws DAOException {
        if (usageStatisticsRecorder == null) {
            synchronized (LOCK) {
                usageStatisticsRecorder = new UsageStatisticsRecorder(this.getDao(), this.getConfiguration(), this.getConfiguration().getTheme());
            }
        }

        return usageStatisticsRecorder;
    }

    public void setUsageStatisticsRecorder(UsageStatisticsRecorder usageStatisticsRecorder) {
        this.usageStatisticsRecorder = usageStatisticsRecorder;
    }

    
    public BearerTokenManager getBearerTokenManager() {
        if (bearerTokenManager == null) {
            synchronized (LOCK) {
                bearerTokenManager = new BearerTokenManager();
            }
        }

        return bearerTokenManager;
    }
}
