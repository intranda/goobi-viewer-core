package io.goobi.viewer.controller.variablereplacer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.DataManager;

class VariableReplacerTest {

    private static final String PULL_THEME = "{config-folder-path}/script_theme-pull.sh {theme-path}";
    private static final String CREATE_DEVELOPER_PACKAGE = "{config-folder-path}/script_create_package.sh viewer {base-path} /var/www {solr-url}";
    
    @Test
    void test_replaceConfig() {
        VariableReplacer vr = new VariableReplacer(DataManager.getInstance().getConfiguration());
        
        String pullTheme = vr.replace(PULL_THEME);
        assertEquals("/opt/digiverso/viewer/config/script_theme-pull.sh {theme-path}", pullTheme);
        
        String createDeveloperPackage = vr.replace(CREATE_DEVELOPER_PACKAGE);
        assertEquals("/opt/digiverso/viewer/config/script_create_package.sh viewer http://localhost:8080/viewer/ /var/www https://viewer.goobi.io/solr/collection1", createDeveloperPackage);

    }

}
