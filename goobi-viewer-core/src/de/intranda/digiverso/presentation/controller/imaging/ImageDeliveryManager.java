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

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author Florian Alpers
 *
 */
public class ImageDeliveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageDeliveryManager.class);

    @Inject
    private ActiveDocumentBean activeDocumentBean;

    private Optional<PhysicalElement> getCurrentPageIfExists() {
      return Optional.ofNullable(activeDocumentBean)
              .map(adb -> adb.getViewManager())
              .map(vm -> {
                try {
                    return vm.getCurrentPage();
                } catch (IndexUnreachableException | DAOException e) {
                    logger.error(e.toString());
                    return null;
                }
            });
    }
    
    private Optional<ActiveDocumentBean> getActiveDocumentBeanIfExists() {
        return Optional.ofNullable(activeDocumentBean);
    }
    
    private Optional<String> getFooterIdIfExists() {
        return getActiveDocumentBeanIfExists()
                .map(adb -> adb.getTopDocument())
                .map(doc -> getFooterId(doc));
    }

    /**
     * Creates the watermark url for the given pageType, adding watermarkId for the current {@link ActiveDocumentBean#getTopDocument()}
     * and watermarkText for the current {@link PhysicalElement page}
     * If the watermark height of the given pageType and image is 0, an empty optional is returned
     * 
     * @param info          ImageInformation as basis for watermark size. Must not be null
     * @param pageType      The pageType of the currentView. Taken into consideration for footer height, if not null
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ConfigurationException
     */
    public Optional<String> getWatermarkUrl(ImageInformation info, PageType pageType) throws IndexUnreachableException, DAOException, ConfigurationException {
        
        int footerHeight = DataManager.getInstance().getConfiguration().getFooterHeight(pageType, getImageType(info));
        if(footerHeight > 0) {            
            String format = DataManager.getInstance()
                    .getConfiguration()
                    .getWatermarkFormat();
            
            Integer width = info.getSizes().stream()
                    .sorted((size1,size2) -> Integer.compare(size2.getWidth(), size2.getWidth()))
                    .map(size -> size.getWidth())
                    .findFirst().orElse(info.getWidth());
            
            StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance()
                    .getConfiguration()
                    .getIiifUrl());
            
            urlBuilder.append("footer/full/!")
            .append(width).append(",") //width
            .append(DataManager.getInstance().getConfiguration().getFooterHeight(pageType, getImageType(info)))
            .append("/0/default.")
            .append(format)
            .append("?");
            
            getFooterIdIfExists().ifPresent(footerId -> urlBuilder.append("watermarkId=").append(footerId).append("&"));
            getCurrentPageIfExists().ifPresent(page -> urlBuilder.append("&watermarkText=").append(page.getWatermarkText()));
            
            return Optional.of(urlBuilder.toString());
        } else {
            return Optional.empty();
        }
    }
    
    private String getFooterId(StructElement topDocument) {
        String footerIdField = DataManager.getInstance()
                .getConfiguration()
                .getWatermarkIdField();
        String footerId = null;
        if (footerIdField != null && topDocument != null) {
            footerId = topDocument.getMetadataValue(footerIdField);
        }
        return footerId;
    }
    
    private ImageType getImageType(ImageInformation info) {
        String id = info.getId();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if(iff != null) {
            return new ImageType(iff);
        } else {
            return null;
        }
    }
    
}
