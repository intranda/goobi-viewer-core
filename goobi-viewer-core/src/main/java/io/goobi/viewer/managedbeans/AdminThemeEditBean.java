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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.viewer.themes.ThemeConfiguration;
import io.goobi.viewer.model.viewer.themes.ThemeLink;

@Named
@ViewScoped
public class AdminThemeEditBean implements Serializable {

    private static final long serialVersionUID = 7858762112113454834L;

    private ThemeConfiguration currentTheme = null;

    public void setCurrentThemeName(String themeName) throws DAOException {
        if (StringUtils.isNotBlank(themeName)) {
            ThemeConfiguration loadedTheme = DataManager.getInstance().getDao().getTheme(themeName);
            if (loadedTheme == null) {
                this.currentTheme = new ThemeConfiguration(themeName);
            } else {
                this.currentTheme = new ThemeConfiguration(loadedTheme);
            }
        }
    }

    public String getCurrentThemeName() {
        return Optional.ofNullable(currentTheme).map(ThemeConfiguration::getName).orElse("");
    }

    public ThemeConfiguration getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(ThemeConfiguration currentTheme) {
        this.currentTheme = currentTheme;
    }

    public List<ThemeLink.SocialMediaService> getSocialMediaServices() {
        return Arrays.asList(ThemeLink.SocialMediaService.values());
    }

    public List<ThemeLink.InternalService> getFooterServices() {
        return Arrays.asList(ThemeLink.InternalService.values());
    }

    public String saveTheme() throws DAOException {
        if (this.currentTheme != null && this.currentTheme.getId() != null) {
            DataManager.getInstance().getDao().updateTheme(currentTheme);
        } else if (this.currentTheme != null) {
            DataManager.getInstance().getDao().addTheme(currentTheme);
        }
        return "pretty:adminThemes";
    }
}
