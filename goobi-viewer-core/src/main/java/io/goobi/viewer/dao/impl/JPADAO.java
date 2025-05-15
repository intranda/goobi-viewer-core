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
package io.goobi.viewer.dao.impl;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.AlphabetIterator;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.MaintenanceMode;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.administration.legal.TermsOfUse;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSProperty;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.HighlightData;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.PublicationStatus;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.upload.UploadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.DownloadTicket;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.statistics.usage.DailySessionUsageStatistics;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.themes.ThemeConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * <p>
 * JPADAO class.
 * </p>
 */
public class JPADAO implements IDAO {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(JPADAO.class);
    private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "intranda_viewer_tomcat";

    private static final String PARAM_STOREMODE = "jakarta.persistence.cache.storeMode";
    private static final String PARAM_STOREMODE_VALUE_REFRESH = "REFRESH";

    private static final String MSG_EXCEPTION_CMS = "Exception \"{}\" when trying to get CMS pages. Returning empty list.";

    static final String QUERY_ELEMENT_AND = " AND ";
    private static final String QUERY_ELEMENT_DESC = " DESC";
    private static final String QUERY_ELEMENT_JOIN = " JOIN ";
    static final String QUERY_ELEMENT_WHERE = " WHERE ";

    static final String MULTIKEY_SEPARATOR = "_";
    static final String KEY_FIELD_SEPARATOR = "-";

    private static final long RETRY_DELAY_MS = 5000;

    /**
     * EntityManagerFactory for the persistence context. Only build once at application startup
     */
    private final EntityManagerFactory factory;
    private Object cmsRequestLock = new Object();
    private Object crowdsourcingRequestLock = new Object();

    /**
     * <p>
     * Constructor for JPADAO.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public JPADAO() throws DAOException {
        this(null);
    }

    /**
     * <p>
     * Constructor for JPADAO.
     * </p>
     *
     * @param inPersistenceUnitName a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public JPADAO(String inPersistenceUnitName) throws DAOException {
        logger.trace("JPADAO({})", inPersistenceUnitName);
        //        logger.debug(System.getProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML));
        //        System.setProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, 
        //        DataManager.getInstance().getConfiguration().getConfigLocalPath() + "persistence.xml");
        //        logger.debug(System.getProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML));
        String persistenceUnitName = inPersistenceUnitName;
        if (StringUtils.isEmpty(persistenceUnitName)) {
            persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
        }
        logger.info("Using persistence unit: {}", persistenceUnitName);
        // Create EntityManagerFactory in a custom class loader
        final Thread currentThread = Thread.currentThread();
        final ClassLoader saveClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(new JPAClassLoader(saveClassLoader));
        factory = Persistence.createEntityManagerFactory(persistenceUnitName);
        currentThread.setContextClassLoader(saveClassLoader);

        int attempts = DataManager.getInstance().getConfiguration().getDatabaseConnectionAttempts() - 1;
        boolean success = init();
        while (!success) {
            if (attempts > 0) {
                logger.warn("Could not connect to database, retrying {} more times...", attempts);
                attempts--;
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
                success = init();
            } else {
                throw new DAOException("DB connection failed.");
            }
        }
    }

    /**
     * @throws DAOException
     * 
     */
    private boolean init() throws DAOException {
        try {
            //Needs to be called for unit tests
            factory.createEntityManager();
            preQuery();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * <p>
     * Getter for the field <code>factory</code>.
     * </p>
     *
     * @return a {@link jakarta.persistence.EntityManagerFactory} object.
     */
    @Override
    public EntityManagerFactory getFactory() {
        return this.factory;
    }

    /**
     * <p>
     * Get a new {@link EntityManager} from the {@link JPADAO#factory}
     *
     * </p>
     *
     * @return {@link jakarta.persistence.EntityManager} for the current thread
     */
    @Override
    public EntityManager getEntityManager() {
        //      em.setFlushMode(FlushModeType.COMMIT);
        return getFactory().createEntityManager();
    }

    /**
     * Operation to call after a query or other kind of transaction is complete
     *
     * @param em
     * @throws DAOException
     */
    @Override
    public void close(EntityManager em) throws DAOException {
        if (em != null && em.isOpen()) {
            em.close();
        } else if (em != null) {
            logger.warn("Attempting to close a closed entityManager");
        }
    }

    /**
     * Call {@link EntityManager#getTransaction() getTransaction()} on the given EntityManager and then {@link EntityTransaction#begin() begin()} on
     * the transaction.
     *
     * @return the transaction gotten from the entity manager
     */
    @Override
    public EntityTransaction startTransaction(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            et.begin();
        } else {
            logger.warn("Attempring to start an already ongoing transaction");
        }
        return et;
    }

    /**
     * Commits a persistence context transaction. Only to be used following a {@link #startTransaction(EntityManager)} call
     */
    @Override
    public void commitTransaction(EntityTransaction et) throws PersistenceException {
        if (et.isActive()) {
            et.commit();
        } else {
            logger.warn("Attempring to commit an inactive transaction");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void commitTransaction(EntityManager em) throws PersistenceException {
        commitTransaction(em.getTransaction());
    }

    /** {@inheritDoc} */
    @Override
    public void handleException(EntityTransaction et) throws PersistenceException {
        if (et.isActive()) {
            et.rollback();
        } else {
            logger.warn("Attempring to roll back an inactive transaction");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleException(EntityManager em) {
        handleException(em.getTransaction());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getAllUsers(boolean refresh) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM User u");
            return q.getResultList();
        } finally {
            close(em);
        }

    }

    /** {@inheritDoc} */
    @Override
    public long getUserCount(Map<String, String> filters) throws DAOException {
        String filterQuery = "";
        Map<String, Object> params = new HashMap<>();
        if (filters != null) {
            String filterValue = filters.values().stream().findFirst().orElse("");
            if (StringUtils.isNotBlank(filterValue)) {
                filterQuery = getUsersFilterQuery("value");
                params.put("value", sanitizeQueryParam(filterValue, true));
            }
        }
        return getFilteredRowCount("User", filterQuery, params);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM User a");
            Map<String, String> params = new HashMap<>();

            if (filters != null) {
                String filterValue = filters.values().stream().findFirst().orElse("");
                if (StringUtils.isNotBlank(filterValue)) {
                    String filterQuery = getUsersFilterQuery("value");
                    params.put("value", sanitizeQueryParam(filterValue, true));
                    sbQuery.append(filterQuery);
                }
            }

            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY a.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            logger.trace(sbQuery);
            Query q = em.createQuery(sbQuery.toString());
            for (Entry<String, String> entry : params.entrySet()) {
                q.setParameter(entry.getKey(), entry.getValue());
            }

            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getUsersByPropertyValue(String propertyName, String propertyValue) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM User a JOIN a.userProperties p WHERE KEY(p) = :key AND VALUE(p) = :value";
            return em.createQuery(query)
                    .setParameter("key", propertyName)
                    .setParameter("value", propertyValue)
                    .setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH)
                    .getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @param param
     * @return Generated query
     */
    public String getUsersFilterQuery(String param) {
        String filterQuery;
        String filterQueryNames =
                "(UPPER(a.firstName) LIKE :%s OR UPPER(a.lastName) LIKE :%s OR UPPER(a.nickName) LIKE :%s OR UPPER(a.email) LIKE :%s)";
        String filterQueryGroup =
                "EXISTS (SELECT role FROM UserRole role LEFT JOIN role.userGroup group WHERE role.user = a AND UPPER(group.name) LIKE :%s)";
        filterQuery = QUERY_ELEMENT_WHERE + filterQueryNames.replace("%s", param) + " OR " + filterQueryGroup.replace("%s", param);
        return filterQuery;
    }

    /**
     *
     * Remove characters from the parameter that may be used to modify the sql query itself. Also puts the parameter to upper case
     *
     * @param param The parameter to sanitize
     * @param addWildCards if true, add '%' to the beginning and end of param
     * @return the sanitized parameter
     */
    private static String sanitizeQueryParam(final String param, boolean addWildCards) {
        String useParam = param.replaceAll("['\"\\(\\)]", "");
        useParam = useParam.toUpperCase();
        if (addWildCards) {
            useParam = "%" + useParam + "%";
        }
        return useParam;
    }

    /** {@inheritDoc} */
    @Override
    public User getUser(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(User.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public User getUserByEmail(String email) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.email) = :email");
            if (email != null) {
                q.setParameter("email", email.toUpperCase());
            }
            try {
                return (User) q.getSingleResult();
            } catch (NonUniqueResultException e) {
                logger.warn(e.getMessage());
                return (User) q.getResultList().get(0);
            }
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public User getUserByOpenId(String identifier) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE :claimed_identifier MEMBER OF u.openIdAccounts");
            q.setParameter("claimed_identifier", identifier);
            q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByNickname(java.lang.String)
     * @should return null if nickname empty
     */
    @Override
    public User getUserByNickname(String nickname) throws DAOException {
        if (StringUtils.isBlank(nickname)) {
            return null;
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.nickName) = :nickname");
            q.setParameter("nickname", nickname.trim().toUpperCase());
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addUser(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(user);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateUser(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(user);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteUser(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            User u = em.getReference(User.class, user.getId());
            em.remove(u);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    // UserGroup

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getAllUserGroups() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ug FROM UserGroup ug");
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM UserGroup o");
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(User owner) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.owner = :owner");
            q.setParameter("owner", owner);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public UserGroup getUserGroup(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(UserGroup.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public UserGroup getUserGroup(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name");
            q.setParameter("name", name);
            return (UserGroup) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(userGroup);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateUserGroup(io.goobi.viewer.model.security.user.UserGroup)
     * @should set id on new license
     */
    @Override
    public boolean updateUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(userGroup);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            UserGroup o = em.getReference(UserGroup.class, userGroup.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (RollbackException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getAllBookmarkLists() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT o FROM BookmarkList o").getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.isPublic=true");
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getBookmarkLists(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.owner = :user ORDER BY o.dateUpdated DESC");
            q.setParameter("user", user);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getBookmarkListCount(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT COUNT(o) FROM BookmarkList o WHERE o.owner = :user");
            q.setParameter("user", user);
            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BookmarkList getBookmarkList(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(BookmarkList.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct row for name
     * @should return correct row for name and user
     * @should return null if no result found
     * 
     */
    @Override
    public BookmarkList getBookmarkList(String name, User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q;
            if (user != null) {
                q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.name = :name AND o.owner = :user");
                q.setParameter("name", name);
                q.setParameter("user", user);
            } else {
                q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.name = :name");
                q.setParameter("name", name);
            }
            try {
                return (BookmarkList) q.getSingleResult();
            } catch (NonUniqueResultException e) {
                return (BookmarkList) q.getResultList().get(0);
            }
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct row
     * @should return null if no result found
     * @should throw IllegalArgumentException if shareKey empty
     */
    @Override
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException {
        if (StringUtils.isEmpty(shareKey)) {
            throw new IllegalArgumentException("shareKey may not be null or empty");

        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.shareKey = :shareKey");
            q.setParameter("shareKey", shareKey);
            return (BookmarkList) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addBookmarkList(BookmarkList bookmarkList) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(bookmarkList);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateBookmarkList(BookmarkList bookmarkList) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(bookmarkList);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteBookmarkList(BookmarkList bookmarkList) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            BookmarkList o = em.getReference(BookmarkList.class, bookmarkList.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (RollbackException e) {
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getAllRoles() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT r FROM Role r");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM Role o");
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Role getRole(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(Role.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Role getRole(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT r FROM Role r WHERE r.name = :name");
            q.setParameter("name", name);
            return (Role) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(role);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(role);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            Role o = em.getReference(Role.class, role.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getAllUserRoles() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ur FROM UserRole ur");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getUserRoleCount(io.goobi.viewer.model.security.user.UserGroup, io.goobi.viewer.model.security.user.User,
     *      io.goobi.viewer.model.security.Role)
     * @should return correct count
     */
    @Override
    public long getUserRoleCount(UserGroup userGroup, User user, Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT COUNT(ur) FROM UserRole ur");
            if (userGroup != null || user != null || role != null) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
                int args = 0;
                if (userGroup != null) {
                    sbQuery.append("ur.userGroup = :userGroup");
                    args++;
                }
                if (user != null) {
                    if (args > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("ur.user = :user");
                    args++;
                }
                if (role != null) {
                    if (args > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("ur.role = :role");
                    args++;
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            // logger.debug(sbQuery.toString());
            if (userGroup != null) {
                q.setParameter("userGroup", userGroup);
            }
            if (user != null) {
                q.setParameter("user", user);
            }
            if (role != null) {
                q.setParameter("role", role);
            }

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT ur FROM UserRole ur");
            if (userGroup != null || user != null || role != null) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
                int args = 0;
                if (userGroup != null) {
                    sbQuery.append("ur.userGroup = :userGroup");
                    args++;
                }
                if (user != null) {
                    if (args > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("ur.user = :user");
                    args++;
                }
                if (role != null) {
                    if (args > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("ur.role = :role");
                    args++;
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            // logger.debug(sbQuery.toString());
            if (userGroup != null) {
                q.setParameter("userGroup", userGroup);
            }
            if (user != null) {
                q.setParameter("user", user);
            }
            if (role != null) {
                q.setParameter("role", role);
            }

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addUserRole(UserRole userRole) throws DAOException {
        logger.trace("addUserRole: {}", userRole);
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(userRole);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateUserRole(UserRole userRole) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(userRole);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteUserRole(UserRole userRole) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            UserRole o = em.getReference(UserRole.class, userRole.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should only return non open access license types
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getRecordLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.core = false");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should filter results correctly
     * @should sort results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=false");
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                for (String key : filterKeys) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getCoreLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=true");
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                for (String key : filterKeys) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public LicenseType getLicenseType(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(LicenseType.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public LicenseType getLicenseType(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.name = :name");
            q.setParameter("name", name);
            return (LicenseType) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getLicenseTypes(java.util.List)
     * @should return all matching rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getLicenseTypes(List<String> names) throws DAOException {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT a FROM LicenseType a WHERE a.name IN :names");
            q.setParameter("names", names);
            return q.getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getOverridingLicenseType(io.goobi.viewer.model.security.LicenseType)
     * @should return all matching rows
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<LicenseType> getOverridingLicenseType(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            return Collections.emptyList();
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT a FROM LicenseType a WHERE :lt MEMBER OF a.overriddenLicenseTypes")
                    .setParameter("lt", licenseType)
                    .getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(licenseType);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(licenseType);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            LicenseType o = em.getReference(LicenseType.class, licenseType.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<License> getAllLicenses() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM License o");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public License getLicense(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.find(License.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getLicenses(io.goobi.viewer.model.security.LicenseType)
     * @should return correct values
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<License> getLicenses(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            throw new IllegalArgumentException("licenseType may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM License a WHERE a.licenseType = :licenseType";
            Query q = em.createQuery(query);
            q.setParameter("licenseType", licenseType);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getLicenseCount(io.goobi.viewer.model.security.LicenseType)
     * @should return correct value
     */
    @Override
    public long getLicenseCount(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            throw new IllegalArgumentException("licenseType may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT COUNT(a) FROM License a WHERE a.licenseType = :licenseType";
            Query q = em.createQuery(query);
            q.setParameter("licenseType", licenseType);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DownloadTicket getDownloadTicket(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.find(DownloadTicket.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DownloadTicket getDownloadTicketByPasswordHash(String passwordHash) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<DownloadTicket> cq = cb.createQuery(DownloadTicket.class);
            Root<DownloadTicket> root = cq.from(DownloadTicket.class);
            cq.select(root).where(cb.equal(root.get("passwordHash"), passwordHash));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct count
     */
    @Override
    public long getActiveDownloadTicketCount(Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM DownloadTicket a");
            Map<String, String> params = new HashMap<>();
            String filterQuery = createFilterQuery(null, filters, params);
            if (StringUtils.isEmpty(filterQuery)) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
            } else {
                sbQuery.append(filterQuery).append(QUERY_ELEMENT_AND);
            }
            // Only tickets that aren't requests
            sbQuery.append("a.passwordHash IS NOT NULL AND a.expirationDate IS NOT NULL");
            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should filter rows correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<DownloadTicket> getActiveDownloadTickets(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM DownloadTicket a");
            Map<String, String> params = new HashMap<>();
            String filterQuery = createFilterQuery(null, filters, params);
            if (StringUtils.isEmpty(filterQuery)) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
            } else {
                sbQuery.append(filterQuery).append(QUERY_ELEMENT_AND);
            }
            // Only tickets that aren't requests
            sbQuery.append("a.passwordHash IS NOT NULL AND a.expirationDate IS NOT NULL");
            if (StringUtils.isNotBlank(sortField)) {
                String[] sortFields = sortField.split("_");
                sbQuery.append(" ORDER BY ");
                for (String sf : sortFields) {
                    sbQuery.append("a.").append(sf);
                    if (descending) {
                        sbQuery.append(QUERY_ELEMENT_DESC);
                    }
                    sbQuery.append(",");
                }
                sbQuery.deleteCharAt(sbQuery.length() - 1);
            }

            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return q.setFirstResult(first).setMaxResults(pageSize).setFlushMode(FlushModeType.COMMIT).getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return tickets that have never been activated
     */
    @Override
    public List<DownloadTicket> getDownloadTicketRequests() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<DownloadTicket> cq = cb.createQuery(DownloadTicket.class);
            Root<DownloadTicket> root = cq.from(DownloadTicket.class);
            cq.select(root).where(cb.and(root.get("passwordHash").isNull(), root.get("expirationDate").isNull()));
            return em.createQuery(cq).getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addDownloadTicket(DownloadTicket downloadTicket) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(downloadTicket);
            commitTransaction(em);
        } catch (PersistenceException e) {
            logger.error(e.getMessage());
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateDownloadTicket(DownloadTicket downloadTicket) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(downloadTicket);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e.getMessage());
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteDownloadTicket(DownloadTicket downloadTicket) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            DownloadTicket o = em.getReference(DownloadTicket.class, downloadTicket.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e.getMessage());
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getAllIpRanges() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ipr FROM IpRange ipr");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM IpRange o");
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                sbQuery.append(QUERY_ELEMENT_WHERE);
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IpRange getIpRange(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.find(IpRange.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IpRange getIpRange(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ipr FROM IpRange ipr WHERE ipr.name = :name");
            q.setParameter("name", name);
            return (IpRange) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(ipRange);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(ipRange);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            IpRange o = em.getReference(IpRange.class, ipRange.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    // CommentGroup

    /**
     * @see io.goobi.viewer.dao.IDAO#getAllCommentGroups()
     * @should return all rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CommentGroup> getAllCommentGroups() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM CommentGroup o");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCommentGroupUnfiltered()
     * @should return correct row
     */
    @Override
    public CommentGroup getCommentGroupUnfiltered() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return (CommentGroup) em.createQuery("SELECT o FROM CommentGroup o WHERE o.coreType = true").setMaxResults(1).getSingleResult();
        } catch (EntityNotFoundException | NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCommentGroup(long)
     */
    @Override
    public CommentGroup getCommentGroup(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(CommentGroup.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#addCommentGroup(io.goobi.viewer.model.annotation.comments.CommentGroup)
     */
    @Override
    public boolean addCommentGroup(CommentGroup commentGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(commentGroup);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e.toString(), e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#updateCommentGroup(io.goobi.viewer.model.annotation.comments.CommentGroup)
     */
    @Override
    public boolean updateCommentGroup(CommentGroup commentGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(commentGroup);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e.toString(), e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#deleteCommentGroup(io.goobi.viewer.model.annotation.comments.CommentGroup)
     */
    @Override
    public boolean deleteCommentGroup(CommentGroup commentGroup) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CommentGroup o = em.getReference(CommentGroup.class, commentGroup.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    // Comment

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getAllComments() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM Comment o");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should sort results correctly
     * @should filter results correctly
     * @should apply target pi filter correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            Set<String> targetPIs) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM Comment a");
            Map<String, String> params = new HashMap<>();
            String filterQuery = createFilterQuery(null, filters, params);
            sbQuery.append(filterQuery);
            if (targetPIs != null && !targetPIs.isEmpty()) {
                if (StringUtils.isEmpty(filterQuery)) {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                } else {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                sbQuery.append("a.targetPI in :targetPIs");
            }
            if (StringUtils.isNotBlank(sortField)) {
                String[] sortFields = sortField.split("_");
                sbQuery.append(" ORDER BY ");
                for (String sf : sortFields) {
                    sbQuery.append("a.").append(sf);
                    if (descending) {
                        sbQuery.append(QUERY_ELEMENT_DESC);
                    }
                    sbQuery.append(",");
                }
                sbQuery.deleteCharAt(sbQuery.length() - 1);
            }

            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
            if (targetPIs != null && !targetPIs.isEmpty()) {
                q.setParameter("targetPIs", targetPIs);
            }
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.setFirstResult(first).setMaxResults(pageSize).setFlushMode(FlushModeType.COMMIT).getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should sort correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsOfUser(User user, int maxResults, String sortField, boolean descending) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(80);
            sbQuery.append("SELECT o FROM Comment o WHERE o.creatorId = :owner");
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("owner", user.getId());
            return q.setMaxResults(maxResults).getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForPage(String pi, int page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(80);
            sbQuery.append("SELECT o FROM Comment o WHERE o.targetPI = :pi AND o.targetPageOrder = :page");
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            q.setParameter("page", page);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForWork(String pi) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(80);
            sbQuery.append("SELECT o FROM Comment o WHERE o.targetPI = :pi");
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Comment getComment(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(Comment.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(comment);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e.toString(), e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(comment);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            Comment o = em.getReference(Comment.class, comment.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should update rows correctly
     */
    @Override
    public int changeCommentsOwner(User fromUser, User toUser) throws DAOException {
        if (fromUser == null || fromUser.getId() == null) {
            throw new IllegalArgumentException("fromUser may not be null or not yet persisted");
        }
        if (toUser == null || toUser.getId() == null) {
            throw new IllegalArgumentException("fromUser may not be null or not yet persisted");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            int rows = em.createQuery("UPDATE Comment o set o.creatorId = :newOwner WHERE o.creatorId = :oldOwner")
                    .setParameter("oldOwner", fromUser.getId())
                    .setParameter("newOwner", toUser.getId())
                    .executeUpdate();
            commitTransaction(em);

            return rows;
        } catch (PersistenceException e) {
            handleException(em);
            return 0;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#deleteComments(java.lang.String, io.goobi.viewer.model.security.user.User)
     * @should delete comments for pi correctly
     * @should delete comments for user correctly
     * @should delete comments for pi and user correctly
     * @should not delete anything if both pi and creator are null
     */
    @Override
    public int deleteComments(String pi, User owner) throws DAOException {
        if (StringUtils.isEmpty(pi) && owner == null) {
            return 0;
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {

            // Fetch relevant IDs
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append("DELETE FROM Comment o WHERE ");
            if (StringUtils.isNotEmpty(pi)) {
                sbQuery.append("o.targetPI = :pi");
            }
            if (owner != null) {
                if (StringUtils.isNotEmpty(pi)) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                sbQuery.append("o.creatorId = :creatorId");
            }

            Query q = em.createQuery(sbQuery.toString());
            if (StringUtils.isNotEmpty(pi)) {
                q.setParameter("pi", pi);
            }
            if (owner != null) {
                q.setParameter("creatorId", owner.getId());
            }
            startTransaction(em);
            int rows = q.executeUpdate();
            commitTransaction(em);
            return rows;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Gets all page numbers (order) within a work with the given pi which contain comments
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getPagesWithComments(String pi) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(80);
            sbQuery.append("SELECT o.targetPageOrder FROM Comment o WHERE o.targetPI = :pi");
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            q.setFlushMode(FlushModeType.COMMIT);
            q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            List<Integer> results = q.getResultList();
            return results.stream().distinct().sorted().collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getAllSearches() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM Search o");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(50);
            sbQuery.append("SELECT COUNT(o) FROM Search o");
            if (owner != null) {
                sbQuery.append(" WHERE o.owner = :owner");
            }
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                if (owner == null) {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                } else {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (owner != null) {
                q.setParameter("owner", owner);
            }
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(50);
            sbQuery.append("SELECT o FROM Search o");
            if (owner != null) {
                sbQuery.append(" WHERE o.owner = :owner");
            }
            List<String> filterKeys = new ArrayList<>();
            if (filters != null && !filters.isEmpty()) {
                if (owner == null) {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                } else {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            if (owner != null) {
                q.setParameter("owner", owner);
            }
            if (filters != null) {
                for (String key : filterKeys) {
                    q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
                }
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT o FROM Search o WHERE o.owner = :owner";
            Query q = em.createQuery(query);
            q.setParameter("owner", owner);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Search getSearch(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.find(Search.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addSearch(Search search) throws DAOException {
        logger.debug("addSearch: {}", search.getQuery());
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(search);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateSearch(Search search) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(search);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteSearch(Search search) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            Search o = em.getReference(Search.class, search.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    // Downloads

    /**
     * {@inheritDoc}
     *
     * @should return all objects
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<DownloadJob> getAllDownloadJobs() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM DownloadJob o");
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<DownloadJob> getDownloadJobsForPi(String pi) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT o FROM DownloadJob o WHERE o.pi = :pi").setParameter("pi", pi).getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJob(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(DownloadJob.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJobByIdentifier(String identifier) throws DAOException {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append("SELECT o FROM DownloadJob o WHERE o.identifier = :identifier");
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("identifier", identifier);
            q.setMaxResults(1);
            return (DownloadJob) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJobByMetadata(String type, String pi, String logId) throws DAOException {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append("SELECT o FROM DownloadJob o WHERE o.type = :type AND o.pi = :pi");
            if (logId != null) {
                sbQuery.append(" AND o.logId = :logId");
            }
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("type", type);
            q.setParameter("pi", pi);
            if (logId != null) {
                q.setParameter("logId", logId);
            }
            q.setMaxResults(1);
            return (DownloadJob) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(downloadJob);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException {
        logger.trace("updateDownloadJob: {}", downloadJob.getId());
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(downloadJob);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            DownloadJob o = em.getReference(DownloadJob.class, downloadJob.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    // UploadJob

    /**
     * {@inheritDoc}
     * 
     * @should return rows with given status
     */
    @Override
    public List<UploadJob> getUploadJobsWithStatus(JobStatus status) throws DAOException {
        if (status == null) {
            throw new IllegalArgumentException("status may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UploadJob> cq = cb.createQuery(UploadJob.class);
            Root<UploadJob> root = cq.from(UploadJob.class);
            cq.select(root).where(cb.equal(root.get("status"), status));
            return em.createQuery(cq).getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return rows in correct order
     * @should return empty list if creatorId null
     */
    @Override
    public List<UploadJob> getUploadJobsForCreatorId(Long creatorId) throws DAOException {
        if (creatorId == null) {
            return Collections.emptyList();
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UploadJob> cq = cb.createQuery(UploadJob.class);
            Root<UploadJob> root = cq.from(UploadJob.class);
            cq.select(root).where(cb.equal(root.get("creatorId"), creatorId)).orderBy(cb.desc(root.get("dateCreated")));

            return em.createQuery(cq).getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addUploadJob(UploadJob uploadJob) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(uploadJob);
            commitTransaction(em);
        } catch (PersistenceException e) {
            logger.trace(e.getMessage(), e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateUploadJob(UploadJob uploadJob) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(uploadJob);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteUploadJob(UploadJob uploadJob) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            UploadJob o = em.getReference(UploadJob.class, uploadJob.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getAllCMSPages() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSPage o");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error(MSG_EXCEPTION_CMS, e.getMessage());
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSPage o WHERE o.staticPageName = :pageName");
                q.setParameter("pageName", pageName);
                q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
                if (!q.getResultList().isEmpty()) {
                    return (CMSPage) q.getSingleResult();
                }
            } finally {
                close(em);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            List<Long> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSPage a");
                StringBuilder order = new StringBuilder();

                Map<String, Object> params = new HashMap<>();

                String filterString = createFilterQuery2(null, filters, params);
                String rightsFilterString = "";
                try {
                    rightsFilterString = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
                    if (!rightsFilterString.isEmpty()) {
                        rightsFilterString = (StringUtils.isBlank(filterString) ? QUERY_ELEMENT_WHERE : QUERY_ELEMENT_AND) + rightsFilterString;
                    }
                } catch (AccessDeniedException e) {
                    //may not request any cms pages at all
                    return Collections.emptyList();
                }

                if (StringUtils.isNotEmpty(sortField)) {
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(QUERY_ELEMENT_DESC);
                    }
                }
                sbQuery.append(filterString).append(rightsFilterString).append(order);

                logger.trace("CMS page query: {}", sbQuery);
                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);

                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error(MSG_EXCEPTION_CMS, e.getMessage());
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, LocalDateTime fromDate, LocalDateTime toDate)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM CMSPage o WHERE o.relatedPI IS NOT NULL AND o.relatedPI <> ''");
            if (fromDate != null) {
                sbQuery.append(QUERY_ELEMENT_AND).append("o.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(QUERY_ELEMENT_AND).append("o.dateUpdated <= :toDate");
            }
            sbQuery.append(" GROUP BY o.relatedPI ORDER BY o.dateUpdated DESC");
            Query q = em.createQuery(sbQuery.toString());
            if (fromDate != null) {
                q.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                q.setParameter("toDate", toDate);
            }
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCMSPagesForRecordHaveUpdates(String pi, CMSCategory category, LocalDateTime fromDate, LocalDateTime toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM CMSPage o WHERE o.relatedPI = :pi");
            if (fromDate != null) {
                sbQuery.append(" AND o.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(" AND o.dateUpdated <= :toDate");
            }
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            if (fromDate != null) {
                q.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                q.setParameter("toDate", toDate);
            }
            q.setMaxResults(1);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return (long) q.getSingleResult() != 0;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getCMSPageWithRelatedPiCount(LocalDateTime fromDate, LocalDateTime toDate) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery =
                    new StringBuilder("SELECT COUNT(DISTINCT o.relatedPI) FROM CMSPage o WHERE o.relatedPI IS NOT NULL AND o.relatedPI <> ''");
            if (fromDate != null) {
                sbQuery.append(QUERY_ELEMENT_AND).append("o.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(QUERY_ELEMENT_AND).append("o.dateUpdated <= :toDate");
            }
            Query q = em.createQuery(sbQuery.toString());
            if (fromDate != null) {
                q.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                q.setParameter("toDate", toDate);
            }

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) o).longValue();
            }
            // H2
            return (long) o;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct result
     */
    @Override
    public CMSPage getCMSPageDefaultViewForRecord(String pi) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em
                    .createQuery(
                            "SELECT o FROM CMSPage o WHERE o.relatedPI = :pi AND o.useAsDefaultRecordView = true"
                                    + " AND o.publicationStatus = :publicationStatus")
                    .setParameter("pi", pi)
                    .setParameter("publicationStatus", PublicationStatus.PUBLISHED)
                    .setMaxResults(1);

            return (CMSPage) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getCMSPageAccessConditions() throws DAOException {
        return getNativeQueryResults("SELECT property_value FROM cms_properties WHERE property_key = '" + CMSProperty.KEY_ACCESS_CONDITION + "'");
    }

    /** {@inheritDoc} */
    @Override
    public CMSPage getCMSPage(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            logger.trace("getCMSPage: {}", id);
            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.getReference(CMSPage.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.cmsPage = :page");
                q.setParameter("page", page);
                return q.getResultList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(page);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error adding cmsPage to database", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.merge(page);
                commitTransaction(em);
                return true;
            } catch (PersistenceException | NullPointerException e) {
                logger.error("Error saving page ", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                CMSPage o = em.getReference(CMSPage.class, page.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSComponent(PersistentCMSComponent component) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                PersistentCMSComponent o = em.getReference(PersistentCMSComponent.class, component.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error deleting cms component", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSContent(CMSContent content) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                CMSContent o = em.getReference(CMSContent.class, content.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error deleting cms component", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSComponent(PersistentCMSComponent persistentCMSComponent) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(persistentCMSComponent);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error adding cmsPage to database", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updatedCMSComponent(PersistentCMSComponent persistentCMSComponent) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.merge(persistentCMSComponent);
                commitTransaction(em);
                return true;
            } catch (PersistenceException | NullPointerException e) {
                logger.error("Error saving page ", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public PersistentCMSComponent getCMSComponent(Long id) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.getReference(PersistentCMSComponent.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o");
                q.setFlushMode(FlushModeType.COMMIT);
                q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error(MSG_EXCEPTION_CMS, e.toString());
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o WHERE o.collection = true");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error(MSG_EXCEPTION_CMS, e.toString());
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getCMSMediaItemByFilename(String filename) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o WHERE o.fileName = :fileName");
                q.setParameter("fileName", filename);
                // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
                return (CMSMediaItem) q.getSingleResult();
            } catch (NoResultException e) {
                //nothing found; no biggie
                return null;
            } catch (PersistenceException e) {
                logger.error("Exception \"{}\" when trying to get CMS media item with filename '{}'", e.toString(), filename);
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.getReference(CMSMediaItem.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(item);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.merge(item);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                CMSMediaItem o = em.getReference(CMSMediaItem.class, item.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSMediaItem> getCMSMediaItemsByCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em
                    .createQuery("SELECT DISTINCT media FROM CMSMediaItem media JOIN media.categories category WHERE category.id = :id");
            q.setParameter("id", category.getId());
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.parentItem IS NULL");
                q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
                q.setFlushMode(FlushModeType.COMMIT);
                List<CMSNavigationItem> list = q.getResultList();
                Collections.sort(list);
                return list;
            } catch (PersistenceException e) {
                logger.error(MSG_EXCEPTION_CMS, e.toString());
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.find(CMSNavigationItem.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(item);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.merge(item);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                CMSNavigationItem o = em.getReference(CMSNavigationItem.class, item.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    // Transkribus

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM TranskribusJob o");
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder(80);
            sbQuery.append("SELECT o FROM TranskribusJob o");
            int filterCount = 0;
            if (pi != null) {
                sbQuery.append(" WHERE o.pi = :pi");
                filterCount++;
            }
            if (transkribusUserId != null) {
                if (filterCount == 0) {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                } else {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                sbQuery.append("o.ownerId = :ownerId");
                filterCount++;
            }
            if (status != null) {
                if (filterCount == 0) {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                } else {
                    sbQuery.append(QUERY_ELEMENT_AND);
                }
                sbQuery.append("o.status = :status");
                filterCount++;
            }
            Query q = em.createQuery(sbQuery.toString());
            if (pi != null) {
                q.setParameter("pi", pi);
            }
            if (transkribusUserId != null) {
                q.setParameter("ownerId", transkribusUserId);
            }
            if (status != null) {
                q.setParameter("status", status);
            }
            return q.getResultList();
        } finally {
            close(em);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(job);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(job);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            TranskribusJob o = em.getReference(TranskribusJob.class, job.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Campaign> getAllCampaigns() throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM Campaign o");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error(e.toString());
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getCampaignCount(Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM Campaign a");
            Map<String, Object> params = new HashMap<>();
            Query q = em.createQuery(sbQuery.append(createCampaignsFilterQuery(null, filters, params)).toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Campaign> getCampaigns(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM Campaign a");
                StringBuilder order = new StringBuilder();
                Map<String, Object> params = new HashMap<>();

                String filterString = createCampaignsFilterQuery(null, filters, params);
                if (StringUtils.isNotEmpty(sortField)) {
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(QUERY_ELEMENT_DESC);
                    }
                }
                sbQuery.append(filterString).append(order);

                logger.trace(sbQuery);
                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"{}\" when trying to get CS comaigns. Returning empty list.", e.toString());
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Campaign getCampaign(Long id) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.getReference(Campaign.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public Question getQuestion(Long id) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                return em.getReference(Question.class, id);
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                String query = "SELECT a FROM CampaignRecordStatistic a WHERE a.pi = :pi";
                if (status != null) {
                    query += " AND a.status = :status";
                }
                Query q = em.createQuery(query);
                q.setParameter("pi", pi);
                if (status != null) {
                    q.setParameter("status", status);
                }
                // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"{}\" when trying to get CS campaigns. Returning empty list.", e.toString());
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CampaignRecordPageStatistic> getCampaignPageStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                String query = "SELECT a FROM CampaignRecordPageStatistic a WHERE a.pi = :pi";
                if (status != null) {
                    query += " AND a.status = :status";
                }
                Query q = em.createQuery(query);
                q.setParameter("pi", pi);
                if (status != null) {
                    q.setParameter("status", status);
                }
                // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"{}\" when trying to get CS campaigns. Returning empty list.", e.toString());
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCampaign(Campaign campaign) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(campaign);
                commitTransaction(em);
                return true;
            } catch (RollbackException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCampaign(Campaign campaign) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                Campaign c = em.merge(campaign);
                commitTransaction(em);
                //solrQueryResults remains unchanged in managed campaign even after merge. Manually reset results to account for changed solrquery
                c.resetSolrQueryResults();
                return true;
            } catch (RollbackException e) {
                handleException(em);
                throw new PersistenceException("Failed to persist campaign " + campaign, e);
            } finally {
                close(em);
            }
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteCampaign(io.goobi.viewer.model.crowdsourcing.campaigns.Campaign)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteCampaign(Campaign campaign) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                Campaign o = em.getReference(Campaign.class, campaign.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteCampaignStatisticsForUser(io.goobi.viewer.model.security.user.User)
     * @should remove user from creators and reviewers lists correctly
     */
    @Override
    public int deleteCampaignStatisticsForUser(User user) throws DAOException {
        if (user == null) {
            return 0;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            int rows = em
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_statistic_annotators WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_page_statistic_annotators WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_statistic_reviewers WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_page_statistic_reviewers WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            commitTransaction(em);
            return rows;
        } catch (PersistenceException e) {
            handleException(em);
            return 0;
        } finally {
            em.close();
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#changeCampaignStatisticContributors(io.goobi.viewer.model.security.user.User,
     *      io.goobi.viewer.model.security.user.User)
     * @should replace user in creators and reviewers lists correctly
     */
    @Override
    public int changeCampaignStatisticContributors(User fromUser, User toUser) throws DAOException {
        if (fromUser == null || toUser == null) {
            return 0;
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            int rows = em
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_statistic_annotators SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_page_statistic_annotators SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_statistic_reviewers SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += em
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_page_statistic_reviewers SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            commitTransaction(em);
            return rows;
        } catch (PersistenceException e) {
            handleException(em);
            return 0;
        } finally {
            close(em);
        }

    }

    @Override
    public boolean checkAvailability() {
        try {
            getRole(1);
            return true;
        } catch (DAOException | PersistenceException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * currently noop since no persistence entity manager is kept
     */
    public void clear() {
        //noop
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {

        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }

    /**
     * <p>
     * Operation to call before getting an entity manager. currently noop
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void preQuery() throws DAOException {
        //NOOP
    }

    /** {@inheritDoc} */
    @Override
    public long getUserGroupCount(Map<String, String> filters) throws DAOException {
        return getRowCount("UserGroup", null, filters);
    }

    /** {@inheritDoc} */
    @Override
    public long getRoleCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Role", null, filters);
    }

    /** {@inheritDoc} */
    @Override
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("LicenseType", " WHERE a.core=false", filters);
    }

    /** {@inheritDoc} */
    @Override
    public long getCoreLicenseTypeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("LicenseType", " WHERE a.core=true", filters);
    }

    /** {@inheritDoc} */
    @Override
    public long getIpRangeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("IpRange", null, filters);
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct count
     * @should filter correctly
     * @should filter for users correctly
     * @should apply target pi filter correctly
     */
    @Override
    public long getCommentCount(Map<String, String> filters, User owner, Set<String> targetPIs) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM Comment a");
            Map<String, String> params = new HashMap<>();
            String filterQuery = createFilterQuery(null, filters, params);
            boolean where = StringUtils.isNotEmpty(filterQuery);
            sbQuery.append(filterQuery);
            if (owner != null) {
                if (where) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                } else {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                    where = true;
                }
                sbQuery.append("a.creatorId = :owner");
            }
            if (targetPIs != null && !targetPIs.isEmpty()) {
                if (where) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                } else {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                    where = true;
                }
                sbQuery.append("a.targetPI in :targetPIs");
            }
            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
            if (owner != null) {
                q.setParameter("owner", owner.getId());
            }
            if (targetPIs != null && !targetPIs.isEmpty()) {
                q.setParameter("targetPIs", targetPIs);
            }

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getCMSPageCount(Map<String, String> filters, List<Long> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(DISTINCT a) FROM CMSPage").append(" a");
            Map<String, Object> params = new HashMap<>();
            sbQuery.append(createFilterQuery2(null, filters, params));
            try {
                String rightsFilter = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
                if (!rightsFilter.isEmpty()) {
                    if (filters.values().stream().anyMatch(StringUtils::isNotBlank)) {
                        sbQuery.append(QUERY_ELEMENT_AND);
                    } else {
                        sbQuery.append(QUERY_ELEMENT_WHERE);
                    }
                    sbQuery.append("(").append(createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories)).append(")");
                }
            } catch (AccessDeniedException e) {
                return 0;
            }
            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    @Override
    public long getCMSPageCountByPropertyValue(String propertyName, String propertyValue) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT COUNT(DISTINCT a) FROM CMSPage a JOIN a.properties p WHERE p.key = :key AND p.value = :value";
            return (long) em.createQuery(query)
                    .setParameter("key", propertyName)
                    .setParameter("value", propertyValue)
                    .setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH)
                    .getSingleResult();
        } finally {
            close(em);
        }
    }

    /**
     * Universal method for returning the row count for the given class and filter string.
     *
     * @param className
     * @param filter Filter query string
     * @param params
     * @return Number of rows matching given filters
     * @throws DAOException
     */
    private long getFilteredRowCount(String className, String filter, Map<String, Object> params) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(DISTINCT a) FROM ").append(className).append(" a").append(" ").append(filter);
            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /**
     * Universal method for returning the row count for the given class and filters.
     *
     * @param className
     * @param staticFilterQuery Optional filter query in case the fuzzy filters aren't sufficient
     * @param filters
     * @return Number of rows matching given filters
     * @throws DAOException
     */
    private long getRowCount(String className, String staticFilterQuery, Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM ").append(className).append(" a");
            Map<String, String> params = new HashMap<>();
            Query q = em.createQuery(sbQuery.append(createFilterQuery(staticFilterQuery, filters, params)).toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSStaticPage> getAllStaticPages() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM CMSStaticPage o");
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public boolean addStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(page);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(page);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSStaticPage o = em.getReference(CMSStaticPage.class, page.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (RollbackException | EntityNotFoundException e) {
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException, NonUniqueResultException {
        if (page == null || page.getId() == null) {
            return Collections.emptyList();
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.cmsPageId = :id");
            q.setParameter("id", page.getId());
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.pageName = :name");
            q.setParameter("name", pageType.getName());
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return getSingleResult(q);
        } finally {
            close(em);
        }
    }

    /**
     * Helper method to get the only result of a query. In contrast to {@link jakarta.persistence.Query#getSingleResult()} this does not throw an
     * exception if no results are found. Instead, it returns an empty Optional
     * 
     * @param <T>
     * @param q the query to perform
     * @return an Optional containing the query result, or an empty Optional if no results are present
     * @throws ClassCastException if the first result cannot be cast to the expected type
     * @throws NonUniqueResultException if the query matches more than one result
     */
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getSingleResult(Query q) throws ClassCastException, NonUniqueResultException {
        List<Object> results = q.getResultList();
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        } else if (results.size() > 1) {
            throw new NonUniqueResultException("Query found " + results.size() + " results instead of only one");
        } else {
            return Optional.ofNullable((T) results.get(0));
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field");
                q.setParameter("field", solrField);
                return q.getResultList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(collection);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(collection);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field AND c.solrFieldValue = :value");
            q.setParameter("field", solrField);
            q.setParameter("value", solrFieldValue);
            return (CMSCollection) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSCollection u = em.getReference(CMSCollection.class, collection.getId());
            em.remove(u);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id");
            q.setParameter("id", category.getId());
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesForSubtheme(String subtheme) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT DISTINCT page FROM CMSPage page WHERE page.subThemeDiscriminatorValue = :subtheme");
            q.setParameter("subtheme", subtheme);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q;
            if (category != null) {
                q = em.createQuery(
                        "SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id AND page.relatedPI = :pi");
                q.setParameter("id", category.getId());
            } else {
                StringBuilder sbQuery = new StringBuilder(70);
                sbQuery.append("SELECT o from CMSPage o WHERE o.relatedPI='").append(pi).append("'");

                q = em.createQuery("SELECT page FROM CMSPage page WHERE page.relatedPI = :pi");
            }
            q.setParameter("pi", pi);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * <p>
     * createCMSPageFilter.
     * </p>
     *
     * @param params a {@link java.util.Map} object.
     * @param pageParameter a {@link java.lang.String} object.
     * @param allowedTemplates a {@link java.util.List} object.
     * @param allowedSubthemes a {@link java.util.List} object.
     * @param allowedCategoryIds a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     */
    public static String createCMSPageFilter(Map<String, Object> params, String pageParameter, List<Long> allowedTemplates,
            List<String> allowedSubthemes, List<String> allowedCategoryIds) throws AccessDeniedException {

        String query = "";

        int index = 0;
        if (allowedTemplates != null && !allowedTemplates.isEmpty()) {
            query += "(";

            StringBuilder sbQueryInner = new StringBuilder();
            for (Long template : allowedTemplates) {
                String templateParameter = "tpl" + ++index;

                sbQueryInner.append(":").append(templateParameter).append(" = ").append(pageParameter).append(".templateId").append(" OR ");
                params.put(templateParameter, template);
            }
            query += sbQueryInner.toString();
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ") AND";
        } else if (allowedTemplates != null) {
            throw new AccessDeniedException("User may not view pages with any templates");
        }

        index = 0;
        if (allowedSubthemes != null && !allowedSubthemes.isEmpty()) {
            query += " (";
            StringBuilder sbQueryInner = new StringBuilder();
            for (String subtheme : allowedSubthemes) {
                String templateParameter = "thm" + ++index;
                sbQueryInner.append(":")
                        .append(templateParameter)
                        .append(" = ")
                        .append(pageParameter)
                        .append(".subThemeDiscriminatorValue")
                        .append(" OR ");
                params.put(templateParameter, subtheme);
            }
            query += sbQueryInner.toString();
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ") AND";
        } else if (allowedSubthemes != null) {
            query += " (" + pageParameter + ".subThemeDiscriminatorValue = \"\") AND";
        }

        index = 0;
        if (allowedCategoryIds != null && !allowedCategoryIds.isEmpty()) {
            query += " (";
            StringBuilder sbQueryInner = new StringBuilder();
            for (String category : allowedCategoryIds) {
                String templateParameter = "cat" + ++index;
                sbQueryInner.append(":")
                        .append(templateParameter)
                        .append(" IN (SELECT c.id FROM ")
                        .append(pageParameter)
                        .append(".categories c)")
                        .append(" OR ");
                params.put(templateParameter, category);
            }
            query += sbQueryInner.toString();
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ")";
        } else if (allowedCategoryIds != null) {
            query += " (SELECT COUNT(c) FROM " + pageParameter + ".categories c = 0)";
        }
        if (query.endsWith(" AND")) {
            query = query.substring(0, query.length() - 4);
        }

        return query.trim();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSCategory> getAllCategories() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM CMSCategory c ORDER BY c.name");
            q.setFlushMode(FlushModeType.COMMIT);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct value
     */
    @Override
    public long getCountPagesUsingCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT COUNT(o) FROM CMSPage o WHERE :category MEMBER OF o.categories");
            q.setParameter("category", category);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct value
     */
    @Override
    public long getCountMediaItemsUsingCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT COUNT(o) FROM CMSMediaItem o WHERE :category MEMBER OF o.categories");
            q.setParameter("category", category);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Persist a new {@link CMSCategory} object
     */
    @Override
    public boolean addCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(category);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Update an existing {@link CMSCategory} object in the persistence context
     */
    @Override
    public boolean updateCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(category);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delete a {@link CMSCategory} object from the persistence context
     */
    @Override
    public boolean deleteCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSCategory o = em.getReference(CMSCategory.class, category.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Search the persistence context for a {@link CMSCategory} with the given name.
     */
    @Override
    public CMSCategory getCategoryByName(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM CMSCategory c WHERE c.name = :name");
            q.setParameter("name", name);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return (CMSCategory) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Search the persistence context for a {@link CMSCategory} with the given unique id.
     */
    @Override
    public CMSCategory getCategory(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM CMSCategory c WHERE c.id = :id");
            q.setParameter("id", id);
            return (CMSCategory) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Check if the database contains a table of the given name. Used by backward-compatibility routines
     *
     * @throws SQLException
     */
    @Override
    public boolean tableExists(String tableName) throws DAOException, SQLException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            Connection connection = em.unwrap(Connection.class);
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(connection.getCatalog(), connection.getSchema(), tableName, null)) {
                return tables.next();
            } finally {
                commitTransaction(em);
            }
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Check if the database contains a column in a table with the given names. Used by backward-compatibility routines
     */
    @Override
    public boolean columnsExists(String tableName, String columnName) throws SQLException, DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            Connection connection = em.unwrap(Connection.class);
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(connection.getCatalog(), connection.getSchema(), tableName, columnName)) {
                return columns.next();
            } finally {
                commitTransaction(em);
            }
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CrowdsourcingAnnotation getAnnotation(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT a FROM CrowdsourcingAnnotation a WHERE a.id = :id");
            q.setParameter("id", id);
            return (CrowdsourcingAnnotation) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct rows
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM CrowdsourcingAnnotation a");
            if (!campaign.getQuestions().isEmpty()) {
                sbQuery.append(" WHERE (");
                int count = 1;
                for (Question question : campaign.getQuestions()) {
                    if (count > 1) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("a.generatorId = :questionId_").append(count);
                    count++;
                }
                sbQuery.append(")");
            }
            Query q = em.createQuery(sbQuery.toString());
            int count = 1;
            for (Question question : campaign.getQuestions()) {
                q.setParameter("questionId_" + count, question.getId());
                count++;
            }
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Get all annotations associated with the work of the given pi
     *
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForWork(String pi) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi";
            Query q = em.createQuery(query);
            q.setParameter("pi", pi);

            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should sort correctly
     * @should throw IllegalArgumentException if sortField unknown
     */
    @Override
    public List<CrowdsourcingAnnotation> getAllAnnotations(String sortField, boolean descending) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<CrowdsourcingAnnotation> cq = cb.createQuery(CrowdsourcingAnnotation.class);
            Root<CrowdsourcingAnnotation> root = cq.from(CrowdsourcingAnnotation.class);
            cq.select(root);
            if (StringUtils.isNotEmpty(sortField)) {
                if (!CrowdsourcingAnnotation.VALID_COLUMNS_FOR_ORDER_BY.contains(sortField)) {
                    throw new IllegalArgumentException("Sorting field not allowed: " + sortField);
                }
                if (descending) {
                    cq.orderBy(cb.desc(root.get(sortField)));
                } else {
                    cq.orderBy(cb.asc(root.get(sortField)));
                }
            }

            return em.createQuery(cq).getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getTotalAnnotationCount() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT COUNT(a) FROM CrowdsourcingAnnotation a";
            Query q = em.createQuery(query);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAllAnnotationsByMotivation(String motivation) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM CrowdsourcingAnnotation a WHERE a.motivation = :motivation";
            Query q = em.createQuery(query);
            q.setParameter("motivation", motivation);

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCountForWork(String pi) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT COUNT(a) FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi";
            Query q = em.createQuery(query);
            q.setParameter("pi", pi);

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page) throws DAOException {
        return getAnnotationsForTarget(pi, page, null);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page, String motivation) throws DAOException {

        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder query = new StringBuilder("SELECT a FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi");
            if (page != null) {
                query.append(" AND a.targetPageOrder = :page");
            } else {
                query.append(" AND a.targetPageOrder IS NULL");
            }
            if (StringUtils.isNotBlank(motivation)) {
                query.append(" AND a.motivation =  + :motivation");
            }
            Query q = em.createQuery(query.toString());
            q.setParameter("pi", pi);
            if (page != null) {
                q.setParameter("page", page);
            }
            if (StringUtils.isNotBlank(motivation)) {
                q.setParameter("motivation", motivation);
            }

            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCountForTarget(String pi, Integer page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder query = new StringBuilder("SELECT COUNT(a) FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi");
            if (page != null) {
                query.append(" AND a.targetPageOrder = :page");
            } else {
                query.append(" AND a.targetPageOrder IS NULL");
            }
            Query q = em.createQuery(query.toString());
            q.setParameter("pi", pi);
            if (page != null) {
                q.setParameter("page", page);
            }

            Object o = q.getResultList().get(0);
            // MySQL
            if (o instanceof BigInteger) {
                return ((BigInteger) q.getResultList().get(0)).longValue();
            }
            // H2
            return (long) q.getResultList().get(0);
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct rows
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi");
            if (!campaign.getQuestions().isEmpty()) {
                sbQuery.append(QUERY_ELEMENT_AND).append("(");
                int count = 1;
                for (Question question : campaign.getQuestions()) {
                    if (count > 1) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("a.generatorId = :questionId_").append(count);
                    count++;
                }
                sbQuery.append(")");
            }
            Query q = em.createQuery(sbQuery.toString());
            if (!campaign.getQuestions().isEmpty()) {
                int count = 1;
                for (Question question : campaign.getQuestions()) {
                    q.setParameter("questionId_" + count, question.getId());
                    count++;
                }

            }
            q.setParameter("pi", pi);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct rows
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi");
            if (page != null) {
                sbQuery.append(" AND a.targetPageOrder = :page");
            } else {
                sbQuery.append(" AND a.targetPageOrder IS NULL");
            }
            if (!campaign.getQuestions().isEmpty()) {
                sbQuery.append(" AND (");
                int count = 1;
                for (Question question : campaign.getQuestions()) {
                    if (count > 1) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append(" a.generatorId = :questionId_").append(count);
                    count++;
                }
                sbQuery.append(" )");
            }
            Query q = em.createQuery(sbQuery.toString());
            int count = 1;
            for (Question question : campaign.getQuestions()) {
                q.setParameter("questionId_" + count, question.getId());
                count++;
            }
            q.setParameter("pi", pi);
            if (page != null) {
                q.setParameter("page", page);
            }

            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     * @should throw IllegalArgumentException if sortField unknown
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForUserId(Long userId, Integer maxResults, String sortField, boolean descending)
            throws DAOException {
        if (userId == null) {
            return Collections.emptyList();
        }

        preQuery();
        EntityManager em = getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<CrowdsourcingAnnotation> cq = cb.createQuery(CrowdsourcingAnnotation.class);
            Root<CrowdsourcingAnnotation> root = cq.from(CrowdsourcingAnnotation.class);
            cq.select(root).where(cb.or(cb.equal(root.get("creatorId"), userId), cb.equal(root.get("reviewerId"), userId)));
            if (StringUtils.isNotEmpty(sortField)) {
                if (!CrowdsourcingAnnotation.VALID_COLUMNS_FOR_ORDER_BY.contains(sortField)) {
                    throw new IllegalArgumentException("Sorting field not allowed: " + sortField);
                }
                if (descending) {
                    cq.orderBy(cb.desc(root.get(sortField)));
                } else {
                    cq.orderBy(cb.asc(root.get(sortField)));
                }
            }

            Query query = em.createQuery(cq);
            if (maxResults != null) {
                query.setMaxResults(maxResults);
            }
            return query.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @should return correct rows
     * @should filter by campaign name correctly
     */
    @Override
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters) throws DAOException {
        Map<String, Object> params = new HashMap<>();
        String filterString = createAnnotationsFilterQuery(null, filters, params);
        return getAnnotations(first, pageSize, sortField, descending, filterString, params);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending,
            String filterString, final Map<String, Object> params) throws DAOException {
        final Map<String, Object> useParams = params == null ? new HashMap<>() : params;
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CrowdsourcingAnnotation a");
                StringBuilder order = new StringBuilder();
                if (StringUtils.isNotEmpty(sortField)) {
                    if (!CrowdsourcingAnnotation.VALID_COLUMNS_FOR_ORDER_BY.contains(sortField)) {
                        throw new IllegalArgumentException("Sorting field not allowed: " + sortField);
                    }
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(QUERY_ELEMENT_DESC);
                    }
                }
                sbQuery.append(filterString).append(order);

                logger.trace(sbQuery);
                Query q = em.createQuery(sbQuery.toString());
                useParams.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"{}\" when trying to get CS annotations. Returning empty list.", e.toString());
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCount(Map<String, String> filters) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM CrowdsourcingAnnotation a");
            Map<String, Object> params = new HashMap<>();
            Query q = em.createQuery(sbQuery.append(createAnnotationsFilterQuery(null, filters, params)).toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

            return (long) q.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAnnotation(CrowdsourcingAnnotation annotation) throws DAOException {
        if (getAnnotation(annotation.getId()) != null) {
            return false;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(annotation);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateAnnotation(CrowdsourcingAnnotation annotation) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(annotation);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteAnnotation(CrowdsourcingAnnotation annotation) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CrowdsourcingAnnotation o = em.getReference(CrowdsourcingAnnotation.class, annotation.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public GeoMap getGeoMap(Long mapId) throws DAOException {
        if (mapId == null) {
            return null;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(GeoMap.class, mapId);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<GeoMap> getAllGeoMaps() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM GeoMap u");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addGeoMap(GeoMap map) throws DAOException {
        if (getGeoMap(map.getId()) != null) {
            return false;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(map);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateGeoMap(GeoMap map) throws DAOException {
        if (map.getId() == null) {
            return false;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(map);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteGeoMap(GeoMap map) throws DAOException {
        if (map.getId() == null) {
            return false;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            GeoMap o = em.getReference(GeoMap.class, map.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            Query qItems = em.createQuery(
                    "SELECT item FROM CMSGeomapContent item WHERE item.map = :map");
            qItems.setParameter("map", map);
            List<CMSContent> itemList = qItems.getResultList();

            Query qWidgets = em.createQuery(
                    "SELECT ele FROM CMSSidebarElementAutomatic ele WHERE ele.map = :map");
            qWidgets.setParameter("map", map);
            List<CMSSidebarElement> widgetList = qWidgets.getResultList();

            Stream<CMSPage> itemPages = itemList.stream()
                    .map(CMSContent::getOwningComponent)
                    .map(PersistentCMSComponent::getOwningPage);

            Stream<CMSPage> widgetPages = widgetList.stream()
                    .map(CMSSidebarElement::getOwnerPage);

            return Stream.concat(itemPages, widgetPages)
                    .distinct()
                    .collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingMapInSidebar(GeoMap map) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            Query qWidgets = em.createQuery(
                    "SELECT ele FROM CMSSidebarElementAutomatic ele WHERE ele.map = :map");
            qWidgets.setParameter("map", map);
            List<CMSSidebarElement> widgetList = qWidgets.getResultList();

            return widgetList.stream()
                    .map(CMSSidebarElement::getOwnerPage)
                    .distinct()
                    .collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean saveTermsOfUse(TermsOfUse tou) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            if (tou.getId() == null) {
                //create initial tou
                em.persist(tou);
            } else {
                em.merge(tou);
            }
            commitTransaction(em);
        } finally {
            close(em);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public TermsOfUse getTermsOfUse() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM TermsOfUse u");
            //         q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            @SuppressWarnings("unchecked")
            List<TermsOfUse> results = q.getResultList();
            if (results.isEmpty()) {
                //No results. Just return a new object which may be saved later
                return new TermsOfUse();
            }
            return results.get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean resetUserAgreementsToTermsOfUse() throws DAOException {
        List<User> users = getAllUsers(true);
        users.forEach(u -> u.setAgreedToTermsOfUse(false));
        users.forEach(u -> {
            try {
                updateUser(u);
            } catch (DAOException e) {
                logger.error("Error resetting user agreement for user {}", u, e);
            }
        });
        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getRecordNotes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSRecordNote a");
            Query q = em.createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            List<CMSRecordNote> notes = q.getResultList();
            notes = notes.stream()
                    .filter(n -> filters == null || n.matchesFilter(filters.values().stream().findAny().orElse(null)))
                    .skip(first)
                    .limit(pageSize)
                    .collect(Collectors.toList());

            return notes;
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get CMSRecordNotes. Returning empty list.", e.toString());
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getAllRecordNotes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSRecordNote a");
            logger.trace(sbQuery);
            Query q = em.createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get CMSRecordNotes. Returning empty list.", e.toString());
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSSingleRecordNote> getRecordNotesForPi(String pi, boolean displayedNotesOnly) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM CMSSingleRecordNote a WHERE a.recordPi = :pi";
            if (displayedNotesOnly) {
                query += " AND a.displayNote = :display";
            }
            // logger.trace(query); //NOSONAR Debug
            Query q = em.createQuery(query);
            q.setParameter("pi", pi);
            if (displayedNotesOnly) {
                q.setParameter("display", true);
            }
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMultiRecordNote> getAllMultiRecordNotes(boolean displayedNotesOnly) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM CMSMultiRecordNote a";
            if (displayedNotesOnly) {
                query += " WHERE a.displayNote = :display";
            }
            // logger.trace(query); //NOSONAR Debug
            Query q = em.createQuery(query);
            if (displayedNotesOnly) {
                q.setParameter("display", true);
            }
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSRecordNote getRecordNote(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CMSRecordNote o = em.getReference(CMSRecordNote.class, id);
            if (o != null) {
                if (o instanceof CMSMultiRecordNote) {
                    return new CMSMultiRecordNote(o);
                }
                return new CMSSingleRecordNote(o);
            }
            return null;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(note);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(note);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSRecordNote o = em.getReference(CMSRecordNote.class, note.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSSlider> getAllSliders() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSSlider a");
            logger.trace(sbQuery);
            Query q = em.createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get CMSSliders. Returning empty list.", e.toString());
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSSlider getSlider(Long id) throws DAOException {
        if (id == null) {
            return null;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CMSSlider o = em.getReference(CMSSlider.class, id);
            return new CMSSlider(o);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addSlider(CMSSlider slider) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(slider);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateSlider(CMSSlider slider) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(slider);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteSlider(CMSSlider slider) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSSlider o = em.getReference(CMSSlider.class, slider.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingSlider(CMSSlider slider) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query qItems = em.createQuery("SELECT item FROM CMSSliderContent item WHERE item.slider = :slider");
            qItems.setParameter("slider", slider);
            List<CMSContent> itemList = qItems.getResultList();
            return itemList.stream()
                    .map(CMSContent::getOwningComponent)
                    .map(PersistentCMSComponent::getOwningPage)
                    .distinct()
                    .collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<ThemeConfiguration> getConfiguredThemes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT t FROM ThemeConfiguration t");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ThemeConfiguration getTheme(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT t FROM ThemeConfiguration t WHERE UPPER(t.name) = :name");
            if (name != null) {
                q.setParameter("name", name.toUpperCase());
            }
            try {
                return (ThemeConfiguration) q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            } catch (NonUniqueResultException e) {
                logger.warn(e.getMessage());
                return (ThemeConfiguration) q.getResultList().get(0);
            }
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addTheme(ThemeConfiguration theme) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(theme);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateTheme(ThemeConfiguration theme) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(theme);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteTheme(ThemeConfiguration theme) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            ThemeConfiguration o = em.getReference(ThemeConfiguration.class, theme.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CustomSidebarWidget> getAllCustomWidgets() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT t FROM CustomSidebarWidget t");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CustomSidebarWidget getCustomWidget(Long id) throws DAOException {
        if (id == null) {
            return null;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            CustomSidebarWidget o = em.getReference(CustomSidebarWidget.class, id);
            return CustomSidebarWidget.clone(o);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCustomWidget(CustomSidebarWidget widget) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(widget);
                commitTransaction(em);
                return true;
            } catch (IllegalArgumentException | PersistenceException e) {
                logger.error(e.toString(), e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCustomWidget(CustomSidebarWidget widget) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.merge(widget);
                commitTransaction(em);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCustomWidget(Long id) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                CustomSidebarWidget o = em.getReference(CustomSidebarWidget.class, id);
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (IllegalArgumentException | PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingWidget(CustomSidebarWidget widget) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            Query qItems = em.createQuery(
                    "SELECT element FROM CMSSidebarElementCustom element WHERE element.widget = :widget");
            qItems.setParameter("widget", widget);
            List<CMSSidebarElementCustom> itemList = qItems.getResultList();

            return itemList.stream()
                    .map(CMSSidebarElementCustom::getOwnerPage)
                    .distinct()
                    .collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CookieBanner getCookieBanner() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM CookieBanner u");
            //         q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            @SuppressWarnings("unchecked")
            List<CookieBanner> results = q.getResultList();
            if (results.isEmpty()) {
                //No results. Just return a new object which may be saved later
                return new CookieBanner();
            }
            return results.get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean saveCookieBanner(CookieBanner banner) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            if (banner.getId() == null) {
                //create initial tou
                em.persist(banner);
            } else {
                em.merge(banner);
            }
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean saveDisclaimer(Disclaimer disclaimer) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            if (disclaimer.getId() == null) {
                //create initial tou
                em.persist(disclaimer);
            } else {
                em.merge(disclaimer);
            }
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error saving disclaimer", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Disclaimer getDisclaimer() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM Disclaimer u");
            //         q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            @SuppressWarnings("unchecked")
            List<Disclaimer> results = q.getResultList();
            if (results.isEmpty()) {
                //No results. Just return a new object which may be saved later
                return null;
            }
            return results.get(0);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Long getNumRecordsWithComments(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query query =
                    em.createNativeQuery(
                            "SELECT COUNT(DISTINCT target_pi) FROM annotations_comments WHERE annotations_comments.creator_id = ?1")
                            .setParameter(1, user.getId());
            return (Long) query.getSingleResult();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public List getNativeQueryResults(String query) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createNativeQuery(query).getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate(String query) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            int numUpdates = em.createNativeQuery(query).executeUpdate();
            commitTransaction(em);
            return numUpdates;
        } catch (PersistenceException e) {
            handleException(em);
            return 0;
        } finally {
            close(em);
        }
    }

    /**
     * 
     * @param staticFilterQuery
     * @param filters
     * @param params
     * @return Generated query
     */
    static String createFilterQuery2(String staticFilterQuery, Map<String, String> filters, Map<String, Object> params) {
        StringBuilder q = new StringBuilder(" ");
        if (StringUtils.isNotEmpty(staticFilterQuery)) {
            q.append(staticFilterQuery);
        }
        if (filters == null || filters.isEmpty()) {
            return q.toString();
        }

        AlphabetIterator abc = new AlphabetIterator();
        String mainTableKey = abc.next(); // = a
        //placeholder keys (a,b,c,...) for all tables to query
        Map<String, String> tableKeys = new HashMap<>();

        for (Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String filterValue = entry.getValue();
            if (StringUtils.isBlank(filterValue)) {
                continue;
            }
            String keyValueParam = key.replaceAll("[" + MULTIKEY_SEPARATOR + KEY_FIELD_SEPARATOR + "]", "");
            params.put(keyValueParam, "%" + filterValue.toUpperCase() + "%");

            List<String> joinStatements = new ArrayList<>();
            List<String> whereStatements = new ArrayList<>();

            //subkeys = all keys this filter applies to, each of the form [field] or [table]-[field]
            String[] subKeys = key.split(MULTIKEY_SEPARATOR);
            for (final String subKey : subKeys) {
                String sk = subKey;
                if (StringUtils.isBlank(sk)) {
                    continue;
                } else if (sk.contains(KEY_FIELD_SEPARATOR)) {
                    String table = sk.substring(0, sk.indexOf(KEY_FIELD_SEPARATOR));
                    String field = sk.substring(sk.indexOf(KEY_FIELD_SEPARATOR) + 1);
                    String tableKey;
                    if (!tableKeys.containsKey(table)) {
                        tableKey = abc.next();
                        tableKeys.put(table, tableKey);
                        String join = "LEFT JOIN " + mainTableKey + "." + table + " " + tableKey;
                        joinStatements.add(join); // JOIN mainTable.joinTable b
                    } else {
                        tableKey = tableKeys.get(table);
                    }
                    sk = tableKey + "." + field;
                } else {
                    sk = mainTableKey + "." + sk;
                }
                String where;
                if ("campaign".equals(sk)) {
                    where = "generatorId IN (SELECT q.id WHERE Question q WHERE q.ownerId IN "
                            + "(SELECT t.ownerId from CampaignTranslation t WHERE UPPER(" + sk + ") LIKE :" + keyValueParam;
                } else {
                    where = "UPPER(" + sk + ") LIKE :" + keyValueParam;
                }
                whereStatements.add(where); // joinTable.field LIKE :param | field LIKE :param
            }
            StringBuilder filterQuery = new StringBuilder().append(joinStatements.stream().collect(Collectors.joining(" ")));
            if (!whereStatements.isEmpty()) {
                filterQuery.append(" WHERE (").append(whereStatements.stream().collect(Collectors.joining(" OR "))).append(")");
            }
            q.append(filterQuery.toString());
        }

        return q.toString();
    }

    /**
     *
     * @param staticFilterQuery
     * @param filters
     * @param params
     * @return Generated query
     * @should create query correctly
     */
    static String createCampaignsFilterQuery(String staticFilterQuery, Map<String, String> filters, Map<String, Object> params) {
        StringBuilder q = new StringBuilder();
        if (StringUtils.isNotEmpty(staticFilterQuery)) {
            q.append(" ").append(staticFilterQuery);
        }
        if (filters == null || filters.isEmpty()) {
            return q.toString();
        }

        AlphabetIterator abc = new AlphabetIterator();
        String mainTableKey = abc.next(); // = a
        //placeholder keys (a,b,c,...) for all tables to query
        List<String> whereStatements = new ArrayList<>();
        for (Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String filterValue = entry.getValue();
            if (StringUtils.isBlank(filterValue)) {
                continue;
            }
            String keyValueParam = key.replaceAll("[" + MULTIKEY_SEPARATOR + KEY_FIELD_SEPARATOR + "]", "");
            if ("groupOwner".equals(key)) {
                params.put(keyValueParam, Long.valueOf(filterValue));
            } else {
                params.put(keyValueParam, "%" + filterValue.toUpperCase() + "%");
            }

            //subkeys = all keys this filter applies to, each of the form [field] or [table]-[field]
            String[] subKeys = key.split(MULTIKEY_SEPARATOR);
            for (final String subKey : subKeys) {
                if (StringUtils.isBlank(subKey)) {
                    continue;
                }
                String sk = mainTableKey + "." + subKey;
                String where = null;
                switch (sk) {
                    case "a.groupOwner":
                        where = mainTableKey + ".userGroup.owner IN (SELECT g.owner FROM UserGroup g WHERE g.owner.id=:" + keyValueParam + ")";
                        break;
                    case "a.name":
                        where = mainTableKey + ".id IN (SELECT t.owner.id FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.value) LIKE :"
                                + keyValueParam + ")";
                        break;
                    default:
                        where = "UPPER(" + sk + ") LIKE :" + keyValueParam;
                        break;
                }

                whereStatements.add(where);
            }
        }
        if (!whereStatements.isEmpty()) {
            StringBuilder sbOwner = new StringBuilder();
            StringBuilder sbOtherStatements = new StringBuilder();
            for (String whereStatement : whereStatements) {
                if (whereStatement.startsWith("a.userGroup.owner")) {
                    sbOwner.append(whereStatement);
                } else {
                    if (sbOtherStatements.length() != 0) {
                        sbOtherStatements.append(" OR ");
                    }
                    sbOtherStatements.append(whereStatement);
                }
            }
            StringBuilder filterQuery = new StringBuilder(QUERY_ELEMENT_WHERE).append(sbOwner.length() > 0 ? sbOwner.toString() : "");
            if (sbOwner.length() > 0 && sbOtherStatements.length() > 0) {
                filterQuery.append(QUERY_ELEMENT_AND);
            }
            if (sbOtherStatements.length() > 0) {
                filterQuery.append("(").append(sbOtherStatements.toString()).append(")");
            }
            q.append(filterQuery.toString());
        }

        return q.toString();

    }

    /**
     *
     * @param staticFilterQuery
     * @param filters
     * @param params
     * @return Generated query
     * @should create query correctly
     */
    static String createAnnotationsFilterQuery(String staticFilterQuery, Map<String, String> filters, Map<String, Object> params) {
        StringBuilder q = new StringBuilder();
        if (StringUtils.isNotEmpty(staticFilterQuery)) {
            q.append(" ").append(staticFilterQuery);
        }
        if (filters == null || filters.isEmpty()) {
            return q.toString();
        }

        AlphabetIterator abc = new AlphabetIterator();
        String mainTableKey = abc.next(); // = a
        //placeholder keys (a,b,c,...) for all tables to query
        List<String> whereStatements = new ArrayList<>();
        for (Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String filterValue = entry.getValue();
            if (StringUtils.isBlank(filterValue)) {
                continue;
            }
            String keyValueParam = key.replaceAll("[" + MULTIKEY_SEPARATOR + KEY_FIELD_SEPARATOR + "]", "");
            if (!"NULL".equals(filterValue)) {
                if ("creatorId_reviewerId".equals(key) || "campaignId".equals(key) || "generatorId".equals(key) || "creatorId".equals(key)
                        || "reviewerId".equals(key)) {
                    params.put(keyValueParam, Long.valueOf(filterValue));
                } else {
                    params.put(keyValueParam, "%" + filterValue.toUpperCase() + "%");
                }
            }

            //subkeys = all keys this filter applies to, each of the form [field] or [table]-[field]
            String[] subKeys = key.split(MULTIKEY_SEPARATOR);
            for (final String subKey : subKeys) {
                if (StringUtils.isBlank(subKey)) {
                    continue;
                }
                String sk = mainTableKey + "." + subKey;
                String where = null;
                switch (sk) {
                    case "a.creatorId":
                    case "a.reviewerId":
                    case "a.motivation":
                        if ("NULL".equalsIgnoreCase(filterValue)) {
                            where = sk + " IS NULL";
                        } else {
                            where = sk + "=:" + keyValueParam;
                        }
                        break;
                    case "a.generatorId":
                        where = mainTableKey + ".generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN "
                                + "(SELECT c FROM Campaign c WHERE c.id=:" + keyValueParam + "))";
                        break;
                    case "a.campaign":
                        where = mainTableKey + ".generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN "
                                + "(SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.translationValue) LIKE :"
                                + keyValueParam + "))";
                        break;
                    default:
                        where = "UPPER(" + sk + ") LIKE :" + keyValueParam;
                        break;
                }

                whereStatements.add(where);
            }
        }
        if (!whereStatements.isEmpty()) {
            StringBuilder sbCreatorReviewer = new StringBuilder();
            StringBuilder sbGenerator = new StringBuilder();
            StringBuilder sbOtherStatements = new StringBuilder();
            for (String whereStatement : whereStatements) {
                if (whereStatement.startsWith("a.creatorId")) {
                    sbCreatorReviewer.append(whereStatement);
                } else if (whereStatement.startsWith("a.reviewerId")) {
                    sbCreatorReviewer.append(" OR ").append(whereStatement);
                } else if (whereStatement.startsWith("a.generatorId")) {
                    sbGenerator.append(whereStatement);
                } else {
                    if (sbOtherStatements.length() != 0) {
                        sbOtherStatements.append(" OR ");
                    }
                    sbOtherStatements.append(whereStatement);
                }
            }
            StringBuilder filterQuery =
                    new StringBuilder(QUERY_ELEMENT_WHERE).append(sbCreatorReviewer.length() > 0 ? "(" + sbCreatorReviewer.toString() + ")" : "");

            if (sbCreatorReviewer.length() > 0 && (sbGenerator.length() > 0 || sbOtherStatements.length() > 0)) {
                filterQuery.append(QUERY_ELEMENT_AND);
            }
            if (sbGenerator.length() > 0) {
                filterQuery.append("(").append(sbGenerator.toString()).append(")");
                if (sbOtherStatements.length() > 0) {
                    filterQuery.append(QUERY_ELEMENT_AND);
                }
            }
            if (sbOtherStatements.length() > 0) {
                filterQuery.append("(").append(sbOtherStatements.toString()).append(")");
            }
            q.append(filterQuery.toString());
        }

        return q.toString();

    }

    /**
     * Builds a query string to filter a query across several tables
     *
     * @param staticFilterQuery
     * @param filters The filters to use
     * @param params Empty map which will be filled with the used query parameters. These to be added to the query
     * @return A string consisting of a WHERE and possibly JOIN clause of a query
     * @should build multikey filter query correctly
     */
    static String createFilterQuery(String staticFilterQuery, Map<String, String> filters, Map<String, String> params) {
        StringBuilder join = new StringBuilder();

        List<String> filterKeys = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (StringUtils.isNotEmpty(staticFilterQuery)) {
            where.append(staticFilterQuery);
        }
        if (filters != null && !filters.isEmpty()) {
            AlphabetIterator abc = new AlphabetIterator();
            String pageKey = abc.next();
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (final String k : filterKeys) {
                String tableKey = pageKey;
                String value = filters.get(k);
                if (StringUtils.isNotBlank(value)) {
                    //separate join table statement from key
                    String joinTable = "";
                    String key = k;
                    if (key.contains("::")) {
                        joinTable = key.substring(0, key.indexOf("::"));
                        key = key.substring(key.indexOf("::") + 2);
                        tableKey = abc.next();
                    }
                    if (count > 0 || StringUtils.isNotEmpty(staticFilterQuery)) {
                        where.append(" AND (");
                    } else {
                        where.append(QUERY_ELEMENT_WHERE);
                    }
                    String[] keyParts = key.split(MULTIKEY_SEPARATOR);
                    int keyPartCount = 0;
                    where.append(" ( ");
                    for (String keyPart : keyParts) {
                        if (keyPartCount > 0) {
                            where.append(" OR ");
                        }

                        where.append("UPPER(" + tableKey + ".")
                                .append(keyPart.replace("-", "."))
                                .append(") LIKE :")
                                .append(key.replace(MULTIKEY_SEPARATOR, "").replace("-", ""));
                        keyPartCount++;
                    }
                    where.append(" ) ");
                    count++;

                    //apply join table if necessary
                    if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                        join.append(QUERY_ELEMENT_JOIN)
                                .append(joinTable)
                                .append(" ")
                                .append(tableKey)
                                .append(" ON")
                                .append(" (")
                                .append(pageKey)
                                .append(".id = ")
                                .append(tableKey)
                                .append(".ownerPage.id)");
                        //                            if(joinTable.equalsIgnoreCase("CMSPageLanguageVersion")) {
                        //                                join.append(QUERY_ELEMENT_AND)
                        //                                .append(" (").append(tableKey).append(".language = :lang) ");
                        //                            }
                    } else if ("classifications".equals(joinTable)) {
                        join.append(QUERY_ELEMENT_JOIN).append(pageKey).append(".").append(joinTable).append(" ").append(tableKey);
                    } else if ("groupOwner".equals(joinTable)) {
                        join.append(QUERY_ELEMENT_JOIN)
                                .append(joinTable)
                                .append(" ")
                                .append(tableKey)
                                .append(" ON")
                                .append(" (")
                                .append(pageKey)
                                .append(".id = ")
                                .append(tableKey)
                                .append(".owner.id)");
                    }
                    params.put(key.replace(MULTIKEY_SEPARATOR, "").replace("-", ""), "%" + value.toUpperCase() + "%");
                }
                if (count > 1) {
                    where.append(" )");
                }
            }
        }
        return join.append(where).toString();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<ClientApplication> getAllClientApplications() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT c FROM ClientApplication c").getResultList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ClientApplication getClientApplication(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(ClientApplication.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ClientApplication getClientApplicationByClientId(String clientId) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return (ClientApplication) em.createQuery("SELECT c FROM ClientApplication c WHERE c.clientIdentifier = :clientId")
                    .setParameter("clientId", clientId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean saveClientApplication(ClientApplication client) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            if (client.getId() == null) {
                em.persist(client);
            } else {
                em.merge(client);
            }
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error saving disclaimer", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteClientApplication(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            ClientApplication o = em.getReference(ClientApplication.class, id);
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<DailySessionUsageStatistics> getAllUsageStatistics() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT s FROM DailySessionUsageStatistics s").getResultList();
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DailySessionUsageStatistics getUsageStatistics(LocalDate date) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT s FROM DailySessionUsageStatistics s WHERE s.date = :date");
            q.setParameter("date", date);
            return (DailySessionUsageStatistics) q.getResultList().getFirst();
        } catch (NoSuchElementException | NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<DailySessionUsageStatistics> getUsageStatistics(LocalDate start, LocalDate end) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT s FROM DailySessionUsageStatistics s WHERE s.date BETWEEN :start AND :end");
            q.setParameter("start", start);
            q.setParameter("end", end);
            return q.getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addUsageStatistics(DailySessionUsageStatistics statistics) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(statistics);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error saving disclaimer", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateUsageStatistics(DailySessionUsageStatistics statistics) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(statistics);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error saving disclaimer", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteUsageStatistics(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            DailySessionUsageStatistics o = em.getReference(DailySessionUsageStatistics.class, id);
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPageTemplate> getAllCMSPageTemplates() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT o FROM CMSPageTemplate o").getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get cms page templates. Returning empty list", e.toString());
            return new ArrayList<>();
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSPageTemplate getCMSPageTemplate(Long id) throws DAOException {
        logger.trace("getCMSPage: {}", id);
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(CMSPageTemplate.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSPageTemplate(CMSPageTemplate template) throws DAOException {

        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(template);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error adding cmsPage to database", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSPageTemplate(CMSPageTemplate template) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(template);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeCMSPageTemplate(CMSPageTemplate template) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            CMSPageTemplate o = em.getReference(CMSPageTemplate.class, template.getId());
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error(e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addViewerMessage(ViewerMessage message) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                em.persist(message);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error adding ViewerMessage to database", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteViewerMessage(ViewerMessage message) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                startTransaction(em);
                ViewerMessage o = em.getReference(ViewerMessage.class, message.getId());
                em.remove(o);
                commitTransaction(em);
                return true;
            } catch (PersistenceException e) {
                logger.error("Error deleting ViewerMessage component", e);
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ViewerMessage getViewerMessage(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(ViewerMessage.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateViewerMessage(ViewerMessage message) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(message);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    @Override
    public ViewerMessage getViewerMessageByMessageID(String messageId) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM ViewerMessage a WHERE a.messageId = :messageId");

            Query q = em.createQuery(sbQuery.toString());

            q.setParameter("messageId", messageId);
            return (ViewerMessage) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    @Override
    public int deleteViewerMessagesBefore(LocalDateTime date)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            em.getTransaction().begin();

            Query q = em.createQuery("DELETE FROM ViewerMessage a WHERE a.lastUpdateTime < :date");
            q.setParameter("date", date);
            int deleted = q.executeUpdate();

            em.getTransaction().commit();

            return deleted;
        } finally {
            close(em);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ViewerMessage> getViewerMessages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM ViewerMessage a");
            Map<String, Object> params = new HashMap<>();

            if (filters != null && !filters.isEmpty()) {
                String filterQuery = addViewerMessageFilterQuery(filters, params);
                sbQuery.append(filterQuery);
            }

            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY a.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            } else {
                sbQuery.append(" ORDER BY a.lastUpdateTime").append(QUERY_ELEMENT_DESC);
            }
            logger.trace(sbQuery);
            Query q = em.createQuery(sbQuery.toString());
            for (Entry<String, Object> entry : params.entrySet()) {
                q.setParameter(entry.getKey(), entry.getValue());
            }

            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return (List<ViewerMessage>) q.getResultList().stream().distinct().collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    String addViewerMessageFilterQuery(Map<String, String> filters, Map<String, Object> params) {
        String filterQuery = " WHERE (";

        for (Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isBlank(field) || "all".equalsIgnoreCase(field)) {
                if (StringUtils.isNotBlank(value)) {
                    if (filterQuery.endsWith(")")) {
                        filterQuery += " AND";
                    }
                    filterQuery += " (a.taskName LIKE :$field OR a.messageId LIKE :$field".replace("$field", field);
                    try {
                        params.put("valueStatus", MessageStatus.valueOf(value.toUpperCase()));
                        filterQuery += " OR a.messageStatus = :valueStatus";
                    } catch (IllegalArgumentException e) {
                        //noop
                    }
                    filterQuery += " OR :valueProperty MEMBER OF (a.properties)";
                    params.put("valueProperty", value);

                    filterQuery += ")";
                    params.put(field, "%" + value.trim() + "%");
                }
            } else {
                if (filterQuery.endsWith(")")) {
                    filterQuery += " AND";
                }
                filterQuery += " (a.$field LIKE :$value)".replace("$field", field).replace("$value", field);
                params.put(field, "%" + value.trim() + "%");
            }
        }
        filterQuery += ")";
        return " WHERE ()".equals(filterQuery) ? "" : filterQuery;
    }

    /** {@inheritDoc} */
    @Override
    public long getViewerMessageCount(Map<String, String> filters) throws DAOException {
        String filterQuery = "";
        Map<String, Object> params = new HashMap<>();
        if (filters != null) {
            filterQuery = addViewerMessageFilterQuery(filters, params);
        }
        return getFilteredRowCount("ViewerMessage", filterQuery, params);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RecurringTaskTrigger> getRecurringTaskTriggers() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT o FROM RecurringTaskTrigger o").getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get RecurringTaskTriggers. Returning empty list", e.toString());
            return new ArrayList<>();
        } finally {
            close(em);
        }
    }

    @Override
    public RecurringTaskTrigger getRecurringTaskTrigger(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(RecurringTaskTrigger.class, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    @Override
    public RecurringTaskTrigger getRecurringTaskTriggerForTask(TaskType task) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT a FROM RecurringTaskTrigger a WHERE a.taskType = :taskType");

            Query q = em.createQuery(sbQuery.toString());

            q.setParameter("taskType", task.name());
            return (RecurringTaskTrigger) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    @Override
    public boolean addRecurringTaskTrigger(RecurringTaskTrigger trigger) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(trigger);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error adding ViewerMessage to database", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    @Override
    public boolean updateRecurringTaskTrigger(RecurringTaskTrigger trigger) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(trigger);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    @Override
    public boolean deleteRecurringTaskTrigger(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            RecurringTaskTrigger o = em.getReference(RecurringTaskTrigger.class, id);
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error deleting RecurringTaskTrigger", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    @Override
    public boolean addHighlight(HighlightData object) throws DAOException {
        return addEntity(object);
    }

    @Override
    public boolean updateHighlight(HighlightData object) throws DAOException {
        return updateEntity(object);
    }

    @Override
    public boolean deleteHighlight(Long id) throws DAOException {
        return deleteEntity(id, HighlightData.class);
    }

    @Override
    public HighlightData getHighlight(Long id) throws DAOException {
        return getEntity(id, HighlightData.class);
    }

    @Override
    public List<HighlightData> getAllHighlights() throws DAOException {
        return getAllEntities(HighlightData.class);
    }

    @Override
    public List<HighlightData> getHighlightsForDate(LocalDateTime date) throws DAOException {
        return getMatchingEntities(HighlightData.class,
                "(:date BETWEEN o.dateStart AND o.dateEnd)"
                        + " OR "
                        + "(o.dateStart IS NULL AND :date < o.dateEnd)"
                        + " OR "
                        + "(o.dateEnd IS NULL AND :date > o.dateStart)"
                        + " OR "
                        + "(o.dateStart IS NULL AND o.dateEnd IS NULL)",
                Map.of("date", date));
    }

    @Override
    public List<HighlightData> getPastHighlightsForDate(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters, LocalDateTime date) throws DAOException {
        return getEntities(HighlightData.class, first, pageSize, sortField, descending, filters, ":date > a.dateEnd", Map.of("date", date));
    }

    @Override
    public List<HighlightData> getFutureHighlightsForDate(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters, LocalDateTime date) throws DAOException {
        return getEntities(HighlightData.class, first, pageSize, sortField, descending, filters, ":date < a.dateStart", Map.of("date", date));
    }

    @Override
    public List<HighlightData> getHighlights(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters) throws DAOException {
        return getEntities(HighlightData.class, first, pageSize, sortField, descending, filters);
    }

    private boolean addEntity(Serializable obj) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(obj);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error adding object to database", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct object
     */
    @Override
    public MaintenanceMode getMaintenanceMode() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT a FROM MaintenanceMode a WHERE a.id = 1");
            return (MaintenanceMode) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateMaintenanceMode(MaintenanceMode maintenanceMode) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(maintenanceMode);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    private boolean updateEntity(Serializable obj) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(obj);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error updating object in database", e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    private <T> T getEntity(Long id, Class<T> clazz) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.getReference(clazz, id);
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getAllEntities(Class<T> clazz) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(String.format("SELECT o FROM %s o", clazz.getSimpleName())).getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get objects of class {}. Returning empty list", e, clazz.getSimpleName());
            return new ArrayList<>();
        } finally {
            close(em);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getMatchingEntities(Class<T> clazz, String whereClause, Map<String, Object> params) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery(String.format("SELECT o FROM %s o WHERE %s", clazz.getSimpleName(), whereClause));
            params.forEach((name, value) -> q.setParameter(name, value));
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"{}\" when trying to get objects of class {}. Returning empty list", e, clazz.getSimpleName());
            return new ArrayList<>();
        } finally {
            close(em);
        }
    }

    private <T> boolean deleteEntity(Long id, Class<T> clazz) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            T o = em.getReference(clazz, id);
            em.remove(o);
            commitTransaction(em);
            return true;
        } catch (PersistenceException e) {
            logger.error("Error deleting {}", clazz.getSimpleName(), e);
            handleException(em);
            return false;
        } finally {
            close(em);
        }
    }

    private <T> List<T> getEntities(Class<T> clazz, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        return getEntities(clazz, first, pageSize, sortField, descending, filters, null, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getEntities(Class<T> clazz, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            String whereClause, Map<String, Object> whereClauseParams)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        Character entityVariable = 'a';
        try {
            StringBuilder sbQuery = new StringBuilder(String.format("SELECT %c FROM %s %c", entityVariable, clazz.getSimpleName(), entityVariable));
            Map<String, Object> params = new HashMap<>();

            if (filters != null && !filters.isEmpty()) {
                String filterQuery = addFilterQueries(filters, params, entityVariable);
                sbQuery.append(" WHERE (").append(filterQuery).append(")");
            }

            if (StringUtils.isNotBlank(whereClause)) {
                if (filters != null && !filters.isEmpty()) {
                    sbQuery.append(QUERY_ELEMENT_AND);
                } else {
                    sbQuery.append(QUERY_ELEMENT_WHERE);
                }
                sbQuery.append("(").append(whereClause).append(")");
                whereClauseParams.forEach(params::put);
            }

            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY a.").append(sortField);
                if (descending) {
                    sbQuery.append(QUERY_ELEMENT_DESC);
                }
            }
            logger.trace(sbQuery);
            Query q = em.createQuery(sbQuery.toString());
            for (Entry<String, Object> entry : params.entrySet()) {
                q.setParameter(entry.getKey(), entry.getValue());
            }

            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setHint(PARAM_STOREMODE, PARAM_STOREMODE_VALUE_REFRESH);

            return (List<T>) q.getResultList().stream().distinct().collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    private static String addFilterQueries(Map<String, String> filters, Map<String, Object> params, Character entityVariable) {
        Stream<String> queries = filters.entrySet().stream().map(entry -> getFilterQuery(entry.getKey(), entry.getValue(), params, entityVariable));
        return "(" + queries.collect(Collectors.joining(") OR (")) + ")";
    }

    /**
     * returns String in the form "a.field LIKE :field if filterValue is a string or "a.field = :field" otherwise
     * 
     * @param filterField A field name to search in
     * @param filterValue The value to search for
     * @param params a parameter
     * @param entityVariable
     * @return Generated query
     */
    private static String getFilterQuery(String filterField, Object filterValue, Map<String, Object> params, Character entityVariable) {
        if (filterValue instanceof String) {
            String query = String.format("%c.%s LIKE :%s", entityVariable, filterField, filterField);
            params.put(filterField, "%" + filterValue + "%");
            return query;
        } else if (filterValue != null) {
            String query = String.format("%c.%s = :%s", entityVariable, filterField, filterField);
            params.put(filterField, filterValue);
            return query;
        } else {
            return "";
        }
    }
}
