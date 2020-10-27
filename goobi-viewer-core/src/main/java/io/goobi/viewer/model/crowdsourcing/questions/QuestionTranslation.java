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

import javax.persistence.Entity;
import javax.persistence.Table;

import io.goobi.viewer.model.misc.PersistentTranslation;
import io.goobi.viewer.model.misc.Translation;

/**
 * A persistence object holding a translated String value
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "cs_question_translations")
public class QuestionTranslation extends PersistentTranslation<Question> {

    public QuestionTranslation() {
        super();
    }
    
    public QuestionTranslation(Translation t, Question q) {
        super(t, q);
    }
    
    public QuestionTranslation(Question q, String language, String value) {
        super(q);
        this.language = language;
        this.value = value;
    }

}
