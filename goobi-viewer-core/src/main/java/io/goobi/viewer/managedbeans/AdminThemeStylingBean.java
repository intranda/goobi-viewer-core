/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.themes.ThemeConfiguration;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class AdminThemeStylingBean implements Serializable {

    private static final long serialVersionUID = 837772138767500963L;
    
    private final String mainThemeName;
    private final List<String> subThemeNames;
    private List<ThemeConfiguration> configuredThemes;
    
    public AdminThemeStylingBean() throws PresentationException, IndexUnreachableException, DAOException {
        mainThemeName = DataManager.getInstance().getConfiguration().getTheme();
        subThemeNames = SolrTools.getExistingSubthemes();
        configuredThemes = DataManager.getInstance().getDao().getConfiguredThemes();
    }
    
    public String getMainThemeName() {
        return mainThemeName;
    }
    
    public boolean isMainThemeConfigured() {
        return configuredThemes.stream().anyMatch(t -> t.getName().equals(mainThemeName));
    }
    
    public boolean isSubThemeConfigured(String name) {
        return configuredThemes.stream().anyMatch(t -> t.getName().equals(name));
    }
    
    public ThemeConfiguration getMainThemeConfiguration() {
        return configuredThemes.stream().filter(t -> t.getName().equals(mainThemeName)).findAny().orElse(null);
    }
    
    public ThemeConfiguration getSubThemeConfiguration(String name) {
        return configuredThemes.stream().filter(t -> t.getName().equals(name)).findAny().orElse(null);
    }
    
    public List<String> getNotConfiguredSubThemes() {
        return subThemeNames.stream().filter(name -> isSubThemeConfigured(name)).collect(Collectors.toList());
    }

    public List<ThemeConfiguration> getConfiguredSubThemes() {
        return configuredThemes.stream().filter(theme -> theme.getName().equals(mainThemeName)).collect(Collectors.toList());
    }
    
    /**
     * @return the subThemeNames
     */
    public List<String> getSubThemeNames() {
        return subThemeNames;
    }
    
}
