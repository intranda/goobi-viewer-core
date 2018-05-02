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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingDirection;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;

/**
 * @author Florian Alpers
 *
 */
public class Sequence extends AbstractPresentationModelElement implements IPresentationModelElement {

    private static final String TYPE = "sc:Sequence";

    
    private final List<Canvas> canvases = new ArrayList<>();

    
    /**
     * @param id
     */
    public Sequence(URI id) {
        super(id);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement#getType()
     */
    @Override
    public String getType() {
        return TYPE;
    }
    
    /**
     * @return the images
     */
    public List<Canvas> getCanvases() {
        return canvases.isEmpty() ? null : canvases;
    }
    
    public void addCanvas(Canvas image) {
        this.canvases.add(image);
    }
    
    /*TODO: viewinghint dependent on configuration or metadata*/
    public ViewingHint getViewingHint() {
        return ViewingHint.paged;
    }
    
    /*TODO: viewingdirection dependent on configuration or metadata*/
    public ViewingDirection getViewingDirection() {
        return ViewingDirection.LEFT_TO_RIGHT;
    }

}
