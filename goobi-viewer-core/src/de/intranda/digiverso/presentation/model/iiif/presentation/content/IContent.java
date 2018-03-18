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

/**
 * @author florian
 *
 */
public interface IContent {

	String getType();

	/**
	 * @return the width
	 */
	int getWidth();

	/**
	 * @param width the width to set
	 */
	void setWidth(int width);

	/**
	 * @return the height
	 */
	int getHeight();

	/**
	 * @param height the height to set
	 */
	void setHeight(int height);

	/**
	 * @return the format
	 */
	Format getFormat();

	/**
	 * @param format the format to set
	 */
	void setFormat(Format format);

	/**
	 * @return the id
	 */
	URI getId();

}