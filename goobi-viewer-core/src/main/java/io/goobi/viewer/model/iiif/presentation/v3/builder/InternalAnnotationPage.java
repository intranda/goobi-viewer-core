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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import de.intranda.api.annotation.wa.collection.AnnotationPage;

/**
 * An {@link AnnotationPage} without '@context' attribute, to use for annotation pages embedded within other documents
 * 
 * @author florian
 *
 */
public class InternalAnnotationPage extends AnnotationPage {

    @Override
    public String getContext() {
        return null;
    }

    public InternalAnnotationPage(AnnotationPage orig) {
        super(orig.getId());
        this.setItems(orig.getItems());
        this.setPartOf(orig.getPartOf());
        this.setNext(orig.getNext());
        this.setPrev(orig.getPrev());
        this.setStartIndex(orig.getStartIndex());
    }
}
