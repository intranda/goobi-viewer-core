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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ImageDeliveryBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);
    
    private ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumb();
    
    private static final List<String> HIDDEN_SOLR_FIELDS = Arrays.asList(new String[] { SolrConstants.IDDOC, SolrConstants.PI,
            SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT, SolrConstants.LOGID, SolrConstants.ISWORK,
            SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.PI_PARENT, SolrConstants.CURRENTNOSORT, SolrConstants.LOGID, SolrConstants.THUMBPAGENO });
    
    private static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT, SolrConstants.LOGID, SolrConstants.ISWORK,
            SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.PI_PARENT, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT, SolrConstants.LOGID, SolrConstants.THUMBPAGENO };

    
    
    protected static String ATTRIBUTION = "Provided by intranda GmbH";
    
    private final URI servletURI;
    private final URI requestURI;
    private final Optional<HttpServletRequest> request;
    protected final ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();

    
    public AbstractBuilder(HttpServletRequest request) throws URISyntaxException {
        this.request = Optional.ofNullable(request);
        this.servletURI = new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        this.requestURI = new URI(ServletUtils.getServletPathWithoutHostAsUrlFromRequest(request) + request.getRequestURI());
    }
    
    public AbstractBuilder(URI servletUri, URI requestURI) {
        this.request = Optional.empty();
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
    

    /**
     * @param manifest
     * @param ele
     */
    public void addMetadata(AbstractPresentationModelElement manifest, StructElement ele) {
        for (String field : ele.getMetadataFields().keySet()) {
            if (!HIDDEN_SOLR_FIELDS.contains(field) && !field.endsWith("_UNTOKENIZED")) {
                Optional<IMetadataValue> mdValue =
                        ele.getMetadataValues(field).stream().reduce((s1, s2) -> s1 + "; " + s2).map(value -> IMetadataValue.getTranslations(value));
                mdValue.ifPresent(value -> {
                    manifest.addMetadata(new Metadata(IMetadataValue.getTranslations(field), value));
                });
            }
        }
    }
    
    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public StructElement getDocument(String pi, String logId) throws PresentationException, IndexUnreachableException {
        String query = "PI_TOPSTRUCT:" + pi + " AND LOGID:" + logId;
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, getSolrFieldList());
        StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
        ele.setImageNumber(1);
        return ele;
    }


    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public StructElement getDocument(String pi) throws PresentationException, IndexUnreachableException {
        String query = "PI:" + pi;
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, getSolrFieldList());
        if(doc != null) {            
            StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
            ele.setImageNumber(1);
            return ele;
        } else {
            return null;
        }
    }
    


    /**
     * @return
     */
    protected List<String> getSolrFieldList() {
        List<String> fields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        for (String string : REQUIRED_SOLR_FIELDS) {
            if (!fields.contains(string)) {
                fields.add(string);
            }
        }
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && !fields.contains(navDateField)) {
            fields.add(navDateField);
        }
        return fields;
    }
    
    /**
     * @return the thumbs
     */
    protected ThumbnailHandler getThumbs() {
        return thumbs;
    }
    
    /**
     * @return the request
     */
    protected Optional<HttpServletRequest> getRequest() {
        return request;
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
    
    public URI getRangeURI(String pi, String logId) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/range/").append(logId);
        return new URI(sb.toString());
    }
    
    public URI getCanvasURI(String pi, int pageNo) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/canvas/").append(pageNo);
        return new URI(sb.toString());
    }
    
    public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/list/").append(pageNo).append("/").append(type.name());
        return new URI(sb.toString());
    }
    
    public URI getAnnotationListURI(String pi, AnnotationType type) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/list/").append(type.name());
        return new URI(sb.toString());
    }
    
    public URI getLayerURI(String pi, AnnotationType type) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/layer");
        sb.append("/").append(type.name());
        return new URI(sb.toString());
    }
    
    public URI getLayerURI(String pi, String logId) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("manifests/").append(pi).append("/layer");
        if(StringUtils.isNotBlank(logId)) {            
            sb.append("/").append(logId);
        } else {
            sb.append("/base");
        }
        return new URI(sb.toString());
    }
    
    protected abstract String getPath();
    
}
