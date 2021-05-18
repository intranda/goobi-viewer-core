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
package io.goobi.viewer.dao.impl;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.AlphabetIterator;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.CMSPageTemplateEnabled;
import io.goobi.viewer.model.cms.CMSRecordNote;
import io.goobi.viewer.model.cms.CMSSidebarElement;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.TermsOfUse;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus;
import io.goobi.viewer.model.viewer.PageType;

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

    private final EntityManagerFactory factory;
    // private ThreadLocal<EntityManager> threadLocalEm = new ThreadLocal<>();
    private EntityManager em;
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
     * Getter for the field <code>factory</code>.
     * </p>
     *
     * @return a {@link javax.persistence.EntityManagerFactory} object.
     */
    public EntityManagerFactory getFactory() {
        return this.factory;
    }

    /**
     * <p>
     * getEntityManager.
     * </p>
     *
     * @return {@link javax.persistence.EntityManager} for the current thread
     */
    public EntityManager getEntityManager() {
        //        if (threadLocalEm.get() == null) {
        //            threadLocalEm.set(factory.createEntityManager());
        //        }
        //
        //        return threadLocalEm.get();

        if (em == null) {
            em = factory.createEntityManager();
        }

        return em;
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
        logger.info("Using persistence unit: {}", persistenceUnitName);
        try {
            // Create EntityManagerFactory in a custom class loader
            final Thread currentThread = Thread.currentThread();
            final ClassLoader saveClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(new JPAClassLoader(saveClassLoader));
            factory = Persistence.createEntityManagerFactory(persistenceUnitName);
            currentThread.setContextClassLoader(saveClassLoader);

            // threadLocalEm.set(factory.createEntityManager());
            em = factory.createEntityManager();
            preQuery();
        } catch (DatabaseException | PersistenceException e) {
            logger.error(e.getMessage(), e);
            throw new DAOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Start a persistence context transaction. Always needs to be succeeded with {@link #commitTransaction()} after the transaction is complete
     */
    @Override
    public void startTransaction() {
        getEntityManager().getTransaction().begin();
    }

    /**
     * {@inheritDoc}
     *
     * Commits a persistence context transaction Only to be used following a {@link #startTransaction()} call
     */
    @Override
    public void commitTransaction() {
        getEntityManager().getTransaction().commit();
    }

    /**
     * {@inheritDoc}
     *
     * Create a query in native sql syntax in the persistence context. Does not provide its own transaction. Use {@link #startTransaction()} and
     * {@link #commitTransaction()} for this
     */
    @Override
    public Query createNativeQuery(String string) {
        return getEntityManager().createNativeQuery(string);
    }

    /**
     * {@inheritDoc}
     *
     * Create a query in jpa query syntax in the persistence context. Does not provide its own transaction. Use {@link #startTransaction()} and
     * {@link #commitTransaction()} for this
     */
    @Override
    public Query createQuery(String string) {
        return getEntityManager().createQuery(string);
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
        Query q = getEntityManager().createQuery("SELECT u FROM User u");
        if (refresh) {
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        }
        return q.getResultList();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
        for (String param : params.keySet()) {
            q.setParameter(param, params.get(param));
        }

        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
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
        try {
            User o = getEntityManager().getReference(User.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT u FROM User u WHERE UPPER(u.email) = :email");
        if (email != null) {
            q.setParameter("email", email.toUpperCase());
        }
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage());
            return (User) q.getResultList().get(0);
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
        Query q = getEntityManager().createQuery("SELECT u FROM User u WHERE :claimed_identifier MEMBER OF u.openIdAccounts");
        q.setParameter("claimed_identifier", identifier);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT u FROM User u WHERE UPPER(u.nickName) = :nickname");
        q.setParameter("nickname", nickname.trim().toUpperCase());
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.getEntityManager().contains(user)) {
                this.getEntityManager().refresh(user);
            }
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            User u = em.getReference(User.class, user.getId());
            em.remove(u);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT ug FROM UserGroup ug");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
        if (filters != null) {
            for (String key : filterKeys) {
                q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
            }
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
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
        Query q = getEntityManager().createQuery("SELECT ug FROM UserGroup ug WHERE ug.owner = :owner");
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
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
        try {
            UserGroup o = getEntityManager().getReference(UserGroup.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name");
        q.setParameter("name", name);
        try {
            UserGroup o = (UserGroup) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(userGroup);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(userGroup);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.getEntityManager().contains(userGroup)) {
                this.getEntityManager().refresh(userGroup);
            }
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            UserGroup o = em.getReference(UserGroup.class, userGroup.getId());
            em.remove(o);
            try {
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            }
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT o FROM BookmarkList o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPublicBookmarkLists()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM BookmarkList o WHERE o.isPublic=true");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
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
        Query q = getEntityManager().createQuery("SELECT o FROM BookmarkList o WHERE o.owner = :user ORDER BY o.dateUpdated DESC");
        q.setParameter("user", user);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
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
        try {
            BookmarkList o = getEntityManager().getReference(BookmarkList.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q;
        if (user != null) {
            q = getEntityManager().createQuery("SELECT o FROM BookmarkList o WHERE o.name = :name AND o.owner = :user");
            q.setParameter("name", name);
            q.setParameter("user", user);
        } else {
            q = getEntityManager().createQuery("SELECT o FROM BookmarkList o WHERE o.name = :name");
            q.setParameter("name", name);
        }
        try {
            BookmarkList o = (BookmarkList) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            BookmarkList o = (BookmarkList) q.getResultList().get(0);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        }
    }

    /** {@inheritDoc} */
    @Override
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM BookmarkList o WHERE o.shareKey = :shareKey");
        q.setParameter("shareKey", shareKey);
        try {
            BookmarkList o = (BookmarkList) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(bookmarkList);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(bookmarkList);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new items have IDs
            if (this.getEntityManager().contains(bookmarkList)) {
                this.getEntityManager().refresh(bookmarkList);
            }
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            BookmarkList o = em.getReference(BookmarkList.class, bookmarkList.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException e) {
            return false;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT r FROM Role r");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
        try {
            Role o = getEntityManager().getReference(Role.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT r FROM Role r WHERE r.name = :name");
        q.setParameter("name", name);
        try {
            Role o = (Role) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(role);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(role);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Role o = em.getReference(Role.class, role.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT ur FROM UserRole ur");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getUserRoleCount(io.goobi.viewer.model.security.user.UserGroup, io.goobi.viewer.model.security.user.User,
     *      io.goobi.viewer.model.security.Role)
     * @should return correct count
     */
    @Override
    public long getUserRoleCount(UserGroup userGroup, User user, Role role) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
        q.setFlushMode(FlushModeType.COMMIT);

        return (long) q.getSingleResult();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(userRole);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(userRole);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            UserRole o = em.getReference(UserRole.class, userRole.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT lt FROM LicenseType lt");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @should only return non open access license types
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getRecordLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.core = false");
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=false");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                sbQuery.append(" AND ");
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getCoreLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=true");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                sbQuery.append(" AND ");
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
        try {
            LicenseType o = getEntityManager().getReference(LicenseType.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT lt FROM LicenseType lt WHERE lt.name = :name");
        q.setParameter("name", name);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            LicenseType o = (LicenseType) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
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
        Query q = getEntityManager().createQuery("SELECT a FROM LicenseType a WHERE a.name IN :names");
        q.setParameter("names", names);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            return q.getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return Collections.emptyList();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(licenseType);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(licenseType);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            LicenseType o = em.getReference(LicenseType.class, licenseType.getId());
            em.remove(o);
            try {
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            }
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllLicenses()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<License> getAllLicenses() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM License o");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getLicense(java.lang.Long)
     */
    @Override
    public License getLicense(Long id) throws DAOException {
        preQuery();
        try {
            License o = getEntityManager().find(License.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        String query = "SELECT a FROM License a WHERE a.licenseType = :licenseType";
        Query q = getEntityManager().createQuery(query);
        q.setParameter("licenseType", licenseType);
        return q.getResultList();
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
        String query = "SELECT COUNT(a) FROM License a WHERE a.licenseType = :licenseType";
        Query q = getEntityManager().createQuery(query);
        q.setParameter("licenseType", licenseType);

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
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
        Query q = getEntityManager().createQuery("SELECT ipr FROM IpRange ipr");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
        try {
            IpRange o = getEntityManager().find(IpRange.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        Query q = getEntityManager().createQuery("SELECT ipr FROM IpRange ipr WHERE ipr.name = :name");
        q.setParameter("name", name);
        try {
            IpRange o = (IpRange) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(ipRange);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(ipRange);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.getEntityManager().contains(ipRange)) {
                this.getEntityManager().refresh(ipRange);
            }
            return true;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            IpRange o = em.getReference(IpRange.class, ipRange.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllComments()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getAllComments() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM Comment o");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT a FROM Comment a");
        List<String> filterKeys = new ArrayList<>();
        Map<String, String> params = new HashMap<>();
        sbQuery.append(createFilterQuery(null, filters, params));
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY a.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }

        Query q = getEntityManager().createQuery(sbQuery.toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.setFirstResult(first).setMaxResults(pageSize).setFlushMode(FlushModeType.COMMIT).getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCommentsForPage(java.lang.String, int)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForPage(String pi, int page) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o FROM Comment o WHERE o.pi = :pi AND o.page = :page");
        Query q = getEntityManager().createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setParameter("page", page);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForWork(String pi) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o FROM Comment o WHERE o.pi = :pi");
        Query q = getEntityManager().createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getComment(long)
     */
    /** {@inheritDoc} */
    @Override
    public Comment getComment(long id) throws DAOException {
        preQuery();
        try {
            Comment o = getEntityManager().getReference(Comment.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(comment);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateComment(io.goobi.viewer.model.annotation.Comment)
     */
    /** {@inheritDoc} */
    @Override
    public boolean updateComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(comment);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteComment(io.goobi.viewer.model.annotation.Comment)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Comment o = em.getReference(Comment.class, comment.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        EntityManager emLocal = factory.createEntityManager();
        try {
            emLocal.getTransaction().begin();
            int rows = emLocal.createQuery("UPDATE Comment o set o.owner = :newOwner WHERE o.owner = :oldOwner")
                    .setParameter("oldOwner", fromUser)
                    .setParameter("newOwner", toUser)
                    .executeUpdate();
            emLocal.getTransaction().commit();

            // Refresh objects in context
            getEntityManager().createQuery("SELECT o FROM Comment o WHERE o.owner = :owner")
                    .setParameter("owner", toUser)
                    .setHint("javax.persistence.cache.storeMode", "REFRESH")
                    .getResultList();

            return rows;
        } finally {
            emLocal.close();
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

        // Fetch relevant IDs
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("DELETE FROM Comment o WHERE ");
        if (StringUtils.isNotEmpty(pi)) {
            sbQuery.append("o.pi = :pi");
        }
        if (owner != null) {
            if (StringUtils.isNotEmpty(pi)) {
                sbQuery.append(" AND ");
            }
            sbQuery.append("o.owner = :owner");
        }

        EntityManager emLocal = factory.createEntityManager();
        try {
            Query q = emLocal.createQuery(sbQuery.toString());
            if (StringUtils.isNotEmpty(pi)) {
                q.setParameter("pi", pi);
            }
            if (owner != null) {
                q.setParameter("owner", owner);
            }
            emLocal.getTransaction().begin();
            int rows = q.executeUpdate();
            emLocal.getTransaction().commit();
            return rows;
        } finally {
            emLocal.close();
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
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o.page FROM Comment o WHERE o.pi = :pi");
        Query q = getEntityManager().createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        List<Integer> results = q.getResultList();
        return results.stream().distinct().sorted().collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllSearches()
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getAllSearches() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM Search o");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearches(io.goobi.viewer.model.user.User)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner) throws DAOException {
        preQuery();
        String query = "SELECT o FROM Search o WHERE o.owner = :owner";
        Query q = getEntityManager().createQuery(query);
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearch(long)
     */
    /** {@inheritDoc} */
    @Override
    public Search getSearch(long id) throws DAOException {
        preQuery();
        try {
            Search o = getEntityManager().find(Search.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(search);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(search);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteSearch(io.goobi.viewer.model.search.Search)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteSearch(Search search) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Search o = em.getReference(Search.class, search.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT o FROM DownloadJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJob(long id) throws DAOException {
        preQuery();
        try {
            DownloadJob o = getEntityManager().getReference(DownloadJob.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
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
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("SELECT o FROM DownloadJob o WHERE o.identifier = :identifier");
        Query q = getEntityManager().createQuery(sbQuery.toString());
        q.setParameter("identifier", identifier);
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
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
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("SELECT o FROM DownloadJob o WHERE o.type = :type AND o.pi = :pi");
        if (logId != null) {
            sbQuery.append(" AND o.logId = :logId");
        }
        Query q = getEntityManager().createQuery(sbQuery.toString());
        q.setParameter("type", type);
        q.setParameter("pi", pi);
        if (logId != null) {
            q.setParameter("logId", logId);
        }
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(downloadJob);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException {
        logger.trace("updateDownloadJob: {}", downloadJob.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(downloadJob);
            em.getTransaction().commit();

            if (this.getEntityManager().contains(downloadJob)) {
                this.getEntityManager().refresh(downloadJob);
            }

            return true;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            DownloadJob o = em.getReference(DownloadJob.class, downloadJob.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCMSTemplateEnabled(java.lang.String)
     * @should return correct value
     */
    @Override
    public CMSPageTemplateEnabled getCMSPageTemplateEnabled(String templateId) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM CMSPageTemplateEnabled o where o.templateId = :templateId");
        q.setParameter("templateId", templateId);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            CMSPageTemplateEnabled o = (CMSPageTemplateEnabled) q.getSingleResult();
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCMSTemplateEnabled(io.goobi.viewer.model.cms.CMSPageTemplateEnabled)
     */
    @Override
    public boolean addCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(o);
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCMSTemplateEnabled(io.goobi.viewer.model.cms.CMSPageTemplateEnabled)
     */
    @Override
    public boolean updateCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(o);
            em.getTransaction().commit();
            // Refresh the object from the DB
            if (this.getEntityManager().contains(o)) {
                this.getEntityManager().refresh(o);
            }
            return true;
        } finally {
            em.close();
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
        try {
            synchronized (cmsRequestLock) {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM CMSPage o");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            }
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
            return new ArrayList<>();
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
            Query q = getEntityManager().createQuery("SELECT o FROM CMSPage o WHERE o.staticPageName = :pageName");
            q.setParameter("pageName", pageName);
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            if (!q.getResultList().isEmpty()) {
                return (CMSPage) q.getSingleResult();
            }
        }
        return null;
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
            try {
                preQuery();
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
                Query q = getEntityManager().createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);

                List<CMSPage> list = q.getResultList();
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
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
        Map<String, String> tableKeys = new HashMap<>();
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
        Map<String, String> tableKeys = new HashMap<>();
        List<String> whereStatements = new ArrayList<>();
        for (Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String filterValue = entry.getValue();
            if (StringUtils.isBlank(filterValue)) {
                continue;
            }
            String keyValueParam = key.replaceAll("[" + MULTIKEY_SEPARATOR + KEY_FIELD_SEPARATOR + "]", "");
            if ("creatorId_reviewerId".equals(key) || "campaignId".equals(key) || "generatorId".equals(key)) {
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
                        where = subKey + "=:" + keyValueParam;
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
                        //                        if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                        //                            where.append("UPPER(" + tableKey + ".").append(keyPart).append(") LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        //                        } else if ("categories".equals(joinTable)) {
                        //                            where.append(tableKey).append(" LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        //
                        //                        } else {
                        where.append("UPPER(" + tableKey + ".")
                                .append(keyPart.replace("-", "."))
                                .append(") LIKE :")
                                .append(key.replaceAll(MULTIKEY_SEPARATOR, "").replace("-", ""));
                        //                        }
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

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, LocalDateTime fromDate, LocalDateTime toDate, List<String> templateIds)
            throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCMSPagesForRecordHaveUpdates(String pi, CMSCategory category, LocalDateTime fromDate, LocalDateTime toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM CMSPage o WHERE o.relatedPI = :pi");
        if (fromDate != null) {
            sbQuery.append(" AND o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(" AND o.dateUpdated <= :toDate");
        }
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /** {@inheritDoc} */
    @Override
    public long getCMSPageWithRelatedPiCount(LocalDateTime fromDate, LocalDateTime toDate, List<String> templateIds) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /** {@inheritDoc} */
    @Override
    public CMSPage getCMSPage(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            logger.trace("getCMSPage: {}", id);
            preQuery();
            try {
                CMSPage o = getEntityManager().getReference(CMSPage.class, id);
                if (o != null) {
                    updateCMSPageFromDatabase(o.getId());
                }
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                logger.trace("getCMSPage END");
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
    @Override
    public CMSSidebarElement getCMSSidebarElement(long id) throws DAOException {

        synchronized (cmsRequestLock) {
            logger.trace("getCMSSidebarElement: {}", id);
            preQuery();
            try {
                CMSSidebarElement o = getEntityManager().getReference(CMSSidebarElement.class, id);
                getEntityManager().refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                logger.trace("getCMSSidebarElement END");
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = getEntityManager().createQuery("SELECT o FROM CMSNavigationItem o WHERE o.cmsPage = :page");
            q.setParameter("page", page);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(page);
                em.getTransaction().commit();
                return updateCMSPageFromDatabase(page.getId());
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(page);
                em.getTransaction().commit();
                return updateCMSPageFromDatabase(page.getId());
            } finally {
                em.close();
            }
        }
    }

    /**
     * Refresh the CMSPage with the given id from the database. If the page is not found or if the refresh fails, false is returned
     * 
     * @param id
     * @return
     */
    private boolean updateCMSPageFromDatabase(Long id) {
        CMSPage o = null;
        try {
            o = this.getEntityManager().getReference(CMSPage.class, id);
            this.getEntityManager().refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                getEntityManager().remove(o);
            }
            return false;
        }
    }

    private void updateCampaignsFromDatabase() throws DAOException {
        List<Campaign> campaigns = this.getAllCampaigns();
        for (Campaign campaign : campaigns) {
            updateFromDatabase(campaign.getId(), Campaign.class);
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean updateFromDatabase(Long id, Class clazz) {
        Object o = null;
        try {
            o = this.getEntityManager().getReference(clazz, id);
            this.getEntityManager().refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                getEntityManager().remove(o);
            }
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSPage o = em.getReference(CMSPage.class, page.getId());
                em.remove(o);
                em.getTransaction().commit();
                return !updateCMSPageFromDatabase(o.getId());
            } catch (RollbackException e) {
                return false;
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM CMSMediaItem o");
                q.setFlushMode(FlushModeType.COMMIT);
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM CMSMediaItem o WHERE o.collection = true");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getCMSMediaItemByFilename(String filename) throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM CMSMediaItem o WHERE o.fileName = :fileName");
                q.setFlushMode(FlushModeType.COMMIT);
                q.setParameter("fileName", filename);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return (CMSMediaItem) q.getSingleResult();
            } catch (NoResultException e) {
                //nothing found; no biggie
                return null;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms media item with filename '" + filename + "'");
                return null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            try {
                CMSMediaItem o = getEntityManager().getReference(CMSMediaItem.class, id);
                getEntityManager().refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(item);
                em.getTransaction().commit();
                return updateFromDatabase(item.getId(), item.getClass());
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSMediaItem o = em.getReference(CMSMediaItem.class, item.getId());
                em.remove(o);
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            List<CMSPage> ownerList = new ArrayList<>();
            try {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM CMSContentItem o WHERE o.mediaItem = :media");
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
                        }
                    }
                }
                return ownerList;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
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
        Query q = getEntityManager()
                .createQuery("SELECT DISTINCT media FROM CMSMediaItem media JOIN media.categories category WHERE category.id = :id");
        q.setParameter("id", category.getId());
        List<CMSMediaItem> pageList = q.getResultList();
        return pageList;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            try {
                Query q = getEntityManager().createQuery("SELECT o FROM CMSNavigationItem o WHERE o.parentItem IS NULL");
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                q.setFlushMode(FlushModeType.COMMIT);
                List<CMSNavigationItem> list = q.getResultList();
                Collections.sort(list);
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            try {
                CMSNavigationItem o = getEntityManager().find(CMSNavigationItem.class, id);
                getEntityManager().refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSNavigationItem o = em.getReference(CMSNavigationItem.class, item.getId());
                em.remove(o);
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            } finally {
                em.close();
            }
        }
    }

    // Transkribus

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM TranskribusJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTranskribusJobs(java.lang.String, java.lang.String, io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException {
        preQuery();
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

        Query q = getEntityManager().createQuery(sbQuery.toString());
        if (pi != null) {
            q.setParameter("pi", pi);
        }
        if (transkribusUserId != null) {
            q.setParameter("ownerId", transkribusUserId);
        }
        if (status != null) {
            q.setParameter("status", status);
        }
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(job);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(job);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            TranskribusJob o = em.getReference(TranskribusJob.class, job.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Campaign> getAllCampaigns() throws DAOException {
        try {
            synchronized (crowdsourcingRequestLock) {
                preQuery();
                Query q = getEntityManager().createQuery("SELECT o FROM Campaign o");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            }
        } catch (PersistenceException e) {
            logger.error(e.toString());
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getCampaignCount(Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM Campaign a");
        Map<String, Object> params = new HashMap<>();
        Query q = getEntityManager().createQuery(sbQuery.append(createCampaignsFilterQuery(null, filters, params)).toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<Campaign> getCampaigns(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM Campaign a");
            StringBuilder order = new StringBuilder();
            try {
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
                Query q = getEntityManager().createQuery(sbQuery.toString());
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
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Campaign getCampaign(Long id) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            try {
                Campaign o = getEntityManager().getReference(Campaign.class, id);
                if (o != null) {
                    //                    getEntityManager().refresh(o);
                }
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public Question getQuestion(Long id) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            try {
                Question o = getEntityManager().getReference(Question.class, id);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            try {
                String query = "SELECT a FROM CampaignRecordStatistic a WHERE a.pi = :pi";
                if (status != null) {
                    query += " AND a.status = :status";
                }
                Query q = getEntityManager().createQuery(query);
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
            try {
                campaign.onPrePersist();
                getEntityManager().getTransaction().begin();
                getEntityManager().persist(campaign);
                getEntityManager().getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
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
            campaign.onPreUpdate();
            try {
                getEntityManager().getTransaction().begin();
                getEntityManager().setFlushMode(FlushModeType.COMMIT);
                Campaign c = getEntityManager().merge(campaign);
                getEntityManager().getTransaction().commit();
                //solrQueryResults remains unchanged in managed campaign even after merge. Manually reset results to account for changed solrquery
                c.resetSolrQueryResults();
                return true;
            } catch (RollbackException e) {
                throw new PersistenceException("Failed to persist campaign " + campaign, e);
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
            try {
                getEntityManager().getTransaction().begin();
                Campaign o = getEntityManager().getReference(Campaign.class, campaign.getId());
                getEntityManager().remove(o);
                getEntityManager().getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
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

        EntityManager emLocal = factory.createEntityManager();
        try {
            emLocal.getTransaction().begin();
            int rows = emLocal
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_statistic_annotators WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_page_statistic_annotators WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_statistic_reviewers WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "DELETE FROM cs_campaign_record_page_statistic_reviewers WHERE user_id=?")
                    .setParameter(1, user.getId())
                    .executeUpdate();
            emLocal.getTransaction().commit();
            updateCampaignsFromDatabase();
            return rows;
        } finally {
            emLocal.close();
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

        List<Campaign> campaignsToUpdate = new ArrayList<>();
        Set<CampaignRecordStatistic> statistics = new HashSet<>();

        EntityManager emLocal = factory.createEntityManager();
        try {
            emLocal.getTransaction().begin();
            int rows = emLocal
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_statistic_annotators SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_page_statistic_annotators SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_statistic_reviewers SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            rows += emLocal
                    .createNativeQuery(
                            "UPDATE cs_campaign_record_page_statistic_reviewers SET user_id=? WHERE user_id=?")
                    .setParameter(1, toUser.getId())
                    .setParameter(2, fromUser.getId())
                    .executeUpdate();
            emLocal.getTransaction().commit();
            updateCampaignsFromDatabase();
            return rows;
        } finally {
            emLocal.close();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#shutdown()
     */
    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (getEntityManager() != null && getEntityManager().isOpen()) {
            getEntityManager().close();
        }
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        //        if (threadLocalEm.get() != null) {
        //            threadLocalEm.remove();
        //        }

        // This is MySQL specific, but needed to prevent OOMs when redeploying
        //        try {
        //            AbandonedConnectionCleanupThread.shutdown();
        //        } catch (InterruptedException e) {
        //            logger.error(e.getMessage(), e);
        //        }
    }

    /**
     * <p>
     * preQuery.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void preQuery() throws DAOException {
        if (getEntityManager() == null) {
            throw new DAOException("EntityManager is not initialized");
        }
        if (!getEntityManager().isOpen()) {
            // threadLocalEm.set(factory.createEntityManager());
            em = factory.createEntityManager();
        }
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

    /** {@inheritDoc} */
    @Override
    public long getCommentCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Comment", null, filters);
    }

    /** {@inheritDoc} */
    @Override
    public long getCMSPageCount(Map<String, String> filters, List<String> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException {
        preQuery();
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
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
        StringBuilder sbQuery = new StringBuilder("SELECT count(DISTINCT a) FROM ").append(className).append(" a").append(" ").append(filter);
        Query q = getEntityManager().createQuery(sbQuery.toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
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
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM ").append(className).append(" a");
        Map<String, String> params = new HashMap<>();
        Query q = getEntityManager().createQuery(sbQuery.append(createFilterQuery(staticFilterQuery, filters, params)).toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllStaticPages()
     */
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSStaticPage> getAllStaticPages() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT o FROM CMSStaticPage o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    /** {@inheritDoc} */
    @Override
    public void addStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(page);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    /** {@inheritDoc} */
    @Override
    public void updateStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(page);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSStaticPage o = em.getReference(CMSStaticPage.class, page.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException | EntityNotFoundException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException, NonUniqueResultException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.cmsPageId = :id");
        q.setParameter("id", page.getId());
        return q.getResultList();
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        //        return getSingleResult(q);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getStaticPageForTypeType(io.goobi.viewer.dao.PageType)
     */
    /** {@inheritDoc} */
    @Override
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.pageName = :name");
        q.setParameter("name", pageType.getName());
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return getSingleResult(q);
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

    /**
     * Helper method to get the first result of the given query if any results are returned, or an empty Optional otherwise
     * 
     * @throws ClassCastException if the first result cannot be cast to the expected type
     * @param q the query to perform
     * @return an Optional containing the first query result, or an empty Optional if no results are present
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private static <T> Optional<T> getFirstResult(Query q) throws ClassCastException {
        List<Object> results = q.getResultList();
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) results.get(0));
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
            Query q = getEntityManager().createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field");
            q.setParameter("field", solrField);
            return q.getResultList();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    /** {@inheritDoc} */
    @Override
    public boolean addCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(collection);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    /** {@inheritDoc} */
    @Override
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(collection);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.getEntityManager().contains(collection)) {
                this.getEntityManager().refresh(collection);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSCollection(java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field AND c.solrFieldValue = :value");
        q.setParameter("field", solrField);
        q.setParameter("value", solrFieldValue);
        return (CMSCollection) getSingleResult(q).orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        this.getEntityManager().refresh(collection);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSCollection u = em.getReference(CMSCollection.class, collection.getId());
            em.remove(u);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id");
        q.setParameter("id", category.getId());
        List<CMSPage> pageList = q.getResultList();
        return pageList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesForSubtheme(String subtheme) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT DISTINCT page FROM CMSPage page WHERE page.subThemeDiscriminatorValue = :subtheme");
        q.setParameter("subtheme", subtheme);
        List<CMSPage> pageList = q.getResultList();
        return pageList;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesForRecord(java.lang.String, io.goobi.viewer.model.cms.Category)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException {
        preQuery();
        Query q;
        if (category != null) {
            q = getEntityManager().createQuery(
                    "SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id AND page.relatedPI = :pi");
            q.setParameter("id", category.getId());
        } else {
            StringBuilder sbQuery = new StringBuilder(70);
            sbQuery.append("SELECT o from CMSPage o WHERE o.relatedPI='").append(pi).append("'");

            q = getEntityManager().createQuery("SELECT page FROM CMSPage page WHERE page.relatedPI = :pi");
        }
        q.setParameter("pi", pi);
        List<CMSPage> pageList = q.getResultList();
        return pageList;
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
        Query q = getEntityManager().createQuery("SELECT c FROM CMSCategory c ORDER BY c.name");
        q.setFlushMode(FlushModeType.COMMIT);
        List<CMSCategory> list = q.getResultList();
        return list;
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCountPagesUsingCategory(io.goobi.viewer.model.cms.CMSCategory)
     * @should return correct value
     */
    @Override
    public long getCountPagesUsingCategory(CMSCategory category) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT COUNT(o) FROM CMSPage o WHERE :category MEMBER OF o.categories");
        q.setParameter("category", category);

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCountMediaItemsUsingCategory(io.goobi.viewer.model.cms.CMSCategory)
     * @should return correct value
     */
    @Override
    public long getCountMediaItemsUsingCategory(CMSCategory category) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT COUNT(o) FROM CMSMediaItem o WHERE :category MEMBER OF o.categories");
        q.setParameter("category", category);

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /**
     * {@inheritDoc}
     *
     * Persist a new {@link CMSCategory} object
     */
    @Override
    public void addCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(category);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Update an existing {@link CMSCategory} object in the persistence context
     */
    @Override
    public void updateCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(category);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSCategory o = em.getReference(CMSCategory.class, category.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
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
        Query q = getEntityManager().createQuery("SELECT c FROM CMSCategory c WHERE c.name = :name");
        q.setParameter("name", name);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
        return category;
    }

    /**
     * {@inheritDoc}
     *
     * Search the persistence context for a {@link CMSCategory} with the given unique id.
     */
    @Override
    public CMSCategory getCategory(Long id) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT c FROM CMSCategory c WHERE c.id = :id");
        q.setParameter("id", id);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
        return category;
    }

    /**
     * {@inheritDoc}
     *
     * Check if the database contains a table of the given name. Used by backward-compatibility routines
     */
    @Override
    public boolean tableExists(String tableName) throws SQLException {
        EntityTransaction transaction = getEntityManager().getTransaction();
        transaction.begin();
        Connection connection = getEntityManager().unwrap(Connection.class);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        } finally {
            transaction.commit();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Check if the database contains a column in a table with the given names. Used by backward-compatibility routines
     */
    @Override
    public boolean columnsExists(String tableName, String columnName) throws SQLException {
        EntityTransaction transaction = getEntityManager().getTransaction();
        transaction.begin();
        Connection connection = getEntityManager().unwrap(Connection.class);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        } finally {
            transaction.commit();
        }
    }

    /** {@inheritDoc} */
    @Override
    public PersistentAnnotation getAnnotation(Long id) throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT a FROM PersistentAnnotation a WHERE a.id = :id");
        q.setParameter("id", id);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        PersistentAnnotation annotation = (PersistentAnnotation) getSingleResult(q).orElse(null);
        return annotation;
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT a FROM PersistentAnnotation a");
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
        int count = 1;
        for (Question question : campaign.getQuestions()) {
            q.setParameter("questionId_" + count, question.getId());
            count++;
        }
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
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
    public List<PersistentAnnotation> getAnnotationsForWork(String pi) throws DAOException {
        preQuery();
        String query = "SELECT a FROM PersistentAnnotation a WHERE a.targetPI = :pi";
        Query q = getEntityManager().createQuery(query);
        q.setParameter("pi", pi);

        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCountForWork(String pi) throws DAOException {
        preQuery();
        String query = "SELECT COUNT(a) FROM PersistentAnnotation a WHERE a.targetPI = :pi";
        Query q = getEntityManager().createQuery(query);
        q.setParameter("pi", pi);

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotationsForTarget(String pi, Integer page) throws DAOException {
        preQuery();
        String query = "SELECT a FROM PersistentAnnotation a WHERE a.targetPI = :pi";
        if (page != null) {
            query += " AND a.targetPageOrder = :page";
        } else {
            query += " AND a.targetPageOrder IS NULL";
        }
        Query q = getEntityManager().createQuery(query);
        q.setParameter("pi", pi);
        if (page != null) {
            q.setParameter("page", page);
        }

        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCountForTarget(String pi, Integer page) throws DAOException {
        preQuery();
        String query = "SELECT COUNT(a) FROM PersistentAnnotation a WHERE a.targetPI = :pi";
        if (page != null) {
            query += " AND a.targetPageOrder = :page";
        } else {
            query += " AND a.targetPageOrder IS NULL";
        }
        Query q = getEntityManager().createQuery(query);
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
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT a FROM PersistentAnnotation a WHERE a.targetPI = :pi");
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
        if (!campaign.getQuestions().isEmpty()) {
            int count = 1;
            for (Question question : campaign.getQuestions()) {
                q.setParameter("questionId_" + count, question.getId());
                count++;
            }

        }
        q.setParameter("pi", pi);
        for (Question question : campaign.getQuestions()) {

        }

        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT a FROM PersistentAnnotation a WHERE a.targetPI = :pi");
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
        Query q = getEntityManager().createQuery(sbQuery.toString());
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
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getAnnotationsForUser(java.lang.Long)
     * @should return correct rows
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotationsForUserId(Long userId) throws DAOException {
        if (userId == null) {
            return Collections.emptyList();
        }

        preQuery();
        String query = "SELECT a FROM PersistentAnnotation a WHERE a.creatorId = :userId OR a.reviewerId = :userId";

        return getEntityManager().createQuery(query).setParameter("userId", userId).getResultList();
    }

    /**
     * {@inheritDoc}
     * 
     * @should return correct rows
     * @should filter by campaign name correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PersistentAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters) throws DAOException {
        synchronized (crowdsourcingRequestLock) {
            preQuery();
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM PersistentAnnotation a");
            StringBuilder order = new StringBuilder();
            try {
                Map<String, Object> params = new HashMap<>();
                String filterString = createAnnotationsFilterQuery(null, filters, params);
                if (StringUtils.isNotEmpty(sortField)) {
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(" DESC");
                    }
                }
                sbQuery.append(filterString).append(order);

                logger.trace(sbQuery.toString());
                Query q = getEntityManager().createQuery(sbQuery.toString());
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
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getAnnotationCount(Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM PersistentAnnotation a");
        Map<String, Object> params = new HashMap<>();
        Query q = getEntityManager().createQuery(sbQuery.append(createAnnotationsFilterQuery(null, filters, params)).toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAnnotation(PersistentAnnotation annotation) throws DAOException {
        if (getAnnotation(annotation.getId()) != null) {
            return false;
        }
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(annotation);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateAnnotation(io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    /** {@inheritDoc} */
    @Override
    public boolean updateAnnotation(PersistentAnnotation annotation) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(annotation);
            em.getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteAnnotation(io.goobi.viewer.model.annotation.PersistentAnnotation)
     */
    /** {@inheritDoc} */
    @Override
    public boolean deleteAnnotation(PersistentAnnotation annotation) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            PersistentAnnotation o = em.getReference(PersistentAnnotation.class, annotation.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            em.close();
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
        try {
            GeoMap o = getEntityManager().find(GeoMap.class, mapId);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllGeoMaps()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<GeoMap> getAllGeoMaps() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT u FROM GeoMap u");
        q.setFlushMode(FlushModeType.COMMIT);
        List<GeoMap> list = q.getResultList();
        list.forEach(map -> {
            updateFromDatabase(map.getId(), GeoMap.class);
        });
        return list;
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(map);
            em.getTransaction().commit();
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(map);
            em.getTransaction().commit();
            //Update the map in the persistent entity manager, removing the feature cache string
            GeoMap o = this.getEntityManager().getReference(GeoMap.class, map.getId());
            o.updateFeatures();
            this.getEntityManager().refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            em.close();
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
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            GeoMap o = em.getReference(GeoMap.class, map.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPagesUsingMap(io.goobi.viewer.model.maps.GeoMap)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException {
        preQuery();

        Query qItems = getEntityManager().createQuery(
                "SELECT item FROM CMSContentItem item WHERE item.geoMap = :map");
        qItems.setParameter("map", map);
        List<CMSContentItem> itemList = qItems.getResultList();

        Query qWidgets = getEntityManager().createQuery(
                "SELECT ele FROM CMSSidebarElement ele WHERE ele.geoMapId = :mapId");
        qWidgets.setParameter("mapId", map.getId());
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
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#saveTermsOfUse(io.goobi.viewer.model.security.TermsOfUse)
     */
    @Override
    public boolean saveTermsOfUse(TermsOfUse tou) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            if (tou.getId() == null) {
                //create initial tou
                em.persist(tou);
            } else {
                em.merge(tou);
            }
            em.getTransaction().commit();

            tou = this.getEntityManager().getReference(TermsOfUse.class, tou.getId());
            this.getEntityManager().refresh(tou);
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTermsOfUse()
     */
    @Override
    public TermsOfUse getTermsOfUse() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT u FROM TermsOfUse u");
        //         q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        @SuppressWarnings("unchecked")
        List<TermsOfUse> results = q.getResultList();
        if (results.isEmpty()) {
            //No results. Just return a new object which may be saved later
            return new TermsOfUse();
        }
        return results.get(0);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#isTermsOfUseActive()
     */
    @Override
    public boolean isTermsOfUseActive() throws DAOException {
        preQuery();
        Query q = getEntityManager().createQuery("SELECT u.active FROM TermsOfUse u");
        @SuppressWarnings("unchecked")
        List<Boolean> results = q.getResultList();
        if (results.isEmpty()) {
            //If no terms of use object exists, it is inactive
            return false;
        }
        return results.get(0);
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getRecordNotes(int, int, java.lang.String, boolean, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getRecordNotes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSRecordNote a");
        StringBuilder order = new StringBuilder();
        try {
            Map<String, Object> params = new HashMap<>();
            if (filters != null) {
                String filterValue = filters.values().stream().findAny().orElse(null);
                if (StringUtils.isNotBlank(filterValue)) {
                    String filterString =
                            " WHERE (UPPER(a.recordPi) LIKE :filter OR UPPER(a.recordTitle) LIKE :filter OR UPPER(a.noteTitle) LIKE :filter)";
                    params.put("filter", sanitizeQueryParam(filterValue, true));
                    sbQuery.append(filterString);
                }
            }
            if (StringUtils.isNotEmpty(sortField)) {
                order.append(" ORDER BY a.").append(sortField);
                if (descending) {
                    order.append(" DESC");
                }
            }
            sbQuery.append(order);

            logger.trace(sbQuery.toString());
            Query q = getEntityManager().createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
            //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSRecordNotes. Returning empty list");
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllRecordNotes()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getAllRecordNotes() throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSRecordNote a");
        try {
            logger.trace(sbQuery.toString());
            Query q = getEntityManager().createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSRecordNotes. Returning empty list");
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getRecordNotesForPi(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSRecordNote> getRecordNotesForPi(String pi, boolean displayedNotesOnly) throws DAOException {
        preQuery();
        String query = "SELECT a FROM CMSRecordNote a WHERE a.recordPi = :pi";
        if (displayedNotesOnly) {
            query += " AND a.displayNote = :display";
        }
        logger.trace(query);
        Query q = getEntityManager().createQuery(query.toString());
        q.setParameter("pi", pi);
        if (displayedNotesOnly) {
            q.setParameter("display", true);
        }
        q.setFlushMode(FlushModeType.COMMIT);
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getRecordNote(java.lang.Long)
     */
    @Override
    public CMSRecordNote getRecordNote(Long id) throws DAOException {
        preQuery();
        try {
            CMSRecordNote o = getEntityManager().getReference(CMSRecordNote.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return new CMSRecordNote(o);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
    @Override
    public boolean addRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        try {
            getEntityManager().getTransaction().begin();
            getEntityManager().persist(note);
            getEntityManager().getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
    @Override
    public boolean updateRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        try {
            getEntityManager().getTransaction().begin();
            getEntityManager().merge(note);
            getEntityManager().getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteRecordNote(io.goobi.viewer.model.cms.CMSRecordNote)
     */
    @Override
    public boolean deleteRecordNote(CMSRecordNote note) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSRecordNote o = em.getReference(CMSRecordNote.class, note.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllSliders()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSSlider> getAllSliders() throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSSlider a");
        try {
            logger.trace(sbQuery.toString());
            Query q = getEntityManager().createQuery(sbQuery.toString());
            q.setFlushMode(FlushModeType.COMMIT);
            return q.getResultList();
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get CMSSliders. Returning empty list");
            return Collections.emptyList();
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
        try {
            CMSSlider o = getEntityManager().getReference(CMSSlider.class, id);
            if (o != null) {
                getEntityManager().refresh(o);
            }
            return new CMSSlider(o);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
    @Override
    public boolean addSlider(CMSSlider slider) throws DAOException {
        preQuery();
        try {
            getEntityManager().getTransaction().begin();
            getEntityManager().persist(slider);
            getEntityManager().getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
    @Override
    public boolean updateSlider(CMSSlider slider) throws DAOException {
        preQuery();
        try {
            getEntityManager().getTransaction().begin();
            getEntityManager().merge(slider);
            getEntityManager().getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
    @Override
    public boolean deleteSlider(CMSSlider slider) throws DAOException {
        preQuery();
        try {
            getEntityManager().getTransaction().begin();
            CMSSlider o = getEntityManager().getReference(CMSSlider.class, slider.getId());
            getEntityManager().remove(o);
            getEntityManager().getTransaction().commit();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            getEntityManager().close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPagesUsingSlider(io.goobi.viewer.model.cms.CMSSlider)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getPagesUsingSlider(CMSSlider slider) throws DAOException {
        preQuery();

        Query qItems = getEntityManager().createQuery(
                "SELECT item FROM CMSContentItem item WHERE item.slider = :slider");
        qItems.setParameter("slider", slider);
        List<CMSContentItem> itemList = qItems.getResultList();

        List<CMSPage> pageList = itemList.stream()
                .map(CMSContentItem::getOwnerPageLanguageVersion)
                .map(CMSPageLanguageVersion::getOwnerPage)
                .distinct()
                .collect(Collectors.toList());

        return pageList;
    }

}
