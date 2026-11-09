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
package io.goobi.viewer.controller.imaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.servlets.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Session independent bundle of the URL handlers used for media delivery (thumbnails, images, PDFs, watermarks, IIIF, ...) together with the URL
 * classification checks (external/CMS/temp/static image URL).
 *
 * <p>
 * Unlike the session scoped {@link io.goobi.viewer.managedbeans.ImageDeliveryBean}, this class does not depend on an HTTP session and can safely be
 * used from REST resources, servlets and filters without causing a session to be created. All handlers only depend on the configuration, the REST API
 * URL managers and the servlet path (scheme, host and context path) of the request.
 */
public class MediaDeliveryHandlers {

    private static final Logger logger = LogManager.getLogger(MediaDeliveryHandlers.class);

    /** Request attribute under which {@link #forRequest(HttpServletRequest)} caches its instance for the duration of a request. */
    static final String REQUEST_ATTRIBUTE = MediaDeliveryHandlers.class.getName();

    private final Configuration config;
    private final String servletPath;
    private final String staticImagesURI;
    private final IIIFUrlHandler iiif;
    private final ImageHandler images;
    private final ThumbnailHandler thumbs;
    private final PdfHandler pdf;
    private final WatermarkHandler footer;
    private final MediaHandler media;
    private final Object3DHandler objects3d;
    private final IIIFPresentationAPIHandler presentation;

    /**
     * Constructor.
     *
     * @param servletPath servlet path including scheme, host and context path; may be empty if unknown
     * @param config viewer configuration to read settings from
     * @param dataUrlManager URL manager for data (IIIF) API endpoints
     * @param contentUrlManager URL manager for content delivery endpoints; may be null
     */
    public MediaDeliveryHandlers(String servletPath, Configuration config, AbstractApiUrlManager dataUrlManager,
            AbstractApiUrlManager contentUrlManager) {
        this.config = config;
        this.servletPath = servletPath == null ? "" : servletPath;
        this.staticImagesURI = getStaticImagesPath(this.servletPath, config.getTheme());

        this.iiif = new IIIFUrlHandler(contentUrlManager);
        this.images = new ImageHandler(contentUrlManager);
        this.objects3d = contentUrlManager != null ? new Object3DHandler(contentUrlManager) : new Object3DHandler(config);
        this.footer = new WatermarkHandler(config, config.getIIIFApiUrl());
        this.thumbs = new ThumbnailHandler(iiif, this.staticImagesURI);
        this.pdf = contentUrlManager != null ? new PdfHandler(footer, contentUrlManager) : new PdfHandler(footer, config);
        this.media = new MediaHandler(contentUrlManager);
        IIIFPresentationAPIHandler presentationHandler = null;
        try {
            presentationHandler = new IIIFPresentationAPIHandler(dataUrlManager, config);
        } catch (URISyntaxException e) {
            logger.error("Failed to initalize presentation api handler {}", e.getMessage());
        }
        this.presentation = presentationHandler;
    }

    /**
     * Creates handlers for the given servlet path, using configuration and REST API URL managers from the {@link DataManager}.
     *
     * @param servletPath servlet path including scheme, host and context path; may be empty if unknown
     * @return a new instance
     */
    public static MediaDeliveryHandlers create(String servletPath) {
        DataManager dm = DataManager.getInstance();
        return new MediaDeliveryHandlers(servletPath, dm.getConfiguration(), dm.getRestApiManager().getIIIFDataApiManager(),
                dm.getRestApiManager().getContentApiManager().orElse(null));
    }

    /**
     * Returns handlers for the given request. The instance is cached as a request attribute so repeated calls within the same request don't rebuild
     * the handlers. Never touches the HTTP session.
     *
     * @param request current request; may be null, in which case an instance with empty servlet path is returned
     * @return handlers for the given request
     * @should cache instance in request
     * @should not create session
     * @should use servlet path of request
     */
    public static MediaDeliveryHandlers forRequest(HttpServletRequest request) {
        if (request == null) {
            return create("");
        }
        Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached instanceof MediaDeliveryHandlers handlers) {
            return handlers;
        }
        MediaDeliveryHandlers handlers = create(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        request.setAttribute(REQUEST_ATTRIBUTE, handlers);
        return handlers;
    }

    /**
     * Retrieves the url path to the viewer image resource folder. If the theme is given, the image resource folder within the theme is returned.
     *
     * @param servletPath base servlet path including host
     * @param theme The name of the theme housing the images. If this is null or empty, the images are taken from the viewer core
     * @return The url to the images folder in resources (possibly in the given theme)
     */
    public static String getStaticImagesPath(String servletPath, String theme) {
        StringBuilder sb = new StringBuilder(servletPath == null ? "" : servletPath);
        if (!sb.toString().endsWith("/")) {
            sb.append("/");
        }
        sb.append("resources").append("/");
        if (StringUtils.isNotBlank(theme)) {
            sb.append("themes").append("/").append(theme).append("/");
        }
        sb.append("images").append("/");
        return sb.toString();
    }

    /**
     * Returns true if the given String denotes an absolute url that is not a file url and does not start with the given servlet path.
     *
     * @param url the string to test
     * @param servletPath servlet path including scheme, host and context path
     * @return whether the given string denotes to an external url resource
     * @should return true for absolute http url of other host
     * @should return false for url within servlet path
     * @should return false for file url
     * @should return false for relative url
     */
    public static boolean isExternalUrl(String url, String servletPath) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url);
            if (uri.isAbsolute() && !uri.getScheme().toLowerCase().startsWith("file")) {
                return !url.startsWith(servletPath == null ? "" : servletPath);
            }
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Tests whether the given url points to a cms media file - i.e. any file within the configured cms media path.
     *
     * @param url The url to test
     * @param config viewer configuration
     * @return true if the url points to a cms media file
     */
    public static boolean isCmsUrl(String url, Configuration config) {
        return isInFolder(url, config.getViewerHome() + config.getCmsMediaFolder());
    }

    /**
     * Tests whether the given url points to a temporary media file - i.e. any file within the configured temp media path.
     *
     * @param url The url to test
     * @param config viewer configuration
     * @return true if the url points to a temp media file
     */
    public static boolean isTempUrl(String url, Configuration config) {
        return isInFolder(url, config.getViewerHome() + config.getTempMediaFolder());
    }

    /**
     * @param url URL to test
     * @param config viewer configuration
     * @return true if given url is cms or temp url; false otherwise
     */
    public static boolean isPublicUrl(String url, Configuration config) {
        return isCmsUrl(url, config) || isTempUrl(url, config);
    }

    /**
     * Tests whether the given url points to a static image resource within the configured theme.
     *
     * @param url the url to test
     * @param servletPath servlet path including scheme, host and context path
     * @param config viewer configuration
     * @return true if the url points to a static image resource within the current theme
     */
    public static boolean isStaticImageUrl(String url, String servletPath, Configuration config) {
        return url != null && url.startsWith(getStaticImagesPath(servletPath, config.getTheme()));
    }

    /**
     * Tests whether the given url points to a resource that is not subject to record access conditions: an external url, a cms or temp media file or
     * a static image of the theme.
     *
     * @param url the url to test
     * @param servletPath servlet path including scheme, host and context path
     * @param config viewer configuration
     * @return true if access to the given url is not restricted by access conditions
     * @should return true for external url
     * @should return true for static image url
     * @should return false for record image file name
     */
    public static boolean isUnrestrictedUrl(String url, String servletPath, Configuration config) {
        return isExternalUrl(url, servletPath) || isPublicUrl(url, config) || isStaticImageUrl(url, servletPath, config);
    }

    private static boolean isInFolder(final String url, String folder) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = PathConverter.toURI(StringTools.decodeUrl(url));
            Path path = PathConverter.getPath(uri);
            if (path.isAbsolute()) {
                return path.normalize().startsWith(Paths.get(folder));
            }
        } catch (URISyntaxException e) {
            logger.trace(e.toString());
        }
        return false;
    }

    /**
     * @param url the url to test
     * @return true if the url is external with respect to this instance's servlet path
     * @see #isExternalUrl(String, String)
     */
    public boolean isExternalUrl(String url) {
        return isExternalUrl(url, servletPath);
    }

    /**
     * @param url the url to test
     * @return true if the url points to a cms media file
     * @see #isCmsUrl(String, Configuration)
     */
    public boolean isCmsUrl(String url) {
        return isCmsUrl(url, config);
    }

    /**
     * @param url the url to test
     * @return true if the url points to a temp media file
     * @see #isTempUrl(String, Configuration)
     */
    public boolean isTempUrl(String url) {
        return isTempUrl(url, config);
    }

    /**
     * @param url the url to test
     * @return true if the url points to a cms or temp media file
     * @see #isPublicUrl(String, Configuration)
     */
    public boolean isPublicUrl(String url) {
        return isPublicUrl(url, config);
    }

    /**
     * @param url the url to test
     * @return true if the url points to a static image resource of the theme
     */
    public boolean isStaticImageUrl(String url) {
        return url != null && url.startsWith(staticImagesURI);
    }

    public String getServletPath() {
        return servletPath;
    }

    public String getStaticImagesURI() {
        return staticImagesURI;
    }

    public IIIFUrlHandler getIiif() {
        return iiif;
    }

    public ImageHandler getImages() {
        return images;
    }

    public ThumbnailHandler getThumbs() {
        return thumbs;
    }

    public PdfHandler getPdf() {
        return pdf;
    }

    public WatermarkHandler getFooter() {
        return footer;
    }

    public MediaHandler getMedia() {
        return media;
    }

    public Object3DHandler getObjects3D() {
        return objects3d;
    }

    /**
     * @return the presentation API handler; may be null if it could not be initialized
     */
    public IIIFPresentationAPIHandler getPresentation() {
        return presentation;
    }
}
