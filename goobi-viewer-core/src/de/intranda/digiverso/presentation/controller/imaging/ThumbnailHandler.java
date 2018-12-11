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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrConstants.MetadataGroupType;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.pageloader.LeanPageLoader;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;

/**
 * Delivers Thumbnail urls for pages and StructElements
 * 
 * @author Florian Alpers
 *
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

    public static final String[] REQUIRED_SOLR_FIELDS =
            { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL,
                    SolrConstants.DOCTYPE, SolrConstants.METADATATYPE, SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED};

    private final int thumbWidth;
    private final int thumbHeight;
    private final int thumbCompression;
    private final String anchorThumbnailMode;
    private final String staticImagesPath;

    private static final Logger logger = LoggerFactory.getLogger(ThumbnailHandler.class);

    private final IIIFUrlHandler iiifUrlHandler;

    public ThumbnailHandler(IIIFUrlHandler iiifUrlHandler, Configuration configuration, String staticImagesPath) {
        this.iiifUrlHandler = iiifUrlHandler;
        thumbWidth = configuration.getThumbnailsWidth();
        thumbHeight = configuration.getThumbnailsHeight();
        thumbCompression = configuration.getThumbnailsCompression();
        anchorThumbnailMode = configuration.getAnchorThumbnailMode();
        this.staticImagesPath = staticImagesPath;
    }

    public URI getThumbnailPath(String filename) {
        if(StringUtils.isBlank(filename)) {
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
     * @param page
     * @return
     */
    public String getThumbnailUrl(PhysicalElement page) {
        return getThumbnailUrl(page, thumbWidth, thumbHeight);
    }

    /**
     * Returns a link to the representative image for the given pi. If the pi doesn't match an indexed item, null is returned
     * 
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public String getThumbnailUrl(String pi) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getThumbnailUrl(pi, DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * Returns a link to the representative image for the given pi with the given width and height. If the pi doesn't match an indexed item, null is
     * returned
     * 
     * @param pi the persistent identifier of the work which representative we want
     * @param width the width of the image
     * @param height the height of the image
     * @return The url string or null of no work is found
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public String getThumbnailUrl(String pi, int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (doc != null) {
            return getThumbnailUrl(doc, width, height);
        }
        return null;
    }

    /**
     * Returns a link to a square representative image for the given pi. If the pi doesn't match an indexed item, null is returned
     * 
     * @param pi the persistent identifier of the work which representative we want
     * @return The url string or null of no work is found
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
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
     * @param pi
     * @param order
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
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
     * @param page
     * @return
     */
    public String getThumbnailUrl(PhysicalElement page, int width, int height) {
        String path = getImagePath(page);
        if (isStaticImageResource(path)) {
            return path;
        } else if (IIIFUrlHandler.isIIIFImageUrl(path)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(path, null, getScale(width, height), null, null, null);
        } else if (IIIFUrlHandler.isIIIFImageInfoUrl(path)) {
            return iiifUrlHandler.getIIIFImageUrl(path, null, getScale(width, height), null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.FULL_IMAGE, "!" + width + "," + height, "0", "default", "jpg",
                    thumbCompression);
        }
    }

    /**
     * returns a link the an image representing the given page. Its size depends on configuration. The image is always square and contains as much of
     * the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     * @throws ViewerConfigurationException
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
     * @throws ViewerConfigurationException
     */
    public String getSquareThumbnailUrl(PhysicalElement page, int size) {
        String path = getImagePath(page);
        if (isStaticImageResource(path)) {
            return path;
        } else if (IIIFUrlHandler.isIIIFImageUrl(path)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(path, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else if (IIIFUrlHandler.isIIIFImageInfoUrl(path)) {
            return iiifUrlHandler.getIIIFImageUrl(path, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(path, page.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg", thumbCompression);
        }
    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration
     * 
     * @param page
     * @return
     * @throws ViewerConfigurationException
     */
    public String getThumbnailUrl(StructElement doc) {
        return getThumbnailUrl(doc, thumbWidth, thumbHeight);

    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration
     * 
     * @param page
     * @return
     * @throws ViewerConfigurationException
     */
    public String getThumbnailUrl(SolrDocument doc) throws ViewerConfigurationException {
        return getThumbnailUrl(getStructElement(doc), thumbWidth, thumbHeight);
    }

    /**
     * Returns a link to a small image representating the given document. The size depends on viewer configuration. The image may be cut at the longer
     * side to provide a square image
     * 
     * @param page
     * @return
     * @throws ViewerConfigurationException
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
     * @param page
     * @return
     * @throws ViewerConfigurationException
     */
    public String getThumbnailUrl(SolrDocument doc, int width, int height) throws ViewerConfigurationException {
        return getThumbnailUrl(getStructElement(doc), width, height);

    }

    /**
     * Returns a link to an image representating the given page of the given size. The image will be cut at the longer side to create a square image
     * 
     * @param page
     * @return
     * @throws ViewerConfigurationException
     */
    public String getSquareThumbnailUrl(SolrDocument doc, int size) throws ViewerConfigurationException {
        return getSquareThumbnailUrl(getStructElement(doc), size);

    }

    /**
     * Returns a link to an image representating the given document of the given size (to be exact: the largest image size which fits within the given
     * bounds and keeps the image proportions
     * 
     * @param doc Needs to have the fields {@link SolrConstants.MIMETYPE} and {@link SolrConstants.THUMBNAIL}
     * @return
     */
    public String getThumbnailUrl(StructElement doc, int width, int height) {
        String thumbnailUrl = getImagePath(doc);
        if (thumbnailUrl != null && isStaticImageResource(thumbnailUrl)) {
            return thumbnailUrl;
        } else if (IIIFUrlHandler.isIIIFImageUrl(thumbnailUrl)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(thumbnailUrl, null, getScale(width, height).toString(), null, null, null);
        } else if (IIIFUrlHandler.isIIIFImageInfoUrl(thumbnailUrl)) {
            return iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, null, getScale(width, height).toString(), null, null, null);
        } else if (thumbnailUrl != null) {
            return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, doc.getPi(), Region.FULL_IMAGE, "!" + width + "," + height, "0", "default",
                    "jpg", thumbCompression);
        } else {
            return null;
        }
    }

    /**
     * returns a link the an image representing the given document. Its size depends on configuration. The image is always square and contains as much
     * of the actual image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param page
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(StructElement doc) {
        return getSquareThumbnailUrl(doc, thumbWidth);
    }

    /**
     * returns a link the an image representing the given document of the given size. The image is always square and contains as much of the actual
     * image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param doc Needs to have the fields {@link SolrConstants.MIMETYPE} and {@link SolrConstants.THUMBNAIL}
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(StructElement doc, int size) {
        String thumbnailUrl = getImagePath(doc);
        if (StringUtils.isNotBlank(thumbnailUrl) && isStaticImageResource(thumbnailUrl)) {
            return thumbnailUrl;
        } else if (IIIFUrlHandler.isIIIFImageUrl(thumbnailUrl)) {
            return iiifUrlHandler.getModifiedIIIFFUrl(thumbnailUrl, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else if (IIIFUrlHandler.isIIIFImageInfoUrl(thumbnailUrl)) {
            return iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, Region.SQUARE_IMAGE, getScale(size, size).toString(), null, null, null);
        } else {
            return this.iiifUrlHandler.getIIIFImageUrl(thumbnailUrl, doc.getPi(), Region.SQUARE_IMAGE, size + ",", "0", "default", "jpg",
                    thumbCompression);
        }
    }

    /**
     * @param doc
     * @param field
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static String getFieldValue(StructElement doc, String field) {
        String imagePath = doc.getMetadataValue(field);
        try {
            if (StringUtils.isBlank(imagePath) && !doc.isWork()) {
                if (doc.isAnchor()) {
                    imagePath = doc.getFirstVolumeFieldValue(field);
                } else {
                    imagePath = doc.getTopStruct().getMetadataValue(field);
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

        String thumbnailUrl = null;

        String mimeType = page.getMimeType();
        switch (mimeType) {
            case "image":
            case "image/png":
            case "image/jpg":
            case "image/tiff":
            case "image/jp2":
                thumbnailUrl = page.getFilepath();
                break;
            case PhysicalElement.MIME_TYPE_VIDEO:
            case PhysicalElement.MIME_TYPE_SANDBOXED_HTML:
                thumbnailUrl = getThumbnailPath(VIDEO_THUMB).toString();
                break;
            case PhysicalElement.MIME_TYPE_AUDIO:
                thumbnailUrl = getThumbnailPath(AUDIO_THUMB).toString();
                break;
            case PhysicalElement.MIME_TYPE_APPLICATION:
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
                    thumbnailUrl = volume.getPi() + "/" + getImagePath(volume);
                } catch (PresentationException | IndexUnreachableException e) {
                    logger.error("Unable to retrieve first volume of " + doc + "from index", e);
                }
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
                        case PhysicalElement.MIME_TYPE_VIDEO:
                        case PhysicalElement.MIME_TYPE_SANDBOXED_HTML:
                            thumbnailUrl = getThumbnailPath(VIDEO_THUMB).toString();
                            break;
                        case PhysicalElement.MIME_TYPE_AUDIO:
                            thumbnailUrl = getThumbnailPath(AUDIO_THUMB).toString();
                            break;
                        case PhysicalElement.MIME_TYPE_APPLICATION:
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

    private Optional<DocType> getDocType(StructElement structElement) {
        DocType docType = DocType.getByName(structElement.getMetadataValue(SolrConstants.DOCTYPE));
        return Optional.ofNullable(docType);
    }

    private Optional<MetadataGroupType> getMetadataGroupType(StructElement structElement) {
        MetadataGroupType type = MetadataGroupType.getByName(structElement.getMetadataValue(SolrConstants.METADATATYPE));
        return Optional.ofNullable(type);
    }

    private Optional<String> getFilename(StructElement structElement) {
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

    private Optional<String> getMimeType(StructElement structElement) {
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
     * Return the url to the image of the given {@link CMSMediaItem}, fit into a box of the default width and height
     * 
     * @param item
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(Optional<CMSMediaItem> item) {
        return getThumbnailUrl(item, thumbWidth, thumbHeight);
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem}, fit into a box of the default width and height
     * 
     * @param item
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(CMSMediaItem item) {
        return getThumbnailUrl(Optional.ofNullable(item), thumbWidth, thumbHeight);
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem}, fit into a box of the given width and height
     * 
     * @param item
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(Optional<CMSMediaItem> optional, int width, int height) {
        return optional.map(item -> {
            String imagePath = item.getImageURI();
            String size = getSize(width, height);
            String format = "jpg";
            if (imagePath.toLowerCase().endsWith(".png")) {
                format = "png";
            }
            return this.iiifUrlHandler.getIIIFImageUrl(imagePath, "-", Region.FULL_IMAGE, size, "0", "default", format, thumbCompression);
        }).orElse("");
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem}, fit into a box of the given width and height
     * 
     * @param item
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(CMSMediaItem media, int width, int height) {
        return getThumbnailUrl(Optional.ofNullable(media), width, height);
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem} of the given size. The image is always square and contains as much of the actual
     * image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param item
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(Optional<CMSMediaItem> optional, int size) {
        return optional.map(item -> {
            String imagePath = item.getImageURI();
            String format = "jpg";
            if (imagePath.toLowerCase().endsWith(".png")) {
                format = "png";
            }
            return this.iiifUrlHandler.getIIIFImageUrl(imagePath, "-", Region.SQUARE_IMAGE, size + ",", "0", "default", format, thumbCompression);
        }).orElse("");
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem} of the given size. The image is always square and contains as much of the actual
     * image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param item
     * @param size
     * @return
     */
    public String getSquareThumbnailUrl(CMSMediaItem media, int size) {
        return getSquareThumbnailUrl(Optional.ofNullable(media), size);
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem} of the default size. The image is always square and contains as much of the
     * actual image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param item
     * @return
     */
    public String getSquareThumbnailUrl(Optional<CMSMediaItem> item) {
        return getSquareThumbnailUrl(item, thumbWidth);
    }

    /**
     * Return the url to the image of the given {@link CMSMediaItem} of the default size. The image is always square and contains as much of the
     * actual image as is possible to fit into a square - the delivered square is always centered within the full image
     * 
     * @param item
     * @return
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
     * @param thumbnailUrl
     * @return true if the url starts with the viewer url path to image resources
     */
    public boolean isStaticImageResource(String thumbnailUrl) {
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
