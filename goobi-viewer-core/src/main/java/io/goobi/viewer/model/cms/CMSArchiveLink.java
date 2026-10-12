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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single help link of an archive resource (usage notes, ordering rules, search help, …),
 * maintained in the CMS archive configuration and displayed in the archive and card catalog views.
 */
@Entity
@Table(name = "cms_archive_config_links")
public class CMSArchiveLink implements Serializable {

    private static final long serialVersionUID = 6103951346566714546L;

    private static final String MAILTO = "mailto:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_archive_config_link_id")
    private Long id;

    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText label = new TranslatedText();

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url = "";

    public CMSArchiveLink() {
    }

    /**
     * Cloning constructor.
     *
     * @param orig {@link CMSArchiveLink} to clone
     */
    public CMSArchiveLink(CMSArchiveLink orig) {
        this.id = orig.id;
        this.label = new TranslatedText(orig.label);
        this.url = orig.url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TranslatedText getLabel() {
        return label;
    }

    public void setLabel(TranslatedText label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Trims an address and prefixes bare "www." addresses with https so they are not resolved as relative paths.
     *
     * @param url address as entered in the admin form
     * @return normalized address; empty for null
     */
    public static String normalizeUrl(String url) {
        String ret = url == null ? "" : url.trim();
        return ret.regionMatches(true, 0, "www.", 0, 4) ? "https://" + ret : ret;
    }

    /**
     * @return the address to link to, normalized also for links saved before normalization existed
     */
    public String getHref() {
        return normalizeUrl(url);
    }

    /**
     * @return true for mailto addresses
     */
    public boolean isMail() {
        return getHref().regionMatches(true, 0, MAILTO, 0, MAILTO.length());
    }

    /**
     * @return true if the address points to a PDF document, ignoring query and fragment
     */
    public boolean isPdf() {
        return !isMail() && getHref().replaceFirst("[?#].*$", "").toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    /**
     * @return Tabler icon name for the link type
     */
    public String getIconName() {
        if (isMail()) {
            return "mail";
        }
        return isPdf() ? "file-type-pdf" : "link";
    }

    /**
     * @return host of an absolute address without a leading "www."; null for relative, mail or unparsable addresses
     */
    public String getHost() {
        if (isMail()) {
            return null;
        }
        try {
            String host = new URI(getHref()).getHost();
            return host == null ? null : stripWww(host);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @param currentHost host name of the current request
     * @return true if the link leads to another host and should open in a new tab
     */
    public boolean isExternal(String currentHost) {
        String host = getHost();
        return host != null && !host.equalsIgnoreCase(stripWww(StringUtils.defaultString(currentHost)));
    }

    /**
     * @return recipient address of a mailto link, without query parameters; null for other links
     */
    public String getMailAddress() {
        return isMail() ? getHref().substring(MAILTO.length()).replaceFirst("\\?.*$", "") : null;
    }

    /**
     * @return label to show when no title is maintained: the mail address, else the host, else the address itself
     */
    public String getFallbackLabel() {
        if (isMail()) {
            return getMailAddress();
        }
        String host = getHost();
        return host != null ? host : getHref();
    }

    private static String stripWww(String host) {
        return host.regionMatches(true, 0, "www.", 0, 4) ? host.substring(4) : host;
    }
}
