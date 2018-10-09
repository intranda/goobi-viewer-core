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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * @author Florian Alpers
 *
 */
public class LoginResult {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Optional<User> user;
    private final boolean refused;
    private final AuthenticationProviderException exception;
    private Object redirectLock = new Object();

    /**
     * @param request
     * @param response
     * @param user
     * @param loginRefused  true if the login has been refused even if the user may exist and be valid. Typically true for wrong password
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, Optional<User> user, boolean loginRefused) {
        super();
        this.request = request;
        this.response = response;
        this.user = user;
        this.exception = null;
        this.refused = loginRefused;
    }

    /**
     * @param request
     * @param response
     * @param exception
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, AuthenticationProviderException exception) {
        super();
        this.request = request;
        this.response = response;
        this.user = Optional.empty();
        this.exception = exception;
        this.refused = true;
    }

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @return the user Optional containing the user if login was successful. Otherwise an empty optional
     * @throws AuthenticationProviderException if an internal error occured while logging in
     */
    public Optional<User> getUser() throws AuthenticationProviderException {
        if (exception != null) {
            throw exception;
        }
        return user;
    }

    public Future<Boolean> isRedirected(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (redirectLock) {
                try {
                    redirectLock.wait(timeout);
                } catch (InterruptedException e) {
                }
                return true;
            }
        });
    }

    public void setRedirected() {
        synchronized (redirectLock) {
            redirectLock.notifyAll();
        }
    }
    
    /**
     * @return the refused
     */
    public boolean isRefused() {
        return refused;
    }
}
