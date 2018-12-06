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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);

    private static final List<String> HIDDEN_SOLR_FIELDS =
            Arrays.asList(new String[] { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE,
                    SolrConstants.THUMBNAIL, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE, SolrConstants.PI_PARENT, SolrConstants.LOGID,
                    SolrConstants.ISWORK, SolrConstants.FILENAME_TEI, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNOSORT,
                    SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.NUMPAGES });

    private static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM, SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
            SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.NUMPAGES, SolrConstants.DATAREPOSITORY };

    private final URI servletURI;
    private final URI requestURI;
    private final Optional<HttpServletRequest> request;

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
        if (uri != null && !uri.isAbsolute()) {
            return new URI(getServletURI().toString() + uri.toString());
        }
        return uri;
    }

    /**
     * @param rssUrl
     * @return
     * @throws URISyntaxException
     */
    protected URI absolutize(String url) throws URISyntaxException {
        if (url != null) {
            url = url.replaceAll("\\s", "+");
        }
        return absolutize(new URI(url));
    }

    /**
     * @return The requested url before any presentation specific parts. Generally the rest api url. Includes a trailing slash
     */
    protected URI getBaseUrl() {

        String request = requestURI.toString();
        if (!request.contains("/iiif/")) {
            return requestURI;
        }
        request = request.substring(0, request.indexOf("/iiif/") + 1);
        try {
            return new URI(request);
        } catch (URISyntaxException e) {
            return requestURI;
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
     * @return viewer image view url for the given page
     */
    public String getViewImageUrl(PhysicalElement ele) {
        try {
            return getServletURI() + "/" + PageType.viewImage.getName() + ele.getPurlPart();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for page {} + in {}.", ele.getOrder(), ele.getPi());
            Messages.error("errGetCurrUrl");
        }
        return getServletURI() + "/metsresolver?id=" + 0;
    }

    /**
     * Simple method to create a label for a {@link SolrDocument} from {@link SolrConstants.LABEL}, {@link SolrConstants.TITLE} or
     * {@link SolrConstants.DOCSTRUCT}
     * 
     * @param solrDocument
     * @return
     */
    public static Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {

        String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
        String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
        String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);

        if (StringUtils.isNotBlank(label)) {
            return Optional.of(new SimpleMetadataValue(label));
        } else if (StringUtils.isNotBlank(title)) {
            return Optional.of(new SimpleMetadataValue(title));
        } else if (StringUtils.isNotBlank(docStruct)) {
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
        for (String field : getMetadataFields(ele)) {
            if (!HIDDEN_SOLR_FIELDS.contains(field) && !field.endsWith("_UNTOKENIZED") && !field.matches(".*_LANG_\\w{2,3}")) {
                IMetadataValue.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(IMetadataValue.getTranslations(field), value))
                        .ifPresent(md -> {
                            md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            manifest.addMetadata(md);
                        });

                //                manifest.addMetadata(new Metadata(IMetadataValue.getTranslations(field), mdValue));
            }
        }
    }

    /**
     * @param ele
     * @return
     */
    private static List<String> getMetadataFields(StructElement ele) {
        Set<String> fields = ele.getMetadataFields().keySet();
        List<String> baseFields = fields.stream().map(field -> field.replaceAll("_LANG_\\w{2,3}$", "")).distinct().collect(Collectors.toList());
        return baseFields;
    }

    /**
     * Queries the StructElement with the given PI and LOGID If nothing is found, null is returned;
     * 
     * @param pi
     * @return The first matching StructElement, or null
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public StructElement getDocument(String pi, String logId) throws PresentationException, IndexUnreachableException {
        String query = "PI_TOPSTRUCT:" + pi + " AND LOGID:" + logId + " AND DOCTYPE:DOCTRCT";
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, getSolrFieldList());
        if (doc != null) {
            StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
            ele.setImageNumber(1);
            return ele;
        }
        return null;
    }

    /**
     * Queries all direct children of the given element
     * 
     * @param pi
     * @param logId
     * @return The list of direct child elements, or an empty list if no elements were found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<StructElement> getChildren(StructElement parent) throws PresentationException, IndexUnreachableException {
        String query = "IDDOC_PARENT:" + parent.getLuceneId() + " AND DOCTYPE:DOCTRCT";
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().getDocs(query, getSolrFieldList());
        List<StructElement> eles = new ArrayList<>();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
                eles.add(ele);
                try {
                    Integer pageNo = (Integer) doc.getFieldValue(SolrConstants.THUMBPAGENO);
                    ele.setImageNumber(pageNo);
                    //                        Integer numPages = (Integer) doc.getFieldValue(SolrConstants.NUMPAGES);
                } catch (NullPointerException | ClassCastException e) {
                    ele.setImageNumber(1);
                }
            }
        }
        Collections.sort(eles, new StructElementComparator());
        return eles;
    }

    /**
     * Queries all DocStructs which have the given PI as PI_TOPSTRUCT or anchor (or are the anchor themselves). Works are sorted by a
     * {@link StructElementComparator} If no hits are found, an empty list is returned
     * 
     * @param pi
     * @return A list of all docstructs with the given pi or children thereof. An empty list if no hits are found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<StructElement> getDocumentWithChildren(String pi) throws PresentationException, IndexUnreachableException {
        String anchorQuery = "(ISWORK:* AND PI_PARENT:" + pi + ") OR (ISANCHOR:* AND PI:" + pi + ")";
        String workQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:DOCSTRCT";
        String query = "(" + anchorQuery + ") OR (" + workQuery + ")";
        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, getSolrFieldList());
        List<StructElement> eles = new ArrayList<>();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
                eles.add(ele);
                try {
                    Integer pageNo = (Integer) doc.getFieldValue(SolrConstants.THUMBPAGENO);
                    ele.setImageNumber(pageNo);
                    //                        Integer numPages = (Integer) doc.getFieldValue(SolrConstants.NUMPAGES);
                } catch (NullPointerException | ClassCastException e) {
                    ele.setImageNumber(1);
                }
            }
        }
        Collections.sort(eles, new StructElementComparator());
        return eles;
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
        if (doc != null) {
            StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
            ele.setImageNumber(1);
            return ele;
        }
        return null;
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
     * Gets the attribution text configured in webapi.iiif.attribution and returns all translations if any are found, or the configured string itself
     * otherwise
     * 
     * @return the configured attribution
     */
    protected IMetadataValue getAttribution() {
        String message = DataManager.getInstance().getConfiguration().getIIIFAttribution();
        return IMetadataValue.getTranslations(message);
    }

    /**
     * @return the request
     */
    protected Optional<HttpServletRequest> getRequest() {
        return request;
    }

    public URI getCollectionURI(String collectionField, String baseCollectionName) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/collections/").append(collectionField);
        if (StringUtils.isNotBlank(baseCollectionName)) {
            sb.append("/").append(baseCollectionName);
        }
        return new URI(sb.toString());
    }

    public URI getManifestURI(String pi) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/manifest");
        return new URI(sb.toString());
    }

    public URI getRangeURI(String pi, String logId) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/range/").append(logId);
        return new URI(sb.toString());
    }

    public URI getSequenceURI(String pi, String label) throws URISyntaxException {
        if (StringUtils.isBlank(label)) {
            label = "basic";
        }
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/sequence/").append(label);
        return new URI(sb.toString());
    }

    public URI getCanvasURI(String pi, int pageNo) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/canvas/").append(pageNo);
        return new URI(sb.toString());
    }

    public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type) throws URISyntaxException {
        StringBuilder sb =
                new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/list/").append(pageNo).append("/").append(
                        type.name());
        return new URI(sb.toString());
    }

    public URI getAnnotationListURI(String pi, AnnotationType type) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/list/").append(type.name());
        return new URI(sb.toString());
    }

    public URI getLayerURI(String pi, AnnotationType type) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/layer");
        sb.append("/").append(type.name());
        return new URI(sb.toString());
    }

    public URI getLayerURI(String pi, String logId) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/layer");
        if (StringUtils.isNotBlank(logId)) {
            sb.append("/").append(logId);
        } else {
            sb.append("/base");
        }
        return new URI(sb.toString());
    }

    /**
     * @param pi
     * @param order
     * @return
     * @throws URISyntaxException
     */
    public URI getImageAnnotationURI(String pi, int order) throws URISyntaxException {
        StringBuilder sb =
                new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append("/canvas/").append(order).append("/image/1");
        return new URI(sb.toString());
    }

    public URI getAnnotationURI(String pi, int order, AnnotationType type, int annoNum) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/")
                .append(pi)
                .append("/canvas/")
                .append(order)
                .append("/")
                .append(type.name())
                .append("/")
                .append(annoNum);
        return new URI(sb.toString());
    }

    public URI getAnnotationURI(String pi, AnnotationType type, int annoNum) throws URISyntaxException {
        StringBuilder sb =
                new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi).append(type.name()).append("/").append(annoNum);
        return new URI(sb.toString());
    }

}
