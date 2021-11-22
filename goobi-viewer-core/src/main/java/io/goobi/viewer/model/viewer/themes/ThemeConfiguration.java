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
package io.goobi.viewer.model.viewer.themes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.goobi.viewer.dao.converter.ThemeLinkConverter;
import io.goobi.viewer.model.viewer.themes.ThemeLink.InternalService;
import io.goobi.viewer.model.viewer.themes.ThemeLink.SocialMediaService;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "theme_configuration")
public class ThemeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long id;
    @Column(name = "name", nullable = true, columnDefinition = "TINYTEXT")
    private String name;
    @Column(name = "label", nullable = true, columnDefinition = "TINYTEXT")
    private String label;
    @Column(name = "logo", nullable = true, columnDefinition = "TINYTEXT")
    private String logoFilename;
    @Column(name = "icon", nullable = true, columnDefinition = "TINYTEXT")
    private String iconFilename;
    @Column(name = "stylesheet", nullable = true, columnDefinition = "LONGTEXT")
    private String styleSheet;
    @Column(name = "social_media_link", nullable = true, columnDefinition = "TINYTEXT")
    @Convert(converter = ThemeLinkConverter.class)
    List<ThemeLink> socialMediaUrls = new ArrayList<>();
    @Column(name = "footer_link", nullable = true, columnDefinition = "TINYTEXT")
    @Convert(converter = ThemeLinkConverter.class)
    List<ThemeLink> footerLinks = new ArrayList<>();
    
    /**
     * Creates the internal lists for theme links
     */
    public ThemeConfiguration() {
        this.socialMediaUrls = Arrays.stream(ThemeLink.SocialMediaService.values()).map(s -> new ThemeLink(s)).collect(Collectors.toList());
        this.footerLinks = Arrays.stream(ThemeLink.InternalService.values()).map(s -> new ThemeLink(s)).collect(Collectors.toList());
    }
    
    /**
     * sets the name and calls default constructor
     */
    public ThemeConfiguration(String themeName) {
        this();
        this.name = themeName;
    }
    
    public ThemeConfiguration(ThemeConfiguration orig) {
        this.id = orig.id;
        this.name = orig.name;
        this.label = orig.label;
        this.logoFilename = orig.logoFilename;
        this.iconFilename = orig.iconFilename;
        this.styleSheet = orig.styleSheet;
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
     * @return the logoFilename
     */
    public String getLogoFilename() {
        return logoFilename;
    }

    /**
     * @param logoFilename the logoFilename to set
     */
    public void setLogoFilename(String logoFilename) {
        this.logoFilename = logoFilename;
    }

    /**
     * @return the iconFilename
     */
    public String getIconFilename() {
        return iconFilename;
    }

    /**
     * @param iconFilename the iconFilename to set
     */
    public void setIconFilename(String iconFilename) {
        this.iconFilename = iconFilename;
    }

    /**
     * @return the styleSheetFilename
     */
    public String getStyleSheet() {
        return styleSheet;
    }

    /**
     * @param styleSheetFilename the styleSheetFilename to set
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
    

    public ThemeLink getFooterLink(ThemeLink.InternalService service) {
        return this.footerLinks.stream().filter(l -> l.getService().equals(service)).findAny().orElseGet(() -> createLink(service));
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



}
