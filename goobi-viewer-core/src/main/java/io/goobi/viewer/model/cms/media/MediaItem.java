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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

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
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.HtmlSanitizer;
import io.goobi.viewer.model.cms.CMSCategory;

/**
 * Simple representation of a cms-media-item.
 * 
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
public class MediaItem {

    private final Long id;
    @JsonSerialize(using = WebAnnotationMetadataValueSerializer.class)
    private final IMetadataValue label;
    @JsonSerialize(using = WebAnnotationMetadataValueSerializer.class)
    private final IMetadataValue description;
    private final IMetadataValue altText;
    private final IContent image;
    private final String link;
    private final List<String> tags;
    private final boolean important;
    private final Integer order;

    /**
     * 
     * @param imageURI URI of the image to display
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
     * @param source CMS media item providing the data
     * @param servletRequest HTTP servlet request for link URI resolution
     */
    public MediaItem(CMSMediaItem source, HttpServletRequest servletRequest) {
        this.label = source.getTranslationsForName();
        // Defense-in-depth (CWE-79): description is a plain-text field maintained by CMS
        // admins via mediaFile.xhtml (<h:inputTextarea>), but is serialized as JSON and
        // consumed by clients that historically rendered it via .html() (see cmsJS.masonry.js).
        // Strip any HTML before it leaves the server so future consumers cannot reintroduce
        // a stored-XSS sink.
        this.description = sanitizeDescriptionPlainText(source);
        this.altText = source.getTranslationsForAlternativeText();
        this.image = getMediaResource(source);
        this.link = Optional.ofNullable(source.getLinkURI(servletRequest)).map(URI::toString).orElse("#");
        this.tags = source.getCategories().stream().map(CMSCategory::getName).collect(Collectors.toList());
        this.id = source.getId();
        this.important = source.isImportant();
        this.order = source.getDisplayOrder();
    }

    /**
     * @param source CMS media item to build content from
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

    
    public Long getId() {
        return id;
    }

    
    public IMetadataValue getLabel() {
        return label;
    }

    
    public IMetadataValue getDescription() {
        return description;
    }

    
    public IContent getImage() {
        return image;
    }

    
    public String getLink() {
        return link;
    }

    
    public List<String> getTags() {
        return tags;
    }

    
    public boolean isImportant() {
        return important;
    }

    
    public Integer getOrder() {
        return order;
    }

    public IMetadataValue getAltText() {
        return altText;
    }

    /**
     * Builds a {@link MultiLanguageMetadataValue} for the description metadata of the given source
     * with each language value run through {@link HtmlSanitizer#cleanCommentPlainText(String)} so
     * that any HTML markup is stripped before the value leaves the server.
     *
     * <p>Mirrors the language-map construction in
     * {@link CMSMediaItem#getTranslationsForDescription()} but applies plain-text sanitization on
     * each entry. The description input widget (<code>&lt;h:inputTextarea&gt;</code> in
     * <code>mediaFile.xhtml</code>) is plain-text by design, so stripping any HTML matches the
     * editorial intent and prevents stored XSS in JSON consumers.
     *
     * @param source CMS media item providing the metadata; may be {@code null}
     * @return sanitized {@link IMetadataValue} (empty when {@code source} is {@code null} or has no
     *         description entries)
     * @should sanitize description values for every language
     * @should drop language entries with blank description
     * @should return empty metadata value when source is null
     */
    static IMetadataValue sanitizeDescriptionPlainText(CMSMediaItem source) {
        if (source == null) {
            return new MultiLanguageMetadataValue(new HashMap<>());
        }
        Map<String, String> sanitized = source.getMetadata().stream()
                .filter(md -> StringUtils.isNotBlank(md.getDescription()))
                .collect(Collectors.toMap(
                        CMSMediaItemMetadata::getLanguage,
                        md -> HtmlSanitizer.cleanCommentPlainText(md.getDescription())));
        return new MultiLanguageMetadataValue(sanitized);
    }

}
