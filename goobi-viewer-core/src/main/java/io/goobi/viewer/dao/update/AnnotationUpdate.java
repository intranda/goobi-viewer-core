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
package io.goobi.viewer.dao.update;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationSaver;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
public class AnnotationUpdate implements IModelUpdate {


    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
     */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {

        int updates = 0;

        if (dao.tableExists("annotations")) {
            updateCrowdsourcingAnnotations(dao);
            updates++;
        }
        if (dao.tableExists("comments")) {
            updateComments(dao);
            updates++;
        }

        return updates > 0;
    }

    /**
     * @param dao
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    private static void updateCrowdsourcingAnnotations(IDAO dao) throws DAOException {
        AnnotationSaver saver = new SqlAnnotationSaver(dao);

        List<Object[]> info = dao.getNativeQueryResults("SHOW COLUMNS FROM annotations");

        List<Object[]> annotations = dao.getNativeQueryResults("SELECT * FROM annotations");

        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());

        for (Object[] annotation : annotations) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> annotation[i] != null)
                    .collect(Collectors.toMap(columnNames::get, i -> annotation[i]));
            try {
                String body = Optional.ofNullable(columns.get("body")).map(o -> (String) o).orElse(null);
                User owner = Optional.ofNullable(columns.get("creator_id")).map(o -> (Long) o).flatMap(id -> getUser(id, dao)).orElse(null);
                User reviewer = Optional.ofNullable(columns.get("reviewer_id")).map(o -> (Long) o).flatMap(id -> getUser(id, dao)).orElse(null);
                Question generator =
                        Optional.ofNullable(columns.get("generator_id")).map(o -> (Long) o).flatMap(id -> getCampaignQuestion(id, dao)).orElse(null);
                LocalDateTime dateCreated =
                        Optional.ofNullable(columns.get("date_created")).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                LocalDateTime dateUpdated =
                        Optional.ofNullable(columns.get("date_modified")).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                String motivation = Optional.ofNullable(columns.get("motivation")).map(o -> (String) o).orElse(Motivation.DESCRIBING);
                String target = Optional.ofNullable(columns.get("target")).map(o -> (String) o).orElse(null);
                String pi = Optional.ofNullable(columns.get("target_pi")).map(o -> (String) o).orElse(null);
                Integer page = Optional.ofNullable(columns.get("target_page")).map(o -> (Integer) o).orElse(null);
                String accessCondition =
                        Optional.ofNullable(columns.get("access_condition")).map(o -> (String) o).orElse(SolrConstants.OPEN_ACCESS_VALUE);
                PublicationStatus status = Optional.ofNullable(columns.get("publication_status"))
                        .map(o -> (Integer) o)
                        .map(i -> PublicationStatus.values()[i])
                        .orElse(null);
                if (status == null) {
                    //status is encoded in access_condition
                    if ("ANNOTATE".equals(accessCondition)) {
                        accessCondition = getAccessConditionForAnnotation(generator);
                        status = PublicationStatus.CREATING;
                    } else if ("REVIEW".equals(accessCondition)) {
                        accessCondition = getAccessConditionForAnnotation(generator);
                        status = PublicationStatus.REVIEW;
                    } else {
                        status = PublicationStatus.PUBLISHED;
                    }
                }

                CrowdsourcingAnnotation anno = new CrowdsourcingAnnotation();
                anno.setDateCreated(dateCreated);
                anno.setDateModified(dateUpdated);
                anno.setTargetPI(pi);
                anno.setTargetPageOrder(page);
                anno.setCreator(owner);
                anno.setBody(body);
                anno.setMotivation(motivation);
                anno.setPublicationStatus(status);
                anno.setAccessCondition(accessCondition);
                anno.setGenerator(generator);
                anno.setReviewer(reviewer);
                anno.setTarget(target);

                saver.save(anno);
            } catch (IOException e) {
                throw new DAOException(e.toString());
            }

        }
        dao.executeUpdate("DROP TABLE annotations");

    }

    /**
     * @param dao
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    private static void updateComments(IDAO dao) throws DAOException {
        AnnotationSaver saver = new SqlAnnotationSaver(dao);
        List<Object[]> comments = dao.getNativeQueryResults("SELECT * FROM comments");
        List<Object[]> info = dao.getNativeQueryResults("SHOW COLUMNS FROM comments");
        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());
        for (Object[] comment : comments) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> comment[i] != null)
                    .collect(Collectors.toMap(columnNames::get, i -> comment[i]));

            try {
                LocalDateTime dateCreated =
                        Optional.ofNullable(columns.get("date_created")).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                LocalDateTime dateUpdated =
                        Optional.ofNullable(columns.get("date_updated")).map(o -> (Timestamp) o).map(Timestamp::toLocalDateTime).orElse(null);
                Integer page = Optional.ofNullable(columns.get("page")).map(o -> (Integer) o).orElse(null);
                String pi = Optional.ofNullable(columns.get("pi")).map(o -> (String) o).orElse(null);
                String text = Optional.ofNullable(columns.get("text")).map(o -> (String) o).orElse(null);
                User owner = Optional.ofNullable(columns.get("owner_id")).map(o -> (Long) o).flatMap(id -> getUser(id, dao)).orElse(null);

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
        dao.executeUpdate("DROP TABLE comments");

    }

    /**
     * @param id
     * @param dao
     * @return
     */
    private static Optional<Question> getCampaignQuestion(Long id, IDAO dao) {
        try {
            return Optional.ofNullable(dao.getQuestion(id));
        } catch (DAOException e) {
            return Optional.empty();
        }
    }

    /**
     * @param text
     * @return
     * @throws JsonProcessingException
     */
    private static String getAsJson(String text) throws JsonProcessingException {
        IResource body = new TextualResource(text);
        return new ObjectMapper().writeValueAsString(body);
    }

    /**
     * 
     * @param id
     * @param dao
     * @return
     */
    private static Optional<User> getUser(Long id, IDAO dao) {
        try {
            return Optional.ofNullable(dao.getUser(id));
        } catch (DAOException e) {
            return Optional.empty();
        }
    }

    /**
     * 
     * @param generator
     * @return
     */
    private static String getAccessConditionForAnnotation(Question generator) {
        return Optional.ofNullable(generator)
                .map(Question::getOwner)
                .filter(Campaign::isRestrictAnnotationAccess)
                .map(Campaign::getAccessConditionValue)
                .orElse(SolrConstants.OPEN_ACCESS_VALUE);
    }

}
