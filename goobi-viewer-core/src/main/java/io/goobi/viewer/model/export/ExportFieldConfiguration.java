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
package io.goobi.viewer.model.export;

public class ExportFieldConfiguration {

    private final String field;
    private String label;

    public ExportFieldConfiguration(String field) {
        this.field = field;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the label
     * @should return field if label null
     */
    public String getLabel() {
        if (label != null) {
            return label;
        }

        return field;
    }

    /**
     * @param label the label to set
     * @return this
     */
    public ExportFieldConfiguration setLabel(String label) {
        this.label = label;
        return this;
    }

}
