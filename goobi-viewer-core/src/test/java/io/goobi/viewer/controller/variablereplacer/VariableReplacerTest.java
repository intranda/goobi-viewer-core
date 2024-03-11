package io.goobi.viewer.controller.variablereplacer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.Configuration;

class VariableReplacerTest {

    private static final String PULL_THEME = "{config-folder-path}/script_theme-pull.sh {theme-path}";
    private static final String CREATE_DEVELOPER_PACKAGE = "{config-folder-path}/script_create_package.sh viewer {base-path} /var/www {solr-url}";
    
    
    @Test
    void test_replaceConfig() {
        Configuration config = new Configuration("config_viewer_developer.xml");
        VariableReplacer vr = new VariableReplacer(config);
        
        String pullTheme = vr.replace(PULL_THEME);
        assertEquals("/opt/digiverso/viewer/config/script_theme-pull.sh /opt/digiverso/goobi-viewer-theme-test/goobi-viewer-theme-test/WebContent/resources/themes/", pullTheme);
        
        String createDeveloperPackage = vr.replace(CREATE_DEVELOPER_PACKAGE);
        assertEquals("/opt/digiverso/viewer/config/script_create_package.sh viewer /opt/digiverso/viewer /var/www http://localhost:8983/solr/collection2", createDeveloperPackage);

    }

}
