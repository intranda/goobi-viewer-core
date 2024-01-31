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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.SecurityQuestion;

class CaptchaBeanTest {
    
    /**
     * @see CaptchaBean#resetSecurityQuestion()
     * @verifies not reset securityQuest if not yet answered
     */
    @Test
    void resetSecurityQuestion_shouldNotResetSecurityQuestIfNotYetAnswered() throws Exception {
        CaptchaBean bean = new CaptchaBean();
        Assertions.assertTrue(bean.resetSecurityQuestion());
        SecurityQuestion sq = bean.getSecurityQuestion();
        Assertions.assertNotNull(sq);
        Assertions.assertFalse(sq.isAnswered());
        for (int i = 0; i < 10; ++i) {
            Assertions.assertTrue(bean.resetSecurityQuestion());
        }
        Assertions.assertEquals(sq, bean.getSecurityQuestion());
    }
}