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
package io.goobi.viewer.model.crowdsourcing.questions;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;

/**
 * @author florian
 *
 */
class QuestionTest extends AbstractDatabaseEnabledTest {

    /** {@inheritDoc} */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void loadTranslation() throws DAOException {
        Question question1 =
                DataManager.getInstance().getDao().getCampaign(1l).getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
        Assertions.assertNotNull(question1);
        String trEnglish = question1.getText().getText(Locale.ENGLISH);
        Assertions.assertNotNull(trEnglish);
        Assertions.assertEquals("English text", trEnglish);
    }

    @Test
    void addTranslation() throws DAOException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);
        Question question1 = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
        Assertions.assertNotNull(question1);
        Assertions.assertEquals("", question1.getText().getText(Locale.GERMAN));
        Assertions.assertEquals(1, question1.getText().toMap().size());
        question1.getText().setText("deutscher Text", Locale.GERMAN);
        Assertions.assertEquals("deutscher Text", question1.getText().getText(Locale.GERMAN));
        Assertions.assertEquals(2, question1.getText().toMap().size());
    }

    @Test
    void persistTranslation() {
        try {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);
            Question question = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
            question.getText().setText("TEST", Locale.GERMAN);
            DataManager.getInstance().getDao().updateCampaign(campaign);

            campaign = DataManager.getInstance().getDao().getCampaign(1l);
            question = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
            Assertions.assertEquals("TEST", question.getText().getText(Locale.GERMAN));
        } catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail(e.toString());
        }
    }

    /**
     * @see Question#getCampaignId(URI)
     * @verifies extract id correctly
     */
    @Test
    void getCampaignId_shouldExtractIdCorrectly() throws Exception {
        Assertions.assertEquals(Long.valueOf(1234567890L),
                Question.getCampaignId(new URI("https://example.com/viewer/crowdsourcing/campaigns/1234567890/questions/9876543210/")));
    }

    /**
     * @see Question#getQuestionId(URI)
     * @verifies extract id correctly
     */
    @Test
    void getQuestionId_shouldExtractIdCorrectly() throws Exception {
        Assertions.assertEquals(Long.valueOf(9876543210L),
                Question.getQuestionId(new URI("https://example.com/viewer/crowdsourcing/campaigns/1234567890/questions/9876543210/")));
    }
}
