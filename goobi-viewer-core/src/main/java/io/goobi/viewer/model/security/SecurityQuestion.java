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
package io.goobi.viewer.model.security;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a security question with a message-key for the question text and a set of
 * accepted answers, tracking whether the question has already been answered.
 */
public class SecurityQuestion {

    private final String questionKey;
    private final Set<String> correctAnswers;
    private boolean answered = false;

    /**
     * Constructor.
     *
     * @param questionKey message key for the question text to display
     * @param correctAnswers set of accepted answer strings (case-insensitive)
     */
    public SecurityQuestion(String questionKey, Set<String> correctAnswers) {
        this.questionKey = questionKey;
        this.correctAnswers = correctAnswers;
    }

    /**
     *
     * @param answer user-provided answer to validate
     * @return true if given answer is among correct answers; false otherwise
     * @should return true on correct answer
     * @should return true on correct answer and ignore case
     * @should return false on incorrect answer
     * @should return false empty answer
     * @should mark question as answered
     */
    public boolean isAnswerCorrect(String answer) {
        answered = true;
        if (StringUtils.isBlank(answer)) {
            return false;
        }

        return correctAnswers.contains(answer.toLowerCase());
    }

    
    public String getQuestionKey() {
        return questionKey;
    }

    
    public Set<String> getCorrectAnswers() {
        return correctAnswers;
    }

    
    public boolean isAnswered() {
        return answered;
    }

}
