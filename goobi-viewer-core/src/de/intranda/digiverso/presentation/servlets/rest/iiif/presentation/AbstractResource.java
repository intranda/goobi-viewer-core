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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class AbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResource.class);
    
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;

    protected String ATTRIBUTION = "Provided by intranda GmbH";

    /**
     * @param language
     * @return
     */
    protected Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    protected String getServletURI() {
        return ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
        //        return servletRequest.getContextPath();
    }

    protected URI absolutize(URI uri) throws URISyntaxException {
        if (uri == null || uri.isAbsolute()) {
            return uri;
        } else {
            return new URI(getServletURI() + uri.toString());
        }
    }
    
    /**
     * @param rssUrl
     * @return
     * @throws URISyntaxException 
     */
    protected URI absolutize(String url) throws URISyntaxException {
        if(url != null) {
            url = url.replaceAll("\\s", "+");
        }
        return absolutize(new URI(url));
    }

    /**
     * @return
     */
    protected String getBaseUrl() {
        String url = servletRequest.getRequestURL().toString();
        url = url.substring(0, url.indexOf(getPath()) + getPath().length());
        return url;
    }
    
    /**
     * @return METS resolver link for the DFG Viewer
     */
    public String getMetsResolverUrl(StructElement ele) {
        try {
            return ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + "/metsresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + "/metsresolver?id=" + 0;
    }
    
    protected abstract String getPath();
    
}
