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
package io.goobi.viewer.model.maps.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataBuilder;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.model.metadata.MetadataParameter;

public class LabelCreator {

    private final Map<String, List<Metadata>> metadataTemplates;
    private final String valueSeparator;

    public LabelCreator(Map<String, List<Metadata>> metadataTemplates) {
        this(metadataTemplates, "");
    }

    public LabelCreator(Map<String, List<Metadata>> metadataTemplates, String valueSeparator) {
        this.metadataTemplates = metadataTemplates;
        this.valueSeparator = valueSeparator;
    }

    public List<Metadata> getMetadata(String template) {
        return this.metadataTemplates.getOrDefault(
                template,
                this.metadataTemplates.getOrDefault(StringConstants.DEFAULT_NAME, Collections.emptyList()));
    }

    public IMetadataValue getValue(MetadataContainer doc, String template) {
        return this.getValue(doc, new MetadataContainer(Collections.emptyMap()), template);
    }

    public IMetadataValue getValue(MetadataContainer doc, MetadataContainer topStruct, String template) {
        return this.getValue(doc, new MetadataContainer(Collections.emptyMap()),
                topStruct != null ? topStruct : new MetadataContainer(Collections.emptyMap()), template);
    }

    public IMetadataValue getValue(MetadataContainer doc, MetadataContainer parentStruct, MetadataContainer topStruct, String template) {
        return new MetadataBuilder(doc, parentStruct, topStruct).build(this.getMetadata(template), this.valueSeparator);
    }

    public IMetadataValue getValue(Map<String, List<IMetadataValue>> metadata, String template) {
        List<Metadata> mdConfig = this.metadataTemplates.get(template);
        if (mdConfig != null) {
            return new MetadataBuilder(metadata).build(mdConfig, this.valueSeparator);
        } else {
            return new SimpleMetadataValue("");
        }
    }

    public Collection<String> getTemplateNames() {
        return metadataTemplates.keySet();
    }

    public List<String> getFieldsToQuery() {
        return metadataTemplates.values().stream().flatMap(List::stream).flatMap(this::getFields).distinct().toList();
    }

    public List<String> getFieldsToQuery(String template) {
        List<Metadata> md = getMetadata(template);
        return md.stream().flatMap(this::getFields).distinct().toList();
    }

    private Stream<String> getFields(Metadata md) {
        Stream<String> paramKeys =
                md.getParams().stream().map(param -> getKeys(param)).flatMap(List::stream);
        Stream<String> childKeys = md.getChildMetadata().stream().flatMap(this::getFields);
        return Stream.concat(paramKeys, childKeys);
    }

    private List<String> getKeys(MetadataParameter param) {
        List<String> keys = new ArrayList<>();
        if (StringUtils.isNotBlank(param.getKey())) {
            keys.add(param.getKey());
            keys.add(param.getKey() + "_LANG_*");
        }
        if (StringUtils.isNotBlank(param.getAltKey())) {
            keys.add(param.getAltKey());
            keys.add(param.getAltKey() + "_LANG_*");

        }
        return keys;
    }

}
