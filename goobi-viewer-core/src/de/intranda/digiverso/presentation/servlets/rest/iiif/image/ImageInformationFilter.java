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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.controller.imaging.WatermarkHandler;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.pageloader.LeanPageLoader;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.AbsoluteScale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.RelativeScale;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageProfile;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageTile;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;

@Provider
@ContentServerImageInfoBinding
public class ImageInformationFilter implements ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageInformationFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    private PageType pageType = PageType.viewImage;
    private ImageType imageType = null;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        Object responseObject = response.getEntity();
        if (responseObject != null && responseObject instanceof ImageInformation) {

            Path requestPath = Paths.get(request.getUriInfo().getPath());

            switch(requestPath.getName(0).toString().toLowerCase()) {
                case "fullscreen":
                    pageType = PageType.viewFullscreen;
                    break;
                case "readingmode":
                    pageType = PageType.viewReadingMode;
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

            imageType = getImageType((ImageInformation) responseObject);

			try {
				List<Integer> imageSizes = getImageSizesFromConfig();
				setImageSizes((ImageInformation) responseObject, imageSizes);
				List<ImageTile> tileSizes;
				tileSizes = getTileSizesFromConfig();
				setTileSizes((ImageInformation) responseObject, tileSizes);
				setMaxImageSizes((ImageInformation) responseObject);
				//This adds 200 or more ms to the request time. So we ignore this unless it is actually requested
//				setWatermark((ImageInformation) responseObject);
			} catch (ConfigurationException e) {
				logger.error(e.toString(), e);
			}
        }
    }
    
    /**
     * @param responseObject
     */
    private void setWatermark(ImageInformation info) {
        Path path = Paths.get(info.getId());
        String filename = path.getName(path.getNameCount()-1).toString();
        String pi = path.getName(path.getNameCount()-2).toString();
        
        if(StringUtils.isNoneBlank(filename, pi) && !pi.equals("-")) {
            try {                
                Optional<StructElement> element = getStructElement(pi);
                Optional<PhysicalElement> page = element.map(ele -> getPage(filename, ele).orElse(null));
                Optional<String> watermarkUrl = BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(page, element, Optional.ofNullable(pageType));
                watermarkUrl.ifPresent(url -> info.setLogo(url));
            } catch(DAOException | ConfigurationException | IndexUnreachableException | PresentationException e) {
                logger.error("Unable to add watermark to image information: " + e.toString(), e);
            }
        }
    }

    /**
     * @param filename
     * @param element
     * @return
     * @throws IndexUnreachableException 
     * @throws DAOException 
     * @throws PresentationException 
     */
    private Optional<PhysicalElement> getPage(String filename, StructElement element) {
        try {
            LeanPageLoader pageLoader = new LeanPageLoader(element, 1);
            return Optional.ofNullable(pageLoader.getPageForFileName(filename));
        } catch (PresentationException | IndexUnreachableException | DAOException e) {
            logger.error("Unbale to get page for file " + filename + " in " + element);
            return Optional.empty();
        }
    }

    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<StructElement> getStructElement(String pi) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder(SolrConstants.PI).append(':').append(pi).toString();
        List<String> fieldList = new ArrayList<>(Arrays.asList(WatermarkHandler.REQUIRED_SOLR_FIELDS));
        fieldList.add(DataManager.getInstance().getConfiguration().getWatermarkIdField());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, fieldList);
        if(doc != null) {            
            Long iddoc = Long.parseLong((String)doc.getFieldValue(SolrConstants.IDDOC));
            StructElement element = new StructElement(iddoc, doc);
            return Optional.ofNullable(element);
        }
        return Optional.empty();
    }

    /**
     * @param responseObject
     * @return
     */
    private ImageType getImageType(ImageInformation info) {
        String id = info.getId();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if(iff != null) {
            return new ImageType(iff);
        } else {
            return null;
        }
    }

    private static void setMaxImageSizes(ImageInformation info){
        Optional<ImageProfile> profile = info.getProfiles().stream()
                .filter(p -> p instanceof ImageProfile)
                .map(p -> (ImageProfile)p)
                .findFirst();
        profile.ifPresent(p -> {            
            int maxWidth = DataManager.getInstance().getConfiguration().getViewerMaxImageWidth();
            int maxHeight = DataManager.getInstance().getConfiguration().getViewerMaxImageHeight();
            if(maxWidth > 0) {
                p.setMaxWidth(maxWidth);
            }
            if(maxHeight > 0) {
                p.setMaxHeight(maxHeight);
            }
        });
    }

    /**
     * @param responseObject
     * @param imageSizes
     */
    private static void setImageSizes(ImageInformation imageInfo, List<Integer> imageSizes) {

        List<Dimension> dimensions = new ArrayList<>();
        for (Integer size : imageSizes) {
            dimensions.add(new Dimension(size, size));
        }
        if (dimensions.isEmpty()) {
            dimensions.add(new Dimension(imageInfo.getWidth(), imageInfo.getHeight()));
        }
        imageInfo.setSizesFromDimensions(dimensions);
    }

    /**
     * @param responseObject
     * @return
     * @throws ConfigurationException 
     */
    private List<Integer> getImageSizesFromConfig() throws ConfigurationException {

        List<String> sizeStrings = DataManager.getInstance().getConfiguration().getImageViewZoomScales(pageType, imageType);
        List<Integer> sizes = new ArrayList<>();
        for (String string : sizeStrings) {
            try {
                int size = Integer.parseInt(string);
                sizes.add(size);
            } catch (NullPointerException | NumberFormatException e) {
                logger.warn("Cannot parse " + string + " as int");
            }
        }
        return sizes;
    }

    /**
     * @return
     * @throws ConfigurationException 
     */
    private List<ImageTile> getTileSizesFromConfig() throws ConfigurationException {
        Map<Integer, List<Integer>> configSizes = Collections.EMPTY_MAP;
        if(DataManager.getInstance().getConfiguration().useTiles(pageType, imageType)) {            
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
     * @param responseObject
     * @param tileSizes
     */
    private static void setTileSizes(ImageInformation imageInfo, List<ImageTile> tileSizes) {
        imageInfo.setTiles(tileSizes);
    }


}
