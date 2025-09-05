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
package io.goobi.viewer.model.iiif.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.auth.v2.AuthAccessToken2;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import jakarta.servlet.http.HttpSession;

class BearerTokenManagerTest extends AbstractSolrEnabledTest {

    /**
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @see ActivityCollectionBuider#getDocs(Long,Long)
     * @verifies add token correctly
     */
    @Test
    void addToken_shouldAddTokenCorrectly() {
        BearerTokenManager btm = new BearerTokenManager();
        AuthAccessToken2 token = new AuthAccessToken2("1", 300);
        HttpSession session = Mockito.mock(HttpSession.class);
        btm.addToken(token, session);
        Assertions.assertEquals(token, btm.getTokenMap().get(token.getAccessToken()));
        Assertions.assertEquals(session, btm.getTokenSessionMap().get(token.getAccessToken()));
    }

    /**
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @see ActivityCollectionBuider#getDocs(Long,Long)
     * @verifies purge expired tokens only
     */
    @Test
    void purgeExpiredTokens_shouldPurgeExpiredTokensOnly() {
        BearerTokenManager btm = new BearerTokenManager();
        AuthAccessToken2 token1 = new AuthAccessToken2("1", 300);
        AuthAccessToken2 token2 = new AuthAccessToken2("2", 0);
        HttpSession session = Mockito.mock(HttpSession.class);
        btm.addToken(token1, session);
        btm.addToken(token2, session);
        Assertions.assertFalse(token1.isExpired());
        Assertions.assertTrue(token2.isExpired());

        btm.purgeExpiredTokens();

        Assertions.assertEquals(token1, btm.getTokenMap().get(token1.getAccessToken()));
        Assertions.assertEquals(session, btm.getTokenSessionMap().get(token1.getAccessToken()));

        Assertions.assertNull(btm.getTokenMap().get(token2.getAccessToken()));
        Assertions.assertNull(btm.getTokenSessionMap().get(token2.getAccessToken()));
    }
}