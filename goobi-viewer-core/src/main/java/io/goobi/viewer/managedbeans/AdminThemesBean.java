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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.themes.ThemeConfiguration;
import io.goobi.viewer.model.viewer.themes.ThemeLink;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class AdminThemesBean implements Serializable {

    private static final long serialVersionUID = 837772138767500963L;

    private static final Logger logger = LogManager.getLogger(AdminThemesBean.class);

    private final String mainThemeName;
    private final List<String> subThemeNames;
    private List<ThemeConfiguration> configuredThemes;

    public AdminThemesBean() {
        mainThemeName = DataManager.getInstance().getConfiguration().getTheme();
        subThemeNames = getExistingSubThemes();
        configuredThemes = getConfiguredThemes();
    }

    private List<ThemeConfiguration> getConfiguredThemes() {
        try {
            return DataManager.getInstance()
                    .getDao()
                    .getConfiguredThemes()
                    .stream()
                    .filter(t -> subThemeNames.contains(t.getName()) || t.getName().equals(mainThemeName))
                    .collect(Collectors.toList());
        } catch (DAOException e) {
            logger.error("Unable to load configured themes: {}", e.toString());
            return Collections.emptyList();
        }
    }

    private static List<String> getExistingSubThemes() {
        try {
            return SolrTools.getExistingSubthemes();
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Unable to load subtheme names from Index: {}", SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()));
            return Collections.emptyList();
        }
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
        return subThemeNames.stream().filter(name -> !isSubThemeConfigured(name)).collect(Collectors.toList());
    }

    public List<ThemeConfiguration> getConfiguredSubThemes() {
        return configuredThemes.stream().filter(theme -> !theme.getName().equals(mainThemeName)).collect(Collectors.toList());
    }

    public List<String> getSubThemeNames() {
        return subThemeNames;
    }

    public ThemeConfiguration getCurrentTheme() throws DAOException {
        String themeName = BeanUtils.getNavigationHelper().getThemeOrSubtheme();
        return DataManager.getInstance().getDao().getTheme(themeName);
    }

    public boolean isCurrentThemeConfigured() throws DAOException {
        return getCurrentTheme() != null;
    }

    public String getSocialMediaUrl(ThemeLink.SocialMediaService service, String defaultUrl) throws DAOException {
        return Optional.ofNullable(getCurrentTheme()).map(t -> t.getSocialMediaLinkUrlOrDefault(service, defaultUrl)).orElse(defaultUrl);
    }

    public String getSocialMediaUrl(ThemeLink.SocialMediaService service) throws DAOException {
        return getSocialMediaUrl(service, "");
    }

    public boolean hasSocialMediaUrl(ThemeLink.SocialMediaService service) throws DAOException {
        return Optional.ofNullable(getCurrentTheme()).map(t -> t.getSocialMediaLink(service)).map(l -> l.hasLink()).orElse(false);
    }

    public String getFooterUrl(ThemeLink.InternalService service, String defaultUrl) throws DAOException {
        return Optional.ofNullable(getCurrentTheme()).map(t -> t.getFooterLinkUrlOrDefault(service, defaultUrl)).orElse(defaultUrl);
    }

    public String getFooterUrl(ThemeLink.InternalService service) throws DAOException {
        return getFooterUrl(service, "");
    }

    public boolean hasFooterUrl(ThemeLink.InternalService service) throws DAOException {
        return Optional.ofNullable(getCurrentTheme()).map(t -> t.getFooterLink(service)).map(l -> l.hasLink()).orElse(false);
    }

    public String getLogo(String defaultUrl) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getLogo())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI().toString())
                .orElse(getFullUrl(defaultUrl));
    }

    public String getLogo(String defaultUrl, int width, int height) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getLogo())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI(width, height).toString())
                .orElse(getFullUrl(defaultUrl));
    }

    private static String getFullUrl(String defaultUrl) {
        String basePath = BeanUtils.getRequest().getContextPath();
        String imagePath = BeanUtils.getNavigationHelper().getResource(defaultUrl.replaceAll("^\\/", ""));
        return basePath + imagePath;
    }

    public String getIcon(String defaultUrl) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getIcon())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI().toString())
                .orElse(getFullUrl(defaultUrl));
    }

    public String getIcon(String defaultUrl, int width, int height) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getIcon())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI(width, height).toString())
                .orElse(getFullUrl(defaultUrl));
    }

    public String getFullscreenLogo(String defaultUrl) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getFullscreenLogo())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI().toString())
                .orElse(getFullUrl(defaultUrl));
    }

    public String getFullscreenLogo(String defaultUrl, int width, int height) throws DAOException {
        return Optional.ofNullable(getCurrentTheme())
                .map(t -> t.getFullscreenLogo())
                .filter(l -> l.hasMediaItem())
                .map(l -> l.getMediaItem().getIconURI(width, height).toString())
                .orElse(getFullUrl(defaultUrl));
    }

    public String getThemeLabel() throws DAOException {
        return getThemeLabel("");
    }

    public String getThemeLabel(String defaultLabel) throws DAOException {
        return Optional.ofNullable(getCurrentTheme()).map(ThemeConfiguration::getLabel).orElse(defaultLabel);
    }

    public String getStyleSheet() throws DAOException {
        String styleSheet = Optional.ofNullable(getCurrentTheme()).map(t -> t.getStyleSheet()).orElse("");
        if (StringUtils.isNotBlank(styleSheet)) {
            return "<style>" + styleSheet + "</style>";
        }

        return "";
    }
}
