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

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

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
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingType;
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

    /** Constant <code>REQUIRED_SOLR_FIELDS</code> */
    protected static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM, SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
            SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.NUMPAGES,
            SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT, SolrConstants.BOOL_IMAGEAVAILABLE };

    /** Constant <code>UGC_SOLR_FIELDS</code> */

    protected final AbstractApiUrlManager urls;

    private final Map<LinkingProperty.LinkingType, List<LinkingProperty>> linkingProperties = new HashMap<>();

    private final List<Locale> translationLocales = DataManager.getInstance().getConfiguration().getIIIFTranslationLocales();

    /**
     * <p>
     * Constructor for AbstractBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    protected AbstractBuilder(final AbstractApiUrlManager apiUrlManager) {
        AbstractApiUrlManager useApiUrlManager = apiUrlManager;
        if (useApiUrlManager == null) {
            String apiUrl = DataManager.getInstance().getConfiguration().getIIIFApiUrl();
            apiUrl = apiUrl.replace("/rest", "/api/v1");
            useApiUrlManager = new ApiUrls(apiUrl);
        }
        this.urls = useApiUrlManager;
        this.initLinkingProperties();
    }

    /**
     * Read config for rendering linking properties and add configured properties to linkingProperties map
     */
    private void initLinkingProperties() {
        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF());
            addRendering(LinkingTarget.PDF, label);
        }
        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer());
            addRendering(LinkingTarget.VIEWER, label);
        }
        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext());
            addRendering(LinkingTarget.PLAINTEXT, label);
        }
        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto());
            addRendering(LinkingTarget.ALTO, label);
        }
    }

    /**
     * @param value
     * @return {@link IMetadataValue}
     */
    protected IMetadataValue getLabel(String value) {
        return ViewerResourceBundle.getTranslations(value, this.translationLocales, false);
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
    public String getMetsResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/metsresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return LIDO resolver link for the DFG Viewer
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public String getLidoResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/lidoresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get LIDO resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/lidoresolver?id=" + 0;
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
     * Simple method to create a label for a {@link org.apache.solr.common.SolrDocument} from {@link io.goobi.viewer.controller.SolrConstants.LABEL},
     * {@link io.goobi.viewer.controller.SolrConstants.TITLE} or {@link io.goobi.viewer.controller.SolrConstants.DOCSTRUCT}
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
    public void addMetadata(AbstractPresentationModelElement2 manifest, StructElement ele) {
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        List<String> eventFields = DataManager.getInstance().getConfiguration().getIIIFEventFields();
        displayFields.addAll(eventFields);

        for (String field : getMetadataFields(ele)) {
            if (contained(field, displayFields) && !field.endsWith(SolrConstants.SUFFIX_UNTOKENIZED) && !field.matches(".*_LANG_\\w{2,3}")) {
                String configuredLabel = DataManager.getInstance().getConfiguration().getIIIFMetadataLabel(field);
                String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel
                        : (field.contains("/") ? field.substring(field.indexOf("/") + 1) : field);
                SolrTools.getTranslations(field, ele, this.translationLocales, (s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(getLabel(label), value))
                        .ifPresent(md -> {
                            md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            manifest.addMetadata(md);
                        });
            }
        }
    }

    /**
     * Add the annotations from the crowdsourcingAnnotations map to the respective canvases in the canvases list as well as to the given annotationMap
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
     * Return true if the field is contained in displayFields, accounting for wildcard characters
     *
     * @param field
     * @param displayFields
     * @return true if displayFields contains field; false otherwise
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
     * @param displayFields
     * @param locales
     * @return List<String> (immutable!)
     */
    protected static List<String> addLanguageFields(List<String> displayFields, List<Locale> locales) {
        return displayFields.stream().flatMap(field -> getLanguageFields(field, locales, true).stream()).toList();
    }

    /**
     * 
     * @param field
     * @param locales
     * @param includeSelf
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
     * @param ele
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
     * @param pi a {@link java.lang.String} object.
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
                    StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
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
     * <p>
     * getEventFields.
     * </p>
     *
     * @return a {@link java.util.Map} object.
     */
    protected Map<String, List<String>> getEventFields() {
        List<String> eventStrings = DataManager.getInstance().getConfiguration().getIIIFEventFields();
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
     * <p>
     * getDocument.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getDocument(String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("getDocument: {}", pi); //NOSONAR Debug
        String query = "PI:" + pi;
        // List<String> displayFields = addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
        if (doc != null) {
            StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
            ele.setImageNumber(1);
            return ele;
        }
        return null;
    }

    /**
     * <p>
     * getSolrFieldList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSolrFieldList() {
        Set<String> fields = new HashSet<>(DataManager.getInstance().getConfiguration().getIIIFMetadataFields());
        fields.addAll(Arrays.asList(REQUIRED_SOLR_FIELDS));
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField)) {
            fields.add(navDateField);
        }
        fields.addAll(DataManager.getInstance().getConfiguration().getIIIFMetadataFields());
        fields.addAll(DataManager.getInstance().getConfiguration().getIIIFDescriptionFields());
        fields.addAll(DataManager.getInstance().getConfiguration().getIIIFLabelFields());
        return new ArrayList<>(fields);
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
        List<String> fields = DataManager.getInstance().getConfiguration().getIIIFDescriptionFields();
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
        List<String> fields = DataManager.getInstance().getConfiguration().getIIIFLabelFields();
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
     * <p>
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getManifestURI(String pi) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build();
        return URI.create(urlString);
    }
    
    /**
     * <p>
     * getManifestURI for page
     * </p>
     *
     * @param pi Persistent identifier of a record
     * @param 1-based page order within the record
     * @return a {@link java.net.URI} object.
     */
    public URI getPageManifestURI(String pi, int pageNo) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_MANIFEST).params(pi, pageNo).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param mode a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getManifestURI(String pi, BuildMode mode) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).query("mode", mode.name()).build();
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
     * getSequenceURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getSequenceURI(String pi, final String label) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(pi, StringUtils.isBlank(label) ? "basic" : label).build();
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
     * Get the page order (1-based) from a canavs URI. That is the number in the last path paramter after '/canvas/' If the URI doesn't match a canvas
     * URI, null is returned
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
     * getLayerURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getLayerURI(String pi, AnnotationType type) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_LAYER).params(pi, type.name()).build();
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
