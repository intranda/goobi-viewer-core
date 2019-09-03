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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.goobi.viewer.model.misc.Translation;

/**
 * A persistence object holding a translated String value
 * 
 * @author Florian Alpers
 *
 */
@Entity
@Table(name = "cs_campaign_translations")
public class CampaignTranslation extends Translation {

    /** Reference to the owning {@link PersistentEntity}. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Campaign owner;

    /**
     * 
     */
    public CampaignTranslation() {
        super();
    }

    /**
     * 
     * @param language
     * @param tag
     * @param value
     * @param owner
     */
    public CampaignTranslation(String language, String tag, String value, Campaign owner) {
        super(language, tag, value);
        this.owner = owner;
    }

    /**
     * 
     * @param translations
     * @param lang
     * @param value
     * @param tag
     * @param owner
     */
    public static void setTranslation(List<CampaignTranslation> translations, String lang, String value, String tag, Campaign owner) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        for (CampaignTranslation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setValue(value);
                return;
            }
        }
        translations.add(new CampaignTranslation(lang, tag, value, owner));
    }

    /**
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }
}
