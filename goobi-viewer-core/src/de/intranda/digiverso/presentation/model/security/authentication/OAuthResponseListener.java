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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link IOAuthResponseListener} which keeps all providers waiting for a response in a {@link ConcurrentHashMap}
 * 
 * @author Florian Alpers
 *
 */
public class OAuthResponseListener implements IOAuthResponseListener {

    private final ConcurrentHashMap<OpenIdProvider, Boolean> authenticationProviders = new ConcurrentHashMap<>(5, 0.75f, 6);

    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IOAuthResponseListener#register(de.intranda.digiverso.presentation.model.security.authentication.OpenIdProvider)
     */
    @Override
    public void register(OpenIdProvider provider) {
        authenticationProviders.put(provider, Boolean.TRUE);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IOAuthResponseListener#unregister(de.intranda.digiverso.presentation.model.security.authentication.OpenIdProvider)
     */
    @Override
    public void unregister(OpenIdProvider provider) {
        authenticationProviders.remove(provider);        
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IOAuthResponseListener#getProviders()
     */
    @Override
    public Set<OpenIdProvider> getProviders() {
        return authenticationProviders.keySet();
    }



}
