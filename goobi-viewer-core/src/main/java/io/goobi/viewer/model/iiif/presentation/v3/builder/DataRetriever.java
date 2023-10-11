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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Responsible for retrieving data from Index to build any IIIF resources
 *
 * @author florian
 *
 */
public class DataRetriever {

    private static final Logger logger = LogManager.getLogger(DataRetriever.class);

    /**
     * Required field to create manifest stubs for works in collection
     */
    public static final String[] CONTAINED_WORKS_QUERY_FIELDS =
            { SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT,
                    SolrConstants.IDDOC };

    /**
     * Required fields to create manifests with structure
     */
    public static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM, SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
            SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.NUMPAGES,
            SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT, SolrConstants.BOOL_IMAGEAVAILABLE };

    /**
     * Queries all DocStructs which have the given PI as PI_TOPSTRUCT or anchor (or are the anchor themselves). Works are sorted by a
     * {@link io.goobi.viewer.model.iiif.presentation.v2.builder.StructElementComparator} If no hits are found, an empty list is returned
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

        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, null);
        List<StructElement> eles = new ArrayList<>();
        List<SolrDocument> events = new ArrayList<>();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                if ("EVENT".equals(doc.get(SolrConstants.DOCTYPE))) {
                    events.add(doc);
                } else {
                    StructElement ele = createStructElement(doc);
                    if (ele == null) {
                        continue;
                    }
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
     * Get all top level collections (i.e. those without splitting-char) along with the number of contained works and direct children
     *
     * @param solrField
     * @return
     * @throws IndexUnreachableException
     */
    public List<CollectionResult> getTopLevelCollections(String solrField) throws IndexUnreachableException {
        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(solrField);
        Map<String, CollectionResult> result = SearchHelper.findAllCollectionsFromField(solrField, null, null, true, true, splittingChar);
        return result.values()
                .stream()
                .filter(c -> !c.getName().contains(splittingChar))
                .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                .peek(c -> c.setChildCount(getChildCount(c.getName(), splittingChar, result.keySet())))
                .collect(Collectors.toList());
    }

    /**
     * Get all collections which are direct children of the given collection along with the number of contained works and direct children
     *
     * @param solrField
     * @param collectionName
     * @return
     * @throws IndexUnreachableException
     */
    public List<CollectionResult> getChildCollections(String solrField, String collectionName) throws IndexUnreachableException {
        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(solrField);
        String filterQuery = "+{field}:{collection}{splittingChar}*"
                .replace("{field}", solrField)
                .replace("{collection}", collectionName)
                .replace("{splittingChar}", splittingChar);
        Map<String, CollectionResult> result = SearchHelper.findAllCollectionsFromField(solrField, null, filterQuery, true, true, splittingChar);
        String regex = collectionName + "[{}][^{}]+$".replace("{}", splittingChar);
        return result.values()
                .stream()
                .filter(c -> c.getName().matches(regex))
                .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                .peek(c -> c.setChildCount(getChildCount(c.getName(), splittingChar, result.keySet())))
                .collect(Collectors.toList());
    }

    /**
     * Get all records directly belonging to the given collection, only the fields in {@link #CONTAINED_WORKS_QUERY_FIELDS} are returned
     *
     * @param solrField
     * @param collectionName
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<StructElement> getContainedRecords(String solrField, String collectionName) throws IndexUnreachableException, PresentationException {
        String query = "+{field}:{collection} +(ISWORK:* ISANCHOR:*)".replace("{field}", solrField).replace("{collection}", collectionName);
        List<String> displayFields = addLanguageFields(Arrays.asList(CONTAINED_WORKS_QUERY_FIELDS), ViewerResourceBundle.getAllLocales());

        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, displayFields);
        if (docs == null) {
            return Collections.emptyList();
        }

        return docs.stream().map(doc -> createStructElement(doc)).filter(struct -> struct != null).collect(Collectors.toList());
    }

    private static StructElement createStructElement(SolrDocument doc) {
        try {
            return new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
        } catch (NumberFormatException | IndexUnreachableException e) {
            logger.error(e.toString());
            return null;
        }
    }

    /**
     * @param name
     * @param keySet
     * @return
     */
    private static long getChildCount(String collection, String splittingChar, Set<String> allCollections) {
        String regex = collection + "[{}][^{}]+$".replace("{}", splittingChar);
        return allCollections.stream().filter(c -> c.matches(regex)).count();
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
                    List<String> fieldValueList = fieldValues.stream().map(SolrTools::getAsString).collect(Collectors.toList());
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
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
        if (doc != null) {
            StructElement ele = createStructElement(doc);
            if (ele != null) {
                ele.setImageNumber(1);
                return ele;
            }
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
        for (String string : REQUIRED_SOLR_FIELDS) {
            fields.add(string);
        }
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
     * @param displayFields
     * @param allLocales
     * @return
     */
    protected static List<String> addLanguageFields(List<String> displayFields, List<Locale> locales) {
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
}
