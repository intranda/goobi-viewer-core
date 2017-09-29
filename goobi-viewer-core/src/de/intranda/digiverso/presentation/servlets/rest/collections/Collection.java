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
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.ConfigurationBean;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;

/**
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"@id", "@type", "label", "description", "thumbnail", "rendering"})
public abstract class Collection {

    /**
     * 
     */
    protected static final String IIIF_PRESENTATION_CONTEXT = "http://iiif.io/api/presentation/2/context.json";
    protected URL id;
    protected String label;
    private CollectionLink link = null;
    private String description = null;
    private URI thumbnail;
    protected String type = "sc:collection";

    /**
     * @param collectionView
     * @throws MalformedURLException
     */
    public Collection(CollectionView collectionView, Locale locale, String baseUrl, HierarchicalBrowseDcElement baseElement, String collectionField, String facetField) throws MalformedURLException {
        String baseCollectionName = collectionView.getTopVisibleElement();
        this.id = getCollectionUrl(locale, baseUrl, collectionField, baseCollectionName);
        
        if(baseElement != null) {

            if(StringUtils.isNotBlank(collectionView.getTopVisibleElement())) {
                label = Helper.getTranslation(collectionView.getTopVisibleElement(), locale);
            } else {
                label = Helper.getTranslation(collectionField, locale);
            }
            
            this.description = baseElement.getDescription();
            if(baseElement.getInfo() != null) {                
                this.description = baseElement.getInfo().getDescription();
                this.thumbnail = baseElement.getInfo().getIconURI();
            }
            this.link = createLink(collectionView, baseUrl, baseElement, label);
        }
        
        


    }

    /**
     * @param collectionView
     * @param baseUrl
     * @param baseElement
     * @throws MalformedURLException
     */
    public CollectionLink createLink(CollectionView collectionView, String baseUrl, HierarchicalBrowseDcElement element, String label) throws MalformedURLException {
        String urlString = collectionView.getCollectionUrl(element);
        if(!urlString.matches("https?://.*")) {
            urlString = baseUrl.substring(0, baseUrl.indexOf("/rest/")) + urlString;
        }
        URL url = new URL(urlString);
        
        return new CollectionLink(url, label);
    }

    /**
     * @param locale
     * @param baseUrl
     * @param collectionField
     * @param baseCollectionName
     * @throws MalformedURLException
     */
    public URL getCollectionUrl(Locale locale, String baseUrl, String collectionField, String baseCollectionName) throws MalformedURLException {
        StringBuilder sb = new StringBuilder(baseUrl)
                .append("/")
                .append(locale.getLanguage()).append("/")
                .append(collectionField).append("/");
        if (StringUtils.isNotBlank(baseCollectionName)) {
            sb.append(baseCollectionName).append("/");
        }
        return new URL(sb.toString());
    }

    /**
     * @return the url
     */
    @JsonProperty("@id")
    public URL getId() {
        return id;
    }
    
    /**
     * @return the type
     */
    @JsonProperty("@type")
    public String getType() {
        return type;
    }

    /**
     * @return the label
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }
    
    /**
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * @return the thumnail
     */
    @JsonProperty("thumbnail")
    public URI getThumbnail() {
        return thumbnail;
    }

    /**
     * @return the collectionURL
     */
    @JsonProperty("rendering")
    public CollectionLink getLink() {
        return link;
    }


}
