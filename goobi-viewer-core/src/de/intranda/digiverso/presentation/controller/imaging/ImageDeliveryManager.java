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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * Provides urls to download pdfs, images and image footer
 * 
 * @author Florian Alpers
 *
 */
public class ImageDeliveryManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageDeliveryManager.class);



//    @Inject
//    private ActiveDocumentBean activeDocumentBean;

//    private Optional<PhysicalElement> getCurrentPageIfExists() {
//        return Optional.ofNullable(activeDocumentBean).map(adb -> adb.getViewManager()).map(vm -> {
//            try {
//                return vm.getCurrentPage();
//            } catch (IndexUnreachableException | DAOException e) {
//                logger.error(e.toString());
//                return null;
//            }
//        });
//    }
//
//    private Optional<ActiveDocumentBean> getActiveDocumentBeanIfExists() {
//        return Optional.ofNullable(activeDocumentBean);
//    }
//
//    private Optional<String> getFooterIdIfExists() {
//        return getActiveDocumentBeanIfExists().map(adb -> adb.getTopDocument()).map(doc -> getFooterId(doc));
//    }








    /**
     * Returns the image link for the given page and pageType. For external images, this links to the IIIF image information json+ls For external
     * images, this may either also be a IIIF image information or the image itself
     * 
     * @param page
     * @return
     */
    public String getImageUrl(PhysicalElement page, PageType pageType) {

        String pageName;
        switch (pageType) {
            case viewFullscreen:
                pageName = "fullscreen/image";
                break;
            case editContent:
            case editOcr:
            case editHistory:
                pageName = "crowdsourcing/image";
                break;
            default:
                pageName = "image";

        }

        if (isRestrictedUrl(page.getFilepath())) {
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
            sb.append(pageName).append("/-/").append(Helper.encodeUrl(page.getFilepath())).append("/info.json");
            return sb.toString();
        } else if (isExternalUrl(page.getFilepath())) {
            return page.getFilepath();
        } else {
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
            sb.append(pageName).append("/-/").append(page.getPi()).append("/").append(page.getFileName()).append("/info.json");
            return sb.toString();
        }
    }

    /**
     * Returns the image link for the given page. For external images, this links to the IIIF image information json+ls For external images, this may
     * either also be a IIIF image information or the image itself
     * 
     * @param page
     * @return
     */
    public String getImageUrl(PhysicalElement page) {
        return getImageUrl(page, PageType.viewImage);
    }











    /**
     * @param path
     * @return  true exactly if the given path starts with {@code http://} or {@code https://}
     */
    public static boolean isExternalUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }


    /**
     * 
     * @param displayableTypesOnly if true, the method only returns true for images that can be directly displayed in a browser (jpg and png)
     * @return true if the url ends with an image file suffix
     */
    protected static boolean isImageUrl(String url, boolean displayableTypesOnly) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String extension = FilenameUtils.getExtension(url.toLowerCase());
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
                return true;
            case "tif":
            case "tiff":
            case "jp2":
                return !displayableTypesOnly;
            default:
                return false;
        }
    }

    /**
     * TODO: implement
     * 
     * @return true if the path is an external url which has restricted access and must therefore be delivered via the contenetServer
     */
    protected static boolean isRestrictedUrl(String path) {
        return false;
    }
    
    protected static ImageType getImageType(ImageInformation info) {
        String id = info.getId();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if (iff != null) {
            return new ImageType(iff);
        } else {
            return null;
        }
    }

    /**
     * @param fileUrl
     * @return
     */
    public static boolean isInternalUrl(String fileUrl) {
        return fileUrl.startsWith("file:/") || fileUrl.startsWith("/");
    }



}
