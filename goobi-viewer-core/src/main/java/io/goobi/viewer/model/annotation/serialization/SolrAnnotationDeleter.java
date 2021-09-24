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
import java.util.Arrays;
import java.util.List;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.modules.interfaces.IndexAugmenter;

/**
 * @author florian
 *
 */
public class SolrAnnotationDeleter implements AnnotationDeleter {

    private final AnnotationLister annotationLister;
    
    public SolrAnnotationDeleter(AnnotationLister annotationLister) throws IndexUnreachableException {
        if(!DataManager.getInstance().getSearchIndex().isSolrIndexOnline()) {
            throw new IndexUnreachableException("Solr index at " + DataManager.getInstance().getSearchIndex().getSolrServerUrl() + " not reachable");
        }
        this.annotationLister = annotationLister;
    }
    
    public SolrAnnotationDeleter() throws IndexUnreachableException, DAOException {
        if(!DataManager.getInstance().getSearchIndex().isSolrIndexOnline()) {
            throw new IndexUnreachableException("Solr index at " + DataManager.getInstance().getSearchIndex().getSolrServerUrl() + " not reachable");
        }
        this.annotationLister = new SqlAnnotationLister();
    }
    
    @Override
    public void delete(PersistentAnnotation annotation) throws IOException {

        List<PersistentAnnotation> allTargetAnnotations = annotationLister.getAnnotations(0, Integer.MAX_VALUE, null, null, null, null, annotation.getTargetPI(), annotation.getTargetPageOrder(), "id", false);
        if(!allTargetAnnotations.remove(annotation)) {
            throw new IllegalArgumentException("Annotation " + annotation + " not listed for PI=" + annotation.getTargetPI() + "and page = " + annotation.getTargetPageOrder());
        } else {
            IndexAugmenter augmenter = new AnnotationIndexAugmenter(allTargetAnnotations);
            reindexTarget(annotation.getTargetPI(), annotation.getTargetPageOrder(), augmenter);
        }
    }
    
    protected void reindexTarget(String pi, Integer page, IndexAugmenter augmenter) {
        if(page != null) {
            try {
                IndexerTools.reIndexPage(pi, page, Arrays.asList(augmenter));
            } catch (DAOException | PresentationException | IndexUnreachableException | IOException e) {
                IndexerTools.triggerReIndexRecord(pi, Arrays.asList(augmenter));
            }
        } else {            
            IndexerTools.triggerReIndexRecord(pi, Arrays.asList(augmenter));
        }
    }

}
