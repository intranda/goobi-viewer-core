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

import java.io.IOException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;

/**
 * @author florian
 *
 */
public class SqlAnnotationSaver implements AnnotationSaver {

    private final IDAO dao;

    public SqlAnnotationSaver() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
    }

    public SqlAnnotationSaver(IDAO dao) {
        this.dao = dao;
    }

    @Override
    public void save(PersistentAnnotation... annotations) throws IOException {
        for (PersistentAnnotation annotation : annotations) {
            if (annotation instanceof Comment) {
                Comment comment = (Comment) annotation;
                try {
                    if (comment.getId() != null) {
                        dao.updateComment(comment);
                    } else {
                        dao.addComment(comment);
                    }
                } catch (DAOException e) {
                    throw new IOException(e);
                }
            } else if (annotation instanceof CrowdsourcingAnnotation) {
                try {
                    if (annotation.getId() != null) {
                        dao.updateAnnotation((CrowdsourcingAnnotation) annotation);
                    } else {
                        dao.addAnnotation((CrowdsourcingAnnotation) annotation);
                    }
                } catch (DAOException e) {
                    throw new IOException(e);
                }
            } else {
                throw new IllegalArgumentException("Saving not implemented for class " + annotation.getClass());
            }
        }
    }

}
