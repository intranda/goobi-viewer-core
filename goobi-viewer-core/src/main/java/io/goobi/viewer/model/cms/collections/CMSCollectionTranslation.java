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

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;

import io.goobi.viewer.model.translations.Translation;

/**
 * A persistence object holding a translated String value
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "translations")
public class CMSCollectionTranslation extends Translation implements Serializable {

    private static final long serialVersionUID = 6110495225727273302L;

    /** Reference to the owning {@link PersistentEntity}. */
    @ManyToOne
    @JoinColumn(name = "translation_owner_id")
    private CMSCollection owner;

    /**
     * <p>
     * Constructor for CMSCollectionTranslation.
     * </p>
     */
    public CMSCollectionTranslation() {
    }

    /**
     * <p>
     * Constructor for CMSCollectionTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public CMSCollectionTranslation(String language, String value) {
        super(language, value);
    }

    /**
     * Cloning constructor
     * 
     * @param tr
     * @param owner
     */
    public CMSCollectionTranslation(CMSCollectionTranslation tr, CMSCollection owner) {
        this.id = tr.id;
        this.language = tr.language;
        this.owner = owner;
        this.tag = tr.tag;
        this.value = tr.value;
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public CMSCollection getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(CMSCollection owner) {
        this.owner = owner;
    }
}
