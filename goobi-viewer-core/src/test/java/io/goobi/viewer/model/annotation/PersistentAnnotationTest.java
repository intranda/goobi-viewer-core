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
package io.goobi.viewer.model.annotation;

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.dao.impl.JPADAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.crowdsourcing.questions.QuestionType;
import io.goobi.viewer.model.crowdsourcing.questions.TargetSelector;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class PersistentAnnotationTest extends AbstractDatabaseEnabledTest {

    private WebAnnotation annotation;
    private PersistentAnnotation daoAnno;
    private User creator;
    private Question generator;
    private IResource body;
    private IResource target;

    private static AbstractApiUrlManager urls;
    private static AnnotationsResourceBuilder annoBuilder;

    private static ObjectMapper mapper;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseEnabledTest.setUpClass();
        urls = new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl());
        annoBuilder = new AnnotationsResourceBuilder(urls);

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        creator = DataManager.getInstance().getDao().getUser(2);

        Campaign campaign = new Campaign();
        campaign.setId(5l);
        generator = new Question(campaign);
        generator.setId(4l);
        generator.setQuestionType(QuestionType.PLAINTEXT);
        generator.setTargetFrequency(0);
        generator.setTargetSelector(TargetSelector.WHOLE_PAGE);

        annotation = new WebAnnotation(URI.create("http://www.example.com/anno/1"));
        annotation.setCreated(DateTools.createDate(2019, 01, 22, 12, 54));
        annotation.setModified(DateTools.createDate(2019, 8, 11, 17, 13));
        annotation.setCreator(new Agent(URI.create(creator.getId().toString()), AgentType.PERSON, creator.getNickName()));
        annotation.setGenerator(new Agent(URI.create(generator.getId().toString()), AgentType.SOFTWARE, ""));

        body = new TextualResource("annotation text");
        target = new SpecificResource(URI.create("http://www.example.com/manifest/7/canvas/10"),
                new FragmentSelector(new Rectangle(10, 20, 100, 200)));

        annotation.setBody(body);
        annotation.setTarget(target);

        daoAnno = new PersistentAnnotation(annotation, null, "7", 10);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSerialize() throws JsonParseException, JsonMappingException, IOException {

        String bodyString = daoAnno.getBody();
        Assert.assertEquals("{\"type\":\"TextualBody\",\"format\":\"text/plain\",\"value\":\"annotation text\"}", bodyString);

        String targetString = daoAnno.getTarget();
        Assert.assertEquals(
                "{\"type\":\"SpecificResource\",\"selector\":{\"value\":\"xywh=10,20,100,200\",\"type\":\"FragmentSelector\"},\"source\":\"http://www.example.com/manifest/7/canvas/10\"}"
                        + "",
                targetString);

        IResource retrievedBody = annoBuilder.getBodyAsResource(daoAnno);
        Assert.assertEquals(body, retrievedBody);

        IResource retrievedTarget = annoBuilder.getTargetAsResource(daoAnno);
        Assert.assertEquals(target, retrievedTarget);

    }

    @Test
    public void testSave() throws DAOException, JsonParseException, JsonMappingException, IOException {

        JPADAO dao = (JPADAO) DataManager.getInstance().getDao();

        EntityManager em = dao.getFactory().createEntityManager();

        long existingAnnotations = getAnnotations(em).size();

        try {
            em.getTransaction().begin();
            em.persist(this.daoAnno);
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        em = dao.getFactory().createEntityManager();
        try {
            List<PersistentAnnotation> list = getAnnotations(em);
            Assert.assertEquals(existingAnnotations + 1, list.size());

            PersistentAnnotation retrieved = list.get(list.size() - 1);
            Assert.assertEquals(body, annoBuilder.getBodyAsResource(retrieved));
            Assert.assertEquals(target, annoBuilder.getTargetAsResource(retrieved));

        } finally {
            em.close();
        }
    }

    /**
     * @param em
     * @return
     */
    @SuppressWarnings("unchecked")
    private static List<PersistentAnnotation> getAnnotations(EntityManager em) {
        Query q = em.createQuery("SELECT c FROM PersistentAnnotation c");
        q.setFlushMode(FlushModeType.COMMIT);
        List<PersistentAnnotation> list = q.getResultList();
        return list;
    }

    @Test
    public void testPersistAnnotation() throws DAOException {
        boolean added = DataManager.getInstance().getDao().addAnnotation(daoAnno);
        Assert.assertTrue(added);
        URI uri = URI.create(Long.toString(daoAnno.getId()));
        PersistentAnnotation fromDAO = DataManager.getInstance().getDao().getAnnotation(daoAnno.getId());
        WebAnnotation webAnno = annoBuilder.getAsWebAnnotation(daoAnno);
        WebAnnotation fromDAOWebAnno = annoBuilder.getAsWebAnnotation(daoAnno);
        Assert.assertEquals(webAnno.getBody(), fromDAOWebAnno.getBody());
        Assert.assertEquals(webAnno.getTarget(), fromDAOWebAnno.getTarget());
        Assert.assertEquals(webAnno, fromDAOWebAnno);

        Date changed = new Date();
        fromDAO.setDateModified(changed);
        boolean updated = DataManager.getInstance().getDao().updateAnnotation(fromDAO);

        PersistentAnnotation fromDAO2 = DataManager.getInstance().getDao().getAnnotation(daoAnno.getId());
        Assert.assertEquals(fromDAO.getDateModified(), fromDAO2.getDateModified());
    }

    @Test
    public void testGetContent_fromOA() throws JsonParseException, JsonMappingException, IOException, DAOException {
        String content = "{\n" +
                "        \"@type\": \"cnt:ContentAsText\",\n" +
                "        \"format\": \"text/plain\",\n" +
                "        \"chars\": \"GROSHERZOGLICH\"\n" +
                "    }";
        PersistentAnnotation pAnno = new PersistentAnnotation();
        pAnno.setBody(content);
        assertEquals("GROSHERZOGLICH", pAnno.getContentString());
    }

    @Test
    public void testGetContent_fromWA() throws JsonParseException, JsonMappingException, IOException, DAOException {
        String content = "{\n" +
                "        \"type\": \"TextualBody\",\n" +
                "        \"format\": \"text/plain\",\n" +
                "        \"value\": \"GROSHERZOGLICH\"\n" +
                "    }";
        PersistentAnnotation pAnno = new PersistentAnnotation();
        pAnno.setBody(content);
        assertEquals("GROSHERZOGLICH", pAnno.getContentString());
    }

}
