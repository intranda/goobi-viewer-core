package io.goobi.viewer.controller.json;

import java.io.IOException;

import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.StringTools;

public class JsonStringConverter<T> {

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

    private String addQuotes(String json) {
        json = StringTools.replaceAllMatches(json, JSON_KEY_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_STRING_VALUE_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_ARRAY_ELEMENT_REGEX, m -> "\"" + m.get(0).trim() + "\"");
        json = StringTools.replaceAllMatches(json, JSON_BOOLEAN_VALUE_REGEX, m -> m.get(1).trim());
        return json;
    }

    public T convert(String json) throws IOException {
        json = json.replace("`", "\"").replaceAll("\\s+", " ");
        json = addQuotes(json);
        T object = JsonTools.getAsObject(json, type);
        return object;
    }

}
