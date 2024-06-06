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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link io.goobi.viewer.model.security.authentication.AuthResponseListener} which keeps all providers waiting for a response in a
 * {@link java.util.concurrent.ConcurrentHashMap}
 *
 * @param <T>
 * @author Florian Alpers
 */
public class AuthResponseListener<T extends IAuthenticationProvider> {

    private final ConcurrentHashMap<T, Boolean> authenticationProviders = new ConcurrentHashMap<>(5, 0.75f, 6);

    /**
     * Make an Auth provider issuing an authentication request eligible for receiving a response
     *
     * @param provider The provider issuing the request
     */

    public void register(T provider) {
        authenticationProviders.put(provider, Boolean.TRUE);
    }

    /**
     * Removing a provider from the list of issuers waiting for a response. To be called either after a request has been answered or if an answer is
     * no longer expected
     *
     * @param provider The provider to remove
     */

    public void unregister(T provider) {
        authenticationProviders.remove(provider);
    }

    /**
     * Gets a list of all registered providers
     *
     * @return The registered providers
     */
    public Set<T> getProviders() {
        return authenticationProviders.keySet();
    }

}
