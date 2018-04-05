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
import java.util.Collections;
import java.util.List;

import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * Parent class for all classes modeling the iiif presentation api resources except images and other canvas content
 * 
 * @author florian
 *
 */
public abstract class AbstractPresentationModelElement implements IPresentationModelElement {
	
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    
	private final URI id;
	private IMetadataValue label;
	private IMetadataValue description;
	private List<IMetadataValue> metadata;
	private ImageContent thumbnail;
	private IMetadataValue attribution;
	private URI license;
	private ImageContent logo;
	private ViewingHint viewingHint;
	private URI related;
	private URI rendering;
	
	public AbstractPresentationModelElement(URI id) {
		this.id = id;
	}
	
	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getType()
	 */
	@Override
	public abstract String getType();

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getLabel()
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

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getDescription()
	 */
	@Override
	public IMetadataValue getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(IMetadataValue description) {
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getMetadata()
	 */
	@Override
	public List<IMetadataValue> getMetadata() {
		if(metadata == null)  {
			return Collections.EMPTY_LIST;
		}
		return metadata;
	}
	public void addMetadata(IMetadataValue md)  {
		if(this.metadata == null) {
			this.metadata = new ArrayList<>();
		}
		this.metadata.add(md);
	}

	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(List<IMetadataValue> metadata) {
		this.metadata = metadata;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getThumbnail()
	 */
	@Override
	public ImageContent getThumbnail() {
		return thumbnail;
	}

	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(ImageContent thumbnail) {
		this.thumbnail = thumbnail;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getAttribution()
	 */
	@Override
	public IMetadataValue getAttribution() {
		return attribution;
	}

	/**
	 * @param attribution the attribution to set
	 */
	public void setAttribution(IMetadataValue attribution) {
		this.attribution = attribution;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getLicense()
	 */
	@Override
	public URI getLicense() {
		return license;
	}

	/**
	 * @param license the license to set
	 */
	public void setLicense(URI license) {
		this.license = license;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getLogo()
	 */
	@Override
	public ImageContent getLogo() {
		return logo;
	}

	/**
	 * @param logo the logo to set
	 */
	public void setLogo(ImageContent logo) {
		this.logo = logo;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getViewingHint()
	 */
	@Override
	public ViewingHint getViewingHint() {
		return viewingHint;
	}

	/**
	 * @param viewingHint the viewingHint to set
	 */
	public void setViewingHint(ViewingHint viewingHint) {
		this.viewingHint = viewingHint;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getRelated()
	 */
	@Override
	public URI getRelated() {
		return related;
	}

	/**
	 * @param related the related to set
	 */
	public void setRelated(URI related) {
		this.related = related;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getRendering()
	 */
	@Override
	public URI getRendering() {
		return rendering;
	}

	/**
	 * @param rendering the rendering to set
	 */
	public void setRendering(URI rendering) {
		this.rendering = rendering;
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getId()
	 */
	@Override
	public URI getId() {
		return id;
	}
	
	
}
