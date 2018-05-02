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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.ContentLinkSerializer;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.ImageContentLinkSerializer;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.PropertyList;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.URLOnlySerializer;
import de.intranda.digiverso.presentation.servlets.rest.services.Service;

/**
 * @author florian
 *
 */
@JsonInclude(Include.NON_EMPTY)
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
	List<Metadata> getMetadata();

	/**
	 * @return the thumbnail
	 */
	@JsonSerialize(using=ImageContentLinkSerializer.class)
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
	List<LinkingContent> getRelated();
	
	/**
	 * @return the rendering
	 */
	List<LinkingContent> getRendering();
	
	/**
	 * @return one or more services - may be null!
	 */
	List<Service> getService();
	
	List<LinkingContent> getSeeAlso();

	/**
	 * 
	 * @param allowedClasses   All classes which should be included in the service list
	 * @return                 A PropertyList of all services of one of the given classes
	 */
	default List<Service> getService(Class... allowedClasses) {
	    List<Class> allowedClassesList = Arrays.asList(allowedClasses);
	    if(this.getService() != null) {
	        return new PropertyList(this.getService().stream()
	        .filter(service -> allowedClassesList.contains(service.getClass()))
	        .collect(Collectors.toList()));
	    } else {
	        return null;
	    }
	}
	
	/**
	 * @return the id
	 */
	URI getId();

    @JsonSerialize(using = URLOnlySerializer.class)
	List<IPresentationModelElement> getWithin();
	

}