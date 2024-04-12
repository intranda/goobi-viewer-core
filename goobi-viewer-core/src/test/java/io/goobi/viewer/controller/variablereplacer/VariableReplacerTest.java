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
package io.goobi.viewer.controller.variablereplacer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;

class VariableReplacerTest extends AbstractTest {

    private static final String PULL_THEME = "{config-folder-path}/script_theme-pull.sh {theme-path}";
    private static final String CREATE_DEVELOPER_PACKAGE = "{config-folder-path}/script_create_package.sh viewer {base-path} /var/www {solr-url}";

    @Test
    void test_replaceConfig() {
        Configuration config = new Configuration("config_viewer_developer.xml");
        VariableReplacer vr = new VariableReplacer(config);

        String pullTheme = vr.replace(PULL_THEME);
        assertEquals(
                "/opt/digiverso/viewer/config/script_theme-pull.sh "
                        + "/opt/digiverso/goobi-viewer-theme-test/goobi-viewer-theme-test/WebContent/resources/themes/",
                pullTheme);

        String createDeveloperPackage = vr.replace(CREATE_DEVELOPER_PACKAGE);
        assertEquals(
                "/opt/digiverso/viewer/config/script_create_package.sh viewer /opt/digiverso/viewer /var/www http://localhost:8983/solr/collection2",
                createDeveloperPackage);

    }
}
