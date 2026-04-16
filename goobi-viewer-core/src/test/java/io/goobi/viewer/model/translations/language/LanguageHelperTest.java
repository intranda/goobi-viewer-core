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
package io.goobi.viewer.model.translations.language;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

/**
 * @author Florian Alpers
 *
 */
class LanguageHelperTest extends AbstractTest {

    /**
     * @verifies resolve both ISO 639 1 and ISO 639 2 codes
     * @see LanguageHelper#getLanguage(final String)
     */
    @Test
    void getLanguage_shouldResolveBothIso639_1AndIso639_2Codes() {
        LanguageHelper helper = new LanguageHelper("src/test/resources/languages.xml");
        try {
            Assertions.assertNotNull(helper.getLanguage("fra"));
            Assertions.assertNotNull(helper.getLanguage("fre"));
            Assertions.assertNotNull(helper.getLanguage("fr"));
        } finally {
            helper.shutdown();
        }
    }

    /**
     * @verifies stop reloading thread
     */
    @Test
    void shutdown_shouldStopReloadingThread() throws InterruptedException {
        Set<Thread> threadsBefore = Thread.getAllStackTraces().keySet();
        LanguageHelper helper = new LanguageHelper("src/test/resources/languages.xml");

        Thread triggerThread = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> !threadsBefore.contains(t) && t.getName().startsWith("ReloadingTrigger") && t.isAlive())
                .findFirst()
                .orElse(null);

        helper.shutdown();

        if (triggerThread != null) {
            triggerThread.join(2000);
            Assertions.assertFalse(triggerThread.isAlive(), "ReloadingTrigger thread should be stopped after shutdown");
        }
    }

}
