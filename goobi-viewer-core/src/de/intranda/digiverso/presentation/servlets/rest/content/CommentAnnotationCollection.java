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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.model.annotation.Comment;

public class CommentAnnotationCollection extends AbstractAnnotation {

    private final String label;
    private final CommentAnnotationPage page;

    /**
     * 
     * @param comments
     * @param servletRequest
     * @param addContext If true, an @context field will be added to the JSON document
     * @throws URISyntaxException 
     */
    public CommentAnnotationCollection(String label, List<Comment> comments, HttpServletRequest servletRequest, boolean addContext) throws URISyntaxException {
        super(servletRequest);
        if (comments == null) {
            throw new IllegalArgumentException("comments may not be null");
        }

        this.label = label;
        this.addContext = addContext;

        List<CommentAnnotation> items = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            items.add(new CommentAnnotation(comment, servletRequest, false));
        }
        page = new CommentAnnotationPage(getId().toString(), items, servletRequest, false);
    }

    @JsonSerialize()
    public String getType() {
        return "AnnotationCollection";
    }

    @JsonSerialize()
    public String getLabel() {
        return label;
    }

    @JsonSerialize()
    public int getTotal() {
        return page.getItems().size();
    }

    @JsonSerialize()
    public CommentAnnotationPage getFirst() {
        return page;
    }
}
