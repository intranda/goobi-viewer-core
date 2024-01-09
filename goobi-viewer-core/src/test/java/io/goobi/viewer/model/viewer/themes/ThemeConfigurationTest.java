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
package io.goobi.viewer.model.viewer.themes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.SimpleMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.viewer.themes.ThemeLink.InternalService;
import io.goobi.viewer.model.viewer.themes.ThemeLink.SocialMediaService;

/**
 * @author florian
 *
 */
class ThemeConfigurationTest extends AbstractDatabaseEnabledTest {

    private static final Long LOGO_ID = 1l;
    private static final String STYLESHEET = "div {color:green;}\n a {color:blue};";
    private static final Long ICON_ID = 2l;
    private static final String INTRANDA_LINK = "http://intranda.com";
    private static final String INSTAGRAM_LINK = "http://instagram.com";
    private static final String TWTTER_LINK = "http://twitter.com";
    private static final String FACEBOOK_LINK = "http://facebook.com";
    private static final String FACEBOOK_LINK_ALT = "http://facebook.de";
    private static final String THEME_LABEL = "Subtheme 1";
    private static final String THEME_LABEL_ALT = "Subtheme 2";
    private static final String THEME_NAME = "subtheme1";
    private static final String THEME_NAME_ALT = "subtheme2";

    private CMSMediaItem logoItem;
    private CMSMediaItem iconItem;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        logoItem = DataManager.getInstance().getDao().getCMSMediaItem(LOGO_ID);
        iconItem = DataManager.getInstance().getDao().getCMSMediaItem(ICON_ID);

    }

    @Test
    void testSaveTheme() throws DAOException {
        ThemeConfiguration theme = new ThemeConfiguration(THEME_NAME);
        theme.setLabel(THEME_LABEL);
        theme.getLogo().setMediaItem(logoItem);
        theme.getIcon().setMediaItem(iconItem);
        theme.setStyleSheet(STYLESHEET);
        theme.getSocialMediaLink(SocialMediaService.facebook).setLinkUrl(FACEBOOK_LINK);
        theme.getSocialMediaLink(SocialMediaService.instagram).setLinkUrl(INSTAGRAM_LINK);
        theme.getFooterLink(InternalService.contact).setLinkUrl(INTRANDA_LINK);

        assertTrue(DataManager.getInstance().getDao().addTheme(theme));
        ThemeConfiguration loadedTheme = DataManager.getInstance().getDao().getTheme(THEME_NAME);
        assertNotNull(loadedTheme);

        assertEquals(THEME_NAME, loadedTheme.getName());
        assertEquals(THEME_LABEL, loadedTheme.getLabel());
        assertEquals(logoItem, loadedTheme.getLogo().getMediaItem());
        assertEquals(iconItem, loadedTheme.getIcon().getMediaItem());
        assertEquals(STYLESHEET, loadedTheme.getStyleSheet());
        assertEquals(FACEBOOK_LINK, loadedTheme.getSocialMediaLink(SocialMediaService.facebook).getLinkUrl());
        assertEquals(INSTAGRAM_LINK, loadedTheme.getSocialMediaLink(SocialMediaService.instagram).getLinkUrl());
        assertEquals(INTRANDA_LINK, loadedTheme.getFooterLink(InternalService.contact).getLinkUrl());

        DataManager.getInstance().getDao().deleteTheme(theme);
    }

    @Test
    void testUpdateTheme() throws DAOException {
        ThemeConfiguration theme = new ThemeConfiguration(THEME_NAME);
        theme.setLabel(THEME_LABEL);
        theme.getLogo().setMediaItem(logoItem);
        theme.getIcon().setMediaItem(iconItem);
        theme.setStyleSheet(STYLESHEET);
        theme.getSocialMediaLink(SocialMediaService.facebook).setLinkUrl(FACEBOOK_LINK);
        theme.getSocialMediaLink(SocialMediaService.instagram).setLinkUrl(INSTAGRAM_LINK);
        theme.getFooterLink(InternalService.contact).setLinkUrl(INTRANDA_LINK);

        assertTrue(DataManager.getInstance().getDao().addTheme(theme));
        ThemeConfiguration loadedTheme = DataManager.getInstance().getDao().getTheme(THEME_NAME);
        assertEquals(logoItem, loadedTheme.getLogo().getMediaItem());
        assertEquals(iconItem, loadedTheme.getIcon().getMediaItem());
        assertEquals(THEME_LABEL, loadedTheme.getLabel());
        assertEquals(FACEBOOK_LINK, loadedTheme.getSocialMediaLink(SocialMediaService.facebook).getLinkUrl());
        assertEquals(null, loadedTheme.getSocialMediaLink(SocialMediaService.twitter).getLinkUrl());

        ThemeConfiguration editableTheme = new ThemeConfiguration(loadedTheme);
        editableTheme.setLabel(THEME_LABEL_ALT);
        editableTheme.getSocialMediaLink(SocialMediaService.facebook).setLinkUrl(FACEBOOK_LINK_ALT);
        editableTheme.getSocialMediaLink(SocialMediaService.twitter).setLinkUrl(TWTTER_LINK);
        assertEquals(logoItem, editableTheme.getLogo().getMediaItem());
        assertEquals(iconItem, editableTheme.getIcon().getMediaItem());

        assertTrue(DataManager.getInstance().getDao().updateTheme(editableTheme));
        ThemeConfiguration loadedTheme2 = DataManager.getInstance().getDao().getTheme(THEME_NAME);
        assertEquals(theme.getId(), loadedTheme2.getId());
        assertEquals(THEME_LABEL_ALT, loadedTheme2.getLabel());
        assertEquals(FACEBOOK_LINK_ALT, loadedTheme2.getSocialMediaLink(SocialMediaService.facebook).getLinkUrl());
        assertEquals(TWTTER_LINK, loadedTheme2.getSocialMediaLink(SocialMediaService.twitter).getLinkUrl());
        assertEquals(logoItem, loadedTheme2.getLogo().getMediaItem());
        assertEquals(iconItem, loadedTheme2.getIcon().getMediaItem());

        DataManager.getInstance().getDao().deleteTheme(theme);

    }

    @Test
    void testDeleteTheme() throws DAOException {
        ThemeConfiguration theme = new ThemeConfiguration(THEME_NAME);
        theme.setLabel(THEME_LABEL);
        theme.getLogo().setMediaItem(logoItem);
        theme.getIcon().setMediaItem(iconItem);
        theme.setStyleSheet(STYLESHEET);
        theme.getSocialMediaLink(SocialMediaService.facebook).setLinkUrl(FACEBOOK_LINK);
        theme.getSocialMediaLink(SocialMediaService.instagram).setLinkUrl(INSTAGRAM_LINK);
        theme.getFooterLink(InternalService.contact).setLinkUrl(INTRANDA_LINK);

        assertTrue(DataManager.getInstance().getDao().addTheme(theme));
        assertNotNull(DataManager.getInstance().getDao().getTheme(THEME_NAME));
        assertTrue(DataManager.getInstance().getDao().deleteTheme(theme));
        assertNull(DataManager.getInstance().getDao().getTheme(THEME_NAME));

    }

    @Test
    void testListThemes() throws DAOException {

        ThemeConfiguration theme = new ThemeConfiguration(THEME_NAME);
        theme.setLabel(THEME_LABEL);
        assertTrue(DataManager.getInstance().getDao().addTheme(theme));
        ThemeConfiguration theme2 = new ThemeConfiguration(THEME_NAME_ALT);
        theme.setLabel(THEME_LABEL_ALT);
        assertTrue(DataManager.getInstance().getDao().addTheme(theme2));

        List<ThemeConfiguration> themes = DataManager.getInstance().getDao().getConfiguredThemes();
        assertEquals(2, themes.size());

        DataManager.getInstance().getDao().deleteTheme(theme);
        DataManager.getInstance().getDao().deleteTheme(theme2);


    }
}
