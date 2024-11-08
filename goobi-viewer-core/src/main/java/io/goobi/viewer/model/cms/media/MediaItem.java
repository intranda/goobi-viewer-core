/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms.media;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.content.IContent;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.serializer.WebAnnotationMetadataValueSerializer;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.model.cms.CMSCategory;

/**
 * Simple representation of a cms-media-item
 * 
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
public class MediaItem {

    private final Long id;
    @JsonSerialize(using = WebAnnotationMetadataValueSerializer.class)
    private final IMetadataValue label;
    @JsonSerialize(using = WebAnnotationMetadataValueSerializer.class)
    private final IMetadataValue description;
    private final String altText;
    private final IContent image;
    private final String link;
    private final List<String> tags;
    private final boolean important;
    private final Integer order;

    /**
     * 
     * @param imageURI
     */
    public MediaItem(URI imageURI) {
        this.image = new ImageContent(imageURI);
        this.tags = new ArrayList<>();
        this.link = "";
        this.label = new SimpleMetadataValue(PathConverter.getPath(imageURI).getFileName().toString());
        this.description = null;
        this.id = null;
        this.important = false;
        this.order = null;
        this.altText = null;

    }

    /**
     * 
     * @param source
     * @param servletRequest
     */
    public MediaItem(CMSMediaItem source, HttpServletRequest servletRequest) {
        this.label = source.getTranslationsForName();
        this.description = source.getTranslationsForDescription();
        this.image = getMediaResource(source);
        this.link = Optional.ofNullable(source.getLinkURI(servletRequest)).map(URI::toString).orElse("#");
        this.tags = source.getCategories().stream().map(CMSCategory::getName).collect(Collectors.toList());
        this.id = source.getId();
        this.important = source.isImportant();
        this.order = source.getDisplayOrder();
        this.altText = source.getAlternativeText();
    }

    /**
     * @param source
     * @return {@link IContent}
     */
    public static IContent getMediaResource(CMSMediaItem source) {
        if (source != null) {
            ImageContent image = new ImageContent(source.getIconURI());
            image.setFormat(Format.fromFilename(source.getFileName()));
            if (IIIFUrlResolver.isIIIFImageUrl(source.getIconURI().toString())) {
                URI imageInfoURI = URI.create(IIIFUrlResolver.getIIIFImageBaseUrl(source.getIconURI().toString()));
                image.setService(new ImageInformation(imageInfoURI.toString()));
            }
            return image;
        } else {
            return null;
        }

    }

    public URI getImageURI() {
        return Optional.ofNullable(image).map(IContent::getId).orElse(URI.create(""));
    }

    public URI getImageURI(int maxWidth, int maxHeight) {
        if (image instanceof ImageContent && ((ImageContent) image).getService() != null) {
            Scale scale;
            if (maxHeight <= 0 && maxWidth <= 0) {
                scale = Scale.MAX;
            } else if (maxHeight <= 0) {
                scale = new Scale.ScaleToWidth(maxWidth);
            } else if (maxWidth <= 0) {
                scale = new Scale.ScaleToHeight(maxHeight);
            } else {
                scale = new Scale.ScaleToBox(maxWidth, maxHeight);
            }
            URI serviceURI = ((ImageContent) image).getService().getId();
            ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(serviceURI.toString());
            String uri =
                    IIIFUrlResolver.getIIIFImageUrl(serviceURI.toString(), Region.FULL_IMAGE, scale.toString(),
                            Rotation.NONE.toString(), "default", ImageFileFormat.getMatchingTargetFormat(iff).getFileExtension());
            return URI.create(uri);
        } else {
            return getImageURI();
        }
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
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
     * @return the image
     */
    public IContent getImage() {
        return image;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @return the important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * @return the order
     */
    public Integer getOrder() {
        return order;
    }

    public String getAltText() {
        return altText;
    }

}
