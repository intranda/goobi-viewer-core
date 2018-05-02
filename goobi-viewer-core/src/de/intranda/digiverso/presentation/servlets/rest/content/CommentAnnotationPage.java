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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

public class CommentAnnotationPage extends AbstractAnnotation {

    private final String collectionIri;
    private final List<CommentAnnotation> items;
    
    @Override
    @JsonSerialize()
    public URI getId() throws URISyntaxException {
        String id = new StringBuilder(servicePath)
                .append("page1/").toString();
        return new URI(id);
    }


    /**
     * 
     * @param collectionIri
     * @param items
     * @param servletRequest
     * @param addContext If true, an @context field will be added to the JSON document
     */
    public CommentAnnotationPage(String collectionIri, List<CommentAnnotation> items, HttpServletRequest servletRequest, boolean addContext) {
        super(servletRequest);
        this.collectionIri = collectionIri;
        this.items = items;
        this.addContext = addContext;
    }

    @JsonSerialize()
    public String getType() {
        return "AnnotationPage";
    }

    @JsonSerialize()
    public String getPartOf() {
        return collectionIri;
    }

    @JsonSerialize()
    public int getStartIndex() {
        return 0;
    }

    @JsonSerialize()
    public List<CommentAnnotation> getItems() {
        return items;
    }
}
