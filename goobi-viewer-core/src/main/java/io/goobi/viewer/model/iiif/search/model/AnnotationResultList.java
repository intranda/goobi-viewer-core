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
 * <p>
 * AnnotationResultList class.
 * </p>
 */
public class AnnotationResultList {

    private long numHits;
    private final List<IAnnotation> annotations;
    private final List<SearchHit> hits;

    /**
     * <p>
     * Constructor for AnnotationResultList.
     * </p>
     */
    public AnnotationResultList() {
        this.annotations = new ArrayList<>();
        this.hits = new ArrayList<>();
    }

    /**
     * <p>
     * add.
     * </p>
     *
     * @param hit a {@link de.intranda.api.iiif.search.SearchHit} object.
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
     * <p>
     * add.
     * </p>
     *
     * @param partialResults a {@link io.goobi.viewer.model.iiif.search.model.AnnotationResultList} object.
     */
    public void add(AnnotationResultList partialResults) {
        numHits += partialResults.numHits;
        annotations.addAll(partialResults.annotations);
        hits.addAll(partialResults.hits);
    }

    /**
     * <p>
     * Constructor for AnnotationResultList.
     * </p>
     *
     * @param numHits a long.
     * @param hits a {@link java.util.List} object.
     */
    public AnnotationResultList(long numHits, List<SearchHit> hits) {
        this.annotations = new ArrayList<>();
        this.hits = new ArrayList<>();
        this.numHits = numHits;
        for (SearchHit searchHit : hits) {
            add(searchHit);
        }
    }

    /**
     * @return the numHits
     */
    public long getNumHits() {
        return numHits;
    }

    /**
     * @param numHits the numHits to set
     */
    public void setNumHits(long numHits) {
        this.numHits = numHits;
    }

    /**
     * @return the annotations
     */
    public List<IAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return the hits
     */
    public List<SearchHit> getHits() {
        return hits;
    }

}
