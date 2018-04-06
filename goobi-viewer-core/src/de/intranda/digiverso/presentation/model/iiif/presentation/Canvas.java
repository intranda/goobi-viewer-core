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

import de.intranda.digiverso.presentation.servlets.rest.content.AbstractAnnotation;

/**
 * @author Florian Alpers
 *
 */
public class Canvas extends AbstractPresentationModelElement implements IPresentationModelElement, ICanvas {

    private static final String TYPE = "sc:canvas";
    
    private int width;
    private int height;
    private Sequence within;
    private final List<AbstractAnnotation> images = new ArrayList<>();
    private final List<AnnotationList> otherContent = new ArrayList<>();
    
    /**
     * @param id
     */
    public Canvas(URI id) {
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
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the within
     */
    public Sequence getWithin() {
        return within;
    }
    
    /**
     * @param within the within to set
     */
    public void setWithin(Sequence within) {
        this.within = within;
    }
    
    /**
     * @return the images
     */
    public List<AbstractAnnotation> getImages() {
        return images;
    }
    
    /**
     * @return the otherContent
     */
    public List<AnnotationList> getOtherContent() {
        return otherContent;
    }
    
    public void addImage(AbstractAnnotation image) {
        this.images.add(image);
    }
    
    public void addOtherContent(AnnotationList content) {
        this.otherContent.add(content);
    }
}
