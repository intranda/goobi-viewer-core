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
import java.util.List;

import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * @author florian
 *
 */
public interface IPresentationModelElement {

	String getType();

	/**
	 * @return the label
	 */
	IMetadataValue getLabel();

	/**
	 * @return the description
	 */
	IMetadataValue getDescription();

	/**
	 * @return the metadata
	 */
	List<IMetadataValue> getMetadata();

	/**
	 * @return the thumbnail
	 */
	ImageContent getThumbnail();

	/**
	 * @return the attribution
	 */
	IMetadataValue getAttribution();

	/**
	 * @return the license
	 */
	URI getLicense();

	/**
	 * @return the logo
	 */
	ImageContent getLogo();

	/**
	 * @return the viewingHint
	 */
	ViewingHint getViewingHint();

	/**
	 * @return the related
	 */
	URI getRelated();

	/**
	 * @return the rendering
	 */
	URI getRendering();

	/**
	 * @return the id
	 */
	URI getId();

}