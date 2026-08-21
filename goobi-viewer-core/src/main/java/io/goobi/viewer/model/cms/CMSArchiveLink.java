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
}
