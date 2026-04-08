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
package io.goobi.viewer.controller.json;

import java.io.IOException;

import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.StringTools;

/**
 * Converts loosely-formatted JSON-like strings (e.g. with unquoted keys, backtick delimiters, or
 * unquoted string values) into a properly quoted JSON string and deserializes it into the target type.
 *
 * @param <T> the target type to deserialize the JSON string into
 */
public final class JsonStringConverter<T> {

    private static final String JSON_KEY_REGEX = "(?<=[{,]\\s*)(\\w+)(?=\\s*:\\s*)";
    private static final String JSON_STRING_VALUE_REGEX = "(?<=:)[^\\[\\],\"']+(?=[,}])";
    private static final String JSON_ARRAY_ELEMENT_REGEX = "(?<=[\\[,])[^:\"']+?(?=[,\\]])";
    private static final String JSON_BOOLEAN_VALUE_REGEX = "(?<=:)[\"'](true|false)[\"']";

    private final Class<T> type;

    public static <T> JsonStringConverter<T> of(Class<T> type) {
        return new JsonStringConverter<T>(type);
    }

    private JsonStringConverter(Class<T> type) {
        this.type = type;
    }

    private String addQuotes(String input) {
        String json = StringTools.replaceAllMatches(input, JSON_KEY_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_STRING_VALUE_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_ARRAY_ELEMENT_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_BOOLEAN_VALUE_REGEX, m -> m.get(1).trim());
        return json;
    }

    public T convert(String json) throws IOException {
        String convertedJson = json.replace("`", "\"").replaceAll("\\s+", " ");
        convertedJson = addQuotes(convertedJson);
        T object = JsonTools.getAsObject(convertedJson, type);
        return object;
    }

}
