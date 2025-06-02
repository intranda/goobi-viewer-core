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
package io.goobi.viewer.managedbeans.storage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.goobi.viewer.controller.DataStorage;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Bean to store variables within a certain scope. Should be extended with beans appplying to a certain jsf scope
 */
public abstract class StorageBean implements DataStorage, Serializable {

    private static final long serialVersionUID = -5738074975696037653L;

    private Map<String, Object> objects = new ConcurrentHashMap<>();

    private static final boolean ALLOW_CACHING = true;

    @Inject
    protected HttpServletRequest request;

    public Object get(String key) {
        return objects.get(key);
    }

    public void put(String key, Object object) {
        this.objects.put(key, object);
    }

    public boolean containsKey(String key) {
        return ALLOW_CACHING && this.objects.containsKey(key);
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public void cleanObjects() {
        this.objects = new ConcurrentHashMap<>();
    }

    public void removeObjects(String keyRegex) {
        List<String> keys = this.objects.keySet().stream().filter(key -> key.matches(keyRegex)).toList();
        keys.forEach(this.objects::remove);
    }
}
