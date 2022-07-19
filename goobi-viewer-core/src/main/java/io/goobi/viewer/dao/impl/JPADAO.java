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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.AlphabetIterator;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.administration.legal.TermsOfUse;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.CMSPageTemplateEnabled;
import io.goobi.viewer.model.cms.CMSRecordNote;
import io.goobi.viewer.model.cms.CMSSingleRecordNote;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.upload.UploadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
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

/**
 * <p>
 * JPADAO class.
 * </p>
 */
public class JPADAO implements IDAO {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(JPADAO.class);
    private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "intranda_viewer_tomcat";
    static final String MULTIKEY_SEPARATOR = "_";
    static final String KEY_FIELD_SEPARATOR = "-";

    /**
     * EntityManagerFactory for the persistence context. Only build once at application startup
     */
    private final EntityManagerFactory factory;
    private Object cmsRequestLock = new Object();
    private Object crowdsourcingRequestLock = new Object();
    private final String persistenceUnitName;

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
        //        System.setProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, DataManager.getInstance().getConfiguration().getConfigLocalPath() + "persistence.xml");
        //        logger.debug(System.getProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML));
        String persistenceUnitName = inPersistenceUnitName;
        if (StringUtils.isEmpty(persistenceUnitName)) {
            persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
        }
        this.persistenceUnitName = persistenceUnitName;
        logger.info("Using persistence unit: {}", persistenceUnitName);
        try {
            // Create EntityManagerFactory in a custom class loader
            final Thread currentThread = Thread.currentThread();
            final ClassLoader saveClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(new JPAClassLoader(saveClassLoader));
            factory = Persistence.createEntityManagerFactory(persistenceUnitName);
            currentThread.setContextClassLoader(saveClassLoader);
            //Needs to be called for unit tests
            factory.createEntityManager();
            preQuery();
        } catch (DatabaseException | PersistenceException e) {
            logger.error(e.getMessage(), e);
            throw new DAOException(e.getMessage());
        }
    }

    /**
     * <p>
     * Getter for the field <code>factory</code>.
     * </p>
     *
     * @return a {@link javax.persistence.EntityManagerFactory} object.
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
     * @return {@link javax.persistence.EntityManager} for the current thread
     */
    @Override
    public EntityManager getEntityManager() {
        EntityManager em = getFactory().createEntityManager();
        //        em.setFlushMode(FlushModeType.COMMIT);
        return em;
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
     * the transaction
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
     * {@inheritDoc}
     *
     * Commits a persistence context transaction Only to be used following a {@link #startTransaction()} call
     */
    @Override
    public void commitTransaction(EntityTransaction et) throws PersistenceException {
        if (et.isActive()) {
            et.commit();
        } else {
            logger.warn("Attempring to commit an inactive transaction");
        }
    }

    @Override
    public void commitTransaction(EntityManager em) throws PersistenceException {
        commitTransaction(em.getTransaction());
    }

    @Override
    public void handleException(EntityTransaction et) throws PersistenceException {
        if (et.isActive()) {
            et.rollback();
        } else {
            logger.warn("Attempring to roll back an inactive transaction");
        }
    }

    @Override
    public void handleException(EntityManager em) {
        handleException(em.getTransaction());
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUsers(boolean)
     */
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
        Map<String, String> params = new HashMap<>();
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
                    sbQuery.append(" DESC");
                }
            }
            logger.trace(sbQuery.toString());
            Query q = em.createQuery(sbQuery.toString());
            for (String param : params.keySet()) {
                q.setParameter(param, params.get(param));
            }

            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @param param
     * @return
     */
    public String getUsersFilterQuery(String param) {
        String filterQuery;
        String filterQueryNames =
                "(UPPER(a.firstName) LIKE :%s OR UPPER(a.lastName) LIKE :%s OR UPPER(a.nickName) LIKE :%s OR UPPER(a.email) LIKE :%s)";
        String filterQueryGroup =
                "EXISTS (SELECT role FROM UserRole role LEFT JOIN role.userGroup group WHERE role.user = a AND UPPER(group.name) LIKE :%s)";
        filterQuery = " WHERE " + filterQueryNames.replace("%s", param) + " OR " + filterQueryGroup.replace("%s", param);
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
    private static String sanitizeQueryParam(String param, boolean addWildCards) {
        param = param.replaceAll("['\"\\(\\)]", "");
        param = param.toUpperCase();
        if (addWildCards) {
            param = "%" + param + "%";
        }
        return param;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUser(long)
     */
    /** {@inheritDoc} */
    @Override
    public User getUser(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            User o = em.getReference(User.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByEmail(java.lang.String)
     */
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
                User o = (User) q.getSingleResult();
                return o;
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByOpenId(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public User getUserByOpenId(String identifier) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE :claimed_identifier MEMBER OF u.openIdAccounts");
            q.setParameter("claimed_identifier", identifier);
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            User o = (User) q.getSingleResult();
            return o;
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
            User o = (User) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUser(io.goobi.viewer.model.user.User)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateUser(io.goobi.viewer.model.user.User)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUser(io.goobi.viewer.model.user.User)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUserGroups()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getAllUserGroups() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ug FROM UserGroup ug");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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
                sbQuery.append(" WHERE ");
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroups(io.goobi.viewer.model.user.User)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroup(long)
     */
    /** {@inheritDoc} */
    @Override
    public UserGroup getUserGroup(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            UserGroup o = em.getReference(UserGroup.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroup(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public UserGroup getUserGroup(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name");
            q.setParameter("name", name);
            UserGroup o = (UserGroup) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUserGroup(io.goobi.viewer.model.user.UserGroup)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUserGroup(io.goobi.viewer.model.user.UserGroup)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllBookmarkLists()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getAllBookmarkLists() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPublicBookmarkLists()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.isPublic=true");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookmarkLists(io.goobi.viewer.model.user.User)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getBookmarkLists(User user) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.owner = :user ORDER BY o.dateUpdated DESC");
            q.setParameter("user", user);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookmarkList(long)
     */
    /** {@inheritDoc} */
    @Override
    public BookmarkList getBookmarkList(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            BookmarkList o = em.getReference(BookmarkList.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookmarkList(java.lang.String)
     */
    /** {@inheritDoc} */
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
                BookmarkList o = (BookmarkList) q.getSingleResult();
                return o;
            } catch (NonUniqueResultException e) {
                BookmarkList o = (BookmarkList) q.getResultList().get(0);
                return o;
            }
        } catch (NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM BookmarkList o WHERE o.shareKey = :shareKey");
            q.setParameter("shareKey", shareKey);
            BookmarkList o = (BookmarkList) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addBookmarkList(io.goobi.viewer.model.bookmark.BookmarkList)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateBookmarkList(io.goobi.viewer.model.bookmark.BookmarkList)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteBookmarkList(io.goobi.viewer.model.bookmark.BookmarkList)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllRoles()
     */
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
                sbQuery.append(" WHERE ");
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getRole(long)
     */
    /** {@inheritDoc} */
    @Override
    public Role getRole(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Role o = em.getReference(Role.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getRole(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Role getRole(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT r FROM Role r WHERE r.name = :name");
            q.setParameter("name", name);
            Role o = (Role) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addRole(io.goobi.viewer.model.user.Role)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateRole(io.goobi.viewer.model.user.Role)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteRole(io.goobi.viewer.model.user.Role)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUserRoles()
     */
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
                sbQuery.append(" WHERE ");
                int args = 0;
                if (userGroup != null) {
                    sbQuery.append("ur.userGroup = :userGroup");
                    args++;
                }
                if (user != null) {
                    if (args > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("ur.user = :user");
                    args++;
                }
                if (role != null) {
                    if (args > 0) {
                        sbQuery.append(" AND ");
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserRoles(io.goobi.viewer.model.user.UserGroup,
     * io.goobi.viewer.model.user.User, io.goobi.viewer.model.user.Role)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT ur FROM UserRole ur");
            if (userGroup != null || user != null || role != null) {
                sbQuery.append(" WHERE ");
                int args = 0;
                if (userGroup != null) {
                    sbQuery.append("ur.userGroup = :userGroup");
                    args++;
                }
                if (user != null) {
                    if (args > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("ur.user = :user");
                    args++;
                }
                if (role != null) {
                    if (args > 0) {
                        sbQuery.append(" AND ");
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUserRole(io.goobi.viewer.model.user.UserRole)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateUserRole(io.goobi.viewer.model.user.UserRole)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUserRole(io.goobi.viewer.model.user.UserRole)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllLicenseTypes()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
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

    /** {@inheritDoc} */
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
                    sbQuery.append(" AND ");
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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
                    sbQuery.append(" AND ");
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getLicenseType(long)
     */
    /** {@inheritDoc} */
    @Override
    public LicenseType getLicenseType(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            LicenseType o = em.getReference(LicenseType.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getLicenseType(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public LicenseType getLicenseType(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.name = :name");
            q.setParameter("name", name);
            LicenseType o = (LicenseType) q.getSingleResult();
            return o;
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllLicenses()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<License> getAllLicenses() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM License o");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getLicense(java.lang.Long)
     */
    @Override
    public License getLicense(Long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            License o = em.find(License.class, id);
            return o;
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllIpRanges()
     */
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
                sbQuery.append(" WHERE ");
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getIpRange(long)
     */
    /** {@inheritDoc} */
    @Override
    public IpRange getIpRange(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            IpRange o = em.find(IpRange.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getIpRange(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public IpRange getIpRange(String name) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT ipr FROM IpRange ipr WHERE ipr.name = :name");
            q.setParameter("name", name);
            IpRange o = (IpRange) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addIpRange(io.goobi.viewer.model.user.IpRange)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateIpRange(io.goobi.viewer.model.user.IpRange)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteIpRange(io.goobi.viewer.model.user.IpRange)
     */
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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
            ;
            return (CommentGroup) em.createQuery("SELECT o FROM CommentGroup o WHERE o.coreType = true").setMaxResults(1).getSingleResult();
        } catch (EntityNotFoundException e) {
            return null;
        } catch (NoResultException e) {
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
            CommentGroup o = em.getReference(CommentGroup.class, id);
            return o;
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllComments()
     */
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
                    sbQuery.append(" WHERE ");
                } else {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("a.targetPI in :targetPIs");
            }
            if (StringUtils.isNotBlank(sortField)) {
                String[] sortFields = sortField.split("_");
                sbQuery.append(" ORDER BY ");
                for (String sf : sortFields) {
                    sbQuery.append("a.").append(sf);
                    if (descending) {
                        sbQuery.append(" DESC");
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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
                    sbQuery.append(" DESC");
                }
            }
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("owner", user.getId());
            return q.setMaxResults(maxResults).getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCommentsForPage(java.lang.String, int)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getComment(long)
     */
    /** {@inheritDoc} */
    @Override
    public Comment getComment(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Comment o = em.getReference(Comment.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addComment(io.goobi.viewer.model.annotation.Comment)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateComment(io.goobi.viewer.model.annotation.Comment)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteComment(io.goobi.viewer.model.annotation.Comment)
     */
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
     * @see io.goobi.viewer.dao.IDAO#changeCommentsOwner(io.goobi.viewer.model.security.user.User, io.goobi.viewer.model.security.user.User)
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
                    sbQuery.append(" AND ");
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
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            List<Integer> results = q.getResultList();
            return results.stream().distinct().sorted().collect(Collectors.toList());
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllSearches()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getAllSearches() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM Search o");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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
                    sbQuery.append(" WHERE ");
                } else {
                    sbQuery.append(" AND ");
                }
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(" AND ");
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

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
                    sbQuery.append(" WHERE ");
                } else {
                    sbQuery.append(" AND ");
                }
                filterKeys.addAll(filters.keySet());
                Collections.sort(filterKeys);
                int count = 0;
                for (String key : filterKeys) {
                    if (count > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                    count++;
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                sbQuery.append(" ORDER BY o.").append(sortField);
                if (descending) {
                    sbQuery.append(" DESC");
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearches(io.goobi.viewer.model.user.User)
     */
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearch(long)
     */
    /** {@inheritDoc} */
    @Override
    public Search getSearch(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Search o = em.find(Search.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addSearch(io.goobi.viewer.model.search.Search)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateSearch(io.goobi.viewer.model.search.Search)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteSearch(io.goobi.viewer.model.search.Search)
     */
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     *
     */
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
            DownloadJob o = em.getReference(DownloadJob.class, id);
            return o;
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
            DownloadJob o = (DownloadJob) q.getSingleResult();
            return o;
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
            DownloadJob o = (DownloadJob) q.getSingleResult();
            return o;
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

    /**
     * @see io.goobi.viewer.dao.IDAO#getCMSTemplateEnabled(java.lang.String)
     * @should return correct value
     */
    @Override
    public CMSPageTemplateEnabled getCMSPageTemplateEnabled(String templateId) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM CMSPageTemplateEnabled o where o.templateId = :templateId");
            q.setParameter("templateId", templateId);
            CMSPageTemplateEnabled o = (CMSPageTemplateEnabled) q.getSingleResult();
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCMSTemplateEnabled(io.goobi.viewer.model.cms.CMSPageTemplateEnabled)
     */
    @Override
    public boolean addCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.persist(o);
            commitTransaction(em);
        } catch (PersistenceException e) {
            handleException(em);
            return false;
        } finally {
            close(em);
        }

        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCMSTemplateEnabled(io.goobi.viewer.model.cms.CMSPageTemplateEnabled)
     */
    @Override
    public boolean updateCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            startTransaction(em);
            em.merge(o);
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
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#saveCMSTemplateEnabledStatuses(java.util.List)
     * @should update rows correctly
     */
    @Override
    public int saveCMSPageTemplateEnabledStatuses(List<CMSPageTemplate> templates) throws DAOException {
        if (templates == null) {
            return 0;
        }

        int count = 0;
        for (CMSPageTemplate template : templates) {
            if (template.getEnabled().getId() != null) {
                updateCMSPageTemplateEnabled(template.getEnabled());
            } else {
                addCMSPageTemplateEnabled(template.getEnabled());
            }
            count++;
        }

        return count;
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
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCmsPageForStaticPage(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSPage o WHERE o.staticPageName = :pageName");
                q.setParameter("pageName", pageName);
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                if (!q.getResultList().isEmpty()) {
                    return (CMSPage) q.getSingleResult();
                }
            } finally {
                close(em);
            }
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPages(int, int, java.lang.String, boolean, java.util.Map)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            List<String> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSPage a");
                StringBuilder order = new StringBuilder();

                Map<String, String> params = new HashMap<>();

                String filterString = createFilterQuery2(null, filters, params);
                String rightsFilterString = "";
                try {
                    rightsFilterString = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
                    if (!rightsFilterString.isEmpty()) {
                        rightsFilterString = (StringUtils.isBlank(filterString) ? " WHERE " : " AND ") + rightsFilterString;
                    }
                } catch (AccessDeniedException e) {
                    //may not request any cms pages at all
                    return Collections.emptyList();
                }

                if (StringUtils.isNotEmpty(sortField)) {
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(" DESC");
                    }
                }
                sbQuery.append(filterString).append(rightsFilterString).append(order);

                logger.trace("CMS page query: {}", sbQuery.toString());
                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);

                List<CMSPage> list = q.getResultList();
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, LocalDateTime fromDate, LocalDateTime toDate, List<String> templateIds)
            throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT o FROM CMSPage o WHERE o.relatedPI IS NOT NULL AND o.relatedPI <> ''");
            if (fromDate != null) {
                sbQuery.append(" AND o.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(" AND o.dateUpdated <= :toDate");
            }
            if (templateIds != null && !templateIds.isEmpty()) {
                sbQuery.append(" AND (");
                int count = 0;
                for (String templateId : templateIds) {
                    if (count != 0) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("o.templateId = '").append(templateId).append("'");
                    count++;
                }
                sbQuery.append(')');
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

            return (long) q.getSingleResult() != 0;
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getCMSPageWithRelatedPiCount(LocalDateTime fromDate, LocalDateTime toDate, List<String> templateIds) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery =
                    new StringBuilder("SELECT COUNT(DISTINCT o.relatedPI) FROM CMSPage o WHERE o.relatedPI IS NOT NULL AND o.relatedPI <> ''");
            if (fromDate != null) {
                sbQuery.append(" AND o.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(" AND o.dateUpdated <= :toDate");
            }
            if (templateIds != null && !templateIds.isEmpty()) {
                sbQuery.append(" AND (");
                int count = 0;
                for (String templateId : templateIds) {
                    if (count != 0) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("o.templateId = '").append(templateId).append("'");
                    count++;
                }
                sbQuery.append(')');
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
                    .createQuery("SELECT o FROM CMSPage o WHERE o.relatedPI = :pi AND o.useAsDefaultRecordView = true and o.published = true")
                    .setParameter("pi", pi)
                    .setMaxResults(1);

            return (CMSPage) getSingleResult(q).orElse(null);
        } finally {
            close(em);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSPage getCMSPage(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            logger.trace("getCMSPage: {}", id);
            preQuery();
            EntityManager em = getEntityManager();
            try {
                CMSPage o = em.getReference(CMSPage.class, id);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                close(em);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSPage getCMSPageForEditing(long id) throws DAOException {
        CMSPage original = getCMSPage(id);
        CMSPage copy = new CMSPage(original);
        return copy;
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
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o");
                q.setFlushMode(FlushModeType.COMMIT);
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
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
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
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
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return (CMSMediaItem) q.getSingleResult();
            } catch (NoResultException e) {
                //nothing found; no biggie
                return null;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms media item with filename '" + filename + "'");
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
                CMSMediaItem o = em.getReference(CMSMediaItem.class, id);
                return o;
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
    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            List<CMSPage> ownerList = new ArrayList<>();
            preQuery();
            EntityManager em = getEntityManager();
            try {
                Query q = em.createQuery("SELECT o FROM CMSContentItem o WHERE o.mediaItem = :media");
                q.setParameter("media", item);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                for (Object o : q.getResultList()) {
                    if (o instanceof CMSContentItem) {
                        try {
                            CMSPage page = ((CMSContentItem) o).getOwnerPageLanguageVersion().getOwnerPage();
                            if (!ownerList.contains(page)) {
                                ownerList.add(page);
                            }
                        } catch (NullPointerException e) {
                            //
                        }
                    }
                }
                return ownerList;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            } finally {
                close(em);
            }
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesByCategory(io.goobi.viewer.model.cms.Category)
     */
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
            List<CMSMediaItem> pageList = q.getResultList();
            return pageList;
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
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                q.setFlushMode(FlushModeType.COMMIT);
                List<CMSNavigationItem> list = q.getResultList();
                Collections.sort(list);
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
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
                CMSNavigationItem o = em.find(CMSNavigationItem.class, id);
                return o;
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTranskribusJobs(java.lang.String, java.lang.String, io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus)
     */
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
                    sbQuery.append(" WHERE ");
                } else {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("o.ownerId = :ownerId");
                filterCount++;
            }
            if (status != null) {
                if (filterCount == 0) {
                    sbQuery.append(" WHERE ");
                } else {
                    sbQuery.append(" AND ");
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
                        order.append(" DESC");
                    }
                }
                sbQuery.append(filterString).append(order);

                logger.trace(sbQuery.toString());
                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get CS campaigns. Returning empty list");
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
                Campaign o = em.getReference(Campaign.class, id);
                return o;
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
                Question o = em.getReference(Question.class, id);
                return o;
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
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

                List<CampaignRecordStatistic> list = q.getResultList();
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get CS campaigns. Returning empty list");
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
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

                List<CampaignRecordPageStatistic> list = q.getResultList();
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get CS campaigns. Returning empty list");
                return Collections.emptyList();
            } finally {
                close(em);
            }
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCampaign(io.goobi.viewer.model.crowdsourcing.campaigns.Campaign)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCampaign(io.goobi.viewer.model.crowdsourcing.campaigns.Campaign)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#checkAvailability()
     */
    @Override
    public boolean checkAvailability() {
        try {
            getRole(1);
            return true;
        } catch (Exception e) {
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

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#shutdown()
     */
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
                    sbQuery.append(" AND ");
                } else {
                    sbQuery.append(" WHERE ");
                    where = true;
                }
                sbQuery.append("a.creatorId = :owner");
            }
            if (targetPIs != null && !targetPIs.isEmpty()) {
                if (where) {
                    sbQuery.append(" AND ");
                } else {
                    sbQuery.append(" WHERE ");
                    where = true;
                }
                sbQuery.append("a.targetPI in :targetPIs");
            }
            logger.trace(sbQuery.toString());
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
    public long getCMSPageCount(Map<String, String> filters, List<String> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT count(DISTINCT a) FROM CMSPage").append(" a");
            Map<String, String> params = new HashMap<>();
            sbQuery.append(createFilterQuery2(null, filters, params));
            try {
                String rightsFilter = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
                if (!rightsFilter.isEmpty()) {
                    if (filters.values().stream().anyMatch(v -> StringUtils.isNotBlank(v))) {
                        sbQuery.append(" AND ");
                    } else {
                        sbQuery.append(" WHERE ");
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

    /**
     * Universal method for returning the row count for the given class and filter string.
     *
     * @param className
     * @param filter Filter query string
     * @return
     * @throws DAOException
     */
    private long getFilteredRowCount(String className, String filter, Map<String, String> params) throws DAOException {
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
     * @return
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllStaticPages()
     */
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSStaticPage> getAllStaticPages() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT o FROM CMSStaticPage o");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getStaticPageForTypeType(io.goobi.viewer.dao.PageType)
     */
    /** {@inheritDoc} */
    @Override
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.pageName = :name");
            q.setParameter("name", pageType.getName());
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return getSingleResult(q);
        } finally {
            close(em);
        }
    }

    /**
     * Helper method to get the only result of a query. In contrast to {@link javax.persistence.Query#getSingleResult()} this does not throw an
     * exception if no results are found. Instead, it returns an empty Optional
     *
     * @throws ClassCastException if the first result cannot be cast to the expected type
     * @throws NonUniqueResultException if the query matches more than one result
     * @param q the query to perform
     * @return an Optional containing the query result, or an empty Optional if no results are present
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSCollections(java.lang.String)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSCollection(java.lang.String, java.lang.String)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesByCategory(io.goobi.viewer.model.cms.Category)
     */
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id");
            q.setParameter("id", category.getId());
            List<CMSPage> pageList = q.getResultList();
            return pageList;
        } finally {
            close(em);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesForSubtheme(String subtheme) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT DISTINCT page FROM CMSPage page WHERE page.subThemeDiscriminatorValue = :subtheme");
            q.setParameter("subtheme", subtheme);
            List<CMSPage> pageList = q.getResultList();
            return pageList;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesForRecord(java.lang.String, io.goobi.viewer.model.cms.Category)
     */
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
            List<CMSPage> pageList = q.getResultList();
            return pageList;
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
     * @param allowedTemplateIds a {@link java.util.List} object.
     * @param allowedSubthemes a {@link java.util.List} object.
     * @param allowedCategoryIds a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     */
    public static String createCMSPageFilter(Map<String, String> params, String pageParameter, List<String> allowedTemplateIds,
            List<String> allowedSubthemes, List<String> allowedCategoryIds) throws AccessDeniedException {

        String query = "";

        int index = 0;
        if (allowedTemplateIds != null && !allowedTemplateIds.isEmpty()) {
            query += "(";
            for (String template : allowedTemplateIds) {
                String templateParameter = "tpl" + ++index;
                query += (":" + templateParameter + " = " + pageParameter + ".templateId");
                query += " OR ";
                params.put(templateParameter, template);
            }
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ") AND";
        } else if (allowedTemplateIds != null) {
            throw new AccessDeniedException("User may not view pages with any templates");
        }

        index = 0;
        if (allowedSubthemes != null && !allowedSubthemes.isEmpty()) {
            query += " (";
            for (String subtheme : allowedSubthemes) {
                String templateParameter = "thm" + ++index;
                query += (":" + templateParameter + " = " + pageParameter + ".subThemeDiscriminatorValue");
                query += " OR ";
                params.put(templateParameter, subtheme);
            }
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
            for (String category : allowedCategoryIds) {
                String templateParameter = "cat" + ++index;
                query += (":" + templateParameter + " IN (SELECT c.id FROM " + pageParameter + ".categories c)");
                query += " OR ";
                params.put(templateParameter, category);
            }
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
            List<CMSCategory> list = q.getResultList();
            return list;
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCountPagesUsingCategory(io.goobi.viewer.model.cms.CMSCategory)
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
     * @see io.goobi.viewer.dao.IDAO#getCountMediaItemsUsingCategory(io.goobi.viewer.model.cms.CMSCategory)
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
            return category;
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
            CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
            return category;
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
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
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
            try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
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
            CrowdsourcingAnnotation annotation = (CrowdsourcingAnnotation) getSingleResult(q).orElse(null);
            return annotation;
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
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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

            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllAnnotationsByMotivation(java.lang.String)
     */
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

    @SuppressWarnings("unchecked")
    @Override
    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page, String motivation) throws DAOException {

        preQuery();
        EntityManager em = getEntityManager();
        try {
            String query = "SELECT a FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi";
            if (page != null) {
                query += " AND a.targetPageOrder = :page";
            } else {
                query += " AND a.targetPageOrder IS NULL";
            }
            if (StringUtils.isNotBlank(motivation)) {
                query += " AND a.motivation =  + :motivation";
            }
            Query q = em.createQuery(query);
            q.setParameter("pi", pi);
            if (page != null) {
                q.setParameter("page", page);
            }
            if (StringUtils.isNotBlank(motivation)) {
                q.setParameter("motivation", motivation);
            }

            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
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
            String query = "SELECT COUNT(a) FROM CrowdsourcingAnnotation a WHERE a.targetPI = :pi";
            if (page != null) {
                query += " AND a.targetPageOrder = :page";
            } else {
                query += " AND a.targetPageOrder IS NULL";
            }
            Query q = em.createQuery(query);
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
                sbQuery.append(" AND (");
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

            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getAnnotationsForUser(java.lang.Long)
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
            String filterString, Map<String, Object> params) throws DAOException {
        params = params == null ? new HashMap<>() : params;
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
                        order.append(" DESC");
                    }
                }
                sbQuery.append(filterString).append(order);

                logger.trace(sbQuery.toString());
                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get CS campaigns. Returning empty list");
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateAnnotation(io.goobi.viewer.model.annotation.CrowdsourcingAnnotation)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteAnnotation(io.goobi.viewer.model.annotation.CrowdsourcingAnnotation)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getGeoMap(java.lang.Long)
     */
    @Override
    public GeoMap getGeoMap(Long mapId) throws DAOException {
        if (mapId == null) {
            return null;
        }
        preQuery();
        EntityManager em = getEntityManager();
        try {
            GeoMap o = em.getReference(GeoMap.class, mapId);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllGeoMaps()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<GeoMap> getAllGeoMaps() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM GeoMap u");
            List<GeoMap> list = q.getResultList();
            return list;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addGeoMap(io.goobi.viewer.model.maps.GeoMap)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateGeoMap(io.goobi.viewer.model.maps.GeoMap)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteGeoMap(io.goobi.viewer.model.maps.GeoMap)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPagesUsingMap(io.goobi.viewer.model.maps.GeoMap)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            Query qItems = em.createQuery(
                    "SELECT item FROM CMSContentItem item WHERE item.geoMap = :map");
            qItems.setParameter("map", map);
            List<CMSContentItem> itemList = qItems.getResultList();

            Query qWidgets = em.createQuery(
                    "SELECT ele FROM CMSSidebarElementAutomatic ele WHERE ele.map = :map");
            qWidgets.setParameter("map", map);
            List<CMSSidebarElement> widgetList = qWidgets.getResultList();

            Stream<CMSPage> itemPages = itemList.stream()
                    .map(CMSContentItem::getOwnerPageLanguageVersion)
                    .map(CMSPageLanguageVersion::getOwnerPage);

            Stream<CMSPage> widgetPages = widgetList.stream()
                    .map(CMSSidebarElement::getOwnerPage);

            List<CMSPage> pageList = Stream.concat(itemPages, widgetPages)
                    .distinct()
                    .collect(Collectors.toList());
            return pageList;
        } finally {
            close(em);
        }
    }

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

            List<CMSPage> pageList = widgetList.stream()
                    .map(CMSSidebarElement::getOwnerPage)
                    .distinct()
                    .collect(Collectors.toList());
            return pageList;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#saveTermsOfUse(io.goobi.viewer.model.security.TermsOfUse)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTermsOfUse()
     */
    @Override
    public TermsOfUse getTermsOfUse() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM TermsOfUse u");
            //         q.setHint("javax.persistence.cache.storeMode", "REFRESH");

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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#resetUserAgreementsToTermsOfUse()
     */
    @Override
    public boolean resetUserAgreementsToTermsOfUse() throws DAOException {
        List<User> users = getAllUsers(true);
        users.forEach(u -> u.setAgreedToTermsOfUse(false));
        users.forEach(u -> {
            try {
                updateUser(u);
            } catch (DAOException e) {
                logger.error("Error resetting user agreement for user " + u, e);
            }
        });
        return true;
    }

    /**
     * Implements filtering with java methods because filtering single-table inheritance objects does not work as expected
     */
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
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSRecordNotes. Returning empty list");
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllRecordNotes()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getAllRecordNotes() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSRecordNote a");
            logger.trace(sbQuery.toString());
            Query q = em.createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSRecordNotes. Returning empty list");
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getRecordNotesForPi(java.lang.String)
     */
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
            // logger.trace(query);
            Query q = em.createQuery(query.toString());
            q.setParameter("pi", pi);
            if (displayedNotesOnly) {
                q.setParameter("display", true);
            }
            return q.getResultList();
        } finally {
            close(em);
        }
    }

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
            // logger.trace(query);
            Query q = em.createQuery(query.toString());
            if (displayedNotesOnly) {
                q.setParameter("display", true);
            }
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getRecordNote(java.lang.Long)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllSliders()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSSlider> getAllSliders() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSSlider a");
            logger.trace(sbQuery.toString());
            Query q = em.createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSSliders. Returning empty list");
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSlider(java.lang.Long)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPagesUsingSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingSlider(CMSSlider slider) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {

            Query qItems = em.createQuery(
                    "SELECT item FROM CMSContentItem item WHERE item.slider = :slider");
            qItems.setParameter("slider", slider);
            List<CMSContentItem> itemList = qItems.getResultList();

            List<CMSPage> pageList = itemList.stream()
                    .map(CMSContentItem::getOwnerPageLanguageVersion)
                    .map(CMSPageLanguageVersion::getOwnerPage)
                    .distinct()
                    .collect(Collectors.toList());

            return pageList;
        } finally {
            close(em);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getConfiguredThemes()
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTheme(java.lang.String)
     */
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
                ThemeConfiguration t = (ThemeConfiguration) q.getSingleResult();
                return t;
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addTheme(io.goobi.viewer.model.viewer.themes.Theme)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateTheme(io.goobi.viewer.model.viewer.themes.Theme)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteTheme(io.goobi.viewer.model.viewer.themes.Theme)
     */
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
            } catch (IllegalArgumentException e) {
                handleException(em);
                return false;
            } catch (PersistenceException e) {
                handleException(em);
                return false;
            } finally {
                close(em);
            }
        }
    }

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

            List<CMSPage> pageList = itemList.stream()
                    .map(CMSSidebarElementCustom::getOwnerPage)
                    .distinct()
                    .collect(Collectors.toList());

            return pageList;
        } finally {
            close(em);
        }
    }

    @Override
    public CookieBanner getCookieBanner() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM CookieBanner u");
            //         q.setHint("javax.persistence.cache.storeMode", "REFRESH");

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

    @Override
    public Disclaimer getDisclaimer() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT u FROM Disclaimer u");
            //         q.setHint("javax.persistence.cache.storeMode", "REFRESH");

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

    static String createFilterQuery2(String staticFilterQuery, Map<String, String> filters, Map<String, String> params) {
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
            for (String subKey : subKeys) {
                if (StringUtils.isBlank(subKey)) {
                    continue;
                } else if (subKey.contains(KEY_FIELD_SEPARATOR)) {
                    String table = subKey.substring(0, subKey.indexOf(KEY_FIELD_SEPARATOR));
                    String field = subKey.substring(subKey.indexOf(KEY_FIELD_SEPARATOR) + 1);
                    String tableKey;
                    if (!tableKeys.containsKey(table)) {
                        tableKey = abc.next();
                        tableKeys.put(table, tableKey);
                        String join = "LEFT JOIN " + mainTableKey + "." + table + " " + tableKey;
                        joinStatements.add(join); // JOIN mainTable.joinTable b
                    } else {
                        tableKey = tableKeys.get(table);
                    }
                    subKey = tableKey + "." + field;
                } else {
                    subKey = mainTableKey + "." + subKey;
                }
                String where;
                if ("campaign".equals(subKey)) {
                    where = "generatorId IN (SELECT q.id WHERE Question q WHERE q.ownerId IN " +
                            "(SELECT t.ownerId from CampaignTranslation t WHERE UPPER(" + subKey + ") LIKE :" + keyValueParam;
                } else {
                    where = "UPPER(" + subKey + ") LIKE :" + keyValueParam;
                }
                whereStatements.add(where); // joinTable.field LIKE :param | field LIKE :param
            }
            String filterQuery = joinStatements.stream().collect(Collectors.joining(" "));
            if (!whereStatements.isEmpty()) {
                filterQuery += " WHERE (" + whereStatements.stream().collect(Collectors.joining(" OR ")) + ")";
            }
            q.append(filterQuery);
        }

        return q.toString();
    }

    /**
     *
     * @param staticFilterQuery
     * @param filters
     * @param params
     * @return
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
            for (String subKey : subKeys) {
                if (StringUtils.isBlank(subKey)) {
                    continue;
                }
                subKey = mainTableKey + "." + subKey;
                String where = null;
                switch (subKey) {
                    case "a.groupOwner":
                        where = mainTableKey + ".userGroup.owner IN (SELECT g.owner FROM UserGroup g WHERE g.owner.id=:" + keyValueParam + ")";
                        break;
                    case "a.name":
                        where = mainTableKey + ".id IN (SELECT t.owner.id FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.value) LIKE :"
                                + keyValueParam + ")";
                        break;
                    default:
                        where = "UPPER(" + subKey + ") LIKE :" + keyValueParam;
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
            String filterQuery = " WHERE " + (sbOwner.length() > 0 ? sbOwner.toString() : "");
            if (sbOwner.length() > 0 && sbOtherStatements.length() > 0) {
                filterQuery += " AND ";
            }
            if (sbOtherStatements.length() > 0) {
                filterQuery += ("(" + sbOtherStatements.toString() + ")");
            }
            q.append(filterQuery);
        }

        return q.toString();

    }

    /**
     *
     * @param staticFilterQuery
     * @param filters
     * @param params
     * @return
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
            if ("NULL".equals(filterValue)) {
                //don't add params
            } else if ("creatorId_reviewerId".equals(key) || "campaignId".equals(key) || "generatorId".equals(key) || "creatorId".equals(key)
                    || "reviewerId".equals(key)) {
                params.put(keyValueParam, Long.valueOf(filterValue));
            } else {
                params.put(keyValueParam, "%" + filterValue.toUpperCase() + "%");
            }

            //subkeys = all keys this filter applies to, each of the form [field] or [table]-[field]
            String[] subKeys = key.split(MULTIKEY_SEPARATOR);
            for (String subKey : subKeys) {
                if (StringUtils.isBlank(subKey)) {
                    continue;
                }
                subKey = mainTableKey + "." + subKey;
                String where = null;
                switch (subKey) {
                    case "a.creatorId":
                    case "a.reviewerId":
                    case "a.motivation":
                        if ("NULL".equalsIgnoreCase(filterValue)) {
                            where = subKey + " IS NULL";
                        } else {
                            where = subKey + "=:" + keyValueParam;
                        }
                        break;
                    case "a.generatorId":
                        where = mainTableKey + ".generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN " +
                                "(SELECT c FROM Campaign c WHERE c.id=:" + keyValueParam + "))";
                        break;
                    case "a.campaign":
                        where = mainTableKey + ".generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN " +
                                "(SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.value) LIKE :" + keyValueParam + "))";
                        break;
                    default:
                        where = "UPPER(" + subKey + ") LIKE :" + keyValueParam;
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
            String filterQuery = " WHERE " + (sbCreatorReviewer.length() > 0 ? "(" + sbCreatorReviewer.toString() + ")" : "");

            if (sbCreatorReviewer.length() > 0 && (sbGenerator.length() > 0 || sbOtherStatements.length() > 0)) {
                filterQuery += " AND ";
            }
            if (sbGenerator.length() > 0) {
                filterQuery += "(" + sbGenerator.toString() + ")";
                if (sbOtherStatements.length() > 0) {
                    filterQuery += " AND ";
                }
            }
            if (sbOtherStatements.length() > 0) {
                filterQuery += "(" + sbOtherStatements.toString() + ")";
            }
            q.append(filterQuery);
        }

        return q.toString();

    }

    /**
     * Builds a query string to filter a query across several tables
     *
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
            for (String key : filterKeys) {
                String tableKey = pageKey;
                String value = filters.get(key);
                if (StringUtils.isNotBlank(value)) {
                    //separate join table statement from key
                    String joinTable = "";
                    if (key.contains("::")) {
                        joinTable = key.substring(0, key.indexOf("::"));
                        key = key.substring(key.indexOf("::") + 2);
                        tableKey = abc.next();
                    }
                    if (count > 0 || StringUtils.isNotEmpty(staticFilterQuery)) {
                        where.append(" AND (");
                    } else {
                        where.append(" WHERE ");
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
                                .append(key.replaceAll(MULTIKEY_SEPARATOR, "").replace("-", ""));
                        keyPartCount++;
                    }
                    where.append(" ) ");
                    count++;

                    //apply join table if necessary
                    if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                        join.append(" JOIN ")
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
                        //                                join.append(" AND ")
                        //                                .append(" (").append(tableKey).append(".language = :lang) ");
                        //                            }
                    } else if ("classifications".equals(joinTable)) {
                        join.append(" JOIN ").append(pageKey).append(".").append(joinTable).append(" ").append(tableKey);
                        //                            .append(" ON ").append(" (").append(pageKey).append(".id = ").append(tableKey).append(".ownerPage.id)");
                    } else if ("groupOwner".equals(joinTable)) {
                        join.append(" JOIN ")
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
                    params.put(key.replaceAll(MULTIKEY_SEPARATOR, "").replace("-", ""), "%" + value.toUpperCase() + "%");
                }
                if (count > 1) {
                    where.append(" )");
                }
            }
        }
        String filterString = join.append(where).toString();
        return filterString;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClientApplication> getAllClientApplications() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM ClientApplication c");
            return q.getResultList();
        } finally {
            close(em);
        }
    }

    @Override
    public ClientApplication getClientApplication(long id) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            ClientApplication o = em.getReference(ClientApplication.class, id);
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        } finally {
            close(em);
        }
    }
    
    @Override
    public ClientApplication getClientApplicationByClientId(String clientId) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT c FROM ClientApplication c WHERE c.clientIdentifier = :clientId");
            q.setParameter("clientId", clientId);
            return (ClientApplication) q.getSingleResult();
        } catch(NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

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
    

    @Override
    @SuppressWarnings("unchecked")
    public List<DailySessionUsageStatistics> getAllUsageStatistics() throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT s FROM DailySessionUsageStatistics s");
            return q.getResultList();
        } catch(NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }

    @Override
    public DailySessionUsageStatistics getUsageStatistics(LocalDate date) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT s FROM DailySessionUsageStatistics s WHERE s.date = :date");
            q.setParameter("date", date);
            return (DailySessionUsageStatistics) q.getSingleResult();
        } catch(NoResultException e) {
            return null;
        } finally {
            close(em);
        }
    }
    
    @Override
    public List<DailySessionUsageStatistics> getUsageStatistics(LocalDate start, LocalDate end) throws DAOException {
        preQuery();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("SELECT s FROM DailySessionUsageStatistics s WHERE s.date BETWEEN :start AND :end");
            q.setParameter("start", start);
            q.setParameter("end", end);
            return q.getResultList();
        } catch(NoResultException e) {
            return Collections.emptyList();
        } finally {
            close(em);
        }
    }

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
}
