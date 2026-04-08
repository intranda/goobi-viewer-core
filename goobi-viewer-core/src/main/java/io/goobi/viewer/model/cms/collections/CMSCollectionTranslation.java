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
package io.goobi.viewer.model.cms.collections;

import java.io.Serializable;

import io.goobi.viewer.model.translations.Translation;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A persistence object holding a translated String value.
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "translations")
public class CMSCollectionTranslation extends Translation implements Serializable {

    private static final long serialVersionUID = 6110495225727273302L;

    /** Reference to the owning {@link CMSCollection}. */
    @ManyToOne
    @JoinColumn(name = "translation_owner_id")
    private CMSCollection owner;

    /**
     * Creates a new CMSCollectionTranslation instance.
     */
    public CMSCollectionTranslation() {
    }

    /**
     * Creates a new CMSCollectionTranslation instance.
     *
     * @param language BCP 47 language code for this translation
     * @param value translated text value
     */
    public CMSCollectionTranslation(String language, String value) {
        super(language, value);
    }

    /**
     * Cloning constructor.
     * 
     * @param tr translation to copy
     * @param owner collection that owns this translation
     */
    public CMSCollectionTranslation(CMSCollectionTranslation tr, CMSCollection owner) {
        this.id = tr.id;
        this.language = tr.language;
        this.owner = owner;
        this.tag = tr.tag;
        this.translationValue = tr.translationValue;
    }

    /**
     * Getter for the field <code>owner</code>.
     *

     */
    public CMSCollection getOwner() {
        return owner;
    }

    /**
     * Setter for the field <code>owner</code>.
     *
     * @param owner the owner to set
     */
    public void setOwner(CMSCollection owner) {
        this.owner = owner;
    }
}
