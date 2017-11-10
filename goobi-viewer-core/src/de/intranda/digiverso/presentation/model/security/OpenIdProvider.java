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
package de.intranda.digiverso.presentation.model.security;

public class OpenIdProvider {
    private String name;
    private String url;
    private String image;
    private boolean useTextField;
    /** OAuth client ID. */
    private String clientId;
    /** OAuth client secret. */
    private String clientSecret;

    public OpenIdProvider(String name, String url, String image, boolean useTextField, String clientId, String clientSecret) {
        super();
        this.name = name;
        this.url = url;
        this.image = image;
        this.useTextField = useTextField;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * @return the name
     */
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

    /**
     * @return the useTextField
     */
    public boolean isUseTextField() {
        return useTextField;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }
}
