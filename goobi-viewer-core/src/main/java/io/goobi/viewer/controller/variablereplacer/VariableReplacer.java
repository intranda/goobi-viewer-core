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
package io.goobi.viewer.controller.variablereplacer;

import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.BASE_PATH;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.CONFIG_FOLDER_PATH;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.REST_API_URL;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.SOLR_URL;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.THEME_PATH;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.goobi.viewer.controller.Configuration;

public class VariableReplacer {

    private static final String REPLACE_GROUP_PATTERN = "\\{([\\w-]+)\\}";
    private final Map<String, String> mappings;

    public VariableReplacer(Configuration config) {
        mappings = readMappingsFromConfig(config);
    }

    private static Map<String, String> readMappingsFromConfig(Configuration config) {
        Map<String, String> temp = new HashMap<>();
        temp.put(BASE_PATH, config.getViewerHome());
        temp.put(SOLR_URL, config.getSolrUrl());
        temp.put(THEME_PATH, config.getThemeRootPath());
        temp.put(CONFIG_FOLDER_PATH, Path.of(config.getViewerHome()).resolve("config").toString());
        temp.put(REST_API_URL, config.getRestApiUrl());

        return temp;
    }

    public String replace(String template) {
        Matcher matcher = Pattern.compile(REPLACE_GROUP_PATTERN).matcher(template);
        String output = template;
        while (matcher.find()) {
            String group = matcher.group();
            String variable = matcher.group(1);
            String replacement = getReplacement(variable);
            output = output.replace(group, replacement);
        }
        return output;
    }

    private String getReplacement(String variable) {
        return Optional.ofNullable(this.mappings.get(variable)).orElse("{" + variable + "}");
    }

}
