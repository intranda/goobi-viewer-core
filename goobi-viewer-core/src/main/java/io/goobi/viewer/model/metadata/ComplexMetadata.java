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
package io.goobi.viewer.model.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Representation of one or more Solr documents of DOCTYPE 'Metadata'. Several documents may be combined into one CompleMetadata object if the share
 * the same 'MD_REFID'. They each represent a translation for a different language for the same data. The language is taken from the value of 'LABEL'.
 * 
 * @author florian
 *
 */
public final class ComplexMetadata {

    private static final String MD_REFID = "MD_REFID";
    private static final List<String> IGNORE_METADATA_FIELDS = List.of(SolrConstants.DOCTYPE, SolrConstants.LABEL, SolrConstants.METADATATYPE,
            SolrConstants.IDDOC_OWNER, SolrConstants.PI_TOPSTRUCT, SolrConstants.GROUPFIELD, SolrConstants.IDDOC, MD_REFID);
    private static final String IGNORE_METADATA_REGEX = String.format("(%s|%s).*", SolrConstants.PREFIX_FACET, SolrConstants.PREFIX_SORT);

    /**
     * The metadata field of this metadata. Taken from the 'LABEL' property of the SOLR document, without the '_LANG_*' extension
     */
    private final String field;
    /**
     * Taken from 'METADATATYPE'
     */
    private final String type;
    /**
     * IDDOC_OWNER
     */
    private final String ownerId;

    /**
     * IDDOC
     */
    private final String id;

    /**
     * PI_TOPSTRUCT
     */
    private final String topStructIdentifier;

    private Map<String, List<IMetadataValue>> metadata = new HashMap<>();

    private ComplexMetadata(SolrDocument doc) {
        //        if (!DocType.METADATA.name().equals(doc.get(SolrConstants.DOCTYPE))) {
        //            throw new IllegalArgumentException("ComplexMetadata must be initialized from SolrDocument of DocType 'METADATA'");
        //        }
        this.field = SolrTools.getBaseFieldName(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
        this.type = SolrTools.getSingleFieldStringValue(doc, SolrConstants.METADATATYPE);
        this.ownerId = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC_OWNER)).map(String.class::cast).orElse(null);
        this.topStructIdentifier = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        this.id = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC)).map(String.class::cast).orElse(null);
    }

    public static ComplexMetadata getFromSolrDoc(SolrDocument doc) {
        ComplexMetadata md = new ComplexMetadata(doc);
        md.metadata = SolrTools.getTranslatedMetadata(doc, new HashMap<>(), null, getMetadataFilter());
        return md;
    }

    public static List<ComplexMetadata> getMetadataFromDocuments(Collection<SolrDocument> docs) {
        Map<Object, List<SolrDocument>> docMap =
                docs.stream().collect(Collectors.toMap(doc -> doc.getFieldValue(MD_REFID), List::of, ListUtils::union));
        List<ComplexMetadata> translatedMetadata = docMap.entrySet()
                .stream()
                .filter(e -> e.getKey() != null)
                .map(Entry::getValue)
                .map(ComplexMetadata::getFromMultilanganguageDocs)
                .toList();
        List<ComplexMetadata> untranslatedMetadata = Optional.ofNullable(docMap.get(null))
                .orElse(Collections.emptyList())
                .stream()
                .map(ComplexMetadata::getFromSolrDoc)
                .toList();
        return ListUtils.union(translatedMetadata, untranslatedMetadata);
    }

    public static ComplexMetadata getFromMultilanganguageDocs(List<SolrDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("Must provide non-empty document list");
        }
        ComplexMetadata md = new ComplexMetadata(docs.get(0));
        Map<String, List<IMetadataValue>> metadata = new HashMap<>();
        for (SolrDocument doc : docs) {
            Locale locale = SolrTools.getLocale(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
            //            if(locale == null) {
            //                locale = IPolyglott.getDefaultLocale();
            //            }
            metadata = SolrTools.getTranslatedMetadata(doc, metadata, locale, getMetadataFilter());
        }
        md.metadata = metadata;
        return md;
    }

    private static Function<String, Boolean> getMetadataFilter() {
        return name -> !IGNORE_METADATA_FIELDS.contains(name) && !name.matches(IGNORE_METADATA_REGEX);
    }

    public String getField() {
        return field;
    }

    public String getType() {
        return type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTopStructIdentifier() {
        return topStructIdentifier;
    }

    public String getId() {
        return id;
    }

    public Map<String, List<IMetadataValue>> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public List<String> getMetadataFields() {
        return new ArrayList<>(metadata.keySet());
    }

    public List<IMetadataValue> getValues(String fieldName) {
        return this.metadata.get(fieldName);
    }

    public IMetadataValue getFirstValue(String fieldName) {
        return Optional.ofNullable(getValues(fieldName)).filter(list -> !list.isEmpty()).map(list -> list.get(0)).orElse(null);
    }

    public List<String> getValues(String fieldName, Locale locale) {
        return Optional.ofNullable(getValues(fieldName))
                .orElse(Collections.emptyList())
                .stream()
                .map(v -> v.getValueOrFallback(locale))
                .collect(Collectors.toList());
    }

    public String getFirstValue(String fieldName, Locale locale) {
        return Optional.ofNullable(getValues(fieldName, locale)).filter(list -> !list.isEmpty()).map(list -> list.get(0)).orElse("");
    }

    public List<IMetadataValue> getValues() {
        return getValues(this.field);
    }

    public IMetadataValue getFirstValue() {
        return getFirstValue(this.field);
    }

    public List<String> getValues(Locale locale) {
        return getValues(this.field, locale);
    }

    public String getFirstValue(Locale locale) {
        return getFirstValue(this.field, locale);
    }

    public boolean hasValue(String fieldName) {
        return !this.metadata.getOrDefault(fieldName, Collections.emptyList()).isEmpty();
    }

    @Override
    public String toString() {
        return getField() + "\t" + this.metadata.toString();
    }

    public IMetadataValue getConfiguredValue(Metadata config) {
        return new MetadataBuilder(this).build(config);
    }

}
