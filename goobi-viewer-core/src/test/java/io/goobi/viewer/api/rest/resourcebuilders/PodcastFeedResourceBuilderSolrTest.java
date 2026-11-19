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
package io.goobi.viewer.api.rest.resourcebuilders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;

class PodcastFeedResourceBuilderSolrTest extends AbstractSolrEnabledTest {

    private static PodcastFeedResourceBuilder builder() {
        return new PodcastFeedResourceBuilder(new ApiUrls("http://localhost:8080/viewer/api/v1"), null);
    }

    /**
     * @see PodcastFeedResourceBuilder#build(String, String)
     * @verifies throw ContentNotFoundException if show missing
     */
    @Test
    void build_shouldThrowContentNotFoundExceptionIfShowMissing() {
        Assertions.assertThrows(ContentNotFoundException.class, () -> builder().build("NONEXISTENT_PODCAST_99999", "http://x/feed.rss"));
    }

    /**
     * @see PodcastFeedResourceBuilder#build(String, String)
     * @verifies throw ContentNotFoundException if record is no podcast group
     */
    @Test
    void build_shouldThrowContentNotFoundExceptionIfRecordIsNoPodcastGroup() {
        Assertions.assertThrows(ContentNotFoundException.class, () -> builder().build(PI_KLEIUNIV, "http://x/feed.rss"));
    }
}
