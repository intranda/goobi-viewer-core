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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthorizationFlowResourceTest {

    @Test
    void isValidOrigin_shouldAcceptWellFormedOrigins() {
        assertTrue(AuthorizationFlowResource.isValidOrigin("https://example.com"));
        assertTrue(AuthorizationFlowResource.isValidOrigin("http://sub.domain.example.com:8080"));
        assertTrue(AuthorizationFlowResource.isValidOrigin("https://localhost"));
        assertTrue(AuthorizationFlowResource.isValidOrigin("https://[2001:db8::1]:443"));
    }

    @Test
    void isValidOrigin_shouldRejectMalformedOrigins() {
        assertFalse(AuthorizationFlowResource.isValidOrigin(null));
        assertFalse(AuthorizationFlowResource.isValidOrigin("https://example.com/path"));
        assertFalse(AuthorizationFlowResource.isValidOrigin("javascript:alert(1)"));
        assertFalse(AuthorizationFlowResource.isValidOrigin("https://exa mple.com"));
        assertFalse(AuthorizationFlowResource.isValidOrigin("ftp://example.com"));
    }

    @Test
    void isValidOrigin_shouldHandleOverlongInputWithoutStackOverflow() {
        // Regression guard for the bounded-repetition fix (java:S5998): a pathologically long
        // multi-label host must be rejected quickly without throwing StackOverflowError.
        String origin = "https://a" + ".a".repeat(5000);
        assertFalse(AuthorizationFlowResource.isValidOrigin(origin));
    }
}
