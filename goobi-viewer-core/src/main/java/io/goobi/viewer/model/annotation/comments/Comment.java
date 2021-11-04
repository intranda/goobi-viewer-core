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
package io.goobi.viewer.model.annotation.comments;

import javax.persistence.Entity;
import javax.persistence.Table;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "annotations_comments")
public class Comment extends PersistentAnnotation implements Comparable<Comment>{

    /**
     * 
     */
    public Comment() {
        super();
    }

    /**
     * @param source
     */
    public Comment(PersistentAnnotation source) {
        super(source);
    }

    /**
     * @param source
     * @param id
     * @param targetPI
     * @param targetPage
     */
    public Comment(WebAnnotation source, Long id, String targetPI, Integer targetPage) {
        super(source, id, targetPI, targetPage);
    }
    
    public String getDisplayText() {
        return StringTools.stripJS(getContentString());
    }

    public String getText() {
        return getContentString();
    }
    
    /**
     * @param c2
     * @return
     */
    @Override
    public int compareTo(Comment o) {
        if (getDateModified() != null) {
            if (o.getDateModified() != null) {
                return getDateModified().compareTo(o.getDateModified());
            }
            return getDateModified().compareTo(o.getDateCreated());
        }
        if (getDateCreated() != null) {
            if (o.getDateModified() != null) {
                return getDateCreated().compareTo(o.getDateModified());
            }
            return getDateCreated().compareTo(o.getDateCreated());
        }

        return 1;
    }

}
