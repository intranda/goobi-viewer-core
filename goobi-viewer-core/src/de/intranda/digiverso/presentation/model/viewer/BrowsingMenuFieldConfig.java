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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class BrowsingMenuFieldConfig implements Serializable {

    private static final long serialVersionUID = 3986773493941416989L;

    private String field;
    private String sortField;
    private List<String> docstructFilters = new ArrayList<>();
    private boolean recordsAndAnchorsOnly = false;

    public BrowsingMenuFieldConfig(String field, String sortField, String docstructFilterString, boolean recordsAndAnchorsOnly) {
        this.field = field;
        this.sortField = sortField;
        if (StringUtils.isNotEmpty(docstructFilterString)) {
            String[] docstrcutFilterStringSplit = docstructFilterString.split(";");
            for (String filter : docstrcutFilterStringSplit) {
                if (StringUtils.isNotEmpty(filter)) {
                    docstructFilters.add(filter.trim());
                }
            }
        }
        this.recordsAndAnchorsOnly = recordsAndAnchorsOnly;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @return the docstructFilters
     */
    public List<String> getDocstructFilters() {
        return docstructFilters;
    }

    /**
     * @return the recordsAndAnchorsOnly
     */
    public boolean isRecordsAndAnchorsOnly() {
        return recordsAndAnchorsOnly;
    }
}
