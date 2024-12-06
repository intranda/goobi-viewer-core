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
package io.goobi.viewer.api.rest.model.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;

import io.goobi.viewer.controller.DataManager;

public class MonitoringStatus {

    public static final String KEY_DATABASE = "database";
    public static final String KEY_IMAGES = "images";
    public static final String KEY_MESSAGE_QUEUE = "mq";
    public static final String KEY_SOLR = "solr";
    public static final String KEY_SOLRSCHEMA = "solrschema";

    public static final String STATUS_DISABLED = "disabled";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_OK = "ok";

    private final Map<String, String> monitoring = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> versions = new LinkedHashMap<>();
    private final String theme;

    public MonitoringStatus() {
        monitoring.put(KEY_SOLR, STATUS_OK);
        monitoring.put(KEY_SOLRSCHEMA, STATUS_OK);
        monitoring.put(KEY_DATABASE, STATUS_OK);
        monitoring.put(KEY_IMAGES, STATUS_OK);
        this.theme = DataManager.getInstance().getConfiguration().getTheme();
    }

    /**
     * @return the monitoring
     */
    public Map<String, String> getMonitoring() {
        return monitoring;
    }

    /**
     * @return the versions
     */
    public Map<String, Map<String, String>> getVersions() {
        return versions;
    }

    /**
     * @return the theme
     */
    public String getTheme() {
        return theme;
    }
}
