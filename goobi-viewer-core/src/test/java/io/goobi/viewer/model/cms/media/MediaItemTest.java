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
package io.goobi.viewer.model.cms.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * Unit tests for {@link MediaItem}.
 */
class MediaItemTest {

    /**
     * @see MediaItem#sanitizeDescriptionPlainText(CMSMediaItem)
     * @verifies sanitize description values for every language
     */
    @Test
    void sanitizeDescriptionPlainText_shouldSanitizeDescriptionValuesForEveryLanguage() {
        CMSMediaItem item = new CMSMediaItem();
        item.addMetadata(buildMetadata("en", "Safe text"));
        item.addMetadata(buildMetadata("de", "<img src=x onerror=alert(1)>injected"));

        IMetadataValue result = MediaItem.sanitizeDescriptionPlainText(item);

        assertNotNull(result);
        assertEquals("Safe text", result.getValue(Locale.ENGLISH).orElse(null));
        String german = result.getValue(Locale.GERMAN).orElse("");
        assertFalse(german.contains("<"), "sanitized value must not contain HTML tags: " + german);
        assertFalse(german.contains("onerror"), "sanitized value must not retain event-handler attributes: " + german);
        assertTrue(german.contains("injected"), "sanitized value must retain plain text: " + german);
    }

    /**
     * @see MediaItem#sanitizeDescriptionPlainText(CMSMediaItem)
     * @verifies drop language entries with blank description
     */
    @Test
    void sanitizeDescriptionPlainText_shouldDropLanguageEntriesWithBlankDescription() {
        CMSMediaItem item = new CMSMediaItem();
        item.addMetadata(buildMetadata("en", ""));
        item.addMetadata(buildMetadata("de", "real description"));

        IMetadataValue result = MediaItem.sanitizeDescriptionPlainText(item);

        assertFalse(result.getValue(Locale.ENGLISH).filter(s -> !s.isEmpty()).isPresent(),
                "blank descriptions must be filtered out");
        assertEquals("real description", result.getValue(Locale.GERMAN).orElse(null));
    }

    /**
     * @see MediaItem#sanitizeDescriptionPlainText(CMSMediaItem)
     * @verifies return empty metadata value when source is null
     */
    @Test
    void sanitizeDescriptionPlainText_shouldReturnEmptyMetadataValueWhenSourceIsNull() {
        IMetadataValue result = MediaItem.sanitizeDescriptionPlainText(null);

        assertNotNull(result);
        assertFalse(result.getValue(Locale.ENGLISH).filter(s -> !s.isEmpty()).isPresent());
    }

    private static CMSMediaItemMetadata buildMetadata(String language, String description) {
        CMSMediaItemMetadata md = new CMSMediaItemMetadata();
        md.setLanguage(language);
        md.setDescription(description);
        return md;
    }
}
