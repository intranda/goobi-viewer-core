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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.AnnotationSqlSaver;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.translations.IPolyglott;
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
        AnnotationConverter converter = new AnnotationConverter();
        AnnotationSaver saver = new AnnotationSqlSaver(dao);
        List<Object[]> comments = dao.createNativeQuery("SELECT * FROM comments").getResultList();
        for (Object[] comment : comments) {
            Timestamp dateCreated = (Timestamp) comment[1];
            
        }
    }

    private void updateCrowdsourcingAnnotations(IDAO dao) throws DAOException {
        List<PersistentAnnotation> annotations = dao.getAnnotations(0, Integer.MAX_VALUE, null, false, Collections.singletonMap("motivation", "NULL"));
        for (PersistentAnnotation pa : annotations) {
            pa.setMotivation(Motivation.DESCRIBING);
            if(StringUtils.isBlank(pa.getAccessCondition())) {
                CrowdsourcingStatus status = pa.getReviewStatus();
                switch(status) {
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

    private String getAccessConditionForAnnotation(PersistentAnnotation pa) throws DAOException {
        String access = Optional.ofNullable(pa.getGenerator())
        .map(Question::getOwner)
        .filter(Campaign::isRestrictAnnotationAccess)
        .map(c -> c.getTitle(IPolyglott.getDefaultLocale().getLanguage()))
        .orElse(SolrConstants.OPEN_ACCESS_VALUE);
        return access;
    }

}
