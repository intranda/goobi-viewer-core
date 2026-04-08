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
package io.goobi.viewer.model.administration;

import java.util.List;

import io.goobi.viewer.model.translations.Translation;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_mode_translations")
public class MaintenanceModeTranslation extends Translation {

    /** Reference to the owning {@link MaintenanceMode}. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private MaintenanceMode owner;
    
    /**
     * Default constructor.
     */
    public MaintenanceModeTranslation() {
        super();
    }

    /**
     * Creates a new MaintenanceModeTranslation instance.
     *
     * @param language BCP 47 language code for this translation
     * @param tag key identifying the translated field
     * @param value translated text value
     * @param owner owning MaintenanceMode entity
     */
    public MaintenanceModeTranslation(String language, String tag, String value, MaintenanceMode owner) {
        super(language, tag, value);
        this.owner = owner;
    }

    /**
     * setTranslation.
     *
     * @param translations mutable list of existing translations to update
     * @param lang BCP 47 language code to set or add
     * @param value translated text to assign
     * @param tag key identifying the translated field
     * @param owner maintenance mode entity to assign when creating a new translation
     */
    public static void setTranslation(List<MaintenanceModeTranslation> translations, String lang, String value, String tag, MaintenanceMode owner) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        for (MaintenanceModeTranslation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setTranslationValue(value);
                return;
            }
        }
        translations.add(new MaintenanceModeTranslation(lang, tag, value, owner));
    }

    /**
     * @return the owner
     */
    public MaintenanceMode getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(MaintenanceMode owner) {
        this.owner = owner;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        MaintenanceModeTranslation other = (MaintenanceModeTranslation) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        
        return true;
    }
}
