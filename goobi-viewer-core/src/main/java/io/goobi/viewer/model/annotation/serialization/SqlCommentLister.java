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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.comments.Comment;

/**
 * @author florian
 *
 */
public class SqlCommentLister implements AnnotationLister<Comment> {

    private static final Logger logger = LogManager.getLogger(SqlCommentLister.class);

    private final IDAO dao;

    public SqlCommentLister() throws DAOException {
        dao = DataManager.getInstance().getDao();
    }

    /**
     * @param dao2
     */
    public SqlCommentLister(IDAO dao) {
        this.dao = dao;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAllAnnotations()
     */
    @Override
    public List<Comment> getAllAnnotations() {
        try {
            return dao.getAllComments();
        } catch (DAOException e) {
            logger.error("Error listing comments", e);
            return Collections.emptyList();

        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getTotalAnnotationCount()
     */
    @Override
    public long getTotalAnnotationCount() {
        try {
            // TODO filter via PI whitelist here?
            return dao.getCommentCount(null, null, null);
        } catch (DAOException e) {
            logger.error("Error getting comment count", e);
            return 0;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotations(int, int, java.lang.String, java.util.List, java.util.List, java.util.List, java.lang.String, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<Comment> getAnnotations(int firstIndex, int items, String textQuery, List<String> motivations, List<Long> generators,
            List<Long> creators, String targetPi, Integer targetPage, String sortField, boolean sortDescending) {
        try {
            // TODO filter via PI whitelist here?
            List<Comment> allAnnos = dao.getComments(0, Integer.MAX_VALUE, sortField, sortDescending, null, null);
            Stream<Comment> stream = allAnnos.stream();
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
            return stream.skip(firstIndex).limit(items).collect(Collectors.toList());
        } catch (DAOException e) {
            logger.error("Error retrieving annotations: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotationCount(java.lang.String, java.util.List, java.util.List, java.util.List, java.lang.String, java.lang.Integer)
     */
    @Override
    public long getAnnotationCount(String textQuery, List<String> motivations, List<Long> generators, List<Long> creators, String targetPi,
            Integer targetPage) {
        return getAnnotations(0, Integer.MAX_VALUE, textQuery, motivations, generators, creators, targetPi, targetPage, "id", false).size();

    }

    @Override
    public Optional<Comment> getAnnotation(Long id) {
        try {
            return Optional.ofNullable(dao.getComment(id));
        } catch (DAOException e) {
            logger.error("Error loading comment with id " + id, e);
            return Optional.empty();
        }
    }

}
