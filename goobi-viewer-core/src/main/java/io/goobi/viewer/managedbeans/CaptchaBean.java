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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.SecurityQuestion;

/**
 * Handles security question checks.
 */
@Named
@ViewScoped
public class CaptchaBean implements Serializable {

    private static final long serialVersionUID = -8342047587240066920L;

    private static final Logger logger = LogManager.getLogger(CaptchaBean.class);

    private transient SecurityQuestion securityQuestion = null;
    private transient String securityAnswer;

    private final Configuration config;

    /** Reusable Random object. */
    private Random random = new SecureRandom();

    public CaptchaBean() {
        this.config = DataManager.getInstance().getConfiguration();
    }

    public CaptchaBean(Configuration config) {
        this.config = config;
    }

    /**
     * Selects a random security question from configured list and sets <code>currentSecurityQuestion</code> to it.
     *
     * @return always true (do not change since that would break rendered conditions on security questions in xhtml)
     * @should not reset securityQuest if not yet answered
     */
    public boolean resetSecurityQuestion() {
        List<SecurityQuestion> questions = config.getSecurityQuestions();
        if (!questions.isEmpty() && (securityQuestion == null || securityQuestion.isAnswered())) {
            // Reset if questions not empty and security question is not yet set or has been already answered
            securityQuestion = questions.get(random.nextInt(questions.size()));
        }

        return true;
    }

    /**
     * 
     * @return true if answer correct; false otherwise
     */
    public boolean checkAnswer() {
        return securityQuestion != null && securityQuestion.isAnswerCorrect(securityAnswer);
    }

    /**
     * @return the securityQuestion
     */
    public SecurityQuestion getSecurityQuestion() {
        return securityQuestion;
    }

    /**
     * @return the securityAnswer
     */
    public String getSecurityAnswer() {
        return securityAnswer;
    }

    /**
     * @param securityAnswer the securityAnswer to set
     */
    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }
}
