package io.goobi.viewer.controller.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
     * 
     * @return List of Solr field names
     */
    public List<String> getFieldNames() {
        List<String> ret = new ArrayList<>(fields.size());
        for (Map<String, String> field : fields) {
            String solrField = field.get("solrField");
            if (StringUtils.isNotEmpty(solrField)) {
                ret.add(field.get("solrField"));
            }
            ret.add(solrField);
        }

        return ret;
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
