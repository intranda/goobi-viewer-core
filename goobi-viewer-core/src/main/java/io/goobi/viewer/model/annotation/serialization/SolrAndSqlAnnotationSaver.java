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

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class SolrAndSqlAnnotationSaver implements AnnotationSaver {

    private final SolrAnnotationSaver solrSaver;
    private final SqlAnnotationSaver sqlSaver;

    public SolrAndSqlAnnotationSaver() throws IndexUnreachableException, DAOException {
        solrSaver = new SolrAnnotationSaver();
        sqlSaver = new SqlAnnotationSaver();
    }

    @Override
    public void save(PersistentAnnotation... annotations) throws IOException {
        //first save to sql then to solr because solr will take longer anyway and sql is the main data-source
        sqlSaver.save(annotations);
        solrSaver.save(annotations);
    }

}
