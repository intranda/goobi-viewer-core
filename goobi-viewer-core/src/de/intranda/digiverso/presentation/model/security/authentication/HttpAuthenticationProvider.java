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
package de.intranda.digiverso.presentation.model.security.authentication;

import java.net.URI;
import java.net.URISyntaxException;

import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class HttpAuthenticationProvider implements IAuthenticationProvider {

    protected final String name;
    protected final String type;
    protected final String url;
    protected final String image;
    protected final long timeoutMillis;
    /**
     * @param name
     * @param url
     * @param image
     */
    public HttpAuthenticationProvider(String name, String type, String url, String image, long timeoutMillis) {
        super();
        this.name = name;
        this.url = url;
        this.image = image;
        this.type = type;
        this.timeoutMillis = timeoutMillis;
    }
    
    /**
     * @return the timeoutMillis
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getName()
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * @return the image
     */
    public String getImage() {
        return image;
    }
    
    public String getImageUrl() {
        try {
            URI uri = new URI(image);
            if(uri.isAbsolute()) {
                return uri.toString();
            }
        } catch(NullPointerException | URISyntaxException e) {
            //construct viewer path uri
        }
        StringBuilder url = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        url.append("/resources/themes/").append(BeanUtils.getNavigationHelper().getTheme()).append("/images/openid/");
        url.append(image);
        return url.toString();
    }
    
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
    
}
