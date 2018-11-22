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
package de.intranda.digiverso.presentation.dao.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.AlphabetIterator;
import de.intranda.digiverso.presentation.dao.IDAO;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPageUpdate;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.user.IpRange;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;
import de.intranda.digiverso.presentation.model.security.user.UserRole;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob.JobStatus;
import de.intranda.digiverso.presentation.model.viewer.PageType;

public class JPADAO implements IDAO {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(JPADAO.class);
    private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "intranda_viewer_tomcat";
    private static final String MULTIKEY_SEPARATOR = "_";

    private final EntityManagerFactory factory;
    private EntityManager em;
    private Object cmsRequestLock = new Object();
    private Object overviewPageRequestLock = new Object();

    public JPADAO() throws DAOException {
        this(null);
        logger.trace("JPADAO()");
    }

    public EntityManagerFactory getFactory() {
        return this.factory;
    }

    public EntityManager getEntityManager() {
        return em;
    }

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

            em = factory.createEntityManager();
            createDiscriminatorRow();
        } catch (DatabaseException | PersistenceException e) {
            logger.error(e.getMessage(), e);
            throw new DAOException(e.getMessage());
        }
    }

    /**
     * @throws DAOException
     * 
     */
    private void createDiscriminatorRow() throws DAOException {
        try {
            preQuery();
            em.getTransaction().begin();
            Query q = em.createQuery("UPDATE CMSSidebarElement element SET element.widgetType = '" + CMSSidebarElement.class.getSimpleName()
                    + "' WHERE element.widgetType IS NULL");
            q.executeUpdate();
            em.getTransaction().commit();
        } catch (DAOException e) {
            throw new DAOException(e.getMessage());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllUsers(boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getAllUsers(boolean refresh) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u");
        if (refresh) {
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        }
        return q.getResultList();
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserCount(java.util.Map)
     * @should return correct count
     * @should filter correctly
     */
    @Override
    public long getUserCount(Map<String, String> filters) throws DAOException {
        return getRowCount("User", filters);
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUsers(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM User o");
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

                String[] keyParts = key.split(MULTIKEY_SEPARATOR);
                int keyPartCount = 0;
                sbQuery.append(" ( ");
                for (String keyPart : keyParts) {
                    if (keyPartCount > 0) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("UPPER(o.").append(keyPart).append(") LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                    keyPartCount++;
                }
                sbQuery.append(" ) ");
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        logger.trace(sbQuery.toString());
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key.replaceAll(MULTIKEY_SEPARATOR, ""), "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUser(long)
     */
    @Override
    public User getUser(long id) throws DAOException {
        preQuery();
        try {
            User o = em.getReference(User.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserByEmail(java.lang.String)
     */
    @Override
    public User getUserByEmail(String email) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.email) = :email");
        if (email != null) {
            q.setParameter("email", email.toUpperCase());
        }
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserByOpenId(java.lang.String)
     */
    @Override
    public User getUserByOpenId(String identifier) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE :claimed_identifier MEMBER OF u.openIdAccounts");
        q.setParameter("claimed_identifier", identifier);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserByNickname(java.lang.String)
     */
    @Override
    public User getUserByNickname(String nickname) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.nickName) = :nickname");
        if (nickname != null) {
            q.setParameter("nickname", nickname.trim().toUpperCase());
        }
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addUser(de.intranda.digiverso.presentation.model.user.User)
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateUser(de.intranda.digiverso.presentation.model.user.User)
     */
    @Override
    public boolean updateUser(User user) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(user)) {
                this.em.refresh(user);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteUser(de.intranda.digiverso.presentation.model.user.User)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllUserGroups()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getAllUserGroups() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserGroups(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
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
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserGroups(de.intranda.digiverso.presentation.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(User owner) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.owner = :owner");
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserGroup(long)
     */
    @Override
    public UserGroup getUserGroup(long id) throws DAOException {
        preQuery();
        try {
            UserGroup o = em.getReference(UserGroup.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserGroup(java.lang.String)
     */
    @Override
    public UserGroup getUserGroup(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name");
        q.setParameter("name", name);
        try {
            UserGroup o = (UserGroup) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addUserGroup(de.intranda.digiverso.presentation.model.user.UserGroup)
     */
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
     * (non-Javadoc)
     *
     * @throws DAOException
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateUserGroup(de.intranda.digiverso.presentation.model.security.user.UserGroup)
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
            if (this.em.contains(userGroup)) {
                this.em.refresh(userGroup);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteUserGroup(de.intranda.digiverso.presentation.model.user.UserGroup)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllBookshelves()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getAllBookshelves() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getPublicBookshelves()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getPublicBookshelves() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Bookshelf o WHERE o.isPublic=true");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getBookshelves(de.intranda.digiverso.presentation.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getBookshelves(User user) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs WHERE bs.owner = :user");
        q.setParameter("user", user);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getBookshelf(long)
     */
    @Override
    public Bookshelf getBookshelf(long id) throws DAOException {
        preQuery();
        try {
            Bookshelf o = em.getReference(Bookshelf.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getBookshelf(java.lang.String)
     */
    @Override
    public Bookshelf getBookshelf(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs WHERE bs.name = :name");
        q.setParameter("name", name);
        try {
            Bookshelf o = (Bookshelf) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addBookshelf(de.intranda.digiverso.presentation.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean addBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(bookshelf);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateBookshelf(de.intranda.digiverso.presentation.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean updateBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(bookshelf);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new items have IDs
            if (this.em.contains(bookshelf)) {
                this.em.refresh(bookshelf);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteBookshelf(de.intranda.digiverso.presentation.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean deleteBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Bookshelf o = em.getReference(Bookshelf.class, bookshelf.getId());
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllRoles()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getAllRoles() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT r FROM Role r");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getRoles(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
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
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getRole(long)
     */
    @Override
    public Role getRole(long id) throws DAOException {
        preQuery();
        try {
            Role o = em.getReference(Role.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getRole(java.lang.String)
     */
    @Override
    public Role getRole(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT r FROM Role r WHERE r.name = :name");
        q.setParameter("name", name);
        try {
            Role o = (Role) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addRole(de.intranda.digiverso.presentation.model.user.Role)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateRole(de.intranda.digiverso.presentation.model.user.Role)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteRole(de.intranda.digiverso.presentation.model.user.Role)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllUserRoles()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getAllUserRoles() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ur FROM UserRole ur");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserRoles(de.intranda.digiverso.presentation.model.user.UserGroup,
     * de.intranda.digiverso.presentation.model.user.User, de.intranda.digiverso.presentation.model.user.Role)
     */
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
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#addUserRole(de.intranda.digiverso.presentation.model.user.UserRole)
     */
    @Override
    public boolean addUserRole(UserRole userRole) throws DAOException {
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateUserRole(de.intranda.digiverso.presentation.model.user.UserRole)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteUserRole(de.intranda.digiverso.presentation.model.user.UserRole)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllLicenseTypes()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT lt FROM LicenseType lt");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOpenAccessLicenseTypes()
     * @should only return non open access license types
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getNonOpenAccessLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.openAccess = :openAccess");
            q.setParameter("openAccess", false);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getLicenseTypes(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o");
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
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getLicenseType(long)
     */
    @Override
    public LicenseType getLicenseType(long id) throws DAOException {
        preQuery();
        try {
            LicenseType o = em.getReference(LicenseType.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getLicenseType(java.lang.String)
     */
    @Override
    public LicenseType getLicenseType(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.name = :name");
        q.setParameter("name", name);
        try {
            LicenseType o = (LicenseType) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addLicenseType(de.intranda.digiverso.presentation.model.user.LicenseType)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateLicenseType(de.intranda.digiverso.presentation.model.user.LicenseType)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteLicenseType(de.intranda.digiverso.presentation.model.user.LicenseType)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllIpRanges()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getAllIpRanges() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ipr FROM IpRange ipr");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getIpRanges(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
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
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getIpRange(long)
     */
    @Override
    public IpRange getIpRange(long id) throws DAOException {
        preQuery();
        try {
            IpRange o = em.find(IpRange.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getIpRange(java.lang.String)
     */
    @Override
    public IpRange getIpRange(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ipr FROM IpRange ipr WHERE ipr.name = :name");
        q.setParameter("name", name);
        try {
            IpRange o = (IpRange) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#addIpRange(de.intranda.digiverso.presentation.model.user.IpRange)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateIpRange(de.intranda.digiverso.presentation.model.user.IpRange)
     */
    @Override
    public boolean updateIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(ipRange);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(ipRange)) {
                this.em.refresh(ipRange);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteIpRange(de.intranda.digiverso.presentation.model.user.IpRange)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllComments()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getAllComments() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Comment o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getComments(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM Comment o");
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
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCommentsForPage(java.lang.String, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForPage(String pi, int page, boolean topLevelOnly) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o FROM Comment o WHERE o.pi = :pi AND o.page = :page");
        if (topLevelOnly) {
            sbQuery.append(" AND o.parent IS NULL");
        }
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setParameter("page", page);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getComment(long)
     */
    @Override
    public Comment getComment(long id) throws DAOException {
        preQuery();
        try {
            Comment o = em.getReference(Comment.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#addComment(de.intranda.digiverso.presentation.model.annotation.Comment)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateComment(de.intranda.digiverso.presentation.model.annotation.Comment)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteComment(de.intranda.digiverso.presentation.model.annotation.Comment)
     */
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
     * Gets all page numbers (order) within a work with the given pi which contain comments
     * 
     * @param pi
     * @return
     * @throws DAOException
     */
    @Override
    public List<Integer> getPagesWithComments(String pi) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o.page FROM Comment o WHERE o.pi = :pi");
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        List<Integer> results = q.getResultList();
        return results.stream().distinct().sorted().collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllSearches()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getAllSearches() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Search o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getSearchCount(de.intranda.digiverso.presentation.model.security.user.User, java.util.Map)
     * @should filter results correctly
     */
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
        Query q = em.createQuery(sbQuery.toString());
        if (owner != null) {
            q.setParameter("owner", owner);
        }
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getSearches(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
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
        Query q = em.createQuery(sbQuery.toString());
        if (owner != null) {
            q.setParameter("owner", owner);
        }
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getSearches(de.intranda.digiverso.presentation.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner) throws DAOException {
        preQuery();
        String query = "SELECT o FROM Search o WHERE o.owner = :owner";
        Query q = em.createQuery(query);
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getSearch(long)
     */
    @Override
    public Search getSearch(long id) throws DAOException {
        preQuery();
        try {
            Search o = em.find(Search.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#addSearch(de.intranda.digiverso.presentation.model.search.Search)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateSearch(de.intranda.digiverso.presentation.model.search.Search)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteSearch(de.intranda.digiverso.presentation.model.search.Search)
     */
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

    // Overview page

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOverviewPage(long)
     * @should load overview page correctly
     */
    @Override
    public OverviewPage getOverviewPage(long id) throws DAOException {
        preQuery();
        try {
            OverviewPage o = em.find(OverviewPage.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOverviewPageForRecord(java.lang.String)
     * @should load overview page correctly
     * @should filter by date range correctly
     */
    @Override
    public OverviewPage getOverviewPageForRecord(String pi, Date fromDate, Date toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPage o WHERE o.pi = :pi");
        if (fromDate != null || toDate != null) {
            // To filter by date, look up OverviewPageUpdate rows that have a datestamp in the requested time frame
            sbQuery.append(" AND o.pi = (SELECT DISTINCT u.pi FROM OverviewPageUpdate u WHERE u.pi = :pi");
            if (fromDate != null) {
                sbQuery.append(" AND u.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(" AND u.dateUpdated <= :toDate");
            }
            sbQuery.append(')');
        }
        //        logger.trace(sbQuery.toString());
        synchronized (overviewPageRequestLock) {
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            if (fromDate != null) {
                q.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                q.setParameter("toDate", toDate);
            }
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            try {
                OverviewPage o = (OverviewPage) q.getSingleResult();
                if (o != null) {
                    em.refresh(o);
                }
                return o;
            } catch (NoResultException e) {
                return null;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#addOverviewPage(de.intranda.digiverso.presentation.model.overviewpage.OverviewPage)
     * @should add overview page correctly
     */
    @Override
    public boolean addOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("addOverviewPage");
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(overviewPage);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateOverviewPage(de.intranda.digiverso.presentation.model.overviewpage.OverviewPage)
     * @should update overview page correctly
     */
    @Override
    public boolean updateOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("updateOverviewPage: {}", overviewPage.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(overviewPage);
            em.getTransaction().commit();
            logger.debug("New ID: {}", overviewPage.getId());
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteOverviewPage(de.intranda.digiverso.presentation.model.overviewpage.OverviewPage)
     * @should delete overview page correctly
     */
    @Override
    public boolean deleteOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("deleteOverviewPage: {}", overviewPage.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            OverviewPage o = em.getReference(OverviewPage.class, overviewPage.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOverviewPageCount(java.util.Date, java.util.Date)
     */
    @Override
    public long getOverviewPageCount(Date fromDate, Date toDate) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM OverviewPage o");
        if (fromDate != null) {
            sbQuery.append(" WHERE o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(fromDate == null ? " WHERE " : " AND ").append("o.dateUpdated <= :toDate");
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
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getRecordsWithOverviewPages(java.util.Date, java.util.Date)
     * @should return all overview pages
     * @should paginate results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<OverviewPage> getOverviewPages(int first, int pageSize, Date fromDate, Date toDate) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPage o");
        if (fromDate != null) {
            sbQuery.append(" WHERE o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(fromDate == null ? " WHERE " : " AND ").append("o.dateUpdated <= :toDate");
        }
        sbQuery.append(" ORDER BY o.dateUpdated DESC");
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
    }

    /**
     * (non-Javadoc)
     *
     * @throws DAOException
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOverviewPageUpdatesForRecord(java.lang.String)
     * @should return all updates for record
     * @should sort updates by date descending
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<OverviewPageUpdate> getOverviewPageUpdatesForRecord(String pi) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        preQuery();

        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPageUpdate o WHERE o.pi = :pi");
        sbQuery.append(" ORDER BY o.dateUpdated desc");
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#isOverviewPageHasUpdates(java.lang.String, java.util.Date, java.util.Date)
     * @should return status correctly
     */
    @Override
    public boolean isOverviewPageHasUpdates(String pi, Date fromDate, Date toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM OverviewPage o WHERE o.pi = :pi");
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
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getOverviewPageUpdate(long)
     * @should load object correctly
     */
    @Override
    public OverviewPageUpdate getOverviewPageUpdate(long id) throws DAOException {
        preQuery();
        try {
            OverviewPageUpdate o = em.find(OverviewPageUpdate.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#addOverviewPageUpdate(de.intranda.digiverso.presentation.model.search.Search)
     * @should add update correctly
     */
    @Override
    public boolean addOverviewPageUpdate(OverviewPageUpdate update) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(update);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteOverviewPageUpdate(de.intranda.digiverso.presentation.model.search.Search)
     * @should delete update correctly
     */
    @Override
    public boolean deleteOverviewPageUpdate(OverviewPageUpdate update) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            OverviewPageUpdate o = em.getReference(OverviewPageUpdate.class, update.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    // Downloads

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllDownloadJobs()
     * @should return all objects
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<DownloadJob> getAllDownloadJobs() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM DownloadJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getDownloadJob(long)
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJob(long id) throws DAOException {
        preQuery();
        try {
            DownloadJob o = em.getReference(DownloadJob.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getDownloadJobByIdentifier(java.lang.String)
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
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("identifier", identifier);
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getDownloadJobByMetadata(java.lang.String, java.lang.String)
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
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("type", type);
        q.setParameter("pi", pi);
        if (logId != null) {
            q.setParameter("logId", logId);
        }
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#addDownloadJob(de.intranda.digiverso.presentation.model.download.DownloadJob)
     * @should add object correctly
     */
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

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateDownloadJob(de.intranda.digiverso.presentation.model.download.DownloadJob)
     * @should update object correctly
     */
    @Override
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException {
        logger.trace("updateDownloadJob: {}", downloadJob.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(downloadJob);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteDownloadJob(de.intranda.digiverso.presentation.model.download.DownloadJob)
     * @should delete object correctly
     */
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
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllCMSPages()
     * @should return all pages
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getAllCMSPages() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSPage o");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCmsPageForStaticPage(java.lang.String)
     */
    @Override
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSPage o WHERE o.staticPageName = :pageName");
            q.setParameter("pageName", pageName);
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            if (!q.getResultList().isEmpty()) {
                return (CMSPage) q.getSingleResult();
            }
        }
        return null;
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPageCount(java.util.Map)
     * @should return correct count
     * @should filter correctly
     */
    @Override
    public long getCMSPageCount(Map<String, String> filters) throws DAOException {
        return getRowCount("CMSPage", filters);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPages(int, int, java.lang.String, boolean, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSPage a");
            StringBuilder order = new StringBuilder();

            Map<String, String> params = new HashMap<>();

            String filterString = createFilterQuery(filters, params);
            
            if (StringUtils.isNotEmpty(sortField)) {
                order.append(" ORDER BY a.").append(sortField);
                if (descending) {
                    order.append(" DESC");
                }
            }
            sbQuery.append(filterString).append(order);

            Query q = em.createQuery(sbQuery.toString());
            params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
            //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

            List<CMSPage> list = q.getResultList();
            return list;
        }
    }

    /**
     * Builds a query string to filter a query across several tables
     * 
     * @param filters   The filters to use
     * @param params    Empty map which will be filled with the used query parameters. These to be added to the query
     * @return  A string consisting of a WHERE and possibly JOIN clause of a query
     */
    public String createFilterQuery(Map<String, String> filters, Map<String, String> params) {
        StringBuilder join = new StringBuilder();

        List<String> filterKeys = new ArrayList<>();
        StringBuilder where = new StringBuilder();
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
                    if (count > 0) {
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
                        if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                            where.append("UPPER(" + tableKey + ".")
                                    .append(keyPart)
                                    .append(") LIKE :")
                                    .append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        } else if ("classifications".equals(joinTable)) {
                            where.append(tableKey).append(" LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));

                        } else {
                            where.append("UPPER(" + tableKey + ".")
                            .append(keyPart)
                            .append(") LIKE :")
                            .append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        }
                        keyPartCount++;
                    }
                    where.append(" ) ");
                    count++;

                    //apply join table if neccessary
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
                    }
                    params.put(key.replaceAll(MULTIKEY_SEPARATOR, ""), "%" + value.toUpperCase() + "%");
                }
                if (count > 1) {
                    where.append(" )");
                }
            }
        }
        String filterString = join.append(where).toString();
        return filterString;
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPagesByClassification()
     * @should return all pages with given classification
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPagesByClassification(String pageClassification) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            StringBuilder sbQuery = new StringBuilder(70);
            sbQuery.append("SELECT o from CMSPage o WHERE '").append(pageClassification).append("' MEMBER OF o.classifications");
            Query q = em.createQuery(sbQuery.toString());
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPage(long)
     * @should return correct page
     */
    @Override
    public CMSPage getCMSPage(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            logger.trace("getCMSPage: {}", id);
            preQuery();
            try {
                CMSPage o = em.getReference(CMSPage.class, id);
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPage(long)
     * @should return correct page
     */
    @Override
    public CMSPage getCMSPageForEditing(long id) throws DAOException {
        CMSPage original = getCMSPage(id);
        CMSPage copy = new CMSPage(original);
        return copy;
    }

    @Override
    public CMSSidebarElement getCMSSidebarElement(long id) throws DAOException {

        synchronized (cmsRequestLock) {
            logger.trace("getCMSSidebarElement: {}", id);
            preQuery();
            try {
                CMSSidebarElement o = em.getReference(CMSSidebarElement.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                logger.trace("getCMSSidebarElement END");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.cmsPage = :page");
            q.setParameter("page", page);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#addCMSPage(de.intranda.digiverso.presentation.model.cms.CMSPage)
     * @should add page correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateCMSPage(de.intranda.digiverso.presentation.model.cms.CMSPage)
     * @should update page correctly
     */
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
        Object o = null;
        try {
            o = this.em.getReference(CMSPage.class, id);
            this.em.refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                em.remove(o);
            }
            return false;
        }
    }

    private boolean updateFromDatabase(Long id, Class clazz) {
        Object o = null;
        try {
            o = this.em.getReference(clazz, id);
            this.em.refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                em.remove(o);
            }
            return false;
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteCMSPage(de.intranda.digiverso.presentation.model.cms.CMSPage)
     * @should delete page correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllCMSMediaItems()
     * @should return all items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSMediaItem o");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllCMSMediaItems()
     * @should return all items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSMediaItem o WHERE o.collection = true");
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSMediaItem(long)
     * @should return correct item
     */
    @Override
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            try {
                CMSMediaItem o = em.getReference(CMSMediaItem.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#addCMSMediaItem(de.intranda.digiverso.presentation.model.cms.CMSMediaItem)
     * @should add item correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateCMSMediaItem(de.intranda.digiverso.presentation.model.cms.CMSMediaItem)
     * @should update item correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteCMSMediaItem(de.intranda.digiverso.presentation.model.cms.CMSMediaItem)
     * @should delete item correctly
     * @should not delete referenced items
     */
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

    @Override
    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            List<CMSPage> ownerList = new ArrayList<>();
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSContentItem o WHERE o.mediaItem = :media");
            q.setParameter("media", item);
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
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllCMSNavigationItems()
     * @should return all top items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.parentItem IS NULL");
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            List<CMSNavigationItem> list = q.getResultList();
            Collections.sort(list);
            return list;
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSNavigationItem(long)
     * @should return correct item and child items
     */
    @Override
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            try {
                CMSNavigationItem o = em.find(CMSNavigationItem.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#addCMSNavigationItem(de.intranda.digiverso.presentation.model.cms.CMSNavigationItem)
     * @should add item and child items correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateCMSNavigationItem(de.intranda.digiverso.presentation.model.cms.CMSNavigationItem)
     * @should update item and child items correctly
     */
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

    /**
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteCMSNavigationItem(de.intranda.digiverso.presentation.model.cms.CMSNavigationItem)
     * @should delete item and child items correctly
     */
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

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllTranskribusJobs()
     * @should return all jobs
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM TranskribusJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getTranskribusJobs(java.lang.String, java.lang.String, de.intranda.digiverso.presentation.model.transkribus.TranskribusJob.JobStatus)
     */
    @Override
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#addTranskribusJob(de.intranda.digiverso.presentation.model.transkribus.TranskribusJob)
     * @should add job correctly
     */
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

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateTranskribusJob(de.intranda.digiverso.presentation.model.transkribus.TranskribusJob)
     * @should update job correctly
     */
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

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteTranskribusJob(de.intranda.digiverso.presentation.model.transkribus.TranskribusJob)
     * @should delete job correctly
     */
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

    /*
     * (non-Javadoc)
     *
     * @see de.intranda.digiverso.presentation.dao.IDAO#shutdown()
     */
    @Override
    public void shutdown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        // This is MySQL specific, but needed to prevent OOMs when redeploying
        //        try {
        //            AbandonedConnectionCleanupThread.shutdown();
        //        } catch (InterruptedException e) {
        //            logger.error(e.getMessage(), e);
        //        }
    }

    public void preQuery() throws DAOException {
        if (em == null) {
            throw new DAOException("EntityManager is not initialized");
        }
        if (!em.isOpen()) {
            em = factory.createEntityManager();
        }
        //        EntityManager em = factory.createEntityManager();
        //        try {
        //            Query q = em.createNativeQuery("SELECT 1");
        //            q.getResultList();
        //        } finally {
        //            em.close();
        //        }

    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getUserGroupCount()
     * @should return correct count
     */
    @Override
    public long getUserGroupCount(Map<String, String> filters) throws DAOException {
        return getRowCount("UserGroup", filters);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getRoleCount()
     * @should return correct count
     */
    @Override
    public long getRoleCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Role", filters);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getLicenseTypeCount()
     * @should return correct count
     */
    @Override
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("LicenseType", filters);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getIpRangeCount()
     * @should return correct count
     */
    @Override
    public long getIpRangeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("IpRange", filters);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCommentCount(java.util.Map)
     * @should return correct count
     * @should filter correctly
     */
    @Override
    public long getCommentCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Comment", filters);
    }

    /**
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSPagesCount(java.util.Map)
     */
    @Override
    public long getCMSPagesCount(Map<String, String> filters) throws DAOException {
        return getRowCount("CMSPage", filters);
    }

    /**
     * Universal method for returning the row count for the given class and filters.
     * 
     * @param className
     * @param filters
     * @return
     * @throws DAOException
     */
    private long getRowCount(String className, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM ").append(className).append(" a");
        Map<String, String> params = new HashMap<>();
        String filterQuery = createFilterQuery(filters, params);
//        StringBuilder sbFilterQuery = null;
//        if (filters != null && !filters.isEmpty()) {
//            sbFilterQuery = new StringBuilder();
//            for (String key : filters.keySet()) {
//                if (StringUtils.isEmpty(filters.get(key))) {
//                    continue;
//                } else if (sbFilterQuery.length() == 0) {
//                    sbFilterQuery.append(" WHERE ");
//                } else {
//                    sbFilterQuery.append(" AND ");
//                }
//                String[] keyParts = key.split(MULTIKEY_SEPARATOR);
//                int keyPartCount = 0;
//                sbFilterQuery.append(" ( ");
//                for (String keyPart : keyParts) {
//                    if (keyPartCount > 0) {
//                        sbFilterQuery.append(" OR ");
//                    }
//
//                    sbFilterQuery.append("(o.").append(keyPart).append(") LIKE '%").append(filters.get(key)).append("%'");
//                    keyPartCount++;
//                }
//                sbFilterQuery.append(" ) ");
//            }
//            sbQuery.append(sbFilterQuery.toString());
//        }
        Query q = em.createQuery(sbQuery.append(filterQuery).toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getMatchingTags(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMatchingTags(String inputString) throws DAOException {
        preQuery();
        StringBuilder sbQuery =
                new StringBuilder("SELECT DISTINCT tag_name FROM cms_media_item_tags").append(" WHERE tag_name LIKE '" + inputString + "%'");
        Query q = em.createNativeQuery(sbQuery.toString());
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllTags()
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public List<String> getAllTags() throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT tag_name FROM cms_media_item_tags");
        Query q = em.createNativeQuery(sbQuery.toString());
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getAllStaticPages()
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSStaticPage> getAllStaticPages() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM CMSStaticPage o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#addStaticPage(de.intranda.digiverso.presentation.model.cms.StaticPage)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateStaticPage(de.intranda.digiverso.presentation.model.cms.StaticPage)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteStaticPage(de.intranda.digiverso.presentation.model.cms.StaticPage)
     */
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
        } catch (RollbackException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /**
     * 
     */
    @Override
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException, NonUniqueResultException {
        preQuery();
        Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.cmsPageId = :id");
        q.setParameter("id", page.getId());
        return q.getResultList();
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
//        return getSingleResult(q);
    }


    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getStaticPageForTypeType(de.intranda.digiverso.presentation.dao.PageType)
     */
    @Override
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.pageName = :name");
        q.setParameter("name", pageType.name());
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
    private static <T> Optional<T> getFirstResult(Query q) throws ClassCastException {
        List<Object> results = q.getResultList();
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) results.get(0));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#detach(java.lang.Object)
     */
    @Override
    public void detach(Object object) throws DAOException {
        preQuery();
        em.detach(object);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSCollections(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field");
        q.setParameter("field", solrField);
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#addCMSCollection(de.intranda.digiverso.presentation.model.cms.CMSCollection)
     */
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
     * @see de.intranda.digiverso.presentation.dao.IDAO#updateCMSCollection(de.intranda.digiverso.presentation.model.cms.CMSCollection)
     */
    @Override
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(collection);
            em.getTransaction().commit();
         // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(collection)) {
                this.em.refresh(collection);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#getCMSCollection(java.lang.String, java.lang.String)
     */
    @Override
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field AND c.solrFieldValue = :value");
        q.setParameter("field", solrField);
        q.setParameter("value", solrFieldValue);
        return (CMSCollection) getSingleResult(q).orElse(null);
    }

    @Override
    public void refreshCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        this.em.refresh(collection);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.dao.IDAO#deleteCMSCollection(de.intranda.digiverso.presentation.model.cms.CMSCollection)
     */
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
}
