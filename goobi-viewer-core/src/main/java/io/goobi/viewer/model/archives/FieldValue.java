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

public class FieldValue {

    private String value;
    private ArchiveMetadataField field;

    public FieldValue(ArchiveMetadataField field) {
        this.field = field;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (field.getLabel().equals("unittitle")) {
            field.getEadEntry().setLabel(value);
        } else {
            this.value = value;
        }
    }
}
