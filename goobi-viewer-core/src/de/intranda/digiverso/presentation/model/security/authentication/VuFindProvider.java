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

/**
 * @author Florian Alpers
 *
 */
public class VuFindProvider extends HttpAuthenticationProvider {

    /**
     * @param name
     * @param url
     * @param image
     */
    public VuFindProvider(String name, String url, String image) {
        super(name, url, image);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login()
     */
    @Override
    public boolean login() throws AuthenticationProviderException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isLoggedIn()
     */
    @Override
    public boolean isLoggedIn() throws AuthenticationProviderException {
        // TODO Auto-generated method stub
        return false;
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

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getUserName()
     */
    @Override
    public Optional<String> getUserName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getUserEmail()
     */
    @Override
    public Optional<String> getUserEmail() {
        // TODO Auto-generated method stub
        return null;
    }

}
