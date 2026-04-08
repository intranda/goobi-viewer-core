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
package io.goobi.viewer.model.cms.media;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Stores locale-specific metadata (title, description, link) for a {@link io.goobi.viewer.model.cms.media.CMSMediaItem}.
 */
@Embeddable
public class CMSMediaItemMetadata implements Serializable {

    private static final long serialVersionUID = -4346127673979299371L;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    
    @Column(name = "image_alt_text", nullable = true)
    private String alternativeText = "";

    /**
     * Default constructor.
     */
    public CMSMediaItemMetadata() {

    }

    /**
     * Copy constructor.
     *
     * @param orig a {@link io.goobi.viewer.model.cms.media.CMSMediaItemMetadata} object.
     */
    public CMSMediaItemMetadata(CMSMediaItemMetadata orig) {
        this.language = orig.language;
        this.name = orig.name;
        this.description = orig.description;
        this.alternativeText = orig.alternativeText;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CMSMediaItemMetadata other = (CMSMediaItemMetadata) obj;
        if (language == null) {
            if (other.language != null) {
                return false;
            }
        } else if (!language.equals(other.language)) {
            return false;
        }
        return true;
    }

    /**
     * Getter for the field <code>language</code>.
     *

     */
    public String getLanguage() {
        return language;
    }

    /**
     * Setter for the field <code>language</code>.
     *

     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Getter for the field <code>name</code>.
     *

     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the field <code>name</code>.
     *

     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the field <code>description</code>.
     *

     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for the field <code>description</code>.
     *

     */
    public void setDescription(String description) {
        this.description = description;
    }

    
    public String getAlternativeText() {
        return alternativeText;
    }

    
    public void setAlternativeText(String alternativeText) {
        this.alternativeText = alternativeText;
    }
}
