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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

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
import io.goobi.viewer.model.security.authentication.AuthResponseListener;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.security.recordlock.RecordLockManager;
import io.goobi.viewer.model.statistics.usage.UsageStatisticsRecorder;
import io.goobi.viewer.model.translations.language.LanguageHelper;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.modules.interfaces.DefaultURLBuilder;
import io.goobi.viewer.modules.interfaces.IURLBuilder;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * <p>
 * DataManager class.
 * </p>
 */
public final class DataManager {

    private static final Logger logger = LogManager.getLogger(DataManager.class);

    private static final Object LOCK = new Object();

    private static final int THREAD_POOL_SIZE = 10;

    private static DataManager instance = null;

    private final List<IModule> modules = new ArrayList<>();

    private final Map<String, Map<String, String>> sessionMap = new LinkedHashMap<>();

    private final RecordLockManager recordLockManager = new RecordLockManager();

    private Configuration configuration;

    private LanguageHelper languageHelper;

    private SolrSearchIndex searchIndex;

    private IDAO dao;

    private SessionStoreBookmarkManager bookmarkManager;

    private AuthResponseListener<OpenIdProvider> oAuthResponseListener;

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

    private ThreadPoolManager threadPoolManager = null;

    /**
     * <p>
     * Getter for the field <code>instance</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.controller.DataManager} object.
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
     * <p>
     * Getter for the field <code>modules</code>.
     * </p>
     *
     * @return the modules
     */
    public List<IModule> getModules() {
        return modules;
    }

    /**
     * <p>
     * getUrlBuilder.
     * </p>
     *
     * @return the urlBuilder
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
     * <p>
     * getModule.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.modules.IModule} object.
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
     * <p>
     * isModuleLoaded.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a boolean.
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
     * <p>
     * registerModule.
     * </p>
     *
     * @param module a {@link io.goobi.viewer.modules.IModule} object.
     * @should not add module if it's already registered
     * @return a boolean.
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
     * <p>
     * getSessionCount.
     * </p>
     *
     * @return a int.
     */
    public int getSessionCount() {
        return sessionMap.size();
    }

    /**
     * <p>
     * Getter for the field <code>sessionMap</code>.
     * </p>
     *
     * @return the sessionMap
     */
    public Map<String, Map<String, String>> getSessionMap() {
        return sessionMap;
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
                configuration = new Configuration(Configuration.CONFIG_FILE_NAME);
            }
        }

        return configuration;
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
                languageHelper = new LanguageHelper("languages.xml");
            }
        }

        return languageHelper;
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
                searchIndex = new SolrSearchIndex(null);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * <p>
     * Getter for the field <code>dao</code>.
     * </p>
     *
     * @return the dao
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
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     *
     * @param searchIndex a {@link io.goobi.viewer.solr.SolrSearchIndex} object.
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }

    /**
     * Sets custom IDAO object (used for unit testing).
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     */
    public void injectDao(IDAO dao) {
        this.dao = dao;
    }

    /**
     * <p>
     * Getter for the field <code>bookmarkManager</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager} object.
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
     * <p>
     * injectBookmarkManager.
     * </p>
     *
     * @param bookmarkManager a {@link io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager} object.
     */
    public void injectBookmarkManager(SessionStoreBookmarkManager bookmarkManager) {
        this.bookmarkManager = bookmarkManager;
    }

    /**
     * <p>
     * injectOAuthResponseListener.
     * </p>
     *
     * @param listener a {@link io.goobi.viewer.model.security.authentication.AuthResponseListener} object.
     */
    public void injectOAuthResponseListener(AuthResponseListener<OpenIdProvider> listener) {
        if (listener != null) {
            this.oAuthResponseListener = listener;
        }
    }

    /**
     * <p>
     * Getter for the field <code>oAuthResponseListener</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.AuthResponseListener} object.
     */
    public AuthResponseListener<OpenIdProvider> getOAuthResponseListener() {
        if (oAuthResponseListener == null) {
            synchronized (LOCK) {
                oAuthResponseListener = new AuthResponseListener<>();
            }
        }

        return oAuthResponseListener;
    }

    /**
     * <p>
     * Getter for the field <code>recordCampaignMap</code>.
     * </p>
     *
     * @return the recordCampaignMap
     */
    public Map<String, List<Campaign>> getRecordCampaignMap() {
        return recordCampaignMap;
    }

    /**
     * <p>
     * Setter for the field <code>recordCampaignMap</code>.
     * </p>
     *
     * @param recordCampaignMap the recordCampaignMap to set
     */
    public void setRecordCampaignMap(Map<String, List<Campaign>> recordCampaignMap) {
        this.recordCampaignMap = recordCampaignMap;
    }

    /**
     * @return the indexerVersion
     */
    public String getIndexerVersion() {
        return indexerVersion;
    }

    /**
     * @param indexerVersion the indexerVersion to set
     */
    public void setIndexerVersion(String indexerVersion) {
        this.indexerVersion = indexerVersion;
    }

    /**
     * @return the connectorVersion
     */
    public String getConnectorVersion() {
        return connectorVersion;
    }

    /**
     * @param connectorVersion the connectorVersion to set
     */
    public void setConnectorVersion(String connectorVersion) {
        this.connectorVersion = connectorVersion;
    }

    /**
     * @return the hotfolderFileCount
     */
    public int getHotfolderFileCount() {
        return hotfolderFileCount;
    }

    /**
     * @param hotfolderFileCount the hotfolderFileCount to set
     */
    public void setHotfolderFileCount(int hotfolderFileCount) {
        this.hotfolderFileCount = hotfolderFileCount;
    }

    /**
     * @return the restApiManager
     */
    public RestApiManager getRestApiManager() {
        if (this.restApiManager == null) {
            this.restApiManager = new RestApiManager(getConfiguration());
        }
        return restApiManager;
    }

    /**
     * @param restApiManager the restApiManager to set
     */
    public void setRestApiManager(RestApiManager restApiManager) {
        this.restApiManager = restApiManager;
    }

    /**
     * @return the recordLockManager
     */
    public RecordLockManager getRecordLockManager() {
        return recordLockManager;
    }

    /**
     * @return the timing
     */
    public TimeAnalysis getTiming() {
        return timing;
    }

    /**
     *
     */
    public void resetTiming() {
        this.timing = new TimeAnalysis();

    }

    public FileResourceManager getFileResourceManager() {
        if (this.fileResourceManager == null) {
            this.fileResourceManager = createFileResourceManager();
        }
        return this.fileResourceManager;
    }

    private FileResourceManager createFileResourceManager() {
        if (FacesContext.getCurrentInstance() != null) {
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String themeName = getConfiguration().getTheme();
            return new FileResourceManager(servletContext, themeName);
        }

        throw new IllegalStateException("Must be called from within faces context");
    }

    /**
     * @return the restApiJobManager
     */
    public TaskManager getRestApiJobManager() {
        return restApiJobManager;
    }

    public ArchiveManager getArchiveManager() {
        if (archiveManager == null) {
            synchronized (LOCK) {
                archiveManager = new ArchiveManager(getSearchIndex(), getConfiguration().getArchiveNodeTypes());
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

    /**
     * 
     * @return the securityManager
     */
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

    public synchronized ThreadPoolManager getThreadPoolManager() {
        if (threadPoolManager == null) {
            this.threadPoolManager = new ThreadPoolManager(THREAD_POOL_SIZE);
        }
        return threadPoolManager;
    }

}
