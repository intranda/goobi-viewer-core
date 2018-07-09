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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.PageType;

public class CommentAnnotation extends AbstractAnnotation  implements IAnnotation {


    private final Comment comment;

    @Override
    @JsonSerialize()
    public URI getId() throws URISyntaxException {
        String idString = new StringBuilder(servicePath)
                .append('/')
                .append(comment.getPi())
                .append('/')
                .append(comment.getPage())
                .append('/')
                .append(comment.getId())
                .append('/')
                .toString();
        return new URI(idString);
    }

    /**
     * 
     * @param comment
     * @param servletRequest
     * @param addContext If true, an @context field will be added to the JSON document
     * @throws URISyntaxException 
     */
    public CommentAnnotation(Comment comment, HttpServletRequest servletRequest, boolean addContext) {
        super(servletRequest);
        if (comment == null) {
            throw new IllegalArgumentException("comment may not be null");
        }

        this.comment = comment;
        this.addContext = addContext;
    }
    
    public CommentAnnotation(Comment comment, String appplicationPath, boolean addContext) throws ViewerConfigurationException {
        super(appplicationPath);

        if (comment == null) {
            throw new IllegalArgumentException("comment may not be null");
        }
        this.comment = comment;
        this.addContext = addContext;
    }

    @Override
    @JsonSerialize()
    public URI getTarget() throws URISyntaxException {
        String uri = new StringBuilder(applicationPath).append('/')
                .append(PageType.viewImage.getName())
                .append('/')
                .append(comment.getPi())
                .append('/')
                .append(comment.getPage())
                .append('/')
                .toString();
        return new URI(uri);
    }

    @JsonSerialize(using = BodySerializer.class)
    public Comment getBody() {
        return comment;
    }

    @JsonSerialize(using = UserSerializerObfuscated.class)
    public User getCreator() {
        return comment.getOwner();
    }

    @JsonFormat(pattern = DATETIME_FORMAT)
    public Date getCreated() {
        return comment.getDateCreated();
    }

    @JsonFormat(pattern = DATETIME_FORMAT)
    @JsonInclude(Include.NON_NULL)
    public Date getModified() {
        return comment.getDateUpdated();

    }

    public static class BodySerializer extends JsonSerializer<Comment> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(Comment comment, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
            generator.writeStartObject();
            generator.writeStringField("format", "text/plain");
            generator.writeStringField("type", "TextualBody");
            generator.writeStringField("value", comment.getText());
            generator.writeEndObject();
        }
    }

    public static class UserSerializerObfuscated extends JsonSerializer<User> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(User user, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
            //            generator.writeStartObject();
            generator.writeString(user.getDisplayNameObfuscated());
        }
    }
}
