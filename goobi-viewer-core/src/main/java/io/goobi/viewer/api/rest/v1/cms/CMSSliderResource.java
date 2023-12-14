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
package io.goobi.viewer.api.rest.v1.cms;

import static io.goobi.viewer.api.rest.v1.ApiUrls.CMS_MEDIA;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CMS_MEDIA_ITEM;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path("/cms/slider/{sliderId}")
@ViewerRestServiceBinding
public class CMSSliderResource {

    /**
     *
     */

    private static final Logger logger = LogManager.getLogger(CMSSliderResource.class);

    private final CMSSlider slider;

    public CMSSliderResource(@PathParam("sliderId") Long sliderId) throws DAOException {
        this.slider = DataManager.getInstance().getDao().getSlider(sliderId);
    }

    public CMSSliderResource(CMSSlider slider) {
        this.slider = slider;
    }

    @GET
    @javax.ws.rs.Path("/slides")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<URI> getSlides() throws ContentNotFoundException, PresentationException, IndexUnreachableException, IllegalRequestException {
        if (this.slider != null) {
            switch (slider.getSourceType()) {
                case COLLECTIONS:
                    return getCollections(slider.getCollections());
                case RECORDS:
                    return getRecords(slider.getSolrQuery(), slider.getMaxEntries(), slider.getSortField());
                case PAGES:
                    return getPages(slider.getCategories());
                case MEDIA:
                    return getMedia(slider.getCategories());
                default:
                    throw new IllegalRequestException("Cannot create collection for slider " + slider.getName());
            }
        }
        throw new ContentNotFoundException("Slider with requested id not found");
    }

    /**
     * @param categories
     * @return List<URI>
     */
    private List<URI> getPages(List<String> categories) {
        return categories.stream()
                .map(this::getCategoryById)
                .filter(Objects::nonNull)
                .flatMap(category -> getPagesForCategory(category).stream())
                .filter(CMSPage::isPublished)
                //not needed. Slides are sorted in javascript
                //                .sorted((page1, page2) -> Long.compare(page1.getPageSortingOrElse(0), page2.getPageSortingOrElse(0)))
                .map(this::getApiUrl)
                .collect(Collectors.toList());
    }

    private CMSCategory getCategoryById(String idString) {
        try {
            Long id = Long.parseLong(idString);
            return DataManager.getInstance().getDao().getCategory(id);
        } catch (NumberFormatException | DAOException e) {
            logger.error(e.toString(), e);
            return null;
        }
    }

    private static List<CMSPage> getPagesForCategory(CMSCategory category) {
        try {
            return DataManager.getInstance().getDao().getCMSPagesByCategory(category);
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return Collections.emptyList();
        }

    }

    private List<URI> getMedia(List<String> categories) {
        return categories.stream()
                .map(this::getCategoryById)
                .filter(Objects::nonNull)
                .flatMap(category -> getMediaForCategory(category).stream())
                .sorted((item1, item2) -> Integer.compare(item1.getDisplayOrder(), item2.getDisplayOrder()))
                .map(this::getApiUrl)
                .collect(Collectors.toList());
    }

    /**
     * @param category
     * @return List<CMSMediaItem>
     */
    private static List<CMSMediaItem> getMediaForCategory(CMSCategory category) {
        try {
            return DataManager.getInstance().getDao().getCMSMediaItemsByCategory(category);
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return Collections.emptyList();
        }
    }

    private URI getApiUrl(CMSPage page) {
        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path("/cms/pages/{pageId}").params(page.getId()).buildURI())
                .orElse(null);
    }

    private URI getApiUrl(CMSMediaItem media) {
        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path(CMS_MEDIA, CMS_MEDIA_ITEM).params(media.getId()).buildURI())
                .orElse(null);
    }

    /**
     * @param solrQuery
     * @param maxResults
     * @param sortField
     * @return List<URI>
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private static List<URI> getRecords(String solrQuery, int maxResults, String sortField) throws PresentationException, IndexUnreachableException {

        //limit query to records only
        solrQuery = "+(" + solrQuery + ") +(ISWORK:* ISANCHOR:*)";

        List<URI> manifests = new ArrayList<>();
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        if (urls == null) {
            return Collections.emptyList();
        }

        List<StringPair> sortFields = StringUtils.isBlank(sortField) ? null : SearchHelper.parseSortString(sortField, null);
        SolrDocumentList solrDocs = DataManager.getInstance()
                .getSearchIndex()
                .search(solrQuery, 0, maxResults, sortFields, null, Arrays.asList(SolrConstants.PI))
                .getResults();
        for (SolrDocument doc : solrDocs) {
            String pi = (String) SolrTools.getSingleFieldValue(doc, SolrConstants.PI);
            URI uri = urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_MANIFEST).params(pi).query("mode", "simple").buildURI();
            manifests.add(uri);
        }
        return manifests;

    }

    /**
     * @param collectionNames
     * @return List<URI>
     */
    private static List<URI> getCollections(List<String> collectionNames) {
        List<URI> collections = new ArrayList<>();
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        if (urls == null) {
            return Collections.emptyList();
        }
        for (String collectionName : collectionNames) {
            String[] nameParts = collectionName.split("/");
            if (nameParts.length != 2) {
                logger.error("Collection name for slider source has wrong format: {}", collectionName);
                continue;
            }
            String field = nameParts[0];
            String value = nameParts[1];
            URI uri = urls.path(ApiUrls.COLLECTIONS, ApiUrls.COLLECTIONS_COLLECTION).params(field, value).buildURI();
            collections.add(uri);
        }
        return collections;
    }

}
