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
package io.goobi.viewer.dao.update;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationSaver;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
public class AnnotationUpdate implements IModelUpdate {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationUpdate.class);

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
     */
    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        updateCrowdsourcingAnnotations(dao);
        updateComments(dao);
        return true;
    }

    /**
     * @param dao
     * @throws DAOException
     */
    private void updateComments(IDAO dao) throws DAOException {
        AnnotationSaver saver = new SqlAnnotationSaver(dao);
        List<Object[]> comments = dao.createNativeQuery("SELECT * FROM comments").getResultList();
        for (Object[] comment : comments) {

            try {
                LocalDateTime dateCreated = Optional.ofNullable(comment[1]).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                LocalDateTime dateUpdated = Optional.ofNullable(comment[2]).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                Integer page = Optional.ofNullable(comment[3]).map(o -> (Integer) o).orElse(null);
                String pi = Optional.ofNullable(comment[4]).map(o -> (String) o).orElse(null);
                String text = Optional.ofNullable(comment[5]).map(o -> (String) o).orElse(null);
                User owner = Optional.ofNullable(comment[6]).map(o -> (Long) o).flatMap(id -> this.getUser(id, dao)).orElse(null);

                Comment anno = new Comment();
                anno.setDateCreated(dateCreated);
                anno.setDateModified(dateUpdated);
                anno.setTargetPI(pi);
                anno.setTargetPageOrder(page);
                anno.setCreator(owner);
                anno.setBody(getAsJson(text));
                anno.setMotivation(Motivation.COMMENTING);
                anno.setPublicationStatus(PublicationStatus.PUBLISHED);
                anno.setAccessCondition(SolrConstants.OPEN_ACCESS_VALUE);

                saver.save(anno);
            } catch (IOException e) {
                throw new DAOException(e.toString());
            }
        }
//        dao.startTransaction();
//        dao.createNativeQuery("DROP TABLE comments").executeUpdate();
//        dao.commitTransaction();
        
        List<Object[]> annotations = dao.createNativeQuery("SELECT * FROM annotations").getResultList();
        for (Object[] annotation : annotations) {

            try {
                Long annotationId = (Long)annotation[0];
                String accessCondition = Optional.ofNullable(annotation[1]).map(o -> (String) o).orElse(null);
                String body = Optional.ofNullable(annotation[2]).map(o -> (String) o).orElse(null);
                User owner = Optional.ofNullable(annotation[3]).map(o -> (Long) o).flatMap(id -> this.getUser(id, dao)).orElse(null);
                LocalDateTime dateCreated = Optional.ofNullable(annotation[4]).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                LocalDateTime dateUpdated = Optional.ofNullable(annotation[5]).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                Long generatorId = Optional.ofNullable(annotation[6]).map(o -> (Long) o).orElse(null);
                //motivation
                //reviewer
                //target
                String pi = Optional.ofNullable(annotation[10]).map(o -> (String) o).orElse(null);
                Integer page = Optional.ofNullable(annotation[11]).map(o -> (Integer) o).orElse(null);
                //publicationStatus

                Comment anno = new Comment();
                anno.setDateCreated(dateCreated);
                anno.setDateModified(dateUpdated);
                anno.setTargetPI(pi);
                anno.setTargetPageOrder(page);
                anno.setCreator(owner);
                anno.setBody(getAsJson(text));
                anno.setMotivation(Motivation.COMMENTING);
                anno.setPublicationStatus(PublicationStatus.PUBLISHED);
                anno.setAccessCondition(SolrConstants.OPEN_ACCESS_VALUE);

                saver.save(anno);
            } catch (IOException e) {
                throw new DAOException(e.toString());
            }
        }
    }

    /**
     * @param text
     * @return
     * @throws JsonProcessingException
     */
    private String getAsJson(String text) throws JsonProcessingException {
        IResource body = new TextualResource(text);
        return new ObjectMapper().writeValueAsString(body);
    }

    private Optional<User> getUser(Long id, IDAO dao) {
        try {
            return Optional.ofNullable(dao.getUser(id));
        } catch (DAOException e) {
            return Optional.empty();
        }
    }

    private void updateCrowdsourcingAnnotations(IDAO dao) throws DAOException {
        List<CrowdsourcingAnnotation> annotations =
                dao.getAnnotations(0, Integer.MAX_VALUE, null, false, Collections.singletonMap("motivation", "NULL"));
        for (CrowdsourcingAnnotation pa : annotations) {
            pa.setMotivation(Motivation.DESCRIBING);
            if (StringUtils.isBlank(pa.getAccessCondition())) {
                CrowdsourcingStatus status = pa.getReviewStatus();
                switch (status) {
                    case ANNOTATE:
                    case REVIEW:
                        pa.setAccessCondition(status.name());
                        break;
                    case FINISHED:
                        String access = getAccessConditionForAnnotation(pa);
                        pa.setAccessCondition(access);
                }
            }
            DataManager.getInstance().getDao().updateAnnotation(pa);
        }
    }

    private String getAccessConditionForAnnotation(CrowdsourcingAnnotation pa) throws DAOException {
        String access = Optional.ofNullable(pa.getGenerator())
                .map(Question::getOwner)
                .filter(Campaign::isRestrictAnnotationAccess)
                .map(c -> c.getTitle(IPolyglott.getDefaultLocale().getLanguage()))
                .orElse(SolrConstants.OPEN_ACCESS_VALUE);
        return access;
    }

}
