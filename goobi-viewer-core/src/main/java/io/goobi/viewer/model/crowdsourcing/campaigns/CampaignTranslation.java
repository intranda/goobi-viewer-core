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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.io.Serializable;
import java.util.List;

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
@Table(name = "cs_campaign_translations")
public class CampaignTranslation extends Translation implements Serializable {

    private static final long serialVersionUID = 3598889812893282906L;

    /** Reference to the owning {@link Campaign}. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Campaign owner;

    /**
     * <p>
     * Constructor for CampaignTranslation.
     * </p>
     */
    public CampaignTranslation() {
        super();
    }

    /**
     * <p>
     * Constructor for CampaignTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param tag a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param owner a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     */
    public CampaignTranslation(String language, String tag, String value, Campaign owner) {
        super(language, tag, value);
        this.owner = owner;
    }

    public CampaignTranslation(CampaignTranslation orig) {
        super(orig.language, orig.tag, orig.translationValue);
        this.id = orig.id;
        this.owner = orig.owner;
    }

    public CampaignTranslation(CampaignTranslation orig, Campaign owner) {
        this(orig);
        this.owner = owner;
    }

    /**
     * <p>
     * setTranslation.
     * </p>
     *
     * @param translations a {@link java.util.List} object.
     * @param lang a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param tag a {@link java.lang.String} object.
     * @param owner a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
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
                translation.setTranslationValue(value);
                return;
            }
        }
        translations.add(new CampaignTranslation(lang, tag, value, owner));
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }
}
