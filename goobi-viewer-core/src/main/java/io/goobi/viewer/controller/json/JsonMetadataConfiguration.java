package io.goobi.viewer.controller.json;

import java.util.List;
import java.util.Map;

public class JsonMetadataConfiguration {

    private final String template;
    private final String query;
    private final List<Map<String, String>> fields;

    /**
     * 
     * @param template Config template
     * @param query Solr query
     * @param fields Field configurations
     */
    public JsonMetadataConfiguration(String template, String query, List<Map<String, String>> fields) {
        this.template = template;
        this.query = query;
        this.fields = fields;
    }

    /**
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @return the fields
     */
    public List<Map<String, String>> getFields() {
        return fields;
    }
}
