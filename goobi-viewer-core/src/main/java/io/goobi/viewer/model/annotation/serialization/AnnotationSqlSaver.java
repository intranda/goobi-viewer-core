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
package io.goobi.viewer.model.annotation.serialization;

import java.io.IOException;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class AnnotationSqlSaver implements AnnotationSaver {

    private final IDAO dao; 
    private final AnnotationConverter converter;

    public AnnotationSqlSaver() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
        this.converter = new AnnotationConverter();
    }
    
    public AnnotationSqlSaver(IDAO dao) {
        this.dao = dao;
        this.converter = new AnnotationConverter();

    }
    
    @Override
    public void save(PersistentAnnotation... annotations) throws IOException {
        for (PersistentAnnotation annotation : annotations) {            
            try {            
                if(annotation.getId() != null) {
                    dao.updateAnnotation(annotation);
                } else {
                    dao.addAnnotation(annotation);
                }
            } catch(DAOException e) {
                throw new IOException(e);
            }
        }
    }

}
