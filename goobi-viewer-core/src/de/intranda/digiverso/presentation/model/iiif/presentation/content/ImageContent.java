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
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author florian
 *
 */
public class ImageContent implements IContent {
	
	public final String TYPE = "dcTypes:Image";
	
	private final URI id;
	private int width;
	private int height;
	private Format format;
	private ImageInformation service;
	
	public ImageContent(URI id) {
		this.id = id;
	}
	
	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getType()
	 */
	@Override
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getWidth()
	 */
	@Override
	public int getWidth() {
		return width;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#setWidth(int)
	 */
	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getHeight()
	 */
	@Override
	public int getHeight() {
		return height;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#setHeight(int)
	 */
	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getFormat()
	 */
	@Override
	public Format getFormat() {
		return format;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#setFormat(de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format)
	 */
	@Override
	public void setFormat(Format format) {
		this.format = format;
	}

	/**
	 * @return the service
	 */
	public ImageInformation getService() {
		return service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(ImageInformation service) {
		this.service = service;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.content.IContent#getId()
	 */
	@Override
	public URI getId() {
		return id;
	}
	

}
