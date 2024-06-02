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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v2.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS_COLLECTION;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_IMAGE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_TEXT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_SECTIONS_RANGE;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.api.annotation.wa.ImageResource;
import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.v3.ImageInformation3;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.IIIFAgent;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.controller.model.ProviderConfiguration;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * Abstract AbstractBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractBuilder.class);

    protected final AbstractApiUrlManager urls;

    private final List<Locale> translationLocales = DataManager.getInstance().getConfiguration().getIIIFTranslationLocales();

    protected final DataRetriever dataRetriever = new DataRetriever();

    private final ThumbnailHandler thumbs;
    protected final io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder v1Builder;

    protected final int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
    protected final int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();

    protected final Configuration config;

    /**
     * <p>
     * Constructor for AbstractBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    protected AbstractBuilder(final AbstractApiUrlManager apiUrlManager) {
        this.config = DataManager.getInstance().getConfiguration();
        this.urls = apiUrlManager != null ? apiUrlManager : DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v2).orElse(null);
        AbstractApiUrlManager contentUrls = DataManager.getInstance().getRestApiManager().getContentApiManager(Version.v2).orElse(this.urls);
        this.thumbs = new ThumbnailHandler(new IIIFUrlHandler(contentUrls),
                ImageDeliveryBean.getStaticImagesPath(this.urls != null ? this.urls.getApplicationUrl() : "",
                        this.config.getTheme()));

        AbstractApiUrlManager v1Urls = DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v1).orElse(null);
        v1Builder = new io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder(v1Urls) {
            //
        };
    }

    /**
     * @param key
     * @return {@link IMetadataValue}
     */
    protected IMetadataValue getLabel(String key) {
        IMetadataValue value = ViewerResourceBundle.getTranslations(key, this.translationLocales, false);
        if (!value.getValue(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).isPresent()) {
            value.setValue(key, MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
        }
        return value;
    }

    /**
     * @param uri
     * @return {@link URI}
     */
    public URI absolutize(URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.isAbsolute()) {
            return uri;
        }
        try {
            return PathConverter.resolve(this.urls.getApplicationUrl(), uri.toString());
        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
            return uri;
        }
    }

    /**
     * 
     * @param uri
     * @return {@link URI}
     */
    public URI absolutize(String uri) {
        return absolutize(URI.create(uri));
    }

    /**
     * <p>
     * getLocale.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.util.Locale} object.
     */
    protected Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    /**
     * <p>
     * getMetsResolverUrl.
     * </p>
     *
     * @return METS resolver link for the DFG Viewer
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public URI getMetsResolverUrl(StructElement ele) {

        return UriBuilder.fromPath(urls.getApplicationUrl()).path("metsresolver").queryParam("id", ele.getPi()).build();
    }

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return LIDO resolver link for the DFG Viewer
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public URI getLidoResolverUrl(StructElement ele) {

        return UriBuilder.fromPath(urls.getApplicationUrl()).path("lidoresolver").queryParam("id", ele.getPi()).build();

    }

    /**
     * <p>
     * getViewUrl.
     * </p>
     *
     * @return viewer url for the given page in the given {@link io.goobi.viewer.model.viewer.PageType}
     * @param ele a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public String getViewUrl(PhysicalElement ele, PageType pageType) {
        try {
            return urls.getApplicationUrl() + "/" + pageType.getName() + ele.getPurlPart();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for page {} + in {}.", ele.getOrder(), ele.getPi());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * Simple method to create a label for a {@link org.apache.solr.common.SolrDocument} from {@link io.goobi.viewer.solr.SolrConstants#LABEL},
     * {@link io.goobi.viewer.solr.SolrConstants#TITLE} or {@link io.goobi.viewer.solr.SolrConstants#DOCSTRCT}
     *
     * @param solrDocument a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {

        String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
        String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
        String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);

        if (StringUtils.isNotBlank(label)) {
            return Optional.of(new SimpleMetadataValue(label));
        } else if (StringUtils.isNotBlank(title)) {
            return Optional.of(new SimpleMetadataValue(title));
        } else if (StringUtils.isNotBlank(docStruct)) {
            return Optional.of(getLabel(docStruct));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * addMetadata.
     * </p>
     *
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public void addMetadata(AbstractPresentationModelElement3 manifest, StructElement ele) {
        List<String> displayFields = this.config.getIIIFMetadataFields();
        List<String> eventFields = this.config.getIIIFEventFields();
        displayFields.addAll(eventFields);

        for (String field : getMetadataFields(ele)) {
            if (contained(field, displayFields) && !field.endsWith(SolrConstants.SUFFIX_UNTOKENIZED) && !field.matches(".*_LANG_\\w{2,3}")) {
                String configuredLabel = this.config.getIIIFMetadataLabel(field);
                String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel
                        : (field.contains("/") ? field.substring(field.indexOf("/") + 1) : field);
                SolrTools.getTranslations(field, ele, this.translationLocales, (s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(getLabel(label), value))
                        .ifPresent(manifest::addMetadata);
            }
        }
    }

    /**
     * @param ele
     * @param fieldName
     * @return Optional<String>
     */
    protected Optional<String> getSolrFieldValue(StructElement ele, String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            String value = ele.getMetadataValue(fieldName);
            return Optional.ofNullable(value);
        }
        return Optional.empty();
    }

    /**
     * 
     * @param ele
     * @return Optional<URI>
     */
    protected Optional<URI> getRightsStatement(StructElement ele) {
        return getSolrFieldValue(ele, this.config.getIIIFRightsField())
                .map(value -> {
                    try {
                        return new URI(value);
                    } catch (URISyntaxException e) {
                        logger.error(e.toString());
                        return null;
                    }
                });
    }

    /**
     * Return true if the field is contained in displayFields, accounting for wildcard characters
     *
     * @param field
     * @param displayFields
     * @return a boolean
     */
    protected boolean contained(String field, List<String> displayFields) {
        return displayFields.stream().anyMatch(displayField -> matches(field, displayField));
    }

    /**
     * 
     * @param field
     * @param template
     * @return a boolean
     */
    private static boolean matches(String field, String template) {

        String cleanedTemplate = template.replace("*", "");
        String cleanedField = field.replaceAll("_LANG_\\w{2,3}", "");
        if (template.startsWith("*") && template.endsWith("*")) {
            return cleanedField.contains(cleanedTemplate);
        } else if (template.startsWith("*")) {
            return cleanedField.endsWith(cleanedTemplate);
        } else if (template.endsWith("*")) {
            return cleanedField.startsWith(cleanedTemplate);
        } else {
            return Objects.equals(cleanedTemplate, cleanedField);
        }
    }

    /**
     * @param ele
     * @return List<String>
     */
    private static List<String> getMetadataFields(StructElement ele) {
        Set<String> fields = ele.getMetadataFields().keySet();
        return fields.stream().map(field -> field.replaceAll("_LANG_\\w{2,3}$", "")).distinct().collect(Collectors.toList());
    }

    /**
     * Gets the attribution text configured in webapi.iiif.attribution and returns all translations if any are found, or the configured string itself
     * otherwise
     *
     * @return the configured attribution
     */
    protected List<IMetadataValue> getAttributions() {
        return DataManager.getInstance()
                .getConfiguration()
                .getIIIFAttribution()
                .stream()
                .map(this::getLabel)
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.util.Optional} object.
     */
    protected Optional<IMetadataValue> getDescription(StructElement ele) {
        List<String> fields = this.config.getIIIFDescriptionFields();
        for (String field : fields) {
            Optional<IMetadataValue> optional = SolrTools.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2).map(md -> {
                md.removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                return md;
            });
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.util.Optional} object.
     */
    protected Optional<IMetadataValue> getLabel(StructElement ele) {
        List<String> fields = this.config.getIIIFLabelFields();
        for (String field : fields) {
            Optional<IMetadataValue> optional = SolrTools.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2).map(md -> {
                md.removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                return md;
            });
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getCollectionURI.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param baseCollectionName a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getCollectionURI(String collectionField, final String baseCollectionName) {
        String urlString;
        if (StringUtils.isNotBlank(baseCollectionName)) {
            urlString =
                    this.urls.path(COLLECTIONS, COLLECTIONS_COLLECTION)
                            .params(collectionField, StringTools.encodeUrl(baseCollectionName))
                            .build();
        } else {
            urlString = this.urls.path(COLLECTIONS).params(collectionField).build();
        }
        return URI.create(urlString);
    }

    /**
     * <p>
     * getRangeURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getRangeURI(String pi, String logId) {
        String urlString = this.urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).params(pi, logId).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getCanvasURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getCanvasURI(String pi, int pageNo) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, pageNo).build();
        return URI.create(urlString);
    }

    /**
     * Get the page order (1-based) from a canavs URI. That is the number in the last path parameter after '/canvas/' If the URI doesn't match a
     * canvas URI, null is returned
     *
     * @param uri a {@link java.net.URI} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPageOrderFromCanvasURI(URI uri) {
        String regex = "/pages/(\\d+)/canvas";
        Matcher matcher = Pattern.compile(regex).matcher(uri.toString());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    /**
     * Get the persistent identifier from a canvas URI. This is the URI path param between '/iiif/manifests/' and '/canvas/'
     *
     * @param uri a {@link java.net.URI} object.
     * @return The pi, or null if the URI doesn't match a iiif canvas URI
     */
    public String getPIFromCanvasURI(URI uri) {
        String regex = "/records/([\\w\\-\\s]+)/pages/(\\d+)/canvas";
        Matcher matcher = Pattern.compile(regex).matcher(uri.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * <p>
     * getAnnotationListURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param openAnnotation
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type, boolean openAnnotation) {
        ApiPath url;
        switch (type) {
            case COMMENT:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(pi, pageNo);
                break;
            case CROWDSOURCING:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
                break;
            default:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_TEXT).params(pi, pageNo);
        }

        if (openAnnotation) {
            url = url.query("format", "oa");
        }

        return URI.create(url.build());
    }

    /**
     * <p>
     * getAnnotationListURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationListURI(String pi, AnnotationType type) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).query("type", type.name()).build();

        return URI.create(urlString);
    }

    /**
     * <p>
     * getCommentAnnotationURI.
     * </p>
     *
     * @param id a long.
     * @return a {@link java.net.URI} object.
     */
    public URI getCommentAnnotationURI(long id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).build();

        return URI.create(urlString);
    }

    /**
     * <p>
     * getImageAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getImageAnnotationURI(String pi, int order) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + "image/1/";
        return URI.create(urlString);
    }

    /**
     * <p>
     * getAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param annoNum a int.
     * @return a {@link java.net.URI} object.
     * @throws java.net.URISyntaxException if any.
     */
    public URI getAnnotationURI(String pi, int order, AnnotationType type, int annoNum) throws URISyntaxException {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + type.name() + "/" + annoNum + "/";
        return URI.create(urlString);
    }

    /**
     * <p>
     * getAnnotationURI.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationURI(String id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build();
        return URI.create(urlString);
    }

    /**
     * Get URL to search service from {@link ApiUrls}
     *
     * @param pi The persistent identifier of the work to search
     * @return the service URI
     */
    public URI getSearchServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_SEARCH).params(pi).build());
    }

    /**
     * Get URL to auto complete service from {@link ApiUrls}
     *
     * @param pi The persistent identifier of the work to search for autocomplete
     * @return the service URI
     */
    public URI getAutoCompleteServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_AUTOCOMPLETE).params(pi).build());
    }

    /**
     * <p>
     * getSearchURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.util.List} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getSearchURI(String pi, String query, List<String> motivation) {
        String uri = getSearchServiceURI(pi).toString();
        uri += ("?q=" + query);
        if (!motivation.isEmpty()) {
            uri += ("&motivation=" + StringUtils.join(motivation, "+"));
        }
        return URI.create(uri);
    }

    /**
     * <p>
     * getAutoSuggestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.util.List} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAutoSuggestURI(String pi, String query, List<String> motivation) {
        String uri = getAutoCompleteServiceURI(pi).toString();
        if (StringUtils.isNotBlank(query)) {
            uri += ("?q=" + query);
            if (!motivation.isEmpty()) {
                uri += ("&motivation=" + StringUtils.join(motivation, "+"));
            }
        }
        return URI.create(uri);
    }

    /**
     * 
     * @param rec
     * @return {@link Manifest3}
     */
    protected Manifest3 createRecordLink(StructElement rec) {
        URI id = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(rec.getPi()).buildURI();
        Manifest3 manifest = new Manifest3(id);
        try {
            manifest.addThumbnail(getThumbnail(rec.getPi()));
        } catch (IndexUnreachableException | PresentationException | ViewerConfigurationException e) {
            logger.error("Error creating thumbnail for record {}", rec.getPi());
        }
        manifest.setLabel(rec.getMultiLanguageDisplayLabel());
        return manifest;
    }

    /**
     * 
     * @param rec
     * @return {@link Collection3}
     */
    protected Collection3 createAnchorLink(StructElement rec) {
        URI id = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(rec.getPi()).buildURI();
        Collection3 manifest = new Collection3(id, null);
        manifest.addBehavior(ViewingHint.multipart);
        manifest.addThumbnail(getThumbnail(rec));
        manifest.setLabel(rec.getMultiLanguageDisplayLabel());
        return manifest;
    }

    protected Metadata getRequiredStatement() {
        Metadata requiredStatement = null;

        String label = this.config.getIIIFRequiredLabel();
        String value = this.config.getIIIFRequiredValue();
        if (StringUtils.isNoneBlank(label, value)) {
            IMetadataValue attributionLabel = ViewerResourceBundle.getTranslations(label, false);
            IMetadataValue attributionValue = ViewerResourceBundle.getTranslations(value, false);
            requiredStatement = new Metadata(attributionLabel, attributionValue);

        }

        return requiredStatement;
    }

    /**
     * @param providerConfig
     * @return {@link IIIFAgent}
     */
    protected IIIFAgent getProvider(ProviderConfiguration providerConfig) {
        IIIFAgent provider = new IIIFAgent(providerConfig.getUri(), ViewerResourceBundle.getTranslations(providerConfig.getLabel(), false));

        providerConfig.getHomepages().forEach(homepageConfig -> {
            IMetadataValue label = ViewerResourceBundle.getTranslations(homepageConfig.getLabel(), false);
            provider.addHomepage(new LabeledResource(homepageConfig.getUri(), "Text", Format.TEXT_HTML.getLabel(), label));
        });

        providerConfig.getLogos()
                .forEach(
                        uri -> provider
                                .addLogo(new ImageResource(uri, ImageFileFormat.getImageFileFormatFromFileExtension(uri.toString()).getMimeType())));

        return provider;
    }

    /**
     * 
     * @param pi
     * @return {@link ImageResource}
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    protected ImageResource getThumbnail(String pi) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ImageResource thumb;
        AbstractApiUrlManager um = DataManager.getInstance().getRestApiManager().getContentApiManager(Version.v2).orElse(null);
        if (um != null) {
            thumb = new ImageResource(urls.path(RECORDS_RECORD, RECORDS_IMAGE).params(pi).build(), thumbWidth, thumbHeight);
        } else {
            thumb = new ImageResource(URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(pi)));
        }
        return thumb;
    }

    /**
     * 
     * @param ele
     * @return {@link ImageResource}
     */
    protected ImageResource getThumbnail(StructElement ele) {
        try {
            String thumbUrl = this.thumbs.getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageResource thumb = new ImageResource(new URI(thumbUrl));
                if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                    String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl);
                    thumb.setService(new ImageInformation3(imageInfoURI));
                }
                return thumb;
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }
        return null;
    }

    protected ImageResource getThumbnail(StructElement ele, int pageNo) {
        try {
            String thumbUrl = this.thumbs.getThumbnailUrl(pageNo, ele.getPi());
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageResource thumb = new ImageResource(new URI(thumbUrl));
                if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                    String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl);
                    thumb.setService(new ImageInformation3(imageInfoURI));
                }
                return thumb;
            }
        } catch (URISyntaxException | IndexUnreachableException | PresentationException | DAOException | ViewerConfigurationException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }
        return null;
    }

    /**
     * @param text
     * @return a simple metadata value with the given text, or null if text is blank
     */
    protected IMetadataValue createLabel(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return new SimpleMetadataValue(text);
    }

    /**
     * 
     * @param path
     * @return File name
     */
    protected String getFilename(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        return StringTools.encodeUrl(Paths.get(path).getFileName().toString());
    }

    /**
     * <p>
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getManifestURI(String pi) {

        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build();
        return getExternalManifestURI(pi).orElse(URI.create(urlString));
    }

    public Optional<URI> getExternalManifestURI(String pi) {
        if (this.config.useExternalManifestUrls()) {
            try {
                Optional<URI> externalURI = readURIFromSolr(pi);
                return externalURI;
            } catch (PresentationException | IndexUnreachableException e) {
                logger.warn("Error reading manifest url from for PI {}", pi);
            } catch (URISyntaxException e) {
                logger.warn("Error reading external manifest uri from record {}: {}", pi, e.toString());
            }
        }
        return Optional.empty();
    }

    private Optional<URI> readURIFromSolr(String pi) throws URISyntaxException, PresentationException, IndexUnreachableException {
        String solrField = this.config.getExternalManifestSolrField();
        if (StringUtils.isNotBlank(solrField)) {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(String.format("PI:%s", pi), List.of(solrField));
            if (doc != null && doc.containsKey(solrField)) {
                String uriString = SolrTools.getMetadataValues(doc, solrField).stream().findAny().orElse("");
                if (StringUtils.isNotBlank(uriString)) {
                    return Optional.of(new URI(uriString));
                }
            }
            return Optional.empty();
        } else {
            throw new PresentationException("No solr field configured containing external manifest urls");
        }
    }

}
