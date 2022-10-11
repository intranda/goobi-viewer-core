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
package io.goobi.viewer.model.cms.pages.content;

import java.util.ArrayList;
import java.util.List;

public class CMSComponentAttribute {

    private final String name;
    private final String label;
    private final String type;
    private final List<String> options;
    private String value;

    public CMSComponentAttribute(String name, String label, String type, List<String> options) {
        super();
        this.name = name;
        this.label = label;
        this.type = type;
        this.options = options;
    }

    public CMSComponentAttribute(CMSComponentAttribute orig) {
        this.name = orig.name;
        this.label = orig.label;
        this.type = orig.type;
        this.options = new ArrayList<>(orig.options);
        this.value = orig.value;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the options
     */
    public List<String> getOptions() {
        return options;
    }

    

}
