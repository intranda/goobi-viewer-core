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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
@Named("imageDelivery")
@SessionScoped
public class ImageDeliveryBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7128779942549718191L;

    private static final Logger logger = LoggerFactory.getLogger(ImageDeliveryBean.class);

    @Inject
    private HttpServletRequest servletRequest;

    private String servletPath;
    private ImageHandler image;
    private ThumbnailHandler thumb;
    private PdfHandler pdf;
    private WatermarkHandler footer;
    private IIIFUrlHandler iiif;


    public void init() {
        try {
            Configuration config = DataManager.getInstance().getConfiguration();
            if(servletRequest != null) {
                this.servletPath = ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
            } else if(BeanUtils.hasJsfContext()) {
                this.servletPath = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            } else {
                logger.error("Failed to initialize ImageDeliveryBean: No servlet request and no jsf context found");
                servletPath = "";
            }
            iiif = new IIIFUrlHandler();
            footer = new WatermarkHandler(config, servletPath);
            thumb = new ThumbnailHandler(iiif, config, getStaticImagesPath(servletPath, config.getTheme()));
            pdf = new PdfHandler(footer, config);
            image = new ImageHandler(config);
        } catch(NullPointerException e) {
            logger.error("Failed to initialize ImageDeliveryBean: Resources misssing");
        }
    }

    private Optional<PhysicalElement> getCurrentPageIfExists() {
        return Optional.ofNullable(BeanUtils.getActiveDocumentBean()).map(adb -> adb.getViewManager()).map(vm -> {
            try {
                return vm.getCurrentPage();
            } catch (IndexUnreachableException | DAOException e) {
                logger.error(e.toString());
                return null;
            }
        });
    }

    private Optional<StructElement> getTopDocumentIfExists() {
        return Optional.ofNullable(BeanUtils.getActiveDocumentBean()).map(bean -> bean.getTopDocument());
    }

    private Optional<StructElement> getCurrentDocumentIfExists() {
        return Optional.ofNullable(BeanUtils.getActiveDocumentBean()).map(bean -> {
            try {
                return bean.getCurrentElement();
            } catch (IndexUnreachableException e) {
                logger.error(e.toString());
                return null;
            }
        });
    }

    /**
     * @return The representative thumbnail for the current top docStruct element
     */
    public String getRepresentativeThumbnail() {
        return getTopDocumentIfExists().map(doc -> getThumb().getThumbnailUrl(doc)).orElse("");
    }

    /**
     * 
     * @param page
     * @return The thumbnail of the current page
     */
    public String getCurrentPageThumbnail() {
        return getCurrentPageIfExists().map(page -> getThumb().getThumbnailUrl(page)).orElse("");
    }

    /**
     * @return the servletRequest
     */
    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }


    /**
     * @param servletRequest the servletRequest to set
     */
    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    /**
     * @return the iiif
     */
    public IIIFUrlHandler getIiif() {
        if(iiif == null) {
            init();
        }
        return iiif;
    }
    
    /**
     * @return the footer
     */
    public WatermarkHandler getFooter() {
        if(footer == null) {
            init();
        }
        return footer;
    }
    
    /**
     * @return the image
     */
    public ImageHandler getImage() {
        if(image == null) {
            init();
        }
        return image;
    }
    
    /**
     * @return the pdf
     */
    public PdfHandler getPdf() {
        if(pdf == null) {
            init();
        }
        return pdf;
    }
    
    /**
     * @return the thumb
     */
    public ThumbnailHandler getThumb() {
        if(thumb == null) {
            init();
        }
        return thumb;
    }
    
    /**
     * @return the servletPath
     */
    public String getServletPath() {
        if(servletPath == null) {
            init();
        }
        return servletPath;
    }

    /**
     * @param decode
     */
    public boolean isExternalUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
                if(uri.isAbsolute() && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {                    
                    return !urlString.startsWith(getServletPath());        
                } else {
                    return false;
                }
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    /**
     * @param theme     The name of the theme housing the images. If this is null or empty, the images are taken from the viewer core
     * @return          The url to the images folder in resources (possibly in the given theme)
     */
    public static String getStaticImagesPath(String servletPath, String theme) {
        StringBuilder sb = new StringBuilder(servletPath);
        if(!sb.toString().endsWith("/")) {
            sb.append("/");
        }
        sb.append("resources").append("/");
        if(StringUtils.isNotBlank(theme)) {
            sb.append("themes").append("/").append("theme").append("/");
        }
        sb.append("images").append("/");
        return sb.toString();
    }
}
