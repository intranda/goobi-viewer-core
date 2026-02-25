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
package io.goobi.viewer.model.variables;

import static io.goobi.viewer.model.variables.ReplacerVariables.BASE_MIME_TYPE;
import static io.goobi.viewer.model.variables.ReplacerVariables.BASE_PATH;
import static io.goobi.viewer.model.variables.ReplacerVariables.CONFIG_FOLDER_PATH;
import static io.goobi.viewer.model.variables.ReplacerVariables.FILENAME;
import static io.goobi.viewer.model.variables.ReplacerVariables.FILENAME_BASE;
import static io.goobi.viewer.model.variables.ReplacerVariables.MIME_TYPE;
import static io.goobi.viewer.model.variables.ReplacerVariables.NAMESPACE_ANCHOR;
import static io.goobi.viewer.model.variables.ReplacerVariables.NAMESPACE_CONFIG;
import static io.goobi.viewer.model.variables.ReplacerVariables.NAMESPACE_PAGE;
import static io.goobi.viewer.model.variables.ReplacerVariables.NAMESPACE_RECORD;
import static io.goobi.viewer.model.variables.ReplacerVariables.NAMESPACE_STRUCT;
import static io.goobi.viewer.model.variables.ReplacerVariables.ORDER;
import static io.goobi.viewer.model.variables.ReplacerVariables.ORDER_LABEL;
import static io.goobi.viewer.model.variables.ReplacerVariables.REST_API_URL;
import static io.goobi.viewer.model.variables.ReplacerVariables.SOLR_URL;
import static io.goobi.viewer.model.variables.ReplacerVariables.THEME_PATH;
import static io.goobi.viewer.model.variables.ReplacerVariables.VIEWER_URL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElementStub;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Replace variable expression denoted by <code>{variable-name}</code> in strings. Possible variables are those listed in {@link ReplacerVariables} or
 * any SOLR fields (partially listed in {@link SolrConstants}). Values may be taken from configuration and/or solr-derived documents passed in
 * construction of the VariableReplacer instance </br>
 * Variables may have more than one value, typically for multivalued metadata fields, for this reason, in general a list of replaced strings is
 * returned, with one element for each replacement value
 */
public class VariableReplacer {

    private static final String REPLACEMENT_REGEX = "\\{[\\w.-]+\\}";
    private static final String REPLACEMENT_GROUP_REGEX = "\\{([\\w-]+)\\}";
    private static final String REPLACEMENT_GROUP_WITH_NAMESPACE_REGEX = "\\{([\\w-]+)\\.([\\w-]+)\\}";

    private final Map<String, Map<String, List<String>>> replacementsMap;

    /**
     * Take variable values from current struct element and page of the given navigationHelper
     * 
     * @param viewManager
     * @throws IndexUnreachableException
     */
    public VariableReplacer(ViewManager viewManager) throws IndexUnreachableException {
        this(DataManager.getInstance().getConfiguration(), viewManager.getAnchorStructElement(), viewManager.getTopStructElement(),
                viewManager.getCurrentStructElement(), viewManager.getCurrentPage());
    }

    /**
     * Take variable values from the given config and structure elements. Any passed objects may be null, in which case the associated values won't be
     * replaced
     * 
     * @param config
     * @param anchor
     * @param topStruct
     * @param structElement
     * @param page
     */
    public VariableReplacer(Configuration config, StructElementStub anchor, StructElementStub topStruct, StructElementStub structElement,
            PhysicalElement page) {
        Map<String, List<String>> configValues = readMappingsFromConfig(config);
        Map<String, List<String>> anchorValues = readMappingsFromStructElement(anchor);
        Map<String, List<String>> recordValues = readMappingsFromStructElement(topStruct);
        Map<String, List<String>> structElementValues = readMappingsFromStructElement(structElement);
        Map<String, List<String>> pageValues = readMappingsFromPhysicalElement(page);

        replacementsMap = new TreeMap<>();
        replacementsMap.put(NAMESPACE_CONFIG, configValues);
        replacementsMap.put(NAMESPACE_ANCHOR, anchorValues);
        replacementsMap.put(NAMESPACE_RECORD, recordValues);
        replacementsMap.put(NAMESPACE_STRUCT, structElementValues);
        replacementsMap.put(NAMESPACE_PAGE, pageValues);

    }

    /**
     * Specifically replace the variables defined in the given replacement map
     * 
     * @param replacementsMap
     */
    public VariableReplacer(Map<String, Map<String, List<String>>> replacementsMap) {
        super();
        this.replacementsMap = replacementsMap;
    }

    /**
     * Only replace configuration variables
     * 
     * @param configuration
     */
    public VariableReplacer(Configuration configuration) {
        this(configuration, null, null, null, null);
    }

    /**
     * Take variable values from global configuration and the given struct element
     * 
     * @param struct
     */
    public VariableReplacer(StructElementStub struct) {
        this(DataManager.getInstance().getConfiguration(), null, struct, null, null);
    }

    /**
     * Replace variables in the given template string. The first element in the returned list will use the first replacement value and so on
     * 
     * @param template
     * @return A list of strings
     */
    public List<String> replace(String template) {
        List<String> replacementStrings = getReplacementStrings(template);
        SortedMap<String, List<String>> replacementValues = getReplacementValues(replacementStrings);
        if (replacementValues.isEmpty()) {
            return List.of(template);
        }

        return getReplacedStrings(template, replacementValues);
    }

    /**
     * Return a single string which only uses the first replacement values for each variable if there is more than one
     * 
     * @param template
     * @return {@link String}
     */
    public String replaceFirst(String template) {
        return this.replace(template).stream().findFirst().orElse("");
    }

    /**
     * Concatenate all returned strings of {@link #replace(String)}, separated by the given separator
     * 
     * @param template
     * @param separator
     * @return {@link String}
     */
    public String replaceAll(String template, String separator) {
        return this.replace(template).stream().collect(Collectors.joining(separator));
    }

    /**
     * Add a custom replacement variable
     * 
     * @param s
     * @param value
     */
    public void addReplacement(String s, String value) {
        Map<String, List<String>> map = this.replacementsMap.computeIfAbsent("custom", key -> new HashMap<String, List<String>>());
        map.put(s, List.of(value));
    }

    /**
     * return a new {@link Metadata} object with the replaced values of the given metadata object as values
     * 
     * @param metadata
     * @return a new metadata object
     */
    public Metadata replace(Metadata metadata) {
        return new Metadata(metadata.getLabel(), this.replace(metadata.getValue()));
    }

    /**
     * return a new {@link IMetadataValue} object with the replaced values of the given object as values
     * 
     * @param value
     * @return a new IMetadata value
     */
    public IMetadataValue replace(IMetadataValue value) {
        if (value instanceof SimpleMetadataValue simple) {
            return new SimpleMetadataValue(this.replaceFirst(simple.getValue().orElse("")));
        } else if (value instanceof MultiLanguageMetadataValue multi) {
            Map<String, String> valueMap =
                    multi.getValues().stream().collect(Collectors.toMap(pair -> pair.getLanguage(), pair -> this.replaceFirst(pair.getValue())));
            return new MultiLanguageMetadataValue(valueMap);
        } else {
            return value;
        }
    }

    private List<String> getReplacedStrings(String template, SortedMap<String, List<String>> replacementValues) {
        int numValues = replacementValues.values().stream().mapToInt(List::size).max().orElse(0);
        ArrayList<Entry<String, List<String>>> entryList = new ArrayList<>(replacementValues.entrySet());
        List<String> replacedStrings = new ArrayList<>(numValues);
        for (int valueIndex = 0; valueIndex < numValues; valueIndex++) {
            String replacedString = template;
            for (int replacementStringIndex = entryList.size() - 1; replacementStringIndex >= 0; replacementStringIndex--) {
                String replacementString = entryList.get(replacementStringIndex).getKey();
                List<String> values = entryList.get(replacementStringIndex).getValue();
                String value = valueIndex < values.size() ? values.get(valueIndex) : "";
                replacedString = replacedString.replace(replacementString, value);
            }
            replacedStrings.add(replacedString);
        }
        return replacedStrings;
    }

    private SortedMap<String, List<String>> getReplacementValues(List<String> replacementStrings) {
        SortedMap<String, List<String>> replacementValues = new TreeMap<>();
        for (String string : replacementStrings) {
            String[] term = getReplacementTerm(string);
            List<String> values = getMetadataValues(term);
            replacementValues.put(string, values);
        }
        return replacementValues;
    }

    private List<String> getMetadataValues(String[] term) {
        String namespace = term[0];
        String value = term[1];

        if (StringUtils.isBlank(namespace)) {
            for (Map<String, List<String>> map : this.replacementsMap.values()) {
                if (map.containsKey(value)) {
                    return map.get(value);
                }
            }
            return Collections.emptyList();
        }

        Map<String, List<String>> map = this.replacementsMap.get(namespace);
        if (map != null) {
            return Optional.ofNullable(map.get(value)).orElse(Collections.emptyList());
        }

        return Collections.emptyList();
    }

    private List<String> getReplacementStrings(String template) {
        if (StringUtils.isBlank(template)) {
            return List.of("");
        }
        Pattern pattern = Pattern.compile(REPLACEMENT_REGEX);
        Matcher matcher = pattern.matcher(template);
        List<String> replacementStrings = new ArrayList<>();
        while (matcher.find()) {
            replacementStrings.add(matcher.group());
        }
        return replacementStrings;
    }

    private static String[] getReplacementTerm(String replacementString) {
        if (replacementString.matches(REPLACEMENT_GROUP_REGEX)) {
            return new String[] { "", replacementString.replaceAll(REPLACEMENT_GROUP_REGEX, "$1") };
        } else if (replacementString.matches(REPLACEMENT_GROUP_WITH_NAMESPACE_REGEX)) {
            Matcher m = Pattern.compile(REPLACEMENT_GROUP_WITH_NAMESPACE_REGEX).matcher(replacementString);
            if (m.find()) {
                String namespace = m.group(1);
                String term = m.group(2);
                return new String[] { namespace, term };
            }
            return new String[] { "", "" };
        } else {
            return new String[] { "", "" };
        }
    }

    private static Map<String, List<String>> readMappingsFromPhysicalElement(PhysicalElement page) {
        Map<String, List<String>> temp = new HashMap<>();
        if (page != null) {
            temp.put(MIME_TYPE, List.of(page.getMimeType()));
            temp.put(BASE_MIME_TYPE, List.of(page.getMediaType().getType()));
            temp.put(ORDER, List.of(Integer.toString(page.getOrder())));
            temp.put(ORDER_LABEL, List.of(page.getOrderLabel()));
            temp.put(FILENAME, List.of(page.getFileName()));
            temp.put(FILENAME_BASE, List.of(page.getFileNameBase()));
        }
        return temp;
    }

    private static Map<String, List<String>> readMappingsFromStructElement(StructElementStub docStruct) {
        if (docStruct != null) {
            return new HashMap<>(docStruct.getMetadataFields());
        }

        return Collections.emptyMap();
    }

    private static Map<String, List<String>> readMappingsFromConfig(Configuration config) {
        String viewerHome = Optional.ofNullable(config.getViewerHome()).orElse("");
        Map<String, List<String>> temp = new HashMap<>();
        temp.put(BASE_PATH, List.of(viewerHome));
        temp.put(SOLR_URL, List.of(config.getSolrUrl()));
        temp.put(THEME_PATH, List.of(Optional.ofNullable(config.getThemeRootPath()).orElse("")));
        temp.put(CONFIG_FOLDER_PATH, List.of(Path.of(viewerHome).resolve("config").toString()));
        temp.put(REST_API_URL, List.of(config.getRestApiUrl()));
        temp.put(VIEWER_URL, List.of(config.getViewerBaseUrl()));
        return temp;
    }

}
