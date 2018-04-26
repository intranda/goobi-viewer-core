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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.ContentLinkSerializer;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.URLOnlySerializer;

/**
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class Range extends AbstractPresentationModelElement implements IPresentationModelElement {

    private static final String TYPE = "sc:Range";

    private final List<Canvas> canvases = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();
    private Layer contentLayer;
    private Canvas startCanvas;
    @JsonIgnore
    private boolean useMembers = false;

    /**
     * @param id
     */
    public Range(URI id) {
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
     * @return the startCanvas
     */
    @JsonSerialize(using=URLOnlySerializer.class)
    public Canvas getStartCanvas() {
        return startCanvas;
    }

    /**
     * @param startCanvas the startCanvas to set
     */
    public void setStartCanvas(Canvas startCanvas) {
        this.startCanvas = startCanvas;
    }

    /**
     * @return the contentLayer
     */
    @JsonSerialize(using=URLOnlySerializer.class)
    public Layer getContentLayer() {
        return contentLayer;
    }

    /**
     * @param contentLayer the contentLayer to set
     */
    public void setContentLayer(Layer contentLayer) {
        this.contentLayer = contentLayer;
    }

    /**
     * @return the canvases
     */
    @JsonSerialize(using = URLOnlySerializer.class)
    public List<Canvas> getCanvases() {
        if(isUseMembers()) {
            return null; 
        } else {            
            return canvases.isEmpty() ? null : canvases;
        }
    }

    /**
     * @return the ranges
     */
    @JsonSerialize(using = URLOnlySerializer.class)
    public List<Range> getRanges() {
        if(isUseMembers()) {
            return null;
        } else {            
            return ranges.isEmpty() ? null : ranges;
        }
    }
    
    @JsonIgnore
    public List<Range> getRangeList() {
        return ranges;
    }
    
    public void resetRanges() {
        this.ranges.clear();
    }

    public void addCanvas(Canvas canvas) {
        this.canvases.add(canvas);
    }

    public void addRange(Range range) {
        this.ranges.add(range);
    }

    @JsonSerialize(using = ContentLinkSerializer.class)
    public List<IPresentationModelElement> getMembers() {
        if(isUseMembers()) {            
            List<IPresentationModelElement> list = new ArrayList<>();
            list.addAll(ranges);
            list.addAll(canvases);
            return list.isEmpty() ? null : list;
        } else {
            return null;
        }
    }

    /**
     * @param useMembers the useMembers to set
     */
    public void setUseMembers(boolean useMembers) {
        this.useMembers = useMembers;
    }

    /**
     * @return the useMembers
     */
    public boolean isUseMembers() {
        return useMembers;
    }

}
