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
package de.intranda.digiverso.presentation.controller.imaging;

import java.net.URI;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;

/**
 * Delivers Thumbnail urls for pages and StructElements
 * 
 * @author Florian Alpers
 *
 */
class ThumbnailHandler {
    
    private static final String ANCHOR_THUMB = "multivolume_thumbnail.jpg";
    private static final String BORN_DIGITAL_THUMB = "thumbnail_epub.jpg";
    
    private static final String ANCHOR_THUMBNAIL_MODE_GENERIC = "GENERIC";
    private static final String ANCHOR_THUMBNAIL_MODE_FIRSTVOLUME = "FIRSTVOLUME";

    private final int thumbWidth;
    private final int thumbHeight;
    private final int thumbCompression;
    private final String anchorThumbnailMode;
    private final URI anchorThumnailReplacement;
    private final URI bornDigitalThumnailReplacement;
    
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailHandler.class);
    
    private final IIIFUrlHandler iiifUrlHandler;
    
    public ThumbnailHandler(IIIFUrlHandler iiifUrlHandler, Configuration configuration, String staticImagesPath) {
        this.iiifUrlHandler = iiifUrlHandler;
        thumbWidth = configuration.getThumbnailsWidth();
        thumbHeight = configuration.getThumbnailsHeight();
        thumbCompression = configuration.getThumbnailsCompression();
        anchorThumbnailMode = configuration.getAnchorThumbnailMode();
        anchorThumnailReplacement = Paths.get(staticImagesPath, ANCHOR_THUMB).toUri();
        bornDigitalThumnailReplacement = Paths.get(staticImagesPath, BORN_DIGITAL_THUMB).toUri();
    }

    /**
     * Returns a link to a small image representating the given page. The size depends on viewer configuration
     * 
     * @param page
     * @return
     */
    public String getThumbnailUrl(PhysicalElement page) {
        return getThumbnailUrl(page, thumbWidth, thumbHeight);

    }
    
    /**
     * Returns a link to an image representating the given page of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     * 
     * @param page
     * @return
     */
    public String getThumbnailUrl(PhysicalElement page, int width, int height) {
        String path = getImagePath(page);
        return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.FULL_IMAGE, "!" + width + "," + height, "0", "default", "jpg", thumbCompression);
    }
    
    /**
     * returns a link the an image representing the given page. Its size depends on configuration. The image is always square and contains as much of the actual image
     * as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(PhysicalElement page) {
        return getSquareThumbnailUrl(page, thumbWidth);
    }
    
    /**
     * returns a link the an image representing the given page of the given size. The image is always square and contains as much of the actual image
     * as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(PhysicalElement page, int size) {
        String path = getImagePath(page);
        return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg", thumbCompression);
    }


    
    
    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration
     * 
     * @param page
     * @return
     */
    public String getThumbnailUrl(StructElement doc) {
        return getThumbnailUrl(doc, thumbWidth, thumbHeight);
    }

    /**
     * Returns a link to an image representating the given document of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     * 
     * @param page
     * @return
     */
    public String getThumbnailUrl(StructElement doc, int width, int height) {
        String thumbnailUrl = getImagePath(doc);
        return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, doc.getPi(), Region.FULL_IMAGE, "!" + width + "," + height, "0", "default", "jpg", thumbCompression);
    }

    
    /**
     * returns a link the an image representing the given document. Its size depends on configuration. The image is always square and contains as much of the actual image
     * as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(StructElement doc) {
        return getSquareThumbnailUrl(doc, thumbWidth);
    }

    /**
     * returns a link the an image representing the given document of the given size. The image is always square and contains as much of the actual image
     * as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(StructElement doc, int size) {
         String thumbnailUrl = getImagePath(doc);
        return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, doc.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg", thumbCompression);
    }

    /**
     * @param doc
     * @param field
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private String getFieldValue(StructElement doc, String field) {
        String imagePath = doc.getMetadataValue(field);
        try {            
            if(StringUtils.isBlank(imagePath) && !doc.isWork()) {
                if(doc.isAnchor()) {
                    imagePath = doc.getFirstVolumeFieldValue(field);
                } else {
                    imagePath = doc.getTopStruct().getMetadataValue(field);
                }
            }
        } catch(IndexUnreachableException | PresentationException e) {
            logger.warn(e.toString());
        }
        return imagePath;
    }
    
    /**
     * @param page
     * @return
     */
    private String getImagePath(PhysicalElement page) {
        String path = page.getFilepath();
        if(PhysicalElement.MIME_TYPE_APPLICATION.equals(page.getMimeType())) {
            path = bornDigitalThumnailReplacement.toString();
        }
        return path;
    }
    

    /**
     * @param doc
     * @return
     */
    private String getImagePath(StructElement doc) {
        String thumbnailUrl;
        if(PhysicalElement.MIME_TYPE_APPLICATION.equals(getFieldValue(doc, SolrConstants.MIMETYPE))) {
            thumbnailUrl = bornDigitalThumnailReplacement.toString();
        } else if(doc.isAnchor() && ANCHOR_THUMBNAIL_MODE_GENERIC.equals(this.anchorThumbnailMode)) {
            thumbnailUrl = anchorThumnailReplacement.toString();
        } else {            
            String field = SolrConstants.THUMBNAIL;
            thumbnailUrl = getFieldValue(doc, field);
            if(StringUtils.isBlank(thumbnailUrl) || ImageDeliveryManager.isImageUrl(thumbnailUrl, false)) {
                thumbnailUrl = bornDigitalThumnailReplacement.toString();
            }
        }
        return thumbnailUrl;
    }
}
