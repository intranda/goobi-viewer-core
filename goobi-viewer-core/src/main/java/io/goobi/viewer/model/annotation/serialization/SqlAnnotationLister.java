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
package io.goobi.viewer.model.annotation.serialization;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;

/**
 * @author florian
 *
 */
public class SqlAnnotationLister implements AnnotationLister<CrowdsourcingAnnotation> {

    private static final Logger logger = LogManager.getLogger(SqlAnnotationLister.class);

    private final IDAO dao;

    public SqlAnnotationLister() throws DAOException {
        dao = DataManager.getInstance().getDao();
    }

    /**
     * @param dao
     */
    public SqlAnnotationLister(IDAO dao) {
        this.dao = dao;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAllAnnotations()
     */
    @Override
    public List<CrowdsourcingAnnotation> getAllAnnotations() {
        try {
            return dao.getAllAnnotations("id", false);
        } catch (DAOException e) {
            logger.error("Error retrieving annotations: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getTotalAnnotationCount()
     */
    @Override
    public long getTotalAnnotationCount() {
        try {
            return dao.getTotalAnnotationCount();
        } catch (DAOException e) {
            logger.error("Error retrieving annotations: {}", e.toString());
            return 0;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotations(int, int, java.lang.String, java.util.List,
     * java.util.List, java.util.List, java.lang.String, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<CrowdsourcingAnnotation> getAnnotations(int firstIndex, int items, String textQuery, List<String> motivations, List<Long> generators,
            List<Long> creators, String targetPi, Integer targetPage, String sortField, boolean sortDescending) {
        try {
            List<CrowdsourcingAnnotation> allAnnos = dao.getAllAnnotations(sortField, sortDescending);
            Stream<CrowdsourcingAnnotation> stream = allAnnos.stream();
            if (StringUtils.isNotBlank(textQuery)) {
                stream = stream.filter(a -> a.getContentString().toLowerCase().contains(textQuery.toLowerCase()));
            }
            if (motivations != null && !motivations.isEmpty()) {
                stream = stream.filter(a -> a.getMotivation() != null)
                        .filter(a -> motivations.stream().anyMatch(m -> a.getMotivation().equalsIgnoreCase(m)));
            }
            if (generators != null && !generators.isEmpty()) {
                stream = stream.filter(a -> generators.stream().anyMatch(g -> Objects.equals(g, a.getGeneratorId())));
            }
            if (creators != null && !creators.isEmpty()) {
                stream = stream.filter(a -> creators.stream().anyMatch(c -> Objects.equals(c, a.getCreatorId())));
            }
            if (StringUtils.isNotBlank(targetPi)) {
                stream = stream.filter(a -> a.getTargetPI().equalsIgnoreCase(targetPi));
            }
            if (targetPage != null) {
                stream = stream.filter(a -> targetPage.equals(a.getTargetPageOrder()));
            }
            return stream.skip(firstIndex).limit(items).toList();
        } catch (DAOException e) {
            logger.error("Error retrieving annotations: {}", e.toString());
            return Collections.emptyList();
        }

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotationCount(java.lang.String, java.util.List, java.util.List, 
     * java.util.List, java.lang.String, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public long getAnnotationCount(String textQuery, List<String> motivations, List<Long> generators, List<Long> creators, String targetPi,
            Integer targetPage) {
        return getAnnotations(0, Integer.MAX_VALUE, textQuery, motivations, generators, creators, targetPi, targetPage, "id", false).size();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotation(java.lang.Long)
     */
    @Override
    public Optional<CrowdsourcingAnnotation> getAnnotation(Long id) {
        try {
            return Optional.ofNullable(dao.getAnnotation(id));
        } catch (DAOException e) {
            return Optional.empty();
        }
    }

}
