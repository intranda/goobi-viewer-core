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
package io.goobi.viewer.model.crowdsourcing.questions;

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
@Table(name = "cs_query_translations")
public class QuestionTranslation extends Translation {

    /** Reference to the owning {@link PersistentEntity}. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Question owner;

    /**
     * 
     */
    public QuestionTranslation() {
        super();
    }

    /**
     * 
     * @param language
     * @param tag
     * @param value
     * @param owner
     */
    public QuestionTranslation(String language, String tag, String value, Question owner) {
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
    public static void setTranslation(List<QuestionTranslation> translations, String lang, String value, String tag, Question owner) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        for (Translation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setValue(value);
                return;
            }
        }
        translations.add(new QuestionTranslation(lang, tag, value, owner));
    }

    /**
     * @return the owner
     */
    public Question getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Question owner) {
        this.owner = owner;
    }
}
