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

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.misc.Translation;

/**
 * @author florian
 *
 */
public class QuestionTest extends AbstractDatabaseEnabledTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void loadTranslation() throws DAOException {
        Question question1 = DataManager.getInstance().getDao().getCampaign(1l).getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
        Assert.assertNotNull(question1);
        String trEnglish = question1.getText().getText(Locale.ENGLISH);
        Assert.assertNotNull(trEnglish);
        Assert.assertEquals("English text", trEnglish);
    }
    
    @Test
    public void addTranslation() throws DAOException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);
        Question question1 = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
        Assert.assertNotNull(question1);
        Assert.assertEquals("", question1.getText().getText(Locale.GERMAN));
        Assert.assertEquals(1, question1.getText().toMap().size());
        question1.getText().setText("deutscher Text", Locale.GERMAN);
        Assert.assertEquals("deutscher Text", question1.getText().getText(Locale.GERMAN));
        Assert.assertEquals(2, question1.getText().toMap().size());
    }
    
    @Test
    public void persistTranslation() throws DAOException {
        try {            
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);
            Question question = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
            question.getText().setText("TEST", Locale.GERMAN);
            DataManager.getInstance().getDao().updateCampaign(campaign);
            
            campaign = DataManager.getInstance().getDao().getCampaign(1l);
            question = campaign.getQuestions().stream().filter(q -> q.getId().equals(1l)).findFirst().orElse(null);
            Assert.assertEquals("TEST", question.getText().getText(Locale.GERMAN));
        } catch(Throwable e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

}
