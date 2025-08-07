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
package io.goobi.viewer.api.rest.v2.auth;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.intranda.api.iiif.auth.v2.AuthAccessToken2;
import jakarta.servlet.http.HttpSession;

/**
 * Application scoped cache of issued bearer tokens for IIIF Auth Flow 2.0. Necessary because the client will not necessarily reuse the same session.
 */
public class BearerTokenManager {

    private final Map<String, AuthAccessToken2> tokenMap = new ConcurrentHashMap<>();
    private final Map<String, HttpSession> tokenSessionMap = new ConcurrentHashMap<>();

    /**
     * 
     * @param token
     * @param session
     * @should add token correctly
     */
    public void addToken(AuthAccessToken2 token, HttpSession session) {
        tokenMap.put(token.getAccessToken(), token);
        tokenSessionMap.put(token.getAccessToken(), session);
    }

    /**
     * 
     * @return Number of purged tokens
     * @should purge expired tokens only
     */
    public int purgeExpiredTokens() {
        Set<String> toPurge = new HashSet<>();
        for (Entry<String, AuthAccessToken2> entry : tokenMap.entrySet()) {
            if (entry.getValue().isExpired()) {
                toPurge.add(entry.getKey());
            }

        }
        for (String token : toPurge) {
            tokenMap.remove(token);
            tokenSessionMap.remove(token);
        }

        return toPurge.size();
    }

    /**
     * @return the tokenMap
     */
    public Map<String, AuthAccessToken2> getTokenMap() {
        return tokenMap;
    }

    /**
     * @return the tokenSessionMap
     */
    public Map<String, HttpSession> getTokenSessionMap() {
        return tokenSessionMap;
    }
}
