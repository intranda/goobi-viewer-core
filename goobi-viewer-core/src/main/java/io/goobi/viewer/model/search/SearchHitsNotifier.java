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
package io.goobi.viewer.model.search;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.mail.MessagingException;

/**
 * <p>
 * SearchHitsNotificationResource class.
 * </p>
 */
public class SearchHitsNotifier {

    private static final Logger logger = LogManager.getLogger(SearchHitsNotifier.class);

    public String sendNewHitsNotifications() throws DAOException, PresentationException, IndexUnreachableException, ViewerConfigurationException {
        logger.trace("sendNewHitsNotifications");
        Map<String, String> filters = new HashMap<>();
        filters.put("newHitsNotification", "1");
        long searchCount = DataManager.getInstance().getDao().getSearchCount(null, filters);
        logger.info("Found {} saved searches with notifications enabled.", searchCount);
        int pageSize = 100;

        StringBuilder sbDebug = new StringBuilder();
        for (int i = 0; i < searchCount; i += pageSize) {
            logger.debug("Getting searches {}-{}", i, i + pageSize);
            List<Search> searches = DataManager.getInstance().getDao().getSearches(null, i, pageSize, null, false, filters);
            for (Search search : searches) {
                // TODO access condition filters for each user
                List<SearchHit> newHits = getNewHits(search);
                if (!newHits.isEmpty()) {
                    String email = search.getOwner().getEmail();
                    sendEmailNotification(newHits, search.getName(), email);
                    DataManager.getInstance().getDao().updateSearch(search);
                }
            }
        }

        return sbDebug.toString();
    }

    /**
     * @param newHits
     * @param searchName
     * @param address
     */
    private static void sendEmailNotification(List<SearchHit> newHits, String searchName, String address) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        int count = 0;
        for (SearchHit newHit : newHits) {
            logger.trace("New hit: {}", newHit.getBrowseElement().getPi());
            count++;
            sb.append(newHit.generateNotificationFragment(count));
        }
        sb.append("</table>");

        // TODO Send notification
        String subject = ViewerResourceBundle.getTranslation("user_mySearches_notificationMailSubject", null);
        subject = subject.replace("{0}", searchName);
        String body = ViewerResourceBundle.getTranslation("user_mySearches_notificationMailBody", null);
        body = body.replace("{0}", searchName);
        body = body.replace("{1}", sb.toString());
        body = body.replace("{2}", "Goobi viewer");

        if (StringUtils.isNotEmpty(address)) {
            try {
                NetTools.postMail(Collections.singletonList(address), null, null, subject, body);
            } catch (UnsupportedEncodingException | MessagingException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

    /**
     * Executes the given search. If after {@link Search#execute(SearchFacets, Map, int, java.util.Locale, boolean, SearchAggregationType) execution}
     * the {@link Search#getHitsCount()} is larger than {@link Search#getLastHitsCount()} the newest (hitsCount - lastHitsCount) hits are returned and
     * the lastHitsCount of the search is updated
     *
     *
     * @param search
     * @return A list of new hits (based on {@link Search#getLastHitsCount()}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public List<SearchHit> getNewHits(Search search)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        //clone the search so any alterations are discarded later
        Search tempSearch = new Search(search);
        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString(tempSearch.getFacetString());
        tempSearch.execute(facets, null, 0, null);
        // TODO what if there're >100 new hits?
        if (tempSearch.getHitsCount() > tempSearch.getLastHitsCount()) {
            int newHitsCount = (int) (tempSearch.getHitsCount() - tempSearch.getLastHitsCount());
            newHitsCount = Math.min(100, newHitsCount); //don't query more than 100 hits
            //sort so newest hits come first
            tempSearch.setSortString('!' + SolrConstants.DATECREATED);
            //after last execution, page is 0, set back to 1 to actually get some results
            tempSearch.setPage(1);
            //reset hits count to 0 to actually perform search
            tempSearch.setHitsCount(0);
            tempSearch.execute(facets, null, newHitsCount, null);
            List<SearchHit> newHits = tempSearch.getHits();

            // Update last count
            search.setLastHitsCount(tempSearch.getHitsCount());
            return newHits;
        }
        return new ArrayList<>();
    }
}
