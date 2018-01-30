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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Part of the IIIF presentation api
 * 
 * Represents a collection as defined in the IIIF presentation api
 * May have other collections in its members field
 * TODO: Allow manifest items in members, once the manifest api part is implemented
 * 
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"@id", "@type", "label", "description", "thumbnail", "metadata", "rendering", "related"})
public abstract class Collection {
    
    @Resource
    WebServiceContext context;

    /**
     * 
     */
    private static final Logger logger = LoggerFactory.getLogger(Collection.class);
    protected static final String IIIF_PRESENTATION_CONTEXT = "http://iiif.io/api/presentation/2/context.json";
    public final static String NUM_MANIFESTS_LABEL = "volumes";
    public final static String NUM_SUBCOLLECTIONS_LABEL = "subCollections";
    public final static String RSS_FEED_LABEL = "Rss feed";
    public final static String RSS_FEED_FORMAT = "Rss feed";
    
    protected URL id;
    protected String label;
    private CollectionLink link = null;
    private CollectionLink rssLink = null;
    private String description = null;
    private URI thumbnail;
    protected String type = "sc:collection";
    private List<Metadata> metadata = new ArrayList<>();

    /**
     * @param collectionView
     * @throws MalformedURLException
     */
    public Collection(CollectionView collectionView, Locale locale, String baseUrl, HierarchicalBrowseDcElement baseElement, String collectionField, String facetField, String contextPath) throws MalformedURLException {
        String baseCollectionName = collectionView.getTopVisibleElement();
        this.id = getCollectionUrl(locale, baseUrl, collectionField, baseCollectionName);
        
        if(baseElement != null) {

            if(StringUtils.isNotBlank(collectionView.getTopVisibleElement())) {
                label = Helper.getTranslation(collectionView.getTopVisibleElement(), locale);
                addMetadata(NUM_MANIFESTS_LABEL, Long.toString(baseElement.getNumberOfVolumes()));
                addMetadata(NUM_SUBCOLLECTIONS_LABEL, Long.toString(baseElement.getChildren().size()));
                this.rssLink = createRssLink(baseElement.getRssUrl(), baseUrl);
            } else {
                label = Helper.getTranslation(collectionField, locale);
            }
            
            this.description = baseElement.getDescription();
            if(baseElement.getInfo() != null) {                
                this.description = baseElement.getInfo().getDescription();
                this.thumbnail = baseElement.getInfo().getIconURI();
                if(this.thumbnail != null && !this.thumbnail.isAbsolute()) {
                    try {
                        this.thumbnail = new URI(contextPath + this.thumbnail.toString());
                    } catch (URISyntaxException e) {
                       logger.error(e.toString(),e);
                    }
                }
            }
            this.link = createLink(collectionView, baseUrl, baseElement, label);
        }

    }

    /**
     * @param rssUrl
     * @return
     */
    private CollectionLink createRssLink(String rssUrl, String baseUrl) {
        if(!rssUrl.matches("https?://.*")) {
            rssUrl = baseUrl.substring(0, baseUrl.indexOf("/rest/")) + rssUrl;
        }
        rssUrl = rssUrl.replaceAll("\\s+", "+");
       try {           
           URL url = new URL(rssUrl);
           CollectionLink link = new CollectionLink(url, RSS_FEED_LABEL);
           return link;
       } catch(NullPointerException | MalformedURLException e) {
           logger.error("Unable to parse rss url " + rssUrl);
           return null;
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

    /**
     * @return the metadata
     */
    @JsonInclude(Include.NON_EMPTY)
    public List<Metadata> getMetadata() {
        return metadata;
    }
    
    public void addMetadata(String label, String value) {
        if(StringUtils.isNotBlank(label) && StringUtils.isNotBlank(value)) {
            this.metadata.add(new Metadata(label, value));
        }
    }
    
    /**
     * @return the rssLink
     */
    @JsonProperty("related")
    public CollectionLink getRssLink() {
        return rssLink;
    }
}
