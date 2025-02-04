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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.goobi.viewer.dao.converter.SimpleMediaHolderConverter;
import io.goobi.viewer.dao.converter.ThemeLinkConverter;
import io.goobi.viewer.model.cms.SimpleMediaHolder;
import io.goobi.viewer.model.viewer.themes.ThemeLink.InternalService;
import io.goobi.viewer.model.viewer.themes.ThemeLink.SocialMediaService;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "theme_configuration")
public class ThemeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "theme_id")
    private Long id;
    @Column(name = "name", nullable = true, columnDefinition = "TINYTEXT")
    private String name;
    @Column(name = "label", nullable = true, columnDefinition = "TINYTEXT")
    private String label;
    @Column(name = "logo", nullable = true, columnDefinition = "BIGINT")
    @Convert(converter = SimpleMediaHolderConverter.class)
    private SimpleMediaHolder logo;
    @Column(name = "fullscreen_logo", nullable = true, columnDefinition = "BIGINT")
    @Convert(converter = SimpleMediaHolderConverter.class)
    private SimpleMediaHolder fullscreenLogo;
    @Column(name = "icon", nullable = true, columnDefinition = "BIGINT")
    @Convert(converter = SimpleMediaHolderConverter.class)
    private SimpleMediaHolder icon;
    @Column(name = "stylesheet", nullable = true, columnDefinition = "LONGTEXT")
    private String styleSheet;
    @Column(name = "javascript", nullable = true, columnDefinition = "LONGTEXT")
    private String javascript;
    @Column(name = "social_media_link", nullable = true, columnDefinition = "TEXT")
    @Convert(converter = ThemeLinkConverter.class)
    private List<ThemeLink> socialMediaUrls = new ArrayList<>();
    @Column(name = "footer_link", nullable = true, columnDefinition = "TEXT")
    @Convert(converter = ThemeLinkConverter.class)
    private List<ThemeLink> footerLinks = new ArrayList<>();

    /**
     * Creates the internal lists for theme links
     */
    public ThemeConfiguration() {
        this.socialMediaUrls = Arrays.stream(ThemeLink.SocialMediaService.values()).map(ThemeLink::new).collect(Collectors.toList());
        this.footerLinks = Arrays.stream(ThemeLink.InternalService.values()).map(ThemeLink::new).collect(Collectors.toList());
        this.logo = new SimpleMediaHolder();
        this.fullscreenLogo = new SimpleMediaHolder();
        this.icon = new SimpleMediaHolder();
    }

    /**
     * sets the name and calls default constructor
     * 
     * @param themeName
     */
    public ThemeConfiguration(String themeName) {
        this();
        this.name = themeName;
    }

    /**
     * 
     * @param orig
     */
    public ThemeConfiguration(ThemeConfiguration orig) {
        this.id = orig.id;
        this.name = orig.name;
        this.label = orig.label;
        this.logo = new SimpleMediaHolder(orig.logo.getMediaItem(), orig.logo.getMediaFilter());
        this.fullscreenLogo = new SimpleMediaHolder(orig.fullscreenLogo.getMediaItem(), orig.fullscreenLogo.getMediaFilter());
        this.icon = new SimpleMediaHolder(orig.icon.getMediaItem(), orig.icon.getMediaFilter());
        this.styleSheet = orig.styleSheet;
        this.javascript = orig.javascript;
        this.socialMediaUrls = orig.socialMediaUrls.stream().map(l -> new ThemeLink(l.getService(), l.getLinkUrl())).collect(Collectors.toList());
        this.footerLinks = orig.footerLinks.stream().map(l -> new ThemeLink(l.getService(), l.getLinkUrl())).collect(Collectors.toList());
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the styleSheet
     */
    public String getStyleSheet() {
        return styleSheet;
    }

    /**
     * @param styleSheet the styleSheet to set
     */
    public void setStyleSheet(String styleSheet) {
        this.styleSheet = styleSheet;
    }

    /**
     * @return the socialMediaUrls
     */
    public List<ThemeLink> getSocialMediaUrls() {
        return socialMediaUrls;
    }

    /**
     * @param socialMediaUrls the socialMediaUrls to set
     */
    public void setSocialMediaUrls(List<ThemeLink> socialMediaUrls) {
        this.socialMediaUrls = socialMediaUrls;
    }

    /**
     * @return the footerLinks
     */
    public List<ThemeLink> getFooterLinks() {
        return footerLinks;
    }

    /**
     * @param footerLinks the footerLinks to set
     */
    public void setFooterLinks(List<ThemeLink> footerLinks) {
        this.footerLinks = footerLinks;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public ThemeLink getSocialMediaLink(ThemeLink.SocialMediaService service) {
        return this.socialMediaUrls.stream().filter(l -> l.getService().equals(service)).findAny().orElseGet(() -> createLink(service));
    }

    public String getSocialMediaLinkUrlOrDefault(ThemeLink.SocialMediaService service, String defaultValue) {
        return Optional.ofNullable(getSocialMediaLink(service)).filter(ThemeLink::hasLink).map(ThemeLink::getLinkUrl).orElse(defaultValue);
    }

    public ThemeLink getFooterLink(ThemeLink.InternalService service) {
        return this.footerLinks.stream().filter(l -> l.getService().equals(service)).findAny().orElseGet(() -> createLink(service));
    }

    public String getFooterLinkUrlOrDefault(ThemeLink.InternalService service, String defaultValue) {
        return Optional.ofNullable(getFooterLink(service)).filter(ThemeLink::hasLink).map(ThemeLink::getLinkUrl).orElse(defaultValue);
    }

    private ThemeLink createLink(SocialMediaService service) {
        ThemeLink link = new ThemeLink(service);
        this.socialMediaUrls.add(link);
        return link;
    }

    private ThemeLink createLink(InternalService service) {
        ThemeLink link = new ThemeLink(service);
        this.footerLinks.add(link);
        return link;
    }

    public SimpleMediaHolder getLogo() {
        return logo;
    }

    public SimpleMediaHolder getFullscreenLogo() {
        return fullscreenLogo;
    }

    public SimpleMediaHolder getIcon() {
        return icon;
    }

    public String getJavascript() {
        return javascript;
    }

    public void setJavascript(String javascript) {
        this.javascript = javascript;
    }
}
