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
package io.goobi.viewer.controller.imaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.monitoring.timer.Time;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.controller.SolrConstants.MetadataGroupType;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.LeanPageLoader;

/**
 * Delivers Thumbnail urls for pages and StructElements
 *
 * @author Florian Alpers
 */
public class ThumbnailHandler {

    private static final String ANCHOR_THUMB = "multivolume_thumbnail.jpg";
    private static final String BORN_DIGITAL_THUMB = "thumbnail_epub.jpg";
    private static final String PERSON_THUMB = "thumbnail_person.jpg";
    private static final String EVENT_THUMB = "thumbnail_event.jpg";
    private static final String VIDEO_THUMB = "thumbnail_video.jpg";
    private static final String AUDIO_THUMB = "thumbnail_audio.jpg";
    private static final String OBJECT_3D_THUMB = "thumbnail_3d.png";
    private static final String GROUP_THUMB = "thumbnail_group.jpg";

    private static final String ANCHOR_THUMBNAIL_MODE_GENERIC = "GENERIC";
    private static final String ANCHOR_THUMBNAIL_MODE_FIRSTVOLUME = "FIRSTVOLUME";

    /** Constant <code>REQUIRED_SOLR_FIELDS</code> */
    public static final String[] REQUIRED_SOLR_FIELDS =
            { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL,
                    SolrConstants.DOCTYPE, SolrConstants.METADATATYPE, SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED };

    private final int thumbWidth;
    private final int thumbHeight;
    private final String anchorThumbnailMode;
    private final String staticImagesPath;

    private static final Logger logger = LoggerFactory.getLogger(ThumbnailHandler.class);

    private final IIIFUrlHandler iiifUrlHandler;

    /**
     * <p>
     * Constructor for ThumbnailHandler.
     * </p>
     *
     * @param iiifUrlHandler a {@link io.goobi.viewer.controller.imaging.IIIFUrlHandler} object.
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     * @param staticImagesPath a {@link java.lang.String} object.
     */
    public ThumbnailHandler(IIIFUrlHandler iiifUrlHandler, Configuration configuration, String staticImagesPath) {
        this.iiifUrlHandler = iiifUrlHandler;
        thumbWidth = configuration.getThumbnailsWidth();
        thumbHeight = configuration.getThumbnailsHeight();
        anchorThumbnailMode = configuration.getAnchorThumbnailMode();
        this.staticImagesPath = staticImagesPath;
    }

    /**
     * <p>
     * getThumbnailPath.
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getThumbnailPath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(staticImagesPath);
            uri = uri.resolve(filename);
            return uri;
        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
        }
        return null;
        //        return Paths.get(staticImagesPath, filename).toUri();
    }

    /**
     * Returns a link to a small image representating the given page. The size depends on viewer configuration
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(PhysicalElement page) {
        return getThumbnailUrl(page, thumbWidth, thumbHeight);
    }

    /**
     * Returns a link to the representative image for the given pi. If the pi doesn't match an indexed item, null is returned
     *
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(String pi) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        try(Time t = DataManager.getInstance().getTiming().takeTime("get thumbnail url")) {            
            return getThumbnailUrl(pi, DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                    DataManager.getInstance().getConfiguration().getThumbnailsHeight());
        }
    }

    /**
     * Returns a link to the representative image for the given pi with the given width and height. If the pi doesn't match an indexed item, null is
     * returned
     *
     * @param pi the persistent identifier of the work which representative we want
     * @param width the width of the image
     * @param height the height of the image
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(String pi, int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        
        if(iiifUrlHandler.getUrlManager() != null) {
            String size = "!" + width + "," + height;
            return iiifUrlHandler.getUrlManager()
                    .path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_IMAGE_IIIF)
                    .params(pi, "full", size, "0", "default", "jpg")
                    .build();
        } else {            
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            if (doc != null) {
                return getThumbnailUrl(doc, width, height);
            }
            return null;
        }
        
        
    }

    /**
     * Returns a link to a square representative image for the given pi. If the pi doesn't match an indexed item, null is returned
     *
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(String pi) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getSquareThumbnailUrl(pi, DataManager.getInstance().getConfiguration().getThumbnailsWidth());
    }

    /**
     * Returns a link to a square representative image for the given pi. If the pi doesn't match an indexed item, null is returned
     *
     * @param pi the persistent identifier of the work which representative we want
     * @param size the size (width and heigt) of the image
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(String pi, int size) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (doc != null) {
            return getSquareThumbnailUrl(doc, size);
        }
        return null;
    }

    /**
     * Returns a link to the image of the page of the given order (=page number) within the work with the given pi . If the pi doesn't match an
     * indexed work or the work desn't contain a page of the given order, null is returned
     *
     * @param order the page number
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(int order, String pi)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        return getThumbnailUrl(order, pi, DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * Returns a link to the image of the page of the given order (=page number) within the work with the given pi of the given width and height. If
     * the pi doesn't match an indexed work or the work desn't contain a page of the given order, null is returned
     *
     * @param order the page number
     * @param pi the persistent identifier of the work which representative we want
     * @param width the width of the image
     * @param height the height of the image
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(int order, String pi, int width, int height)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        PhysicalElement page = getPage(pi, order);
        if (page != null) {
            return getThumbnailUrl(page, width, height);
        }
        return null;
    }

    /**
     * Returns a link to a square image of the page of the given order (=page number) within the work with the given pi. If the pi doesn't match an
     * indexed work or the work desn't contain a page of the given order, null is returned
     *
     * @param order the page number
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(int order, String pi)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        return getSquareThumbnailUrl(order, pi, DataManager.getInstance().getConfiguration().getThumbnailsWidth());
    }

    /**
     * Returns a link to a square image of the page of the given order (=page number) within the work with the given pi of the given size. If the pi
     * doesn't match an indexed work or the work desn't contain a page of the given order, null is returned
     *
     * @param order the page number
     * @param pi the persistent identifier of the work which representative we want
     * @param size the width and height of the image
     * @return The url string or null of no work is found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(int order, String pi, int size)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        PhysicalElement page = getPage(pi, order);
        if (page != null) {
            return getSquareThumbnailUrl(page, size);
        }
        return null;
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPage(String pi, int order) throws IndexUnreachableException, PresentationException, DAOException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        StructElement struct = new StructElement(Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString()), doc);
        LeanPageLoader pageLoader = new LeanPageLoader(struct, 1);
        PhysicalElement page = pageLoader.getPage(order);
        return page;
    }

    /**
     * Returns a link to an image representating the given page of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(PhysicalElement page, int width, int height) {
        return getThumbnailUrl(page, getScale(width, height));
    }

    /**
     * <p>
     * getThumbnailUrl.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(PhysicalElement page, Scale scale) {

        String path = getImagePath(page);
        if (path == null) {
            return "";
        }
        if (isStaticImageResource(path)) {
            return path;
        } else if (IIIFUrlResolver.isIIIFImageUrl(path)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(path, null, scale, null, null, null);
        } else if (IIIFUrlResolver.isIIIFImageInfoUrl(path)) {
            return iiifUrlHandler.getIIIFImageUrl(path, null, scale, null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.FULL_IMAGE, scale.toString(), "0", "default", "jpg");
        }
    }

    /**
     * returns a link the an image representing the given page. Its size depends on configuration. The image is always square and contains as much of
     * the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(PhysicalElement page) {
        return getSquareThumbnailUrl(page, thumbWidth);
    }

    /**
     * returns a link the an image representing the given page of the given size. The image is always square and contains as much of the actual image
     * as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param size a int.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(PhysicalElement page, int size) {
        String path = getImagePath(page);
        if (path == null) {
            return "";
        }
        if (isStaticImageResource(path)) {
            return path;
        } else if (IIIFUrlResolver.isIIIFImageUrl(path)) {
            return IIIFUrlResolver.getModifiedIIIFFUrl(path, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else if (IIIFUrlResolver.isIIIFImageInfoUrl(path)) {
            return IIIFUrlResolver.getIIIFImageUrl(path, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg");
        }
    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(StructElement doc) {
        return getThumbnailUrl(doc, thumbWidth, thumbHeight);

    }

    /**
     * Returns a link to a small image representating the given document with the given pi. The size depends on viewer configuration
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(StructElement doc, String pi) {
        return getThumbnailUrl(doc, pi, thumbWidth, thumbHeight);

    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(SolrDocument doc) throws ViewerConfigurationException {
        return getThumbnailUrl(getStructElement(doc), thumbWidth, thumbHeight);
    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration. The image may be cut at the longer
     * side to provide a square image
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(SolrDocument doc) throws ViewerConfigurationException {
        return getSquareThumbnailUrl(getStructElement(doc), thumbWidth);
    }

    /**
     * @param doc
     * @return
     */
    private static StructElement getStructElement(SolrDocument doc) {
        String value = (String) doc.getFirstValue(SolrConstants.IDDOC);
        Long iddoc = 0l;
        if (value != null) {
            iddoc = Long.valueOf(value);
        }
        try {
            StructElement ele = new StructElement(iddoc, doc);
            return ele;
        } catch (IndexUnreachableException e) {
            logger.error("Unable to create StructElement", e);
            return new StructElement();
        }
    }

    /**
     * Returns a link to an image representating the given page of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl(SolrDocument doc, int width, int height) throws ViewerConfigurationException {
        return getThumbnailUrl(getStructElement(doc), width, height);

    }

    /**
     * Returns a link to an image representating the given page of the given size. The image will be cut at the longer side to create a square image
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param size a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getSquareThumbnailUrl(SolrDocument doc, int size) throws ViewerConfigurationException {
        return getSquareThumbnailUrl(getStructElement(doc), size);

    }

    /**
     * Returns a link to an image representating the given document of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     *
     * @param se Needs to have the fields {@link io.goobi.viewer.controller.SolrConstants.MIMETYPE} and
     *            {@link io.goobi.viewer.controller.SolrConstants.THUMBNAIL}
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(StructElement se, int width, int height) {
        return getThumbnailUrl(se, se.getPi(), width, height);
    }

    /**
     * <p>
     * getThumbnailUrl.
     * </p>
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pi a {@link java.lang.String} object.
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(StructElement doc, String pi, int width, int height) {
        String thumbnailUrl = getImagePath(doc);
        if (thumbnailUrl != null && isStaticImageResource(thumbnailUrl)) {
            return thumbnailUrl;
        } else if (IIIFUrlResolver.isIIIFImageUrl(thumbnailUrl)) {
            return IIIFUrlResolver.getModifiedIIIFFUrl(thumbnailUrl, null, getScale(width, height).toString(), null, null, null);
        } else if (IIIFUrlResolver.isIIIFImageInfoUrl(thumbnailUrl)) {
            return IIIFUrlResolver.getIIIFImageUrl(thumbnailUrl, null, getScale(width, height).toString(), null, null, null);
        } else if (thumbnailUrl != null) {
            String region = Region.FULL_IMAGE;
            if (doc.getShapeMetadata() != null && !doc.getShapeMetadata().isEmpty()) {
                region = doc.getShapeMetadata().get(0).getCoords();
            }
            return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, pi, region, "!" + width + "," + height, "0", "default", "jpg");
        } else {
            return null;
        }
    }

    /**
     * <p>
     * getFullImageUrl.
     * </p>
     *
     * @return the url of the entire, max-size image in the original format. If no Watermark needs to be included and forwarding images is allowed in
     *         contentServer, then this streams the original image file to the client
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     */
    public String getFullImageUrl(PhysicalElement page) {
        return getFullImageUrl(page, Scale.MAX);
    }

    /**
     * <p>
     * getFullImageUrl.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFullImageUrl(PhysicalElement page, Scale scale) {

        String path = getImagePath(page);
        if (path == null) {
            return "";
        }
        ImageFileFormat format = ImageFileFormat.getImageFileFormatFromFileExtension(path);
        if (format == null) {
            logger.warn("Format not recognized for: {}", path);
            return "";

        }
        if (isStaticImageResource(path)) {
            return path;
        } else if (IIIFUrlResolver.isIIIFImageUrl(path)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(path, RegionRequest.FULL, Scale.MAX, Rotation.NONE, Colortype.DEFAULT, format);
        } else if (IIIFUrlResolver.isIIIFImageInfoUrl(path)) {
            return iiifUrlHandler.getIIIFImageUrl(path, RegionRequest.FULL, Scale.MAX, Rotation.NONE, Colortype.DEFAULT, format);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.FULL_IMAGE, scale.toString(), "0", "default",
                    format.getFileExtension());
        }
    }

    /**
     * returns a link the an image representing the given document. Its size depends on configuration. The image is always square and contains as much
     * of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param se a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(StructElement se) {
        return getSquareThumbnailUrl(se, thumbWidth);
    }

    /**
     * returns a link the an image representing the given document of the given size. The image is always square and contains as much of the actual
     * image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param se Needs to have the fields {@link io.goobi.viewer.controller.SolrConstants.MIMETYPE} and
     *            {@link io.goobi.viewer.controller.SolrConstants.THUMBNAIL}
     * @param size a int.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(StructElement se, int size) {
        String thumbnailUrl = getImagePath(se);
        if (StringUtils.isNotBlank(thumbnailUrl) && isStaticImageResource(thumbnailUrl)) {
            return thumbnailUrl;
        } else if (IIIFUrlResolver.isIIIFImageUrl(thumbnailUrl)) {
            return IIIFUrlResolver.getModifiedIIIFFUrl(thumbnailUrl, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else if (IIIFUrlResolver.isIIIFImageInfoUrl(thumbnailUrl)) {
            return IIIFUrlResolver.getIIIFImageUrl(thumbnailUrl, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, se.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg");
        }
    }

    /**
     * @param se
     * @param field
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static String getFieldValue(StructElement se, String field) {
        String imagePath = se.getMetadataValue(field);
        try {
            if (StringUtils.isBlank(imagePath) && !se.isWork()) {
                if (se.isAnchor()) {
                    imagePath = se.getFirstVolumeFieldValue(field);
                } else if (se.getTopStruct() != null) {
                    imagePath = se.getTopStruct().getMetadataValue(field);
                }
            }
        } catch (IndexUnreachableException | PresentationException e) {
            logger.warn(e.toString());
        }
        return imagePath;
    }

    /**
     * @param page
     * @return
     */
    private String getImagePath(PhysicalElement page) {
        if (page == null) {
            return "";
        }

        String thumbnailUrl = null;

        String mimeType = page.getMimeType();
        // TODO use enum
        switch (mimeType) {
            case "image":
            case "image/png":
            case "image/jpg":
            case "image/tiff":
            case "image/jp2":
                thumbnailUrl = page.getFilepath();
                break;
            case "video":
            case "text":
                thumbnailUrl = page.getFilepath();
                if (StringUtils.isEmpty(thumbnailUrl)) {
                    thumbnailUrl = getThumbnailPath(VIDEO_THUMB).toString();
                }
                break;
            case "audio":
                thumbnailUrl = page.getFilepath();
                if (StringUtils.isEmpty(thumbnailUrl)) {
                    thumbnailUrl = getThumbnailPath(AUDIO_THUMB).toString();
                }
                break;
            case "application":
            case "application/pdf":
                thumbnailUrl = getThumbnailPath(BORN_DIGITAL_THUMB).toString();
                break;
            case "application/object":
            case "object":
                thumbnailUrl = getThumbnailPath(OBJECT_3D_THUMB).toString();
                break;
        }
        return thumbnailUrl;
    }

    /**
     * @param doc Needs to have the fields {@link SolrConstants.MIMETYPE} and {@link SolrConstants.THUMBNAIL}
     * @return The representative thumbnail url for the given doc, or a replacement image url if no representative thumbnail url is applicable (born
     *         digital material and - depending on configuration - anchors)
     */
    private String getImagePath(StructElement doc) {

        String thumbnailUrl = null;

        if (doc == null) {
            return null;
        } else if (doc.isAnchor()) {
            if (ANCHOR_THUMBNAIL_MODE_GENERIC.equals(this.anchorThumbnailMode)) {
                thumbnailUrl = getThumbnailPath(ANCHOR_THUMB).toString();
            } else if (ANCHOR_THUMBNAIL_MODE_FIRSTVOLUME.equals(this.anchorThumbnailMode)) {
                try {
                    StructElement volume = doc.getFirstVolume(Arrays.asList(REQUIRED_SOLR_FIELDS));
                    if (volume != null) {
                        thumbnailUrl = volume.getPi() + "/" + getImagePath(volume);
                    } else {
                        thumbnailUrl = getThumbnailPath(ANCHOR_THUMB).toString();
                    }
                } catch (PresentationException | IndexUnreachableException e) {
                    logger.error("Unable to retrieve first volume of " + doc + "from index", e);
                }
            } else {
                logger.error(
                        "Unknown value in viewer.anchorThumbnailMode: " + this.anchorThumbnailMode + ". No thumbnail can be rendered for " + doc);
            }
        } else {
            DocType docType = getDocType(doc).orElse(DocType.DOCSTRCT);
            switch (docType) {
                case EVENT:
                    thumbnailUrl = getThumbnailPath(EVENT_THUMB).toString();
                    break;
                case GROUP:
                    thumbnailUrl = getThumbnailPath(GROUP_THUMB).toString();
                    break;
                case METADATA:
                    MetadataGroupType metadataGroupType = getMetadataGroupType(doc).orElse(MetadataGroupType.SUBJECT);
                    switch (metadataGroupType) {
                        case PERSON:
                            thumbnailUrl = getThumbnailPath(PERSON_THUMB).toString();
                            break;
                        default:
                            break;
                    }
                    break;
                case DOCSTRCT:
                case PAGE:
                default:
                    // TODO use enum
                    String mimeType = getMimeType(doc).orElse("unknown");
                    switch (mimeType) {
                        case "image":
                        case "image/png":
                        case "image/jpg":
                        case "image/jpeg":
                        case "image/tiff":
                        case "image/jp2":
                            thumbnailUrl = getFieldValue(doc, SolrConstants.THUMBNAIL);
                            break;
                        case "video":
                        case "text":
                            thumbnailUrl = getFieldValue(doc, SolrConstants.THUMBNAIL);
                            if (StringUtils.isEmpty(thumbnailUrl)) {
                                thumbnailUrl = getThumbnailPath(VIDEO_THUMB).toString();
                            }
                            break;
                        case "audio":
                            thumbnailUrl = getFieldValue(doc, SolrConstants.THUMBNAIL);
                            if (StringUtils.isEmpty(thumbnailUrl)) {
                                thumbnailUrl = getThumbnailPath(AUDIO_THUMB).toString();
                            }
                            break;
                        case "application":
                        case "application/pdf":
                            thumbnailUrl = getThumbnailPath(BORN_DIGITAL_THUMB).toString();
                            break;
                        case "application/object":
                        case "object":
                            thumbnailUrl = getThumbnailPath(OBJECT_3D_THUMB).toString();
                            break;
                    }
            }
        }
        return thumbnailUrl;
    }

    private static Optional<DocType> getDocType(StructElement structElement) {
        DocType docType = DocType.getByName(structElement.getMetadataValue(SolrConstants.DOCTYPE));
        return Optional.ofNullable(docType);
    }

    private static Optional<MetadataGroupType> getMetadataGroupType(StructElement structElement) {
        MetadataGroupType type = MetadataGroupType.getByName(structElement.getMetadataValue(SolrConstants.METADATATYPE));
        return Optional.ofNullable(type);
    }

    private static Optional<String> getFilename(StructElement structElement) {
        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (StringUtils.isEmpty(filename)) {
            try {
                filename = structElement.getFirstPageFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.warn("Unable to retrieve first page of structElement from index");
            }
        }
        return Optional.ofNullable(filename).filter(name -> StringUtils.isNotBlank(name));
    }

    private static Optional<String> getMimeType(StructElement structElement) {
        Optional<String> mimeType = Optional.empty();
        if (structElement.isAnchor()) {
            try {
                mimeType = Optional.ofNullable(structElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE));
            } catch (PresentationException | IndexUnreachableException e) {
                logger.warn("Unable to retrieve first page of structElement from index");
            }
        } else {
            mimeType = Optional.ofNullable(structElement.getMetadataValue(SolrConstants.MIMETYPE));
        }
        if (!mimeType.isPresent()) {
            mimeType = getFilename(structElement).map(filename -> ImageFileFormat.getImageFileFormatFromFileExtension(filename).getMimeType());
        }
        return mimeType;
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem}, fit into a box of the default width and height
     *
     * @param item a {@link java.util.Optional} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(Optional<CMSMediaItem> item) {
        return getThumbnailUrl(item, thumbWidth, thumbHeight);
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem}, fit into a box of the default width and height
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(CMSMediaItem item) {
        return getThumbnailUrl(Optional.ofNullable(item), thumbWidth, thumbHeight);
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem}, fit into a box of the given width and height
     *
     * @param width a int.
     * @param height a int.
     * @param optional a {@link java.util.Optional} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(Optional<CMSMediaItem> optional, int width, int height) {
        return optional.map(item -> {
            String imagePath = item.getImageURI();
            String size = getSize(width, height);
            String format = "jpg";
            ImageFileFormat formatType = ImageFileFormat.getImageFileFormatFromFileExtension(imagePath);
            if (formatType != null && !formatType.getMimeType().matches("(?i)(image\\/(?!png|jpg|gif).*)")) { //match any image-mimetype except jpg and png
                format = formatType.getFileExtension();
            }
            String imageApiUrl = getCMSMediaImageApiUrl();
            String url = this.iiifUrlHandler.getIIIFImageUrl(imageApiUrl, imagePath, "-", Region.FULL_IMAGE, size, "0", "default", format);
            url += "?updated=" + item.getLastModifiedTime();
            return url;
        }).orElse("");
    }
    
    /**
     * Get the thumbnailUrl for an image file in the file system
     * 
     * @param imagePath
     * @return
     */
    public String getThumbnailUrl(Path imagePath) {
        return getThumbnailUrl(imagePath, thumbWidth, thumbHeight);
    }
    
    /**
     * Get the square thumbnailUrl for an image file in the file system
     * 
     * @param imagePath
     * @return
     */
    public String getSquareThumbnailUrl(Path imagePath) {
        return getSquareThumbnailUrl(imagePath, thumbWidth);
    }
    
    /**
     * Get the thumbnailUrl for an image file in the file system
     * 
     * @param imagePath
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(Path imagePath, int width, int height) {
            String size = getSize(width, height);
            String format = "jpg";
            ImageFileFormat formatType = ImageFileFormat.getImageFileFormatFromFileExtension(imagePath.toString());
            if (formatType != null && !formatType.getMimeType().matches("(?i)(image\\/(?!png|jpg).*)")) { //match any image-mimetype except jpg and png
                format = formatType.getFileExtension();
            }
            String imageApiUrl = getCMSMediaImageApiUrl();
            String url = this.iiifUrlHandler.getIIIFImageUrl(imageApiUrl, imagePath.toString(), "-", Region.FULL_IMAGE, size, "0", "default", format);
            return url;

    }
    
    /**
     * Get the square thumbnailUrl for an image file in the file system
     * 
     * @param imagePath
     * @param width
     * @param height
     * @return
     */
    public String getSquareThumbnailUrl(Path imagePath, int width) {
            String size = getSize(width, width);
            String format = "jpg";
            ImageFileFormat formatType = ImageFileFormat.getImageFileFormatFromFileExtension(imagePath.toString());
            if (formatType != null && !formatType.getMimeType().matches("(?i)(image\\/(?!png|jpg).*)")) { //match any image-mimetype except jpg and png
                format = formatType.getFileExtension();
            }
            String imageApiUrl = getCMSMediaImageApiUrl();
            String url = this.iiifUrlHandler.getIIIFImageUrl(imageApiUrl, imagePath.toString(), "-", Region.SQUARE_IMAGE, size, "0", "default", format);
            return url;

    }

    /**
     * @return
     */
    private String getCMSMediaImageApiUrl() {
        if(DataManager.getInstance().getConfiguration().isUseIIIFApiUrlForCmsMediaUrls()) {
            return DataManager.getInstance().getConfiguration().getIIIFApiUrl();
        } else {
            return DataManager.getInstance().getConfiguration().getRestApiUrl();

        }
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem}, fit into a box of the given width and height
     *
     * @param width a int.
     * @param height a int.
     * @param media a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(CMSMediaItem media, int width, int height) {
        return getThumbnailUrl(Optional.ofNullable(media), width, height);
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem} of the given size. The image is always square and
     * contains as much of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param size a int.
     * @param optional a {@link java.util.Optional} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(Optional<CMSMediaItem> optional, int size) {
        return optional.map(item -> {
            String imagePath = item.getImageURI();
            String format = "jpg";
            if (imagePath.toLowerCase().endsWith(".png")) {
                format = "png";
            }
            String url = this.iiifUrlHandler.getIIIFImageUrl(imagePath, "-", Region.SQUARE_IMAGE, size + ",", "0", "default", format);
            url += "?updated=" + item.getLastModifiedTime();
            return url;
        }).orElse("");
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem} of the given size. The image is always square and
     * contains as much of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param size a int.
     * @param media a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(CMSMediaItem media, int size) {
        return getSquareThumbnailUrl(Optional.ofNullable(media), size);
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem} of the default size. The image is always square and
     * contains as much of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param item a {@link java.util.Optional} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(Optional<CMSMediaItem> item) {
        return getSquareThumbnailUrl(item, thumbWidth);
    }

    /**
     * Return the url to the image of the given {@link io.goobi.viewer.model.cms.CMSMediaItem} of the default size. The image is always square and
     * contains as much of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSquareThumbnailUrl(CMSMediaItem item) {
        return getSquareThumbnailUrl(Optional.ofNullable(item));
    }

    /**
     * @param width
     * @param height
     * @return
     */
    private static String getSize(Integer width, Integer height) {
        String size = "max";
        if (height == null || (height.equals(0) && width != null && !width.equals(0))) {
            size = width + ",";
        } else if ((width == null || (width.equals(0)) && !height.equals(0))) {
            size = "," + height;
        } else if (!width.equals(0) && !width.equals(0)) {
            size = "!" + width + "," + height;
        }
        return size;
    }

    /**
     * Tests whether the given url refers to an image within the viewer image resource folder
     *
     * @param thumbnailUrl a {@link java.lang.String} object.
     * @return true if the url starts with the viewer url path to image resources
     */
    public boolean isStaticImageResource(String thumbnailUrl) {
        if (thumbnailUrl == null) {
            throw new IllegalArgumentException("thumbnailUrl may not be null");
        }
        return thumbnailUrl.contains(staticImagesPath);
    }

    /**
     * Creates a {@link Scale} representing the given width and height. If both values are greater than 0, a scale is returned which scaled the image
     * to fit a box of the given size. If just of width and height is greater than 0, a scale is returned to that value; if both values are 0 or less,
     * the full (max) image scale is returned
     * 
     * @param width
     * @param height
     * @return An instance of {@link Scale} which represents the given values for width and height
     */
    private static Scale getScale(int width, int height) {
        if (width > 0 && height > 0) {
            return new Scale.ScaleToBox(width, height);
        } else if (width > 0) {
            return new Scale.ScaleToWidth(width);
        } else if (height > 0) {
            return new Scale.ScaleToHeight(height);
        } else {
            return Scale.MAX;
        }
    }
}
