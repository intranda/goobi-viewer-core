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
package io.goobi.viewer.servlets.rest.rss;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.Description;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.rss.RssItem;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * REST resource providing a rss feed as json response
 *
 * @author Florian Alpers
 */
@Path("/rss")
@ViewerRestServiceBinding
public class RssResource {

    private static final Logger logger = LoggerFactory.getLogger(RssResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects All metadata is provided in the passed language
     *
     * @param numHits a int.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{language}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("numhits") int numHits, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        String query = null;
        String partnerId = null;

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }
    
    
    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects which match the given filterQuery
     * All metadata is provided in the passed language
     *
     * @param numHits a int.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{language}/{numhits}/filterBy/{filterQuery}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageAndFilterJson(@PathParam("numhits") int numHits, @PathParam("language") String language, @PathParam("filterQuery") String filterQuery)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        String query = null;
        String partnerId = null;
        if(filterQuery != null) {
            filterQuery = filterQuery.replaceAll("^-$", "");
            filterQuery = BeanUtils.unescapeCriticalUrlChracters(filterQuery);
        }
        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), Collections.singletonList(filterQuery), numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId All metadata is provided in the passed language
     *
     * @param numHits a int.
     * @param language a {@link java.lang.String} object.
     * @param partnerId a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{language}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("numhits") int numHits, @PathParam("language") String language,
            @PathParam("partnerId") String partnerId)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        String query = null;
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     *
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId, and further filtered by the given @query All metadata is provided in the
     * passed language
     *
     * @param partnerId a {@link java.lang.String} object.
     * @param numHits a int.
     * @param query a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{query}/{language}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("partnerId") String partnerId, @PathParam("numhits") int numHits, @PathParam("query") String query,
            @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (query.equals("-")) {
            query = null;
        }
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     *
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId and are in the given @bookshelf All metadata is provided in the passed
     * language
     *
     * @param bookshelfIdString a {@link java.lang.String} object.
     * @param numHits a int.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("bookshelf/{bookshelfId}/{language}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("bookshelfId") String bookshelfIdString, @PathParam("numhits") int numHits,
            @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (bookshelfIdString.matches("\\d+")) {
            bookshelfId = Long.parseLong(bookshelfIdString);
        }
        String query = null;
        String partnerId = null;

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned that are within the given @bookshelf All
     * metadata is provided in the passed language
     *
     * @param partnerId a {@link java.lang.String} object.
     * @param bookshelfIdString a {@link java.lang.String} object.
     * @param numHits a int.
     * @param query a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/{query}/{language}/{bookshelfId}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("partnerId") String partnerId, @PathParam("bookshelfId") String bookshelfIdString,
            @PathParam("numhits") int numHits, @PathParam("query") String query, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (bookshelfIdString.matches("\\d+")) {
            bookshelfId = Long.parseLong(bookshelfIdString);
        }
        if (query.equals("-")) {
            query = null;
        }
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }


    /**
     * <p>
     * getSearchRssFeed.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param facets a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @param advancedSearchGroupOperator a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws com.rometools.rome.io.FeedException if any.
     */
    @GET
    @Path("/search/{query}/{facets}/{advancedSearchGroupOperator}/{language}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getSearchRssFeed(@PathParam("query") String query, @PathParam("facets") String facets,
            @PathParam("advancedSearchGroupOperator") String advancedSearchGroupOperator, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, FeedException {
        if (query.equals("-")) {
            query = createQuery(null, null, null, servletRequest, true);
        } else {
            try {
                query = URLDecoder.decode(query, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
            query = SearchHelper.buildFinalQuery(query, DataManager.getInstance().getConfiguration().isAggregateHits());
        }

        // Optional faceting
        List<String> filterQueries = null;
        if (!"-".equals(facets)) {
            SearchFacets searchFacets = new SearchFacets();
            searchFacets.setCurrentFacetString(facets);
            if ("-".equals(advancedSearchGroupOperator)) {
                advancedSearchGroupOperator = "0";
            }
            filterQueries = searchFacets.generateFacetFilterQueries(Integer.valueOf(advancedSearchGroupOperator), true);
        }
        int rssFeedItems = DataManager.getInstance().getConfiguration().getRssFeedItems();
        SyndFeedOutput output = new SyndFeedOutput();
        return output
                .outputString(RSSFeed.createRss(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), query, filterQueries, language, rssFeedItems));
    }

    /**
     * 
     * 
     * @param feed
     * @return
     */
    private static Channel createFeed(SyndFeed feed) {
        Channel rss = new Channel();
        rss.setCopyright(feed.getCopyright());
        rss.setDescription(feed.getDescription());
        rss.setLanguage(feed.getLanguage());
        rss.setLink(feed.getLink());
        rss.setPubDate(feed.getPublishedDate());
        rss.setTitle(feed.getTitle());
        for (Object object : feed.getEntries()) {
            if (object instanceof SyndEntry) {
                SyndEntry entry = (SyndEntry) object;

                RssItem item = new RssItem();
                item.setCreator(entry.getAuthor());
                item.setDescription(new Description(entry.getDescription().getValue()));
                item.setLink(entry.getLink());
                item.setPubDate(entry.getPublishedDate());
                item.setTitle(entry.getTitle());

                rss.addItem(item);
            }
        }
        return rss;
    }

    /**
     * @param query
     * @param bookshelfId
     * @param partnerId
     * @param servletRequest
     * @param addSuffixes
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should augment given query correctly
     * @should create basic query correctly
     * @should add suffixes if requested
     */
    static String createQuery(String query, Long bookshelfId, String partnerId, HttpServletRequest servletRequest, boolean addSuffixes)
            throws IndexUnreachableException, PresentationException, DAOException {
        // Build query, if none given
        if (StringUtils.isEmpty(query)) {
            if (bookshelfId != null) {
                // Bookshelf RSS feed
                BookmarkList bookshelf = DataManager.getInstance().getDao().getBookmarkList(bookshelfId);
                if (bookshelf == null) {
                    throw new PresentationException("Requested bookshelf not found: " + bookshelfId);
                }
                if (!bookshelf.isIsPublic()) {
                    throw new PresentationException("Requested bookshelf not public: " + bookshelfId);
                }
                query = bookshelf.generateSolrQueryForItems();
            } else {
                // Main RSS feed
                query = SolrConstants.ISWORK + ":true";
            }
        }

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("(").append(query).append(")");

        if (StringUtils.isNotBlank(partnerId)) {
            sbQuery.append(" AND ")
                    .append(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField())
                    .append(':')
                    .append(partnerId.trim());
        }

        if (addSuffixes) {
            sbQuery.append(
                    SearchHelper.getAllSuffixes(servletRequest, null, true, true, DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()));
        }

        return sbQuery.toString();
    }

}
