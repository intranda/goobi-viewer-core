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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.goobi.viewer.model.security.user.User;

/**
 * Encapsulates the outcome of an authentication attempt, including the authenticated user and any error message.
 *
 * @author Florian Alpers
 */
public class LoginResult {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Optional<User> user;
    private final boolean refused;
    private final AuthenticationProviderException exception;
    private final long delay;
    private final Object redirectLock = new Object();
    private volatile boolean redirected = false;

    /**
     * Creates a new LoginResult instance.
     *
     * @param request HTTP request associated with the login attempt
     * @param response HTTP response associated with the login attempt
     * @param user optional containing the authenticated user, or empty if login failed
     * @param loginRefused true if the login has been refused even if the user may exist and be valid. Typically true for wrong password
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, Optional<User> user, boolean loginRefused) {
        this(request, response, user, loginRefused, 0L);
    }

    /**
     * Creates a new LoginResult instance.
     *
     * @param request HTTP request associated with the login attempt
     * @param response HTTP response associated with the login attempt
     * @param user optional containing the authenticated user, or empty if login failed
     * @param loginRefused true if the login has been refused even if the user may exist and be valid. Typically true for wrong password
     * @param delay configured delay in milliseconds before completing the result
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, Optional<User> user, boolean loginRefused, long delay) {
        super();
        this.request = request;
        this.response = response;
        this.user = user;
        this.exception = null;
        this.refused = loginRefused;
        this.delay = delay;
    }

    /**
     * Creates a new LoginResult instance.
     *
     * @param request HTTP request associated with the login attempt
     * @param response HTTP response associated with the login attempt
     * @param exception exception that caused the login failure
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, AuthenticationProviderException exception) {
        this(request, response, exception, 0);
    }

    /**
     * Creates a new LoginResult instance.
     *
     * @param request HTTP request associated with the login attempt
     * @param response HTTP response associated with the login attempt
     * @param exception exception that caused the login failure
     * @param delay configured delay in milliseconds before completing the result
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, AuthenticationProviderException exception, long delay) {
        super();
        this.request = request;
        this.response = response;
        this.user = Optional.empty();
        this.exception = exception;
        this.refused = true;
        this.delay = delay;
    }

    /**
     * Getter for the field <code>request</code>.
     *
     * @return the HTTP request associated with this login attempt
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Getter for the field <code>response</code>.
     *
     * @return the HTTP response associated with this login attempt
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Getter for the field <code>user</code>.
     *
     * @return the user Optional containing the user if login was successful. Otherwise an empty optional
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public Optional<User> getUser() throws AuthenticationProviderException {
        if (exception != null) {
            throw exception;
        }
        return user;
    }

    /**
     * isRedirected.
     *
     * @param timeout maximum wait time in milliseconds for the redirect signal
     * @return a Future resolving to true once a redirect has been signaled
     */
    public Future<Boolean> isRedirected(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (redirectLock) {
                try {
                    while (!redirected) {
                        redirectLock.wait(timeout);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        });
    }

    /**
     * setRedirected.
     */
    public void setRedirected() {
        synchronized (redirectLock) {
            redirected = true;
            redirectLock.notifyAll();
        }
    }

    /**
     * isRefused.
     *
     * @return true if the login attempt was explicitly refused, false otherwise
     */
    public boolean isRefused() {
        return refused;
    }

    
    public long getDelay() {
        return delay;
    }

}
