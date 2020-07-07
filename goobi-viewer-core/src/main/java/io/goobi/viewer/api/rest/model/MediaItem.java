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
package io.goobi.viewer.api.rest.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.serializer.MetadataSerializer;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;

/**
 * @author florian
 *
 */

public class MediaItem {

    @JsonSerialize(using = MetadataSerializer.class)
    private final IMetadataValue label;
    @JsonSerialize(using = MetadataSerializer.class)
    private final IMetadataValue description;
    private final String link;
    private final ImageContent image;
    private final List<String> tags;

    public MediaItem(URI imageURI) {
        this.image = new ImageContent(imageURI);
        this.tags = new ArrayList<>();
        this.link = "";
        this.label = new SimpleMetadataValue(PathConverter.getPath(imageURI).getFileName().toString());
        this.description = null;
    }
    
    public MediaItem(CMSMediaItem source, HttpServletRequest servletRequest) {
        this.label = source.getTranslationsForName();
        this.description = source.getTranslationsForDescription();
        this.image = new ImageContent(source.getIconURI());
        if (IIIFUrlResolver.isIIIFImageUrl(source.getIconURI().toString())) {
            URI imageInfoURI = URI.create(IIIFUrlResolver.getIIIFImageBaseUrl(source.getIconURI().toString()));
            this.image.setService(new ImageInformation(imageInfoURI.toString()));
        }
        this.link = Optional.ofNullable(source.getLinkURI(servletRequest)).map(URI::toString).orElse("#");
        this.tags = source.getCategories().stream().map(CMSCategory::getName).collect(Collectors.toList());
    }

    /**
     * @return the label
     */
    public IMetadataValue getLabel() {
        return label;
    }

    /**
     * @return the description
     */
    public IMetadataValue getDescription() {
        return description;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @return the image
     */
    public ImageContent getImage() {
        return image;
    }

    /**
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

}
