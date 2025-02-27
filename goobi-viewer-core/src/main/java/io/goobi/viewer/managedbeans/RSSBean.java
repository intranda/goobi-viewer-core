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
package io.goobi.viewer.managedbeans;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rometools.rome.feed.synd.SyndEntry;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("rssBean")
@RequestScoped
public class RSSBean {

    private static final Logger logger = LogManager.getLogger(RSSBean.class);

    /**
     * 
     * @param maxHits
     * @param query
     * @param sortField
     * @param sortDescending
     * @return List<SyndEntry>
     */
    public List<SyndEntry> getRssFeed(Integer maxHits, String query, String sortField, Boolean sortDescending) {
        try {
            return RSSFeed.createRssFeed(null, maxHits, null, query, null, BeanUtils.getRequest(), sortField,
                    sortDescending == null || sortDescending).getEntries();

        } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException | DAOException e) {
            logger.error(e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 
     * @param maxHits
     * @param query
     * @param sortField
     * @param sortDescending
     * @return {@link Channel}
     */
    public Channel getRssFeedChannel(Integer maxHits, String query, String sortField, Boolean sortDescending) {
        try {
            return RSSFeed.createRssResponse(null, maxHits, null, query, null, BeanUtils.getRequest(), sortField,
                    sortDescending == null || sortDescending);

        } catch (ContentLibException e) {
            logger.error(e.getMessage());
        }

        return null;
    }
}
