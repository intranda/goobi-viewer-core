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
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.ImageContentLinkSerializer;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.PropertyList;
import de.intranda.digiverso.presentation.servlets.rest.services.Service;

/**
 * Parent class for all classes modeling the iiif presentation api resources except images and other canvas content
 * 
 * @author florian
 *
 */
@JsonPropertyOrder({"@context", "@id", "@type"})
@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractPresentationModelElement implements IPresentationModelElement {
	
    
    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    
	private final URI id;
	private IMetadataValue label;
	private IMetadataValue description;
	private List<Metadata> metadata = new ArrayList<Metadata>();
	private ImageContent thumbnail;
	private IMetadataValue attribution;
	private URI license;
	private ImageContent logo;
	private ViewingHint viewingHint;
	private List<LinkingContent> related = new PropertyList<>();
	private List<LinkingContent> rendering = new PropertyList<>();
	private String context = null;
	private List<Service> service = new PropertyList<Service>();
	private List<LinkingContent> seeAlso = new PropertyList<>();
	private List<IPresentationModelElement> within = new PropertyList<>();
	
	public AbstractPresentationModelElement(URI id) {
		this.id = id;
	}
	
	/**
     * @param context the context to set
     */
    public void setContext(String context) {
        this.context = context;
    }
    
    /**
     * @return the context
     */
    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

	
	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getType()
	 */
	@Override
    @JsonProperty("@type")
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
	public List<Metadata> getMetadata() {
		return this.metadata.isEmpty() ? null : this.metadata;
	}
	public void addMetadata(Metadata md)  {
		this.metadata.add(md);
	}

	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(List<Metadata> metadata) {
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
	   @JsonSerialize(using=ImageContentLinkSerializer.class)
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
	public List<LinkingContent> getRelated() {
		return related.isEmpty() ? null : related;
	}

	/**
	 * @param related the related to set
	 */
	public void addRelated(LinkingContent related) {
		this.related.add(related);
	}

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getRendering()
	 */
	@Override
	public List<LinkingContent> getRendering() {
		return rendering.isEmpty() ? null : rendering;
	}

	/**
	 * @param rendering the rendering to set
	 */
	public void addRendering(LinkingContent rendering) {
		this.rendering.add(rendering);
	}
	
	   /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getRendering()
     */
    @Override
    public List<LinkingContent> getSeeAlso() {
        return seeAlso.isEmpty() ? null : seeAlso;
    }

    /**
     * @param rendering the rendering to set
     */
    public void addSeeAlso(LinkingContent seeAlso) {
        this.seeAlso.add(seeAlso);
    }
	
	

	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getId()
	 */
	@Override
    @JsonProperty("@id")
	public URI getId() {
		return id;
	}
	
	/* (non-Javadoc)
	 * @see de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement#getService()
	 */
	@Override
	public List<Service> getService() {
	    return service.isEmpty() ? null : service;
	}
	
	public void addService(Service service) {
	    this.service.add(service);
	}
	
    /**
     * @return the navDate
     */
    @JsonFormat(pattern = DATETIME_FORMAT)
    public Date getNavDate() {
        return null;
    }

    /**
     * @param navDate the navDate to set
     */
    public void setNavDate(Date navDate) {
    }
    
    /**
     * @return the within
     */
    public List<IPresentationModelElement> getWithin() {
        return within.isEmpty() ? null : within;
    }
    
    public void addWithin(IPresentationModelElement within) {
        this.within.add(within);
    }

}
