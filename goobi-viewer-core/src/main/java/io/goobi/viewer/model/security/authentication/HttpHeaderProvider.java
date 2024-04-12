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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.faces.validators.EmailValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;

public class HttpHeaderProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(HttpHeaderProvider.class);

    /** Constant <code>TYPE_OPENID="openId"</code> */
    public static final String TYPE_HTTP_HEADER = "httpHeader";

    public static final String PARAMETER_TYPE_HEADER = "header";

    private final String parameterType;
    private final String parameterName;

    private volatile LoginResult loginResult = null; //NOSONAR   LoginResult is immutable, so thread-savety is guaranteed

    /**
     * Lock to be opened once login is completed
     */
    private Object responseLock = new Object();

    /**
     * 
     * @param name
     * @param label
     * @param url
     * @param image
     * @param timeoutMillis
     * @param parameterType
     * @param parameterName
     */
    public HttpHeaderProvider(String name, String label, String url, String image, long timeoutMillis, String parameterType, String parameterName) {
        super(name, label, TYPE_HTTP_HEADER, url, image, timeoutMillis);
        this.parameterType = parameterType;
        this.parameterName = parameterName;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String ssoId, String password) throws AuthenticationProviderException {
        if (StringUtils.isNotEmpty(url)) {
            String fullUrl = url + (StringUtils.isNoneEmpty(redirectUrl) ? "?redirectUrl=" + redirectUrl : "");
            try {
                logger.trace("Redirecting to: {}", fullUrl);
                BeanUtils.getResponse().sendRedirect(fullUrl);
            } catch (IOException e) {
                throw new AuthenticationProviderException(e);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            synchronized (responseLock) {
                try {
                    responseLock.wait(getTimeoutMillis());
                    logger.trace("Returning result");
                    return this.loginResult;
                } catch (InterruptedException e) {
                    logger.trace("interrupted");
                    Thread.currentThread().interrupt();
                    return new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), new AuthenticationProviderException(e));
                }
            }
        });
    }

    /**
     * 
     * @param parameterValue
     * @return {@link User} if found; otherwise null
     */
    public User loadUser(String parameterValue) {
        try {
            List<User> users = DataManager.getInstance().getDao().getUsersByPropertyValue(parameterName, parameterValue);
            if (users.size() == 1) {
                logger.trace("User found: {}", users.get(0).getId());
                return users.get(0);
            } else if (users.size() > 1) {
                logger.error("{} found on multiple users: {}", parameterName, parameterValue);
            }
            logger.trace("No user found for {}={}", parameterName, parameterValue);
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * 
     * @param ssoId User identifier
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link java.util.concurrent.Future} object.
     */
    public Future<Boolean> completeLogin(String ssoId, HttpServletRequest request, HttpServletResponse response) {
        boolean success = true;
        try {
            User user = null;
            if (StringUtils.isEmpty(ssoId)) {
                logger.error("No ssoId value, cannot login.");
                success = false;
            } else {
                user = loadUser(ssoId);
                if (user == null) {
                    // Create new user
                    user = new User();
                    user.getUserProperties().put(parameterName, ssoId);
                    user.setActive(true);
                    if (EmailValidator.validateEmailAddress(ssoId)) {
                        user.setEmail(ssoId);
                        try {
                            DataManager.getInstance().getDao().addUser(user);
                            logger.info("New user created.");
                        } catch (DAOException e) {
                            logger.error(e.getMessage(), e);
                            success = false;
                        }
                    } else {
                        logger.error("No valid e-mail address found in request, cannot create user.");
                        success = false;
                    }
                } else {
                    success = !user.isSuspended() && user.isActive();
                }
            }
            loginResult = new LoginResult(request, response, Optional.ofNullable(user), !success);
        } finally {
            synchronized (responseLock) {
                responseLock.notifyAll();
                logger.trace("lock released");
            }
        }

        return this.loginResult.isRedirected(getTimeoutMillis());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        // noop
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getAddUserToGroups()
     */
    @Override
    public List<String> getAddUserToGroups() {
        return Collections.emptyList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#setAddUserToGroups(java.util.List)
     */
    @Override
    public void setAddUserToGroups(List<String> addUserToGroups) {
        // noop

    }

    /**
     * @return the parameterType
     */
    public String getParameterType() {
        return parameterType;
    }

    /**
     * @return the parameterName
     */
    public String getParameterName() {
        return parameterName;
    }

    public void setLoginResult(LoginResult loginResult) {
        logger.debug("setLoginResult");
        this.loginResult = loginResult;
    }
}
