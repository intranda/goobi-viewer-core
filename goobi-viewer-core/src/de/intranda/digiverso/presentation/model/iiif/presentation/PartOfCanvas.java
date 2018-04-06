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
package de.intranda.digiverso.presentation.model.iiif.presentation;

import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Florian Alpers
 *
 */
public class PartOfCanvas implements ICanvas {

    private static final String ANCHOR = "#xywh=";
    
    private Canvas canvas;
    private Rectangle area;
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.ICanvas#getId()
     */
    @Override
    @JsonValue
    public URI getId() {
        StringBuilder id = new StringBuilder(canvas.getId().toString());
        id.append(ANCHOR);
        id.append(area.x).append(",");
        id.append(area.y).append(",");
        id.append(area.width).append(",");
        id.append(area.height);        
        
        try {
            return new URI(id.toString());
        } catch (URISyntaxException e) {
            return canvas.getId();
        }
    }

    /**
     * @return the canvas
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * @param canvas the canvas to set
     */
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * @return the area
     */
    public Rectangle getArea() {
        return area;
    }

    /**
     * @param area the area to set
     */
    public void setArea(Rectangle area) {
        this.area = area;
    }
    
    

}
