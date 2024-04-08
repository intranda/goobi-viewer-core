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
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.goobi.viewer.model.viewer.StructElement;

public class VariableReplacer {

    private static final String REPLACEMENT_GROUP_REGEX = "\\{(\\w+)\\}";

    private final StructElement metadataContainer;

    public VariableReplacer(StructElement metadataContainer) {
        this.metadataContainer = metadataContainer;
    }

    public List<String> replace(String template) {
        List<String> replacementStrings = getReplacementStrings(template);
        SortedMap<String, List<String>> replacementValues = getReplacementValues(replacementStrings);
        if (replacementValues.isEmpty()) {
            return List.of(template);
        } else {
            return getReplacedStrings(template, replacementValues);
        }
    }

    public List<String> getReplacedStrings(String template, SortedMap<String, List<String>> replacementValues) {
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

    public SortedMap<String, List<String>> getReplacementValues(List<String> replacementStrings) {
        SortedMap<String, List<String>> replacementValues = new TreeMap<>();
        for (String string : replacementStrings) {
            String term = getReplacementTerm(string);
            List<String> values = metadataContainer.getMetadataValues(term);
            replacementValues.put(string, values);
        }
        return replacementValues;
    }

    public List<String> getReplacementStrings(String template) {
        Pattern pattern = Pattern.compile(REPLACEMENT_GROUP_REGEX);
        Matcher matcher = pattern.matcher(template);
        List<String> replacementStrings = new ArrayList<>();
        while (matcher.find()) {
            replacementStrings.add(matcher.group());
        }
        return replacementStrings;
    }

    private String getReplacementTerm(String replacementString) {
        if (replacementString.matches(REPLACEMENT_GROUP_REGEX)) {
            return replacementString.replaceAll(REPLACEMENT_GROUP_REGEX, "$1");
        } else {
            return "";
        }
    }

}
