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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class ViewBean implements Serializable {

    private static final long serialVersionUID = 1408443482641406496L;

    private Map<String, Object> viewObjects = new ConcurrentHashMap<String, Object>();

    private static final boolean ALLOW_CACHING = true;

    @Inject
    private HttpServletRequest httpRequest;

    public Object get(String key) {
        return viewObjects.get(key);
    }

    public Object put(String key, Object object) {
        return this.viewObjects.put(key, object);
    }

    public boolean containsKey(String key) {
        return ALLOW_CACHING && this.viewObjects.containsKey(key);
    }

    public HttpServletRequest getRequest() {
        return this.httpRequest;
    }

    public void cleanViewObjects() {
        this.viewObjects = new ConcurrentHashMap<String, Object>();
    }

    public void removeObjects(String keyRegex) {
        List<String> keys = this.viewObjects.keySet().stream().filter(key -> key.matches(keyRegex)).collect(Collectors.toList());
        keys.forEach(this.viewObjects::remove);
    }

}
