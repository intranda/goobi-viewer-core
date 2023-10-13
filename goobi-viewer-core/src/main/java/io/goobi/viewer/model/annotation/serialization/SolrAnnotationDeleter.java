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
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class SolrAnnotationDeleter implements AnnotationDeleter {

    public SolrAnnotationDeleter() throws IndexUnreachableException, DAOException {
        if (!DataManager.getInstance().getSearchIndex().isSolrIndexOnline()) {
            throw new IndexUnreachableException("Solr index at " + DataManager.getInstance().getSearchIndex().getSolrServerUrl() + " not reachable");
        }
    }

    @Override
    public void delete(PersistentAnnotation annotation) throws IOException {
        reindexTarget(annotation.getTargetPI(), annotation.getTargetPageOrder());
    }

    protected void reindexTarget(String pi, Integer page) {
        if (page != null) {
            try {
                IndexerTools.reIndexPage(pi, page);
            } catch (DAOException | PresentationException | IndexUnreachableException | IOException e) {
                IndexerTools.triggerReIndexRecord(pi);
            }
        } else {
            IndexerTools.triggerReIndexRecord(pi);
        }
    }

}
