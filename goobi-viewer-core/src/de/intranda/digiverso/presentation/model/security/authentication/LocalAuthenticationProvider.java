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
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import de.intranda.digiverso.presentation.controller.BCrypt;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * An authentication provider using the local login provided by the viewer database
 * 
 * @author Florian Alpers
 *
 */
public class LocalAuthenticationProvider implements IAuthenticationProvider {

    boolean userActive = false;
    boolean userSuspended = false;
    boolean userBlocked = false;
    private final String name;

    public LocalAuthenticationProvider(String name) {
        this.name = name;
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login()
     */
    @Override
    public CompletableFuture<LoginResult> login(String email, String password) throws AuthenticationProviderException {
        HttpServletRequest request = BeanUtils.getRequest();
        HttpServletResponse response = BeanUtils.getResponse();
        if (StringUtils.isNotEmpty(email)) {
            try {
                User user = DataManager.getInstance().getDao().getUserByEmail(email);
                if (user == null || !user.isActive()) {
                    userActive = false;
                } else if (user.isSuspended()) {
                    userActive = true;
                    userSuspended = true;
                } else if (StringUtils.isBlank(password) || (user.getPasswordHash() == null || !BCrypt.checkpw(password, user.getPasswordHash()))) {
                    userActive = true;
                    userSuspended = false;
                    userBlocked = true;
                } else {
                    return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.ofNullable(user)));
                }
            } catch (DAOException e) {
                throw new AuthenticationProviderException(e);
            }
        }
        return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.empty()));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isActive()
     */
    @Override
    public boolean isActive() {
        return userActive;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        return userSuspended;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isRefused()
     */
    @Override
    public boolean isRefused() {
        return userBlocked;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getUserGroup()
     */
    @Override
    public Optional<String> getUserGroup() {
        return Optional.empty();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return true;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getProviderName()
     */
    @Override
    public String getName() {
        return name;
    }

}
