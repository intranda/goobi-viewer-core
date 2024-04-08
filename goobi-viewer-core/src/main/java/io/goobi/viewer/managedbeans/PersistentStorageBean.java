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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.omnifaces.cdi.Eager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DataStorage;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.dao.update.DatabaseUpdater;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

/**
 * Used for application wide storage of objects accessible to other managed objects
 *
 * @author florian
 *
 */
@Named("applicationBean")
@Eager
@ApplicationScoped
public class PersistentStorageBean implements DataStorage, Serializable {

    private static final long serialVersionUID = -5127431137772735598L;

    private Map<String, Pair<Object, Instant>> map = new HashMap<>();

    @Inject
    private transient CMSTemplateManager templateManager;
    @Inject
    private transient MessageQueueManager messageBroker;
    private IDAO dao;

    @PostConstruct
    public void startup() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
        new DatabaseUpdater(dao, templateManager).update();
    }

    @PreDestroy
    public void shutdown() {
        //
    }

    public Object get(String key) {
        synchronized (map) {
            return Optional.ofNullable(map.get(key)).map(Pair::getLeft).orElse(null);
        }
    }

    public boolean olderThan(String key, Instant time) {
        synchronized (map) {
            return Optional.ofNullable(map.get(key)).map(Pair::getRight).map(i -> i.isBefore(time)).orElse(true);
        }
    }

    public void put(String key, Object object) {
        synchronized (map) {
            map.put(key, Pair.of(object, Instant.now()));
        }
    }

    /**
     * If the given key exists and the entry is no older than the given timeToLiveMinutes, return the object stored under the key, otherwise store the
     * given object under the given key and return it
     * 
     * @param <T>
     * @param key the identifier under which to store the object
     * @param object the object to store under the given key if the key doesn't exist yet or is older than timeToLiveMinutes
     * @param timeToLive the maximum age in the given time unit the stored object may have to be returned. If it's older, it will be replaced with the
     *            passed object
     * @param unit  The {@link TemporalUnit} in which the timeToLive parameter is given
     * @return the object stored under the given key if viable, otherwise the given object
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T getIfRecentOrPut(String key, T object, long timeToLive, TemporalUnit unit) {
        Instant oldestViable = Instant.now().minus(timeToLive, unit);
        if (contains(key) && !olderThan(key, oldestViable)) {
            return (T) get(key);
        } else {
            put(key, object);
            return object;
        }
    }
    
    /**
     * If the given key exists and the entry is no older than the given timeToLiveMinutes, return the object stored under the key, otherwise store the
     * given object under the given key and return it
     * 
     * @param key the identifier under which to store the object
     * @param object the object to store under the given key if the key doesn't exist yet or is older than timeToLiveMinutes
     * @param timeToLiveMinutes the maximum age in minutes the stored object may have to be returned. If it's older, it will be replaced with the
     *            passed object
     * @return the object stored under the given key if viable, otherwise the given object
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T getIfRecentOrPut(String key, T object, long timeToLiveMinutes) {
        return getIfRecentOrPut(key, object, timeToLiveMinutes, ChronoUnit.MINUTES);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized <T> Optional<T> getIfRecentOrRemove(String key, long timeToLiveMinutes) {
        return getIfRecentOrRemove(key, timeToLiveMinutes, ChronoUnit.MINUTES);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized <T> Optional<T> getIfRecentOrRemove(String key, long timeToLive, TemporalUnit unit) {
        Instant oldestViable = Instant.now().minus(timeToLive, unit);
        if (contains(key) && !olderThan(key, oldestViable)) {
            return Optional.ofNullable((T) get(key));
        } else {
            remove(key);
            return Optional.empty();
        }
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public CMSTemplateManager getTemplateManager() {
        return templateManager;
    }

    public void setTemplateManager(CMSTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public MessageQueueManager getMessageBroker() {
        return messageBroker;
    }

    public void setMessageBroker(MessageQueueManager messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void remove(String key) {
        synchronized (map) {
            this.map.remove(key);
        }
    }
}
