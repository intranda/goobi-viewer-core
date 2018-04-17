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
package de.intranda.digiverso.presentation.model.iiif.presentation.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Florian Alpers
 *
 */
public enum ViewingDirection {

    LEFT_TO_RIGHT("left-to-right"),
    RIGHT_TO_LEFT("right-to-left"),
    TOP_TO_BOTTOM("top-to-bottom"),
    BOTTOM_TO_TOP("bottom-to-top");
    
    private String label;
    
    private ViewingDirection(String label) {
        this.label = label;
    }
    
    /**
     * @return the label
     */
    @JsonValue
    public String getLabel() {
        return label;
    }
} 
