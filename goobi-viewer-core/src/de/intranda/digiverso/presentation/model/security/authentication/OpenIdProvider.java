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

import java.util.Optional;

import de.intranda.digiverso.presentation.model.security.user.User;

public class OpenIdProvider extends HttpAuthenticationProvider {
    private String name;
    private String url;
    private String image;
    private boolean useTextField;
    /** OAuth client ID. */
    private String clientId;
    /** OAuth client secret. */
    private String clientSecret;

    public OpenIdProvider(String name, String url, String image, boolean useTextField, String clientId, String clientSecret) {
        super(name, url, image);
        this.useTextField = useTextField;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * @return the name
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


    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public Optional<User> login(String loginName, String password) throws AuthenticationProviderException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isActive()
     */
    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isRefused()
     */
    @Override
    public boolean isRefused() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getUserGroup()
     */
    @Override
    public Optional<String> getUserGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        // TODO Auto-generated method stub
        return false;
    }

}
