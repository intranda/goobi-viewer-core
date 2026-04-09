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
package io.goobi.viewer.model.iiif.search.model;

import java.util.ArrayList;
import java.util.List;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.iiif.search.SearchHit;

/**
 * Holds the result set of a IIIF Search API query, containing matched annotations grouped by canvas.
 */
public class AnnotationResultList {

    private long numHits;
    private final List<IAnnotation> annotations;
    private final List<SearchHit> hits;

    /**
     * Creates a new AnnotationResultList instance.
     */
    public AnnotationResultList() {
        this.annotations = new ArrayList<>();
        this.hits = new ArrayList<>();
    }

    /**
     * add.
     *
     * @param hit search hit with associated annotations to add
     */
    public void add(SearchHit hit) {
        numHits++;
        this.hits.add(hit);
        for (IAnnotation annotation : hit.getAnnotations()) {
            if (!annotations.contains(annotation)) {
                annotations.add(annotation);
            }
        }
    }

    /**
     * add.
     *
     * @param partialResults result list to merge into this one
     */
    public void add(AnnotationResultList partialResults) {
        numHits += partialResults.numHits;
        annotations.addAll(partialResults.annotations);
        hits.addAll(partialResults.hits);
    }

    /**
     * Creates a new AnnotationResultList instance.
     *
     * @param numHits total number of hits
     * @param hits search hits to populate the list with
     */
    public AnnotationResultList(long numHits, List<SearchHit> hits) {
        this.annotations = new ArrayList<>();
        this.hits = new ArrayList<>();
        this.numHits = numHits;
        for (SearchHit searchHit : hits) {
            add(searchHit);
        }
    }

    
    public long getNumHits() {
        return numHits;
    }

    
    public void setNumHits(long numHits) {
        this.numHits = numHits;
    }

    
    public List<IAnnotation> getAnnotations() {
        return annotations;
    }

    
    public List<SearchHit> getHits() {
        return hits;
    }

}
