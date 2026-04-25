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
package io.goobi.viewer.filters;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationProvider;

class RewriteAbortConfigurationProviderTest {

    /**
     * @see RewriteAbortConfigurationProvider#priority()
     * @verifies return Integer MAX_VALUE
     */
    @Test
    void priority_shouldReturnIntegerMAX_VALUE() {
        Assertions.assertEquals(Integer.MAX_VALUE, new RewriteAbortConfigurationProvider().priority());
    }

    /**
     * @see RewriteAbortConfigurationProvider#getConfiguration(jakarta.servlet.ServletContext)
     * @verifies return non null configuration with at least one rule
     */
    @Test
    void getConfiguration_shouldReturnNonNullConfigurationWithAtLeastOneRule() {
        Configuration configuration = new RewriteAbortConfigurationProvider().getConfiguration(null);
        Assertions.assertNotNull(configuration);
        Assertions.assertFalse(configuration.getRules().isEmpty(), "expected at least one rule in the configuration");
    }

    /**
     * Verifies that the META-INF/services entry registers the provider so the rewrite ServletContextListener
     * picks it up at deployment time.
     */
    @Test
    void serviceLoader_shouldDiscoverProvider() {
        boolean found = false;
        for (ConfigurationProvider<?> provider : ServiceLoader.load(ConfigurationProvider.class)) {
            if (provider instanceof RewriteAbortConfigurationProvider) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "RewriteAbortConfigurationProvider not registered via META-INF/services");
    }
}
