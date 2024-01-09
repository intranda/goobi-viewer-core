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
package io.goobi.viewer.model.rss;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndFeed;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;

public class RSSFeedTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see RSSFeed#createRss(String,String,List,String,int)
     * @verifies produce feed correctly
     */
    @Test
    void createRss_shouldProduceFeedCorrectly() throws Exception {
        SyndFeed feed = RSSFeed.createRss("https://example.com", "PI:*", null, "en", 10, null, true);
        Assertions.assertNotNull(feed);
        Assertions.assertEquals(10, feed.getEntries().size());
        // TODO in-detail assertions
    }

    /**
     * @see RSSFeed#createRssFeed(String,String,List,int,String)
     * @verifies produce feed correctly
     */
    @Test
    void createRssFeed_shouldProduceFeedCorrectly() throws Exception {
        Channel channel = RSSFeed.createRssFeed("https://example.com", "PI:*", null, 10, "en", null, true);
        Assertions.assertNotNull(channel);
        Assertions.assertEquals(10, channel.getItems().size());
        // TODO in-detail assertions
    }
}
