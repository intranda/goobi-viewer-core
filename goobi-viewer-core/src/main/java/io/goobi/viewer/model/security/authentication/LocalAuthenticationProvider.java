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
package io.goobi.viewer.model.security.authentication;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;

/**
 * An authentication provider using the local login provided by the viewer database
 *
 * @author Florian Alpers
 */
public class LocalAuthenticationProvider implements IAuthenticationProvider {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(LocalAuthenticationProvider.class);

    /** Constant <code>TYPE_LOCAL="local"</code> */
    public static final String TYPE_LOCAL = "local";
    private final String name;
    protected List<String> addUserToGroups;

    private BCrypt bcrypt = new BCrypt();

    /**
     * <p>
     * Constructor for LocalAuthenticationProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public LocalAuthenticationProvider(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login()
     */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<LoginResult> login(String email, String password) throws AuthenticationProviderException {
        HttpServletRequest request = BeanUtils.getRequest();
        HttpServletResponse response = BeanUtils.getResponse();

        String ipAddress = NetTools.getIpAddress(request);
        long delay = DataManager.getInstance().getSecurityManager().getDelayForIpAddress(ipAddress);
        if (delay > 0) {
            // refuse with delay
            return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.empty(), true, delay));
        }

        if (StringUtils.isNotEmpty(email)) {
            delay = DataManager.getInstance().getSecurityManager().getDelayForUserName(email);
            if (delay > 0) {
                // refuse with delay
                return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.empty(), true, delay));
            }
            try {
                User user = DataManager.getInstance().getDao().getUserByEmail(email);
                boolean refused = true;
                if (user != null && StringUtils.isNotBlank(password) && user.getPasswordHash() != null
                        && bcrypt.checkpw(password, user.getPasswordHash())) {
                    refused = false;
                    // Reset failed failed login attempt penalty counters
                    DataManager.getInstance().getSecurityManager().resetFailedLoginAttemptForUserName(email);
                    DataManager.getInstance().getSecurityManager().resetFailedLoginAttemptForIpAddress(ipAddress);
                } else {
                    // Register failed attempt for user name and IP address
                    DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForUserName(email);
                    DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForIpAddress(ipAddress);
                }
                return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.ofNullable(user), refused));
            } catch (DAOException e) {
                throw new AuthenticationProviderException(e);
            }
        }
        return CompletableFuture.completedFuture(new LoginResult(request, response, Optional.empty(), true));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    /** {@inheritDoc} */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsPasswordChange() {
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getProviderName()
     */
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getType()
     */
    /** {@inheritDoc} */
    @Override
    public String getType() {
        return TYPE_LOCAL;
    }

    /**
     * Set custom bcrypt for testing
     *
     * @param bcrypt the bcrypt to set
     */
    protected void setBcrypt(BCrypt bcrypt) {
        this.bcrypt = bcrypt;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsNicknameChange() {
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getAddUserToGroups()
     */
    /** {@inheritDoc} */
    @Override
    public List<String> getAddUserToGroups() {
        return addUserToGroups;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#setAddUserToGroups(java.util.List)
     */
    /** {@inheritDoc} */
    @Override
    public void setAddUserToGroups(List<String> addUserToGroups) {
        this.addUserToGroups = addUserToGroups;
    }

    @Override
    public String getRedirectUrl() {
        return null;
    }

    @Override
    public void setRedirectUrl(String redirectUrl) {
        //        
    }
}
