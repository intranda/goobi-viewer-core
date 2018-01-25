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
package de.intranda.digiverso.presentation.servlets.rest.collections;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * IIIF REST resource providing a collection object as defined in the IIIF presentation api
 * 
 * @author Florian Alpers
 *
 */

@Path("/collections")
@ViewerRestServiceBinding
public class CollectionResource {

    private static final Logger logger = LoggerFactory.getLogger(CollectionResource.class);

    private static Map<String, String> facetFieldMap = new HashMap<>();
    private static Map<String, CollectionView> collectionViewMap = new HashMap<>();

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     * 
     * @param language
     * @param collectionField
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws MalformedURLException
     */
    @GET
    @Path("/{language}/{collectionField}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection getCollections(@PathParam("language") String language, @PathParam("collectionField") String collectionField)
            throws PresentationException, IndexUnreachableException, MalformedURLException {

        Collection collection = generateCollection(collectionField, null, getBaseUrl(), getLocale(language), getFacetField(collectionField));

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        return collection;

    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     * 
     */
    @GET
    @Path("/{language}/{collectionField}/{topElement}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection getCollection(@PathParam("language") String language, @PathParam("collectionField") String collectionField,
            @PathParam("topElement") final String topElement) throws IndexUnreachableException, MalformedURLException {

        Collection collection = generateCollection(collectionField, topElement, getBaseUrl(), getLocale(language), getFacetField(collectionField));

        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        return collection;

    }

    /**
     * @param collectionField
     * @param topElement
     * @param url
     * @param locale
     * @param facetField
     * @return
     * @throws IndexUnreachableException
     * @throws MalformedURLException
     */
    public Collection generateCollection(String collectionField, final String topElement, String url, Locale locale, final String facetField)
            throws IndexUnreachableException, MalformedURLException {

        
        CollectionView collectionView = getCollectionView(collectionField, facetField);

        if (StringUtils.isNotBlank(topElement) && !"-".equals(topElement)) {
            collectionView.setTopVisibleElement(topElement);
            collectionView.setDisplayParentCollections(false);
        }
        collectionView.calculateVisibleDcElements(true);

        HierarchicalBrowseDcElement baseElement = null;
        if (StringUtils.isNotBlank(collectionView.getTopVisibleElement())) {
            baseElement = collectionView.getCompleteList().stream().filter(element -> topElement.startsWith(element.getName())).flatMap(
                    element -> element.getAllDescendents(true).stream()).filter(element -> topElement.equals(element.getName())).findFirst().orElse(
                            null);
        }

        Collection collection = new BaseCollection(collectionView, locale, url, baseElement, collectionField, facetField, getServletPath());
        return collection;
    }

    /**
     * @param collectionField
     * @param facetField
     * @return
     * @throws IndexUnreachableException
     */
    public CollectionView getCollectionView(String collectionField, final String facetField) throws IndexUnreachableException {

        synchronized (collectionViewMap) {
            if (collectionViewMap.containsKey(collectionField)) {
                return new CollectionView(collectionViewMap.get(collectionField));
            }
        }

        CollectionView view = new CollectionView(collectionField, () -> SearchHelper.findAllCollectionsFromField(collectionField, facetField, true,
                true, true, true));
        view.populateCollectionList();

        synchronized (collectionViewMap) {
            if (collectionViewMap.containsKey(collectionField)) {
                return new CollectionView(collectionViewMap.get(collectionField));
            }
            collectionViewMap.put(collectionField, view);
            return view;
        }
    }

    /**
     * @param language
     * @return
     */
    public Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }
    
    public String getServletPath() {
        return servletRequest.getContextPath();
    }

    /**
     * @return
     */
    public String getBaseUrl() {
        String url = servletRequest.getRequestURL().toString();
        url = url.substring(0, url.indexOf("/collections") + "/collections".length());
        return url;
    }

    /**
     * @param collectionField
     * @return
     */
    public String getFacetField(String collectionField) {

        synchronized (facetFieldMap) {
            if (facetFieldMap.containsKey(collectionField)) {
                return facetFieldMap.get(collectionField);
            }
            String facetField = collectionField;
            if (collectionField.startsWith("MD_")) {
                facetField = collectionField.replace("MD_", "FACET_");
            } else {
                facetField = "MD_" + collectionField;
            }
            try {
                if (!DataManager.getInstance().getSearchIndex().getAllFieldNames().contains(facetField)) {
                    facetField = collectionField;
                }
            } catch (SolrServerException | IOException e) {
                logger.warn("Unable to query for facet field", e);
                facetField = collectionField;
            }
            facetFieldMap.put(collectionField, facetField);
            return facetField;
        }

    }
}
