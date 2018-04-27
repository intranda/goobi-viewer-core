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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.intranda.digiverso.presentation.model.iiif.presentation.enums.DcType;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * @author florian
 *
 */
@JsonPropertyOrder({"@id", "@type"})
public interface IContent {

    @JsonProperty("@type")
	public DcType getType();

	/**
	 * @return the width
	 */
	public Integer getWidth();

	/**
	 * @return the height
	 */
	public Integer getHeight();

	/**
	 * @return the format
	 */
	public Format getFormat();

	/**
	 * @return the id
	 */
	@JsonProperty("@id")
	public URI getId();
	
	/**
	 * 
	 * @return the label
	 */
	public IMetadataValue getLabel();

}