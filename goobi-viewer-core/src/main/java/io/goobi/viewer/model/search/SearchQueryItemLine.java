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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;

/**
 * Object representing a single line with operator + value(s) within an item.
 */
public class SearchQueryItemLine implements Serializable {

    private static final long serialVersionUID = -4514089633841899389L;

    private static final Logger logger = LogManager.getLogger(SearchQueryItemLine.class);

    /** This operator now describes the relation of this item with the other items rather than between terms within this item's query! */
    private SearchItemOperator operator = SearchItemOperator.AND;
    private List<String> values = new ArrayList<>();

    /**
     * Getter for the field <code>operator</code>.
     *
     * @return the operator
     */
    public SearchItemOperator getOperator() {
        return operator;
    }

    /**
     * Setter for the field <code>operator</code>.
     *
     * @param operator the operator to set
     */
    public void setOperator(SearchItemOperator operator) {
        // logger.trace("setOperator: {}", operator);StringTools
        this.operator = operator;
    }

    /**
     * 
     * @return First value
     */
    public String getValue() {
        if (!values.isEmpty()) {
            return values.get(0);
        }

        return null;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value the value to set
     */
    public void setValue(final String value) {
        // logger.trace("setValue: {}", value); //NOSONAR Debug
        String val = StringTools.stripJS(value);
        if (values.isEmpty()) {
            values.add(0, val);
        } else {
            values.set(0, val);
        }
    }

    /**
     * 
     * @param value
     * @return true if values contains given value; false otherwise
     */
    public boolean isValueSet(String value) {
        return values.contains(value);
    }

    /**
     * Sets/unsets the given value in the item, depending on the current status.
     * 
     * @param value Value to set/unset
     * @should set values correctly
     * @should unset values correctly
     */
    public void toggleValue(final String value) {
        String val = StringTools.stripJS(value);
        int index = this.values.indexOf(val);
        if (index >= 0) {
            this.values.remove(index);
        } else {
            this.values.add(val);
        }
    }

    /**
     * @return the value2
     */
    public String getValue2() {
        if (values.size() < 2) {
            return null;
        }

        return values.get(1);
    }

    /**
     * @param value2 the value2 to set
     */
    public void setValue2(final String value2) {
        logger.trace("setValue2: {}", value2);
        String val2 = StringTools.stripJS(value2);
        if (values.isEmpty()) {
            values.add(null);
        }
        values.add(1, val2);
    }

    /**
     * @return the values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<String> values) {
        this.values = values;
    }
}
