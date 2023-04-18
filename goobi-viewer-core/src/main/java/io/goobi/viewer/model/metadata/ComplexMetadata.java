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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Representation of one or more Solr documents of DOCTYPE 'Metadata'. Several documents may be combined into one CompleMetadata object if the share
 * the same 'MD_REFID'. They each represent a translation for a different language for the same data. The language is taken from the value of 'LABEL'.
 * 
 * @author florian
 *
 */
public class ComplexMetadata {

    private static final List<String> IGNORE_METADATA_FIELDS = List.of(SolrConstants.DOCTYPE, SolrConstants.LABEL, SolrConstants.METADATATYPE, SolrConstants.IDDOC_OWNER, SolrConstants.PI_TOPSTRUCT, SolrConstants.GROUPFIELD, SolrConstants.IDDOC, "MD_REFID");
    private static final String IGNORE_METADATA_REGEX = String.format("(%s|%s).*", SolrConstants.PREFIX_FACET, SolrConstants.PREFIX_SORT);
    
    /**
     * Taken from the 'LABEL' property of the SOLR document, without the '_LANG_*' extension
     */
    private final String field;
    /**
     * Taken from 'METADATATYPE'
     */
    private final String type;
    /**
     * IDDOC_OWNER
     */
    private final Long ownerId;
    
    /**
     * IDDOC
     */
    private final Long id;
    
    /**
     * PI_TOPSTRUCT
     */
    private final String topStructIdentifier;

    private Map<String, List<IMetadataValue>> metadata = new HashMap<>();
    
    private ComplexMetadata(SolrDocument doc) {
        if(!DocType.METADATA.name().equals(doc.get(SolrConstants.DOCTYPE))) {
            throw new IllegalArgumentException("ComplexMetadata must be initialized from SolrDocument of DocType 'METADATA'");
        }
        this.field = SolrTools.getBaseFieldName(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
        this.type = SolrTools.getSingleFieldStringValue(doc, SolrConstants.METADATATYPE);
        this.ownerId = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC_OWNER)).map(Long.class::cast).orElse(null);
        this.topStructIdentifier = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        this.id = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC)).map(Long.class::cast).orElse(null);
    }
    
    public static ComplexMetadata getFromSolrDoc(SolrDocument doc) {
        ComplexMetadata md = new ComplexMetadata(doc);
        md.metadata = parseMetadata(doc, new HashMap<>(), null);
        return md;
    }

    
    public static ComplexMetadata getFromMultilanganguageDocs(List<SolrDocument> docs) {
        if(docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("Must provide non-empty document list");
        }
        ComplexMetadata md = new ComplexMetadata(docs.get(0));
        Map<String, List<IMetadataValue>> metadata = new HashMap<>();
        for (SolrDocument doc : docs) {
            Locale locale = SolrTools.getLocale(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
            if(locale != null) {
                metadata = parseMetadata(doc, metadata, locale);
            }
        }
        md.metadata = metadata;
        return md;
    }
    
    private static Map<String, List<IMetadataValue>> parseMetadata(SolrDocument doc, Map<String, List<IMetadataValue>> metadata, Locale locale) {
        List<String> fieldNames = doc.getFieldNames().stream().filter(name -> !IGNORE_METADATA_FIELDS.contains(name)).filter(name -> !name.matches(IGNORE_METADATA_REGEX)).collect(Collectors.toList());
        for (String fieldName : fieldNames) {
            List<String> values = SolrTools.getMetadataValues(doc, fieldName);
            String baseFieldName = fieldName;
            if(SolrTools.isLanguageCodedField(fieldName)) {
                baseFieldName = SolrTools.getBaseFieldName(fieldName);
                locale = SolrTools.getLocale(fieldName);
            } else if("VALUE".equals(fieldName)) {
                baseFieldName = SolrTools.getBaseFieldName(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
            }
            for (String strValue : values) {
                int valueIndex = values.indexOf(strValue);
                List<IMetadataValue> existingValues = metadata.get(baseFieldName);
                IMetadataValue existingValue = existingValues == null || existingValues.size() <= valueIndex ? null : existingValues.get(valueIndex);
                if(existingValue == null) {
                    if(locale == null) {
                        IMetadataValue value = new SimpleMetadataValue(strValue);
                        metadata.computeIfAbsent(baseFieldName, l -> new ArrayList<>()).add(value);
                    } else {
                        IMetadataValue value = new MultiLanguageMetadataValue(new HashMap<>(Map.of(locale.getLanguage(), strValue)));
                        metadata.computeIfAbsent(baseFieldName, l -> new ArrayList<>()).add(value);
                    }
                } else {
                    if(locale == null) {                        
                        existingValue.setValue(strValue);
                    } else {
                        existingValue.setValue(strValue, locale);
                    }
                }
            }
        }
        return metadata;
    }
    

    public String getField() {
        return field;
    }
    
    public String getType() {
        return type;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }
    
    public String getTopStructIdentifier() {
        return topStructIdentifier;
    }
    
    public Long getId() {
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
        return Optional.ofNullable(getValues(fieldName)).orElse(Collections.emptyList()).stream().map(v -> v.getValueOrFallback(locale)).collect(Collectors.toList());
    }
    
    public String getFirstValue(String fieldName, Locale locale) {
        return Optional.ofNullable(getValues(fieldName, locale)).filter(list -> !list.isEmpty()).map(list -> list.get(0)).orElse("");

    }

}
