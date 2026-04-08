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
package io.goobi.viewer.model.administration.legal;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.goobi.viewer.model.translations.Translation;

/**
 * @author Florian Alpers
 */
@Entity
@Table(name = "terms_of_use_translations")
public class TermsOfUseTranslation extends Translation {

    /** Reference to the owning {@link PersistentEntity}. */
    @ManyToOne
    @JoinColumn(name = "translation_owner_id")
    private TermsOfUse owner;

    /**
     * Creates a new CMSCollectionTranslation instance.
     */
    public TermsOfUseTranslation() {
    }

    /**
     * Creates a new CMSCollectionTranslation instance.
     *
     * @param language BCP 47 language code for this translation
     * @param value translated text value
     * @param owner owning TermsOfUse entity
     */
    public TermsOfUseTranslation(String language, String value, TermsOfUse owner) {
        super(language, value);
        this.owner = owner;
    }

    public TermsOfUseTranslation(TermsOfUseTranslation orig) {
        super(orig);
        this.owner = orig.owner;
    }

    /**
     * Getter for the field <code>owner</code>.
     *

     */
    public TermsOfUse getOwner() {
        return owner;
    }

    /**
     * Setter for the field <code>owner</code>.
     *
     * @param owner the TermsOfUse entity that this translation belongs to
     */
    public void setOwner(TermsOfUse owner) {
        this.owner = owner;
    }
}
