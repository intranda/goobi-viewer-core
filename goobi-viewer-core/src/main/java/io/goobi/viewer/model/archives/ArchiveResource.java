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
package io.goobi.viewer.model.archives;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
public class ArchiveResource implements Serializable {

    private static final long serialVersionUID = -234029216818092944L;

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /** Displayed name of the resource. */
    private final String resourceName;
    /** Unique identifier of the resource. */
    private final String resourceId;
    private final LocalDateTime modifiedDate;
    private final Long size;
    /** Access conditions of the root element (excluding OPENACCESS) */
    private final List<String> accessConditions = new ArrayList<>();

    /**
     * 
     * @param resourceName
     * @param resourceId
     * @param modifiedDate
     * @param size
     */
    public ArchiveResource(String resourceName, String resourceId, String modifiedDate, String size) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        if (StringUtils.isNotEmpty(modifiedDate)) {
            this.modifiedDate = LocalDateTime.parse(modifiedDate, DATE_TIME_FORMATTER);
        } else {
            this.modifiedDate = LocalDateTime.of(1970, 1, 1, 0, 0);
        }
        this.size = Long.parseLong(size);
    }

    /**
     * @return the resourceName
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * @return the resourceId
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * @return the modifiedDate
     */
    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @return the accessConditions
     */
    public List<String> getAccessConditions() {
        return accessConditions;
    }

    /**
     * @return Combination of databaseName and resourceName
     */
    @Deprecated
    public String getCombinedName() {
        return resourceName.replaceAll("(?i)\\.xml", "");
    }

    @Deprecated
    public String getCombinedId() {
        return getResourceId();
    }

    @Override
    public String toString() {
        return resourceName.replaceAll("(?i)\\.xml", "");
    }
}
