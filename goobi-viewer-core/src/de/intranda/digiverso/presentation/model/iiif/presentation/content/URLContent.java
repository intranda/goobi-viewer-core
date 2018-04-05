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

import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * @author Florian Alpers
 *
 */
public class URLContent implements IContent {

    /**
     * 
     */
    public URLContent() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getType()
     */
    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getWidth()
     */
    @Override
    public int getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getHeight()
     */
    @Override
    public int getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getFormat()
     */
    @Override
    public Format getFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getId()
     */
    @Override
    public URI getId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getLabel()
     */
    @Override
    public IMetadataValue getLabel() {
        // TODO Auto-generated method stub
        return null;
    }

}
