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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.COLLECTIONS_COLLECTION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_LAYER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_TEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingType;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Abstract base class for IIIF Presentation API v2 resource builders, providing shared URI construction and metadata helpers.
 *
 * @author Florian Alpers
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractBuilder.class);

    /** Constant <code>REQUIRED_SOLR_FIELDS</code>. */
    protected static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM, SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
            SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.NUMPAGES,
            SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT, SolrConstants.BOOL_IMAGEAVAILABLE };

    /** Constant <code>UGC_SOLR_FIELDS</code>. */

    protected final AbstractApiUrlManager urls;

    protected final Configuration config;

    private final Map<LinkingProperty.LinkingType, List<LinkingProperty>> linkingProperties = new HashMap<>();

    private final List<Locale> translationLocales;

    /**
     * Creates a new AbstractBuilder instance.
     *
     * @param apiUrlManager API URL manager for building IIIF resource URIs
     */
    protected AbstractBuilder(final AbstractApiUrlManager apiUrlManager) {
        this(apiUrlManager, DataManager.getInstance().getConfiguration());
    }

    /**
     * Creates a new AbstractBuilder instance.
     *
     * @param apiUrlManager API URL manager for building IIIF resource URIs
     * @param config viewer configuration instance
     */
    protected AbstractBuilder(final AbstractApiUrlManager apiUrlManager, Configuration config) {
        AbstractApiUrlManager useApiUrlManager = apiUrlManager;
        if (useApiUrlManager == null) {
            String apiUrl = config.getIIIFApiUrl();
            apiUrl = apiUrl.replace("/rest", "/api/v1");
            useApiUrlManager = new ApiUrls(apiUrl);
        }
        this.translationLocales = config.getIIIFTranslationLocales();
        this.urls = useApiUrlManager;
        this.config = config;
        this.initLinkingProperties();
    }

    /**
     * Read config for rendering linking properties and add configured properties to linkingProperties map.
     */
    private void initLinkingProperties() {
        if (this.config.isVisibleIIIFRenderingPDF()) {
            IMetadataValue label = getLabel(this.config.getLabelIIIFRenderingPDF());
            addRendering(LinkingTarget.PDF, label);
        }
        if (this.config.isVisibleIIIFRenderingViewer()) {
            IMetadataValue label = getLabel(this.config.getLabelIIIFRenderingViewer());
            addRendering(LinkingTarget.VIEWER, label);
        }
        if (this.config.isVisibleIIIFRenderingPlaintext()) {
            IMetadataValue label = getLabel(this.config.getLabelIIIFRenderingPlaintext());
            addRendering(LinkingTarget.PLAINTEXT, label);
        }
        if (this.config.isVisibleIIIFRenderingAlto()) {
            IMetadataValue label = getLabel(this.config.getLabelIIIFRenderingAlto());
            addRendering(LinkingTarget.ALTO, label);
        }
    }

    /**
     * @param value message key or literal string to translate
     * @return {@link IMetadataValue}
     */
    protected IMetadataValue getLabel(String value) {
        return ViewerResourceBundle.getTranslations(value, this.translationLocales, false);
    }

    /**
     * @param uri the URI to make absolute; returned unchanged if already absolute
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

    public URI absolutize(String uri) {
        return absolutize(URI.create(uri));
    }

    /**
     * getLocale.
     *
     * @param language BCP 47 language tag string
     * @return the Locale for the given language tag, defaulting to English if not resolvable
     */
    protected Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    /**
     * getMetsResolverUrl.
     *
     * @param ele structure element whose PI is used to build the URL
     * @return METS resolver link for the DFG Viewer
     */
    public String getMetsResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/metsresolver?id=" + ele.getPi();
        } catch (NullPointerException e) {
            logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * getLidoResolverUrl.
     *
     * @param ele structure element whose PI is used to build the URL
     * @return LIDO resolver link for the DFG Viewer
     */
    public String getLidoResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/lidoresolver?id=" + ele.getPi();
        } catch (NullPointerException e) {
            logger.error("Could not get LIDO resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/lidoresolver?id=" + 0;
    }

    /**
     * getViewUrl.
     *
     * @param ele physical page element whose PURL part is appended to the URL
     * @param pageType page type determining the viewer URL prefix
     * @return viewer url for the given page in the given {@link io.goobi.viewer.model.viewer.PageType}
     */
    public String getViewUrl(PhysicalElement ele, PageType pageType) {
        try {
            return urls.getApplicationUrl() + "/" + pageType.getName() + ele.getPurlPart();
        } catch (NullPointerException e) {
            logger.error("Could not get METS resolver URL for page {} + in {}.", ele.getOrder(), ele.getPi());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * Simple method to create a label for a {@link org.apache.solr.common.SolrDocument} from {@link io.goobi.viewer.solr.SolrConstants#LABEL},
     * {@link io.goobi.viewer.solr.SolrConstants#TITLE} or {@link io.goobi.viewer.solr.SolrConstants#DOCSTRCT}.
     *
     * @param solrDocument Solr document to extract label information from
     * @return an Optional containing the label derived from LABEL, TITLE, or DOCSTRCT fields, or empty if none are present
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
     * addMetadata.
     *
     * @param manifest IIIF presentation element to attach metadata to
     * @param ele structure element providing the metadata values
     */
    public void addMetadata(AbstractPresentationModelElement2 manifest, StructElement ele) {
        List<String> displayFields = this.config.getIIIFMetadataFields();
        List<String> eventFields = this.config.getIIIFEventFields();
        displayFields.addAll(eventFields);

        for (String field : getMetadataFields(ele)) {
            // Use SolrTools.isLanguageCodedField() instead of per-call Pattern.compile() via String.matches()
            if (contained(field, displayFields) && !field.endsWith(SolrConstants.SUFFIX_UNTOKENIZED) && !SolrTools.isLanguageCodedField(field)) {
                String configuredLabel = this.config.getIIIFMetadataLabel(field);
                String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel
                        : (field.contains("/") ? field.substring(field.indexOf("/") + 1) : field);
                SolrTools.getTranslations(field, ele, this.translationLocales, (s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(getLabel(label), value))
                        .filter(value -> !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(value.getValue().getValue().get()))
                        .ifPresent(md -> {
                            md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            manifest.addMetadata(md);
                        });
            }
        }
    }

    /**
     * Adds the annotations from the crowdsourcingAnnotations map to the respective canvases in the
     * canvases list as well as to the given annotationMap.
     *
     * @param canvases The list of canvases which should receive the annotations as otherContent
     * @param crowdsourcingAnnotations A map of annotations by page number
     * @param annotationMap A global annotation map for a whole manifest; may be null if not needed
     */
    public void addCrowdourcingAnnotations(List<Canvas2> canvases, Map<Integer, List<OpenAnnotation>> crowdsourcingAnnotations,
            Map<AnnotationType, List<AnnotationList>> annotationMap) {

        for (Canvas2 canvas : canvases) {
            Integer order = this.getPageOrderFromCanvasURI(canvas.getId());
            String pi = this.getPIFromCanvasURI(canvas.getId());
            if (crowdsourcingAnnotations.containsKey(order)) {
                AnnotationList crowdList = new AnnotationList(getAnnotationListURI(pi, order, AnnotationType.CROWDSOURCING, true));
                crowdList.setLabel(getLabel(AnnotationType.CROWDSOURCING.name()));
                List<OpenAnnotation> annos = crowdsourcingAnnotations.get(order);
                annos.forEach(anno -> crowdList.addResource(anno));
                canvas.addOtherContent(crowdList);
                if (annotationMap != null) {
                    List<AnnotationList> crowdAnnos = annotationMap.get(AnnotationType.CROWDSOURCING);
                    if (crowdAnnos == null) {
                        crowdAnnos = new ArrayList<>();
                        annotationMap.put(AnnotationType.CROWDSOURCING, crowdAnnos);
                    }
                    crowdAnnos.add(crowdList);
                }
            }
        }
    }

    /**
     * Return true if the field is contained in displayFields, accounting for wildcard characters.
     *
     * @param field Solr field name to look up
     * @param displayFields list of configured display field names (may contain wildcards)
     * @return true if displayFields contains field; false otherwise
     * @should return true for given input
     */
    protected boolean contained(String field, List<String> displayFields) {
        return displayFields.stream().anyMatch(displayField -> matches(field, displayField));
    }

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
     * @param displayFields list of base Solr field names to expand
     * @param locales locales for which language-specific field variants should be added
     * @return List<String> (immutable!)
     */
    protected static List<String> addLanguageFields(List<String> displayFields, List<Locale> locales) {
        return displayFields.stream().flatMap(field -> getLanguageFields(field, locales, true).stream()).toList();
    }

    /**
     * 
     * @param field base Solr field name to expand with language variants
     * @param locales locales for which language-specific field names are generated
     * @param includeSelf if true, includes the base field name itself in the result
     * @return List<String>
     */
    private static List<String> getLanguageFields(String field, List<Locale> locales, boolean includeSelf) {
        List<String> fields = new ArrayList<>();
        if (includeSelf) {
            fields.add(field);
        }
        fields.addAll(locales.stream()
                .map(Locale::getLanguage)
                .map(String::toUpperCase)
                .map(string -> field.concat("_LANG_").concat(string))
                .toList());
        return fields;
    }

    /**
     * @param ele structure element whose metadata field names are retrieved
     * @return List<String> (immutable!)
     */
    private static List<String> getMetadataFields(StructElement ele) {
        Set<String> fields = ele.getMetadataFields().keySet();
        return fields.stream().map(field -> field.replaceAll("_LANG_\\w{2,3}$", "")).distinct().toList();
    }

    /**
     * Queries all DocStructs which have the given PI as PI_TOPSTRUCT or anchor (or are the anchor themselves). Works are sorted by a
     * {@link io.goobi.viewer.model.iiif.presentation.v2.builder.StructElementComparator} If no hits are found, an empty list is returned.
     *
     * @param pi persistent identifier of the top-level record
     * @return A list of all docstructs with the given pi or children thereof. An empty list if no hits are found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<StructElement> getDocumentWithChildren(String pi) throws PresentationException, IndexUnreachableException {
        String anchorQuery = "(ISWORK:* AND PI_PARENT:" + pi + ") OR (ISANCHOR:* AND PI:" + pi + ")";
        String workQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:DOCSTRCT";
        String query = "(" + anchorQuery + ") OR (" + workQuery + ")";
        List<String> displayFields = new ArrayList<>();
        displayFields.addAll(addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales()));

        // handle metadata fields from events
        Map<String, List<String>> eventFields = getEventFields();
        if (!eventFields.isEmpty()) {
            String eventQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:EVENT";
            query += " OR (" + eventQuery + ")";
            displayFields.addAll(eventFields.values().stream().flatMap(Collection::stream).toList());
            displayFields.add(SolrConstants.EVENTTYPE);
        }

        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, null);
        List<StructElement> eles = new ArrayList<>();
        List<SolrDocument> events = new ArrayList<>();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                if ("EVENT".equals(doc.get(SolrConstants.DOCTYPE))) {
                    events.add(doc);
                } else {
                    StructElement ele = new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
                    eles.add(ele);
                    try {
                        Integer pageNo = (Integer) doc.getFieldValue(SolrConstants.THUMBPAGENO);
                        ele.setImageNumber(pageNo);
                        // Integer numPages = (Integer) doc.getFieldValue(SolrConstants.NUMPAGES);
                    } catch (NullPointerException | ClassCastException e) {
                        ele.setImageNumber(1);
                    }
                }
            }
        }
        Collections.sort(eles, new StructElementComparator());
        addEventMetadataToWorkElement(eles, events);
        return eles;
    }

    /**
     * Adds all metadata from the given events to the first work document contained in eles. All metadata will be attached twice, once in the form
     * "/[fieldName]" and once in the form "[eventType]/[fieldName]"
     *
     * @param eles The list of StructElements from which to select the first work document. All metadata are attached to this document
     * @param events The list of event SolrDocuments from which to take the metadata
     */
    protected void addEventMetadataToWorkElement(List<StructElement> eles, List<SolrDocument> events) {
        Optional<StructElement> mainO = eles.stream().filter(ele -> ele.isWork()).findFirst();
        if (mainO.isPresent()) {
            StructElement main = mainO.get();
            for (SolrDocument event : events) {
                String eventType = event.getFieldValue(SolrConstants.EVENTTYPE).toString();
                Map<String, List<String>> mds = main.getMetadataFields();
                for (String eventField : event.getFieldNames()) {
                    Collection<Object> fieldValues = event.getFieldValues(eventField);
                    List<String> fieldValueList = new ArrayList<>(fieldValues.stream().map(SolrTools::getAsString).toList());
                    // add the event field twice to the md-list: Once for unspecified event type and
                    // once for the specific event type
                    mds.put("/" + eventField, fieldValueList);
                    mds.put(eventType + "/" + eventField, fieldValueList);
                }
            }
        }
    }

    /**
     * getEventFields.
     *
     * @return a map of event type names to their associated Solr field names, as configured for IIIF
     * @should return non null result
     */
    protected Map<String, List<String>> getEventFields() {
        List<String> eventStrings = this.config.getIIIFEventFields();
        Map<String, List<String>> events = new HashMap<>();
        for (String string : eventStrings) {
            String event;
            String field;
            int separatorIndex = string.indexOf("/");
            if (separatorIndex > -1) {
                event = string.substring(0, separatorIndex);
                field = string.substring(separatorIndex + 1);
            } else {
                event = "";
                field = string;
            }
            List<String> eventFields = events.computeIfAbsent(event, k -> new ArrayList<>());
            eventFields.add(field);
        }
        return events;
    }

    /**
     * getDocument.
     *
     * @param pi persistent identifier of the record to load
     * @return the StructElement for the given PI, or null if the document was not found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getDocument(String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("getDocument: {}", pi); //NOSONAR Debug
        String query = "PI:" + pi;
        // List<String> displayFields = addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
        if (doc != null) {
            StructElement ele = new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
            ele.setImageNumber(1);
            return ele;
        }
        return null;
    }

    /**
     * getSolrFieldList.
     *
     * @return a list of Solr field names required for IIIF presentation data retrieval
     */
    public List<String> getSolrFieldList() {
        Set<String> fields = new HashSet<>(this.config.getIIIFMetadataFields());
        fields.addAll(Arrays.asList(REQUIRED_SOLR_FIELDS));
        String navDateField = this.config.getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField)) {
            fields.add(navDateField);
        }
        fields.addAll(this.config.getIIIFMetadataFields());
        fields.addAll(this.config.getIIIFDescriptionFields());
        fields.addAll(this.config.getIIIFLabelFields());
        return new ArrayList<>(fields);
    }

    /**
     * Gets the attribution text configured in webapi.iiif.attribution and returns all translations if any are found, or the configured string itself
     * otherwise.
     *
     * @return the configured attribution
     */
    protected List<IMetadataValue> getAttributions() {
        VariableReplacer variableReplacer = new VariableReplacer(DataManager.getInstance().getConfiguration());
        return DataManager.getInstance()
                .getConfiguration()
                .getIIIFAttribution()
                .stream()
                .map(variableReplacer::replace)
                .flatMap(List::stream)
                .filter(StringUtils::isNotBlank)
                .map(this::getLabel)
                .collect(Collectors.toList());
    }

    /**
     * getDescription.
     *
     * @param ele structure element to extract description fields from
     * @return an Optional containing the description metadata value, or empty if no configured field has a value
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
     * getDescription.
     *
     * @param ele structure element to extract label fields from
     * @return an Optional containing the label metadata value, or empty if no configured field has a value
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
     * getCollectionURI.
     *
     * @param collectionField Solr field used to group the collection
     * @param baseCollectionName top-level collection name, or blank for root collection
     * @return the URI identifying the IIIF collection resource
     */
    public URI getCollectionURI(String collectionField, final String baseCollectionName) {
        String urlString;
        String useBaseCollectionName = baseCollectionName;
        if (StringUtils.isNotBlank(useBaseCollectionName)) {
            useBaseCollectionName = StringTools.encodeUrl(useBaseCollectionName);
            urlString = this.urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, useBaseCollectionName).build();
        } else {
            urlString = this.urls.path(COLLECTIONS).params(collectionField).build();
        }
        return URI.create(urlString);
    }

    /**
     * getManifestURI.
     *
     * @param pi persistent identifier of the record
     * @return the manifest URI for the given record, using an external URL if configured
     * @should not contain placeholder when valid pi
     */
    public URI getManifestURI(String pi) {
        // A null or blank PI causes the {pi} URL placeholder to remain unsubstituted, which
        // makes URI.create() throw IllegalArgumentException. Fail early with a clear message.
        if (StringUtils.isBlank(pi)) {
            throw new IllegalArgumentException("Cannot build manifest URI: PI is null or blank");
        }
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

    /**
     * getManifestURI for page.
     *
     * @param pi Persistent identifier of a record
     * @param pageNo 1-based page order within the record
     * @return the manifest URI for the specified page of the given record
     */
    public URI getPageManifestURI(String pi, int pageNo) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_MANIFEST).params(pi, pageNo).build();
        return URI.create(urlString);
    }

    /**
     * getManifestURI.
     *
     * @param pi persistent identifier of the record
     * @param mode build mode appended as a query parameter
     * @return the manifest URI for the given record with the build mode as a query parameter
     */
    public URI getManifestURI(String pi, BuildMode mode) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).query("mode", mode.name()).build();
        return URI.create(urlString);
    }

    /**
     * getRangeURI.
     *
     * @param pi persistent identifier of the record
     * @param logId logical structure element ID for the range
     * @return the range URI for the given record and logical structure element
     */
    public URI getRangeURI(String pi, String logId) {
        String urlString = this.urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).params(pi, logId).build();
        return URI.create(urlString);
    }

    /**
     * getSequenceURI.
     *
     * @param pi persistent identifier of the record
     * @param label sequence label; defaults to "basic" if blank
     * @return the sequence URI for the given record and label
     */
    public URI getSequenceURI(String pi, final String label) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(pi, StringUtils.isBlank(label) ? "basic" : label).build();
        return URI.create(urlString);
    }

    /**
     * getCanvasURI.
     *
     * @param pi persistent identifier of the record
     * @param pageNo 1-based physical page order number
     * @return the canvas URI for the given page of the record
     */
    public URI getCanvasURI(String pi, int pageNo) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, pageNo).build();
        return URI.create(urlString);
    }

    /**
     * Get the page order (1-based) from a canvas URI. That is the number in the last path parameter after '/canvas/'
     * If the URI doesn't match a canvas URI, null is returned
     *
     * @param uri canvas URI to extract the page order from
     * @return the 1-based page order number extracted from the canvas URI, or null if the URI does not match
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
     * @param uri canvas URI to extract the persistent identifier from
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
     * getAnnotationListURI.
     *
     * @param pi persistent identifier of the record
     * @param pageNo 1-based physical page order number
     * @param type annotation type determining the URL path
     * @param openAnnotation if true, appends a format=oa query parameter for Open Annotation format
     * @return the annotation list URI for the given record page and annotation type
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
            case ALTO:
            case FULLTEXT:
            default:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_TEXT).params(pi, pageNo);
        }

        if (openAnnotation) {
            url = url.query("format", "oa");

        }

        return URI.create(url.build());
    }

    /**
     * getAnnotationListURI.
     *
     * @param pi persistent identifier of the record
     * @param type annotation type appended as a query parameter
     * @return the annotation list URI for the given record and annotation type
     */
    public URI getAnnotationListURI(String pi, AnnotationType type) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).query("type", type.name()).build();

        return URI.create(urlString);
    }

    /**
     * getCommentAnnotationURI.
     *
     * @param id database ID of the comment annotation
     * @return the URI identifying the comment annotation with the given ID
     */
    public URI getCommentAnnotationURI(long id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).build();

        return URI.create(urlString);
    }

    /**
     * getLayerURI.
     *
     * @param pi persistent identifier of the record
     * @param type annotation type identifying the layer
     * @return the layer URI for the given record and annotation type
     */
    public URI getLayerURI(String pi, AnnotationType type) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_LAYER).params(pi, type.name()).build();
        return URI.create(urlString);
    }

    /**
     * getImageAnnotationURI.
     *
     * @param pi persistent identifier of the record
     * @param order 1-based physical page order number
     * @return the image annotation URI for the given page of the record
     */
    public URI getImageAnnotationURI(String pi, int order) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + "image/1/";
        return URI.create(urlString);
    }

    /**
     * getAnnotationURI.
     *
     * @param pi persistent identifier of the record
     * @param order 1-based physical page order number
     * @param type annotation type used as a path segment
     * @param annoNum 1-based annotation index within the page
     * @return the URI identifying the specific annotation on the given page
     * @throws java.net.URISyntaxException if any.
     */
    public URI getAnnotationURI(String pi, int order, AnnotationType type, int annoNum) throws URISyntaxException {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + type.name() + "/" + annoNum + "/";
        return URI.create(urlString);
    }

    /**
     * getAnnotationURI.
     *
     * @param id annotation ID used as a path parameter
     * @return the URI identifying the annotation with the given ID
     */
    public URI getAnnotationURI(String id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build();
        return URI.create(urlString);
    }

    /**
     * Get URL to search service from {@link ApiUrls}.
     *
     * @param pi The persistent identifier of the work to search
     * @return the service URI
     */
    public URI getSearchServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_SEARCH).params(pi).build());
    }

    /**
     * Get URL to auto complete service from {@link ApiUrls}.
     *
     * @param pi The persistent identifier of the work to search for autocomplete
     * @return the service URI
     */
    public URI getAutoCompleteServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_AUTOCOMPLETE).params(pi).build());
    }

    /**
     * getSearchURI.
     *
     * @param pi persistent identifier of the record to search within
     * @param query search term appended as the q parameter
     * @param motivation list of motivation strings to filter results
     * @return the search URI for the given record with the query and motivation parameters
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
     * getAutoSuggestURI.
     *
     * @param pi persistent identifier of the record to autocomplete within
     * @param query partial search term for autocomplete suggestions
     * @param motivation list of motivation strings to filter suggestions
     * @return the autocomplete URI for the given record with the query and motivation parameters
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

    public AbstractBuilder addSeeAlso(LinkingProperty.LinkingTarget target, IMetadataValue label) {
        LinkingType type = LinkingType.SEE_ALSO;
        addLinkingProperty(target, label, type);
        return this;
    }

    public AbstractBuilder addRendering(LinkingProperty.LinkingTarget target, IMetadataValue label) {
        LinkingType type = LinkingType.RENDERING;
        addLinkingProperty(target, label, type);
        return this;
    }

    private void addLinkingProperty(LinkingProperty.LinkingTarget target, IMetadataValue label, LinkingType type) {
        LinkingProperty property = new LinkingProperty(type, target, label);
        List<LinkingProperty> seeAlsos = this.linkingProperties.computeIfAbsent(type, k -> new ArrayList<>());
        seeAlsos.add(property);
    }

    public List<LinkingProperty> getSeeAlsos() {
        List<LinkingProperty> seeAlsos = this.linkingProperties.get(LinkingType.SEE_ALSO);
        if (seeAlsos != null) {
            return seeAlsos;
        }

        return Collections.emptyList();
    }

    public List<LinkingProperty> getRenderings() {
        List<LinkingProperty> renderings = this.linkingProperties.get(LinkingType.RENDERING);
        if (renderings != null) {
            return renderings;
        }

        return Collections.emptyList();
    }

}
