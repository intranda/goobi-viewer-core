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

    
    public String getTemplate() {
        return template;
    }

    
    public String getQuery() {
        return query;
    }

    
    public List<Map<String, String>> getFields() {
        return fields;
    }
}
