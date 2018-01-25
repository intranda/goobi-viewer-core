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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;

/**
 * Part of the IIIF-presentation api
 * 
 *  represents a iiif collection which is the topmost element of a json+ld response
 *  
 * @author Florian Alpers
 *
 */
@JsonPropertyOrder({"@context", "@id", "@type", "label", "viewingHint", "description", "thumbnail", "attribution", "rendering", "collections"})
public class BaseCollection extends Collection {

    private ViewingHint viewingHint;
    private String attribution = "Provided by intranda GmbH";
    private URL parentCollectionUrl = null;
    private List<Collection> subCollections = new ArrayList<>();

    
    /**
     * @param collectionView
     * @param locale
     * @param collectionField
     * @param facetField
     * @throws MalformedURLException
     */
    public BaseCollection(CollectionView collectionView, Locale locale, String baseUrl, HierarchicalBrowseDcElement baseElement, String collectionField, String facetField, String contextPath) throws MalformedURLException {
        super(collectionView, locale, baseUrl, baseElement, collectionField, facetField, contextPath);
        
        if(StringUtils.isNotBlank(collectionView.getTopVisibleElement())) {
            viewingHint = ViewingHint.multipart;
        } else {
            viewingHint = ViewingHint.top;
        }
                
        if(baseElement != null) {
            String parentName = baseElement.getParentName();
            parentCollectionUrl = getCollectionUrl(locale, baseUrl, collectionField, parentName);

        }
        
        
        for (HierarchicalBrowseDcElement element : collectionView.getVisibleDcElements()) {
            if(!element.equals(baseElement)) {                
                collectionView.setTopVisibleElement(element);
                subCollections.add(new SubCollection(collectionView, locale, baseUrl, element, collectionField, facetField, contextPath));
            }
        }

        collectionView.setTopVisibleElement("");
    }
    
    /**
     * The iiif-presentation context. Static
     * 
     * @return
     */
    @JsonProperty("@context")
    public String getContext() {
        return IIIF_PRESENTATION_CONTEXT;
    }


    
    /**
     * @return the viewingHint
     */
    @JsonProperty("viewingHint")
    public ViewingHint getViewingHint() {
        return viewingHint;
    }

    /**
     * @return the attribution
     */
    @JsonProperty("attribution")
    public String getAttribution() {
        return attribution;
    }

    /**
     * @return the parentCollectionUrl
     */
    @JsonProperty("within")
    public URL getParentCollectionUrl() {
        return parentCollectionUrl;
    }
    /**
     * @return the subCollections
     */
    @JsonProperty("members")
    public List<Collection> getSubCollections() {
        return subCollections;
    }
    

}
