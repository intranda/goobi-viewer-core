/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CMSArchiveLinkTest {

    private static final String SERVER = "katzoom.goobi.cloud";

    private static CMSArchiveLink link(String url) {
        CMSArchiveLink ret = new CMSArchiveLink();
        ret.setUrl(url);
        return ret;
    }

    @Test
    void normalizeUrl_shouldTrimAndPrefixBareWwwAddressesWithHttps() {
        assertEquals("https://www.gei.de/hilfe", CMSArchiveLink.normalizeUrl("  www.gei.de/hilfe "));
        assertEquals("https://gei.de", CMSArchiveLink.normalizeUrl("https://gei.de"));
        assertEquals("/viewer/cms/12/", CMSArchiveLink.normalizeUrl("/viewer/cms/12/"));
        assertEquals("", CMSArchiveLink.normalizeUrl(null));
    }

    @Test
    void getHref_shouldNormalizeLinksStoredBeforeNormalizationExisted() {
        assertEquals("https://www.gei.de", link("www.gei.de").getHref());
    }

    @Test
    void getIconName_shouldFollowTheLinkType() {
        assertEquals("file-type-pdf", link("https://gei.de/regeln.PDF?download=1#seite=2").getIconName());
        assertEquals("mail", link("mailto:bibliothek@gei.de").getIconName());
        assertEquals("link", link("https://gei.de/hilfe").getIconName());
        assertEquals("link", link("https://gei.de/pdf-hilfe").getIconName());
    }

    @Test
    void isExternal_shouldCompareTheHostWithTheCurrentServer() {
        assertTrue(link("https://www.gei.de/hilfe").isExternal(SERVER));
        assertFalse(link("https://katzoom.goobi.cloud/viewer/cms/12/").isExternal(SERVER));
        assertFalse(link("https://www.katzoom.goobi.cloud/viewer/").isExternal(SERVER));
        assertFalse(link("/viewer/cms/12/").isExternal(SERVER));
        assertFalse(link("mailto:bibliothek@gei.de").isExternal(SERVER));
    }

    @Test
    void getHost_shouldDropTheWwwPrefixAndIgnoreUnparsableAddresses() {
        assertEquals("gei.de", link("https://www.gei.de/hilfe").getHost());
        assertEquals("gei.de", link("www.gei.de").getHost());
        assertNull(link("/viewer/cms/12/").getHost());
        assertNull(link("https://exa mple.org").getHost());
    }

    @Test
    void getFallbackLabel_shouldPreferMailAddressThenHostThenAddress() {
        assertEquals("bibliothek@gei.de", link("mailto:bibliothek@gei.de?subject=Frage").getFallbackLabel());
        assertEquals("gei.de", link("www.gei.de").getFallbackLabel());
        assertEquals("/viewer/cms/12/", link("/viewer/cms/12/").getFallbackLabel());
    }
}
