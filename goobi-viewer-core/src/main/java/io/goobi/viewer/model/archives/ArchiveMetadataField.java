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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ArchiveMetadataField {

    /** contains the internal name of the field. The value can be used to translate the field in the messages files */
    private String label;

    /**
     * metadata level, allowed values are 1-7:
     * <ul>
     * <li>1: metadata for Identity Statement Area</li>
     * <li>2: Context Area</li>
     * <li>3: Content and Structure Area</li>
     * <li>4: Condition of Access and Use Area</li>
     * <li>5: Allied Materials Area</li>
     * <li>6: Note Area</li>
     * <li>7: Description Control Area</li>
     * </ul>
     */
    private Integer type;

    /** Index field containing the metadata value. */
    private String indexField;

    /** contains the metadata values */
    private List<FieldValue> values;

    /** links to the ead node. Required to set the title field for the entry while parsing metadata */
    //    @ToString.Exclude
    private ArchiveEntry eadEntry;

    /**
     * 
     * @param label
     * @param type
     * @param indexField
     */
    public ArchiveMetadataField(String label, Integer type, String indexField) {
        this.label = label;
        this.type = type;
        this.indexField = indexField;
    }

    public boolean isFilled() {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (FieldValue val : values) {
            if (StringUtils.isNotBlank(val.getValue())) {
                return true;
            }
        }
        return false;
    }

    public void addFieldValue(FieldValue value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
    }

    public void addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(new FieldValue(this));
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the type
     */
    public Integer getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * @return the indexField
     */
    public String getIndexField() {
        return indexField;
    }

    /**
     * @param indexField the indexField to set
     */
    public void setIndexField(String indexField) {
        this.indexField = indexField;
    }

    public String getValue() {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0).getValue();
    }

    /**
     * @return the values
     */
    public List<FieldValue> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<FieldValue> values) {
        this.values = values;
    }

    /**
     * @return the eadEntry
     */
    public ArchiveEntry getEadEntry() {
        return eadEntry;
    }

    /**
     * @param eadEntry the eadEntry to set
     */
    public void setEadEntry(ArchiveEntry eadEntry) {
        this.eadEntry = eadEntry;
    }
}
