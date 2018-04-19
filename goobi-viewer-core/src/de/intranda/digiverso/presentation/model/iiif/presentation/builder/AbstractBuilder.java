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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.ImageDeliveryBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);
    
    protected static String ATTRIBUTION = "Provided by intranda GmbH";
    
    private final URI servletURI;
    private final URI requestURI;
    protected final ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();

    
    public AbstractBuilder(HttpServletRequest request) throws URISyntaxException {
        this.servletURI = new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        this.requestURI = new URI(request.getRequestURI());
    }
    
    public AbstractBuilder(URI servletUri, URI requestURI) {
        this.servletURI = servletUri;
        this.requestURI = requestURI;
    }

    /**
     * @param language
     * @return
     */
    protected Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    protected URI getServletURI() {
        return servletURI;
    }

    protected URI absolutize(URI uri) throws URISyntaxException {
        
        return getServletURI().resolve(uri);
    }
    
    /**
     * @param rssUrl
     * @return
     * @throws URISyntaxException 
     */
    protected URI absolutize(String url) throws URISyntaxException {
        if(url != null) {
            url = url.replaceAll("\\s", "+");
        }
        return absolutize(new URI(url));
    }

    /**
     * @return  The requested url before any presentation specific parts. Generally the rest api url. Includes a trailing slash
     */
    protected URI getBaseUrl() {
        
        String request = requestURI.toString();
        if(!request.contains(getPath())) {
            return requestURI;
        } else {
            request = request.substring(0, request.indexOf(getPath())+1);
            try {
                return new URI(request);
            } catch (URISyntaxException e) {
                return requestURI;
            }
        }
        
    }
    
    /**
     * @return METS resolver link for the DFG Viewer
     */
    public String getMetsResolverUrl(StructElement ele) {
        try {
            return getServletURI() + "/metsresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return getServletURI() + "/metsresolver?id=" + 0;
    }
    

    /**
     * Simple method to create a label for a {@link SolrDocument} from {@link SolrConstants.LABEL}, {@link SolrConstants.TITLE} or {@link SolrConstants.DOCSTRUCT}
     * 
     * @param solrDocument
     * @return
     */
    public static Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {
        
        String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
        String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
        String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);
                
        if(StringUtils.isNotBlank(label)) {
            return Optional.of(new SimpleMetadataValue(label));
        } else if(StringUtils.isNotBlank(title)) {
            return Optional.of(new SimpleMetadataValue(title));
        } else if(StringUtils.isNotBlank(docStruct)) {
            return Optional.of(IMetadataValue.getTranslations(docStruct));
        } else {
            return Optional.empty();
        }
    }
    
    
    

    public URI getCollectionURI(String collectionField, String baseCollectionName) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("collections/").append(collectionField);
        if (StringUtils.isNotBlank(baseCollectionName)) {
            sb.append("/").append(baseCollectionName);
        }
        return new URI(sb.toString());
    }
    
    public URI getManifestURI(String pi) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi);
        return new URI(sb.toString());
    }
    
    protected abstract String getPath();
    
}
