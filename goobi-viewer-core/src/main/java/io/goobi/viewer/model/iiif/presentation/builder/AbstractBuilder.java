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
package io.goobi.viewer.model.iiif.presentation.builder;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_UGC;
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
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;

import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.JSONResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.oa.FragmentSelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResourceURI;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * Abstract AbstractBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);

    /** Constant <code>REQUIRED_SOLR_FIELDS</code> */
    public static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM, SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
            SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.NUMPAGES,
            SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT };

    /** Constant <code>UGC_SOLR_FIELDS</code> */
    public static final String[] UGC_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.UGCTYPE,
            SolrConstants.MD_TEXT, SolrConstants.UGCCOORDS, SolrConstants.MD_BODY, SolrConstants.UGCTERMS };

    protected final AbstractApiUrlManager urls;
    protected final AnnotationsResourceBuilder annoBuilder;

    /**
     * <p>
     * Constructor for AbstractBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public AbstractBuilder(AbstractApiUrlManager apiUrlManager) {
        if (apiUrlManager == null) {
            String apiUrl = DataManager.getInstance().getConfiguration().getIIIFApiUrl();
            apiUrl = apiUrl.replace("/rest", "/api/v1");
            apiUrlManager = new ApiUrls(apiUrl);
        }
        this.urls = apiUrlManager;
        this.annoBuilder = new AnnotationsResourceBuilder(this.urls);
    }

    /**
     * @param iconURI
     * @return
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
    public static Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {

        String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
        String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
        String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);

        if (StringUtils.isNotBlank(label)) {
            return Optional.of(new SimpleMetadataValue(label));
        } else if (StringUtils.isNotBlank(title)) {
            return Optional.of(new SimpleMetadataValue(title));
        } else if (StringUtils.isNotBlank(docStruct)) {
            return Optional.of(ViewerResourceBundle.getTranslations(docStruct));
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
    public void addMetadata(AbstractPresentationModelElement manifest, StructElement ele) {
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        List<String> eventFields = DataManager.getInstance().getConfiguration().getIIIFEventFields();
        displayFields.addAll(eventFields);

        for (String field : getMetadataFields(ele)) {
            if (contained(field, displayFields) && !field.endsWith(SolrConstants._UNTOKENIZED) && !field.matches(".*_LANG_\\w{2,3}")) {
                String configuredLabel = DataManager.getInstance().getConfiguration().getIIIFMetadataLabel(field);
                String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel
                        : (field.contains("/") ? field.substring(field.indexOf("/") + 1) : field);
                SolrSearchIndex.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(ViewerResourceBundle.getTranslations(label), value))
                        .ifPresent(md -> {
                            md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            manifest.addMetadata(md);
                        });
            }
        }
    }

    /**
     * Return true if the field is contained in displayFields, accounting for wildcard characters
     * 
     * @param field
     * @param displayFields
     * @return
     */
    private static boolean contained(String field, List<String> displayFields) {

        return displayFields.stream().map(displayField -> displayField.replace("*", "")).anyMatch(displayField -> field.startsWith(displayField));
    }

    /**
     * @param displayFields
     * @param allLocales
     * @return
     */
    private static List<String> addLanguageFields(List<String> displayFields, List<Locale> locales) {
        return displayFields.stream().flatMap(field -> getLanguageFields(field, locales, true).stream()).collect(Collectors.toList());
    }

    private static List<String> getLanguageFields(String field, List<Locale> locales, boolean includeSelf) {
        List<String> fields = new ArrayList<>();
        if (includeSelf) {
            fields.add(field);
        }
        fields.addAll(locales.stream()
                .map(Locale::getLanguage)
                .map(String::toUpperCase)
                .map(string -> field.concat("_LANG_").concat(string))
                .collect(Collectors.toList()));
        return fields;
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
     * Queries all DocStructs which have the given PI as PI_TOPSTRUCT or anchor (or are the anchor themselves). Works are sorted by a
     * {@link io.goobi.viewer.model.iiif.presentation.builder.StructElementComparator} If no hits are found, an empty list is returned
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
        List<String> displayFields = addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales());

        // handle metadata fields from events
        Map<String, List<String>> eventFields = getEventFields();
        if (!eventFields.isEmpty()) {
            String eventQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:EVENT";
            query += " OR (" + eventQuery + ")";
            displayFields.addAll(eventFields.values().stream().flatMap(value -> value.stream()).collect(Collectors.toList()));
            displayFields.add(SolrConstants.EVENTTYPE);
        }

        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, displayFields);
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
        Optional<StructElement> main_o = eles.stream().filter(ele -> ele.isWork()).findFirst();
        if (main_o.isPresent()) {
            StructElement main = main_o.get();
            for (SolrDocument event : events) {
                String eventType = event.getFieldValue(SolrConstants.EVENTTYPE).toString();
                Map<String, List<String>> mds = main.getMetadataFields();
                for (String eventField : event.getFieldNames()) {
                    Collection<Object> fieldValues = event.getFieldValues(eventField);
                    List<String> fieldValueList = fieldValues.stream().map(Object::toString).collect(Collectors.toList());
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
            String event, field;
            int separatorIndex = string.indexOf("/");
            if (separatorIndex > -1) {
                event = string.substring(0, separatorIndex);
                field = string.substring(separatorIndex + 1);
            } else {
                event = "";
                field = string;
            }
            List<String> eventFields = events.get(event);
            if (eventFields == null) {
                eventFields = new ArrayList<>();
                events.put(event, eventFields);
            }
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
        String query = "PI:" + pi;
        List<String> displayFields = addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, displayFields);
        if (doc != null) {
            StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
            ele.setImageNumber(1);
            return ele;
        }
        return null;
    }

    /**
     * Get all annotations for the given PI from the SOLR index, sorted by page number. The annotations are stored as DOCTYPE:UGC in the SOLR and are
     * converted to OpenAnnotations here
     *
     * @param pi The persistent identifier of the work to query
     * @return A map of page numbers (1-based) mapped to a list of associated annotations
     * @param urlOnlyTarget a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Map<Integer, List<OpenAnnotation>> getCrowdsourcingAnnotations(String pi, boolean urlOnlyTarget)
            throws PresentationException, IndexUnreachableException {
        String query = "DOCTYPE:UGC AND PI_TOPSTRUCT:" + pi;
        List<String> displayFields = addLanguageFields(Arrays.asList(UGC_SOLR_FIELDS), ViewerResourceBundle.getAllLocales());
        SolrDocumentList ugcDocs = DataManager.getInstance().getSearchIndex().getDocs(query, displayFields);
        Map<Integer, List<OpenAnnotation>> annoMap = new HashMap<>();
        if (ugcDocs != null && !ugcDocs.isEmpty()) {
            for (SolrDocument doc : ugcDocs) {
                OpenAnnotation anno = createUGCOpenAnnotation(pi, doc, urlOnlyTarget);
                Integer page = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
                List<OpenAnnotation> annoList = annoMap.get(page);
                if (annoList == null) {
                    annoList = new ArrayList<>();
                    annoMap.put(page, annoList);
                }
                annoList.add(anno);
            }
        }
        return annoMap;
    }

    public IAnnotation getCrowdsourcingAnnotation(String id) throws PresentationException, IndexUnreachableException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +IDDOC:").append(id);

        SolrDocumentList docList = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString());
        if (docList != null && !docList.isEmpty()) {
            SolrDocument doc = docList.get(0);
            IAnnotation anno = createUGCOpenAnnotation(doc, false);
            return anno;
        }

        return null;
    }

    /**
     * <p>
     * createOpenAnnotation.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     */
    public OpenAnnotation createUGCOpenAnnotation(SolrDocument doc, boolean urlOnlyTarget) {
        String pi = Optional.ofNullable(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).map(Object::toString).orElse("");
        return createUGCOpenAnnotation(pi, doc, urlOnlyTarget);

    }

    /**
     * <p>
     * createOpenAnnotation.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     */
    public OpenAnnotation createUGCOpenAnnotation(String pi, SolrDocument doc, boolean urlOnlyTarget) {
        OpenAnnotation anno;
        String iddoc = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC)).map(Object::toString).orElse("");
        String coordString = Optional.ofNullable(doc.getFieldValue(SolrConstants.UGCCOORDS)).map(Object::toString).orElse("");
        Integer pageOrder = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
        URI annoURI = URI.create(urls.path(ApiUrls.ANNOTATIONS, ANNOTATIONS_UGC).params(iddoc).build());
        anno = new OpenAnnotation(annoURI);

        IResource body = null;
        if (doc.containsKey(SolrConstants.MD_BODY)) {
            String bodyString = readSolrField(doc, doc.getFieldValue(SolrConstants.MD_BODY));
            try {
                JSONObject json = new JSONObject(bodyString);
                body = new JSONResource(json);
            } catch (JSONException e) {
                logger.error("Error building annotation body from '" + bodyString + "'");
            }
        } else if (doc.containsKey(SolrConstants.MD_TEXT)) {
            String text = readSolrField(doc, doc.getFieldValue(SolrConstants.MD_TEXT));
            body = new TextualResource(text);
        }
        anno.setBody(body);

        try {
            FragmentSelector selector = new FragmentSelector(coordString);
            if (urlOnlyTarget) {
                anno.setTarget(new SpecificResourceURI(this.getCanvasURI(pi, pageOrder), selector));
            } else {
                anno.setTarget(new SpecificResource(this.getCanvasURI(pi, pageOrder), selector));
            }
        } catch (IllegalArgumentException e) {
            //old UGC coords format
            String regex = "([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+)";
            Matcher matcher = Pattern.compile(regex).matcher(coordString);
            if (matcher.find()) {
                int x1 = Math.round(Float.parseFloat(matcher.group(1)));
                int y1 = Math.round(Float.parseFloat(matcher.group(2)));
                int x2 = Math.round(Float.parseFloat(matcher.group(3)));
                int y2 = Math.round(Float.parseFloat(matcher.group(4)));
                FragmentSelector selector = new FragmentSelector(new Rectangle(x1, y1, x2 - x1, y2 - y1));
                if (urlOnlyTarget) {
                    anno.setTarget(new SpecificResourceURI(this.getCanvasURI(pi, pageOrder), selector));
                } else {
                    anno.setTarget(new SpecificResource(this.getCanvasURI(pi, pageOrder), selector));
                }
            } else {
                anno.setTarget(new SimpleResource(getCanvasURI(pi, pageOrder)));
            }
        }
        anno.setMotivation(Motivation.DESCRIBING);
        return anno;
    }

    /**
     * @param doc
     * @return
     */
    private static String readSolrField(SolrDocument doc, Object fieldValue) {
        String text;
        Object textObject = Optional.ofNullable(fieldValue).orElse("");
        if (textObject != null && textObject instanceof Collection) {
            text = (String) ((Collection) textObject).stream().map(Object::toString).collect(Collectors.joining(", "));
        } else {
            text = Optional.ofNullable(textObject).map(Object::toString).orElse("");
        }
        return text;
    }

    /**
     * Add the annotations from the crowdsourcingAnnotations map to the respective canvases in the canvases list as well as to the given annotationMap
     *
     * @param canvases The list of canvases which should receive the annotations as otherContent
     * @param crowdsourcingAnnotations A map of annotations by page number
     * @param annotationMap A global annotation map for a whole manifest; may be null if not needed
     */
    public void addCrowdourcingAnnotations(List<Canvas> canvases, Map<Integer, List<OpenAnnotation>> crowdsourcingAnnotations,
            Map<AnnotationType, List<AnnotationList>> annotationMap) {

        for (Canvas canvas : canvases) {
            Integer order = this.getPageOrderFromCanvasURI(canvas.getId());
            String pi = this.getPIFromCanvasURI(canvas.getId());
            if (crowdsourcingAnnotations.containsKey(order)) {
                AnnotationList crowdList = new AnnotationList(getAnnotationListURI(pi, order, AnnotationType.CROWDSOURCING));
                crowdList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.CROWDSOURCING.name()));
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
     * <p>
     * getSolrFieldList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSolrFieldList() {
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
        fields.addAll(DataManager.getInstance().getConfiguration().getIIIFMetadataFields());
        return fields;
    }

    /**
     * Gets the attribution text configured in webapi.iiif.attribution and returns all translations if any are found, or the configured string itself
     * otherwise
     *
     * @return the configured attribution
     */
    protected List<IMetadataValue> getAttributions() {
        List<IMetadataValue> messages = DataManager.getInstance()
                .getConfiguration()
                .getIIIFAttribution()
                .stream()
                .map(ViewerResourceBundle::getTranslations)
                .collect(Collectors.toList());

        return messages;
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
            Optional<IMetadataValue> optional = SolrSearchIndex.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2).map(md -> {
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
    public URI getCollectionURI(String collectionField, String baseCollectionName) {
        String urlString;
        if (StringUtils.isNotBlank(baseCollectionName)) {
            baseCollectionName = StringTools.encodeUrl(baseCollectionName);
            urlString = this.urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, baseCollectionName).build();
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
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param mode a {@link io.goobi.viewer.model.iiif.presentation.builder.BuildMode} object.
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
    public URI getSequenceURI(String pi, String label) {
        if (StringUtils.isBlank(label)) {
            label = "basic";
        }
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(pi, label).build();
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
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type) {
        ApiPath url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
        if (type != null) {
            url = url.query("type", type.name());

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
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
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
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + "/image/1/";
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
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + "/" + type.name() + "/annoNum/";
        return URI.create(urlString);
    }

    /**
     * <p>
     * getAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param id a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationURI(String id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getSearchServiceURI.
     * </p>
     *
     * @param target a {@link java.net.URI} object.
     * @return a {@link java.net.URI} object.
     * @deprecated use {@link #getSearchServiceURI(String)} instead
     */
    public URI getSearchServiceURI(URI target) {
        String baseURI = target.toString();
        if (!baseURI.endsWith("/")) {
            baseURI += "/";
        }
        return URI.create(baseURI + "search");
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
     * <p>
     * getAutoSuggestServiceURI.
     * </p>
     *
     * @param target a {@link java.net.URI} object.
     * @return a {@link java.net.URI} object.
     * @deprecated use {@link #getAutoCompleteServiceURI(String)} instead
     */
    public URI getAutoSuggestServiceURI(URI target) {
        String baseURI = target.toString();
        if (!baseURI.endsWith("/")) {
            baseURI += "/";
        }
        return URI.create(baseURI + "autocomplete");
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
        String uri = getSearchServiceURI(getManifestURI(pi)).toString();
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
        String uri = getAutoSuggestServiceURI(getManifestURI(pi)).toString();
        if (StringUtils.isNotBlank(query)) {
            uri += ("?q=" + query);
            if (!motivation.isEmpty()) {
                uri += ("&motivation=" + StringUtils.join(motivation, "+"));
            }
        }
        return URI.create(uri);
    }

}
