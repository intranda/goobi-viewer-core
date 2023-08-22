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
package io.goobi.viewer.model.annotation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.wa.WebAnnotation;

/**
 * An Annotation class to store annotation in a database
 *
 * @author florian
 */
@Entity
@Table(name = "annotations_crowdsourcing")
public class CrowdsourcingAnnotation extends PersistentAnnotation {

    public static final Set<String> VALID_COLUMNS_FOR_ORDER_BY = new HashSet<>(Arrays.asList("id", "dateCreated", "dateModified"));

    /**
     *
     */
    public CrowdsourcingAnnotation() {
        super();
    }

    /**
     * @param source
     */
    public CrowdsourcingAnnotation(PersistentAnnotation source) {
        super(source);
    }

    /**
     * @param source
     * @param id
     * @param targetPI
     * @param targetPage
     */
    public CrowdsourcingAnnotation(AbstractAnnotation source, Long id, String targetPI, Integer targetPage) {
        super(source, id, targetPI, targetPage);
    }

}
