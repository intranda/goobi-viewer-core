package io.goobi.viewer.controller.variablereplacer;

import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.BASE_PATH;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.CONFIG_FOLDER_PATH;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.REST_API_URL;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.SOLR_URL;
import static io.goobi.viewer.controller.variablereplacer.ReplacerVariables.THEME_PATH;

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

    private Map<String, String> readMappingsFromConfig(Configuration config) {
        Map<String, String> temp = new HashMap<>();
        temp.put(BASE_PATH, config.getViewerHome());
        temp.put(SOLR_URL, config.getSolrUrl());
        temp.put(THEME_PATH, config.getThemeRootPath());
        temp.put(CONFIG_FOLDER_PATH, config.getConfigLocalPath());
        temp.put(REST_API_URL, config.getRestApiUrl());
        
        return temp;
    }

    public String replace(String template) {
        Matcher matcher = Pattern.compile(REPLACE_GROUP_PATTERN).matcher(template);
        String output = template;
        while(matcher.find()) {
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
