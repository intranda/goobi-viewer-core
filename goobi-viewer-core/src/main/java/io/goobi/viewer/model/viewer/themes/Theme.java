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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import io.goobi.viewer.dao.converter.FooterLinkConverter;
import io.goobi.viewer.dao.converter.SocialMediaLinkConverter;

/**
 * @author florian
 *
 */
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long id;
    @Column(name = "name", nullable = true, columnDefinition = "TINYTEXT")
    private final String name;
    @Column(name = "label", nullable = true, columnDefinition = "TINYTEXT")
    private String label;
    @Column(name = "logo", nullable = true, columnDefinition = "TINYTEXT")
    private String logoFilename;
    @Column(name = "icon", nullable = true, columnDefinition = "TINYTEXT")
    private String iconFilename;
    @Column(name = "stylesheet", nullable = true, columnDefinition = "LONGTEXT")
    private String styleSheet;
    @Column(name = "social_media_link", nullable = true, columnDefinition = "TINYTEXT")
    @Convert(converter = SocialMediaLinkConverter.class)
    List<SocialMediaLink> socialMediaUrls = new ArrayList<>();
    @Column(name = "footer_link", nullable = true, columnDefinition = "TINYTEXT")
    @Convert(converter = FooterLinkConverter.class)
    List<FooterLink> footerLinks = new ArrayList<>();
    
    /**
     * 
     */
    public Theme(String themeName) {
        this.name = themeName;
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
    public String getStyleSheetFilename() {
        return styleSheet;
    }

    /**
     * @param styleSheetFilename the styleSheetFilename to set
     */
    public void setStyleSheetFilename(String styleSheetFilename) {
        this.styleSheet = styleSheetFilename;
    }

    /**
     * @return the socialMediaUrls
     */
    public List<SocialMediaLink> getSocialMediaUrls() {
        return socialMediaUrls;
    }

    /**
     * @param socialMediaUrls the socialMediaUrls to set
     */
    public void setSocialMediaUrls(List<SocialMediaLink> socialMediaUrls) {
        this.socialMediaUrls = socialMediaUrls;
    }

    /**
     * @return the footerLinks
     */
    public List<FooterLink> getFooterLinks() {
        return footerLinks;
    }

    /**
     * @param footerLinks the footerLinks to set
     */
    public void setFooterLinks(List<FooterLink> footerLinks) {
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

}
