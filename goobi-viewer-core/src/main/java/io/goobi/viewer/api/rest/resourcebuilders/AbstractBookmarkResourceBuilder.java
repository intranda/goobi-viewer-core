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

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Collection2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder;

/**
 * @author florian
 *
 */
public abstract class AbstractBookmarkResourceBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractBookmarkResourceBuilder.class);

    public abstract List<BookmarkList> getAllBookmarkLists() throws DAOException, IOException, RestApiException;

    public abstract SuccessMessage addBookmarkList() throws DAOException, IOException, RestApiException, IllegalRequestException;

    public abstract SuccessMessage addBookmarkList(String name) throws DAOException, IOException, RestApiException, IllegalRequestException;

    public abstract BookmarkList getBookmarkListById(Long id) throws DAOException, IOException, RestApiException;

    public abstract SuccessMessage deleteBookmarkList(Long id) throws DAOException, IOException, RestApiException, IllegalRequestException;

    public abstract Collection2 getAsCollection(Long id, AbstractApiUrlManager urls) throws DAOException, RestApiException, IOException;

    public abstract String getBookmarkListForMirador(Long id, AbstractApiUrlManager urls)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException;

    public abstract SuccessMessage addBookmarkToBookmarkList(Long id, String pi, String logId,
            String pageString) throws DAOException, IOException, RestApiException;

    public abstract SuccessMessage addBookmarkToBookmarkList(Long id, String pi) throws DAOException, IOException, RestApiException;

    public abstract SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi, String logId,
            String pageString) throws DAOException, IOException, RestApiException;

    public abstract SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi)
            throws DAOException, IOException, RestApiException;

    public abstract Collection2 createCollection(BookmarkList list, AbstractApiUrlManager urls);

    /**
     * <p>
     * getPageOrder.
     * </p>
     *
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPageOrder(String pageString) {
        Integer order = null;
        if (StringUtils.isNotBlank(pageString) && pageString.matches("\\d+")) {
            try {
                order = Integer.parseInt(pageString);
            } catch (NumberFormatException e) {
                //not in integer range
            }
        }
        return order;
    }

    public String getSharedBookmarkListForMirador(String key, AbstractApiUrlManager urls)
            throws DAOException, PresentationException, ContentNotFoundException, ViewerConfigurationException, IndexUnreachableException {
        try {
            BookmarkList bookmarkList = getSharedBookmarkList(key);
            return bookmarkList.getMiradorJsonObject(urls.getApplicationUrl(), urls.getApiUrl());
        } catch (ContentNotFoundException | RestApiException | NullPointerException e) {
            throw new ContentNotFoundException("No matching bookmark list found");
        }
    }

    /**
     * Returns all public bookmark lists.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public List<BookmarkList> getAllPublicBookmarkLists() throws DAOException, IOException, RestApiException {
        return DataManager.getInstance().getDao().getPublicBookmarkLists();
    }

    /**
     * <p>
     * getAsCollection.
     * </p>
     *
     * @param sharedKey
     * @param urls
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */

    public Collection2 getAsCollection(String sharedKey, AbstractApiUrlManager urls) throws ContentNotFoundException, DAOException {

        try {
            BookmarkList list = getSharedBookmarkList(sharedKey);
            return createCollection(list, urls);
        } catch (ContentNotFoundException | RestApiException e) {
            throw new ContentNotFoundException("No matching bookmark list found");
        }
    }

    /**
     * @param list
     * @param url
     * @return {@link Collection2}
     */
    protected Collection2 createCollection(BookmarkList list, String url) {
        ManifestBuilder builder = new ManifestBuilder(new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl()));
        Collection2 collection = new Collection2(URI.create(url), list.getName());
        collection.setLabel(new SimpleMetadataValue(list.getName()));
        collection.setDescription(new SimpleMetadataValue(list.getDescription()));
        list.getItems().forEach(item -> {
            try {
                URI manifestURI = builder.getManifestURI(item.getPi());
                Manifest2 manifest = new Manifest2(manifestURI);
                manifest.setLabel(new SimpleMetadataValue(item.getName()));
                manifest.addThumbnail(new ImageContent(URI.create(item.getRepresentativeImageUrl())));
                collection.addManifest(manifest);
                if (item.getOrder() != null) {
                    Canvas2 canvas = new Canvas2(builder.getCanvasURI(item.getPi(), item.getOrder()));
                    Sequence sequence = new Sequence(builder.getSequenceURI(item.getPi(), ""));
                    sequence.setStartCanvas(canvas);
                    manifest.setSequence(sequence);
                }
            } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException | DAOException e) {
                logger.error("Failed to add item {} to manifest", item.getId());
            }
        });
        return collection;
    }

    /**
     * @param shareKey
     * @return {@link BookmarkList}
     * @throws DAOException If an error occured talking to the database
     * @throws RestApiException If no user session exists or if the user has no access to the requested list
     * @throws ContentNotFoundException If no list with the given key was found
     */
    public BookmarkList getSharedBookmarkList(String shareKey) throws DAOException, RestApiException, ContentNotFoundException {
        BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(shareKey);

        if (bookmarkList == null) {
            throw new ContentNotFoundException("No bookmarklist found for key " + shareKey);
        }
        if (bookmarkList.hasShareKey()) {
            logger.trace("Serving shared bookmark list {}", bookmarkList.getId());
            return bookmarkList;
        }
        throw new RestApiException("No access to bookmark list " + bookmarkList.getId() + " - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * @param orig
     * @throws IllegalRequestException
     * @throws DAOException
     */
    public abstract void updateBookmarkList(BookmarkList orig) throws IllegalRequestException, DAOException;
}
