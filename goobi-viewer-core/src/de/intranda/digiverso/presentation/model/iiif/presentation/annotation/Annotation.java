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
package de.intranda.digiverso.presentation.model.iiif.presentation.annotation;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Canvas;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Motivation;
import de.intranda.digiverso.presentation.servlets.rest.content.IAnnotation;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.URLOnlySerializer;

/**
 * @author Florian Alpers
 *
 */
public class Annotation extends AbstractPresentationModelElement implements IPresentationModelElement, IAnnotation{
    
    public final static String TYPE = "oa:Annotation";

    private Motivation motivation;
    private Canvas on;
    private IContent resource;
    
    /**
     * @param id
     */
    public Annotation(URI id) {
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
     * @return the motivation
     */
    public Motivation getMotivation() {
        return motivation;
    }

    /**
     * @param motivation the motivation to set
     */
    public void setMotivation(Motivation motivation) {
        this.motivation = motivation;
    }

    /**
     * @return the on
     */
    @JsonSerialize(using=URLOnlySerializer.class)
    public Canvas getOn() {
        return on;
    }

    /**
     * @param on the on to set
     */
    public void setOn(Canvas on) {
        this.on = on;
    }

    /**
     * @return the resource
     */
    public IContent getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(IContent resource) {
        this.resource = resource;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.servlets.rest.content.IComment#getTarget()
     */
    @Override
    @JsonIgnore
    public URI getTarget() throws URISyntaxException {
        return on.getId();
    }

    

}
