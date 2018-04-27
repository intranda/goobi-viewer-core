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
package de.intranda.digiverso.presentation.model.iiif.presentation.content;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.intranda.digiverso.presentation.model.iiif.presentation.enums.DcType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class LinkingContent implements IContent {

    private final URI id;
    private IMetadataValue label;
    private DcType type = null;
    private Format format = Format.TEXT_HTML;
    
    /**
     * 
     */
    public LinkingContent(URI id) {
        this.id = id;
    }
    
    public LinkingContent(URI id, IMetadataValue label) {
        this.id = id;
        this.label = label;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getType()
     */
    @Override
    public DcType getType() {
        return type;
    }
    
    /**
     * @param type the type to set
     */
    public void setType(DcType type) {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getWidth()
     */
    @Override
    public Integer getWidth() {
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getHeight()
     */
    @Override
    public Integer getHeight() {
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getFormat()
     */
    @Override
    public Format getFormat() {
        return format;
    }
    
    /**
     * @param format the format to set
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getId()
     */
    @Override
    public URI getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getLabel()
     */
    @Override
    public IMetadataValue getLabel() {
        return label;
    }
    
    /**
     * @param label the label to set
     */
    public void setLabel(IMetadataValue label) {
        this.label = label;
    }

}
