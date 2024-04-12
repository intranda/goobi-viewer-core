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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.goobi.viewer.model.security.user.User;

/**
 * <p>
 * LoginResult class.
 * </p>
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
     * <p>
     * Constructor for LoginResult.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param user a {@link java.util.Optional} object.
     * @param loginRefused true if the login has been refused even if the user may exist and be valid. Typically true for wrong password
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, Optional<User> user, boolean loginRefused) {
        this(request, response, user, loginRefused, 0L);
    }

    /**
     * <p>
     * Constructor for LoginResult.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param user a {@link java.util.Optional} object.
     * @param loginRefused true if the login has been refused even if the user may exist and be valid. Typically true for wrong password
     * @param delay a configured delay time
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
     * <p>
     * Constructor for LoginResult.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param exception a {@link io.goobi.viewer.model.security.authentication.AuthenticationProviderException} object.
     */
    public LoginResult(HttpServletRequest request, HttpServletResponse response, AuthenticationProviderException exception) {
        this(request, response, exception, 0);
    }

    /**
     * <p>
     * Constructor for LoginResult.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param exception a {@link io.goobi.viewer.model.security.authentication.AuthenticationProviderException} object.
     * @param delay a configured delay time
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
     * <p>
     * Getter for the field <code>request</code>.
     * </p>
     *
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * <p>
     * Getter for the field <code>response</code>.
     * </p>
     *
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
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
     * <p>
     * isRedirected.
     * </p>
     *
     * @param timeout a long.
     * @return a {@link java.util.concurrent.Future} object.
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
     * <p>
     * setRedirected.
     * </p>
     */
    public void setRedirected() {
        synchronized (redirectLock) {
            redirected = true;
            redirectLock.notifyAll();
        }
    }

    /**
     * <p>
     * isRefused.
     * </p>
     *
     * @return the refused
     */
    public boolean isRefused() {
        return refused;
    }

    /**
     * 
     * @return the delay
     */
    public long getDelay() {
        return delay;
    }

}
