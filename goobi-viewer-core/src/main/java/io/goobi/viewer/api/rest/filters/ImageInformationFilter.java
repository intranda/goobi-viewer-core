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
package io.goobi.viewer.api.rest.filters;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.image.ImageProfile;
import de.intranda.api.iiif.image.ImageTile;
import de.intranda.api.iiif.image.v3.ImageInformation3;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.WatermarkHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * Filter for IIIF Image info.json requests. Sets the tile sizes, image sizes and maximum sizes configured in config_viewer.xml
 * </p>
 */
@Provider
@ContentServerImageInfoBinding
public class ImageInformationFilter implements ContainerResponseFilter {

    private static final Logger logger = LogManager.getLogger(ImageInformationFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    private PageType pageType = PageType.viewImage;
    private ImageType imageType = null;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        Object responseObject = response.getEntity();
        if (responseObject instanceof ImageInformation) {

            Path requestPath = Paths.get(request.getUriInfo().getPath());

            if (request.getUriInfo().getQueryParameters().containsKey("pageType")) {
                String pageTypeName = request.getUriInfo().getQueryParameters().get("pageType").stream().findAny().orElse("image");
                this.pageType = PageType.getByName(pageTypeName);
            } else {

                switch (requestPath.getName(0).toString().toLowerCase()) {
                    case "readingmode":
                    case "fullscreen":
                        pageType = PageType.viewFullscreen;
                        break;
                    case "image":
                        pageType = PageType.viewImage;
                        break;
                    case "crowdsourcing":
                        pageType = PageType.editContent;
                        break;
                    default:
                        pageType = PageType.getByName(requestPath.getName(0).toString());

                }
            }

            imageType = getImageType((ImageInformation) responseObject);

            try {
                boolean mayZoom = isZoomingAllowed();
                List<Integer> imageSizes = getImageSizesFromConfig(mayZoom);
                setImageSizes((ImageInformation) responseObject, imageSizes);
                setMaxImageSizes((ImageInformation) responseObject, mayZoom);
                if (mayZoom) {
                    List<ImageTile> tileSizes = getTileSizesFromConfig();
                    setTileSizes((ImageInformation) responseObject, tileSizes);
                } else {
                    ((ImageInformation) responseObject).setTiles(null);
                }
                //This adds 200 or more ms to the request time. So we ignore this unless it is actually requested
                //				setWatermark((ImageInformation) responseObject);
            } catch (ViewerConfigurationException e) {
                logger.error(e.toString());
            }
        }
    }

    /**
     * @return true if zoom allowed; false otherwise
     */
    private boolean isZoomingAllowed() {
        if (DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth() > 0) {
            String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
            String filename = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);
            try {
                return AccessConditionUtils
                        .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(servletRequest, pi, filename, IPrivilegeHolder.PRIV_ZOOM_IMAGES)
                        .isGranted();
            } catch (IndexUnreachableException | DAOException e) {
                logger.error(e.toString(), e);
                return false;
            }
        }

        return true;
    }

    /**
     * @param info
     * @throws ViewerConfigurationException
     */
    private void setWatermark(ImageInformation info) {
        Path path = Paths.get(info.getId());
        String filename = path.getName(path.getNameCount() - 1).toString();
        String pi = path.getName(path.getNameCount() - 2).toString();

        if (StringUtils.isNoneBlank(filename, pi) && !pi.equals("-")) {
            try {
                Optional<StructElement> element = getStructElement(pi);
                Optional<PhysicalElement> page = element.map(ele -> getPage(filename, ele).orElse(null));
                Optional<String> watermarkUrl =
                        BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(page, element, Optional.ofNullable(pageType));
                watermarkUrl.ifPresent(url -> info.setLogo(url));
            } catch (DAOException | ViewerConfigurationException | IndexUnreachableException | PresentationException e) {
                logger.error("Unable to add watermark to image information: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * @param filename
     * @param element
     * @return Optional<PhysicalElement>
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    private static Optional<PhysicalElement> getPage(String filename, StructElement element) {
        try {
            IPageLoader pageLoader = AbstractPageLoader.create(element);
            return Optional.ofNullable(pageLoader.getPageForFileName(filename));
        } catch (PresentationException | IndexUnreachableException | DAOException e) {
            logger.error("Unbale to get page for file {} in {}", filename, element);
            return Optional.empty();
        }
    }

    /**
     * <p>
     * getStructElement.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Optional<StructElement> getStructElement(String pi) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder(SolrConstants.PI).append(':').append(pi).toString();
        List<String> fieldList = new ArrayList<>(Arrays.asList(WatermarkHandler.REQUIRED_SOLR_FIELDS));
        fieldList.addAll(DataManager.getInstance().getConfiguration().getWatermarkIdField());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, fieldList);
        if (doc != null) {
            Long iddoc = Long.parseLong((String) doc.getFieldValue(SolrConstants.IDDOC));
            StructElement element = new StructElement(iddoc, doc);
            return Optional.ofNullable(element);
        }
        return Optional.empty();
    }

    /**
     * @param info
     * @return {@link ImageType}
     */
    private static ImageType getImageType(ImageInformation info) {
        String id = info.getId().toString();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if (iff != null) {
            return new ImageType(iff);
        }

        return null;
    }

    /**
     * Write
     *
     * @param info
     * @param mayZoom
     */
    private static void setMaxImageSizes(ImageInformation info, boolean mayZoom) {

        Integer maxWidth = mayZoom ? DataManager.getInstance().getConfiguration().getViewerMaxImageWidth()
                : DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth();
        Integer maxHeight = mayZoom ? DataManager.getInstance().getConfiguration().getViewerMaxImageHeight()
                : DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth() * info.getHeight() / info.getWidth();

        if (info instanceof ImageInformation3) {
            ((ImageInformation3) info).setMaxWidth(maxWidth);
            ((ImageInformation3) info).setMaxHeight(maxHeight);
        } else {
            Optional<ImageProfile> profile =
                    info.getProfiles().stream().filter(p -> p instanceof ImageProfile).map(p -> (ImageProfile) p).findFirst();
            profile.ifPresent(p -> {
                if (maxWidth != null && maxWidth > 0) {
                    p.setMaxWidth(maxWidth);
                }
                if (maxHeight != null && maxHeight > 0) {
                    p.setMaxHeight(maxHeight);
                }
            });
        }
    }

    /**
     * Set the IIIF image info property "sizes". Create one size object per entry of imageSizes. Values of imageSizes are interpreted as width
     *
     * @param imageInfo
     * @param imageSizes
     */
    private static void setImageSizes(ImageInformation imageInfo, List<Integer> imageSizes) {

        List<Dimension> dimensions = new ArrayList<>();
        for (Integer size : imageSizes) {
            float ratio = imageInfo.getHeight() / (float) imageInfo.getWidth();
            dimensions.add(new Dimension(size, Math.round(size * ratio)));
        }
        if (dimensions.isEmpty()) {
            dimensions.add(new Dimension(imageInfo.getWidth(), imageInfo.getHeight()));
        }
        imageInfo.setSizesFromDimensions(dimensions);
    }

    /**
     * @param mayZoom
     * @return List<Integer>
     * @throws ViewerConfigurationException
     */
    private List<Integer> getImageSizesFromConfig(boolean mayZoom) throws ViewerConfigurationException {

        List<String> sizeStrings = DataManager.getInstance().getConfiguration().getImageViewZoomScales(pageType, imageType);
        List<Integer> sizes = new ArrayList<>();
        int maxWidth = mayZoom ? Integer.MAX_VALUE : DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth();
        for (String string : sizeStrings) {
            try {
                int size = Integer.parseInt(string);
                if (size <= maxWidth) {
                    sizes.add(size);
                }
            } catch (NullPointerException | NumberFormatException e) {
                //                logger.warn("Cannot parse " + string + " as int");
            }
        }
        if (sizes.isEmpty()) {
            sizes.add(maxWidth);
        }
        return sizes;
    }

    /**
     * @return List<ImageTile>
     * @throws ViewerConfigurationException
     */
    private List<ImageTile> getTileSizesFromConfig() throws ViewerConfigurationException {
        Map<Integer, List<Integer>> configSizes = Collections.emptyMap();
        if (DataManager.getInstance().getConfiguration().useTiles(pageType, imageType)) {
            configSizes = DataManager.getInstance().getConfiguration().getTileSizes(pageType, imageType);
        }
        List<ImageTile> tiles = new ArrayList<>();
        for (Integer size : configSizes.keySet()) {
            ImageTile tile = new ImageTile(size, size, configSizes.get(size));
            tiles.add(tile);
        }
        return tiles;
    }

    /**
     * @param imageInfo
     * @param tileSizes
     */
    private static void setTileSizes(ImageInformation imageInfo, List<ImageTile> tileSizes) {
        imageInfo.setTiles(tileSizes);
    }

}
