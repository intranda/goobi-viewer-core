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
package de.intranda.digiverso.presentation.managedbeans;

import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler;
import de.intranda.digiverso.presentation.controller.imaging.ImageHandler;
import de.intranda.digiverso.presentation.controller.imaging.PdfHandler;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.controller.imaging.WatermarkHandler;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
@Named
@SessionScoped
public class ImageDeliveryBean {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageDeliveryBean.class);
    
    @Inject
    ActiveDocumentBean activeDocumentBean;
    @Inject
    NavigationHelper navigationHelper;
    
    
    private final ImageHandler image;
    private final ThumbnailHandler thumb;
    private final PdfHandler pdf;
    private final WatermarkHandler footer;
    private final IIIFUrlHandler iiif;
    
    public ImageDeliveryBean() {
        Configuration config = DataManager.getInstance().getConfiguration();
        iiif = new IIIFUrlHandler();
        footer = new WatermarkHandler(config, BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        thumb = new ThumbnailHandler(iiif, config, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "resources/themes/" + navigationHelper.getTheme() + "/images/");
        pdf = new PdfHandler(footer, config);
        image = new ImageHandler(config);
    }

  private Optional<PhysicalElement> getCurrentPageIfExists() {
      return Optional.ofNullable(activeDocumentBean).map(adb -> adb.getViewManager()).map(vm -> {
          try {
              return vm.getCurrentPage();
          } catch (IndexUnreachableException | DAOException e) {
              logger.error(e.toString());
              return null;
          }
      });
  }
  
  private Optional<StructElement> getTopDocumentIfExists() {
      return Optional.ofNullable(activeDocumentBean)
              .map(bean -> bean.getTopDocument());
  }

    
    private Optional<StructElement>  getCurrentDocumentIfExists() {
        return Optional.ofNullable(activeDocumentBean)
                .map(bean -> {
                    try {
                        return bean.getCurrentElement();
                    } catch (IndexUnreachableException e) {
                        logger.error(e.toString());
                        return null;
                    }
                });
    }
    
    /**
     * @return  The representative thumbnail for the current top docStruct element
     */
    public String getRepresentativeThumbnail() {
        return getTopDocumentIfExists()
                .map(doc -> thumb.getThumbnailUrl(doc))
                .orElse("");
    }
    
    /**
     * 
     * @param page
     * @return  The thumbnail of the current page
     */
    public String getCurrentPageThumbnail() {
        return getCurrentPageIfExists().map(page -> thumb.getThumbnailUrl(page)).orElse("");
    }
    
}
