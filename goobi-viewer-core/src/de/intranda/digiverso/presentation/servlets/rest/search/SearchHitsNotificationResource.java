/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets.rest.search;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchFacets;
import de.intranda.digiverso.presentation.model.search.SearchHit;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path(SearchHitsNotificationResource.RESOURCE_PATH)
@ViewerRestServiceBinding
public class SearchHitsNotificationResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchHitsNotificationResource.class);

    public static final String RESOURCE_PATH = "/search";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Path("/sendnotifications/")
    @Produces({ MediaType.TEXT_HTML })
    public String sendNewHitsNotifications() throws DAOException, PresentationException, IndexUnreachableException {
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
                SearchFacets facets = new SearchFacets();
                facets.setCurrentFacetString(search.getFacetString());
                facets.setCurrentHierarchicalFacetString(search.getHierarchicalFacetString());
                String oldSortString = search.getSortString();
                search.setSortString('!' + SolrConstants.DATECREATED);
                search.execute(facets, null, 100, 0, null, null);
                // TODO what if there're >100 new hits?
                if (search.getHitsCount() > search.getLastHitsCount()) {
                    List<SearchHit> newHits = search.getHits().subList(0, (int) (search.getHitsCount() - search.getLastHitsCount()));
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
                    subject = subject.replace("{0}", search.getName());
                    String body = ViewerResourceBundle.getTranslation("user_mySearches_notificationMailBody", null);
                    body = body.replace("{0}", search.getName());
                    body = body.replace("{1}", sb.toString());
                    body = body.replace("{2}", "Goobi viewer");

                    String address = search.getOwner().getEmail();
                    if (StringUtils.isNotEmpty(address)) {
                        try {
                            Helper.postMail(Collections.singletonList(address), subject, body);
                        } catch (UnsupportedEncodingException | MessagingException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    //                    sbDebug.append(body)
                    //                            .append("<br /><br />");

                    // Update last count in DB
                    search.setLastHitsCount(search.getHitsCount());
                    search.setSortString(oldSortString);
                    DataManager.getInstance().getDao().updateSearch(search);
                }
            }
        }

        return sbDebug.toString();
    }
}
